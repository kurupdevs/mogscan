package com.kurupdevs.moggr.util

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Pure on-device voice analysis.
 *
 * Audio is captured into memory, analyzed on the phone, and then dropped.
 * Nothing is uploaded, nothing is saved to disk, nothing leaves the device.
 */
object VoiceAnalyzer {

    const val SAMPLE_RATE = 16000
    const val RECORD_SECONDS = 10

    /**
     * Records [seconds] of 16kHz mono PCM into memory.
     * @param isCancelled polled each chunk — return true to stop early.
     * @param onLevel receives RMS level (0..1) per chunk for the live waveform.
     * @return samples, or null if the mic was unavailable / stopped / too short.
     */
    suspend fun record(
        seconds: Int = RECORD_SECONDS,
        isCancelled: () -> Boolean = { false },
        onLevel: (rms: Float) -> Unit = {}
    ): ShortArray? = withContext(Dispatchers.IO) {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) return@withContext null
        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuf, SAMPLE_RATE * 2) // 1 second of 16-bit mono
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return@withContext null
        }
        try {
            rec.startRecording()
            if (rec.recordingState != AudioRecord.RECORDSTATE_RECORDING) return@withContext null
            val target = SAMPLE_RATE * seconds
            val out = ShortArray(target)
            var collected = 0
            val chunk = ShortArray(2048)
            while (collected < target && !isCancelled()) {
                ensureActive()
                val n = rec.read(chunk, 0, chunk.size)
                if (n > 0) {
                    val take = minOf(n, target - collected)
                    chunk.copyInto(out, collected, 0, take)
                    collected += take
                    var sum = 0.0
                    for (i in 0 until take) {
                        val s = chunk[i].toDouble() / 32768.0
                        sum += s * s
                    }
                    onLevel(sqrt(sum / take).toFloat())
                } else if (n < 0) {
                    break
                }
            }
            if (isCancelled() || collected < SAMPLE_RATE) null else out.copyOf(collected)
        } finally {
            try {
                rec.stop()
            } catch (_: Exception) {
                // already stopped — fine
            }
            rec.release()
        }
    }

    /** Analyzes a recorded clip. Pure math, no network, no storage. */
    fun analyze(samples: ShortArray): VoiceResult {
        val frameLen = 1024
        val hop = 512
        if (samples.size < frameLen * 2) return VoiceResult(speechDetected = false)

        val frameCount = (samples.size - frameLen) / hop + 1
        val energies = DoubleArray(frameCount)
        for (f in 0 until frameCount) {
            var sum = 0.0
            val base = f * hop
            for (i in 0 until frameLen) {
                val s = samples[base + i].toDouble() / 32768.0
                sum += s * s
            }
            energies[f] = sum / frameLen
        }
        val maxE = energies.maxOrNull() ?: 0.0
        if (maxE < 1e-6) return VoiceResult(speechDetected = false)

        val gate = maxE * 0.12
        val pitches = mutableListOf<Double>()
        var voicedFrames = 0
        var runStart = -1
        var segments = 0
        for (f in 0 until frameCount) {
            val voiced = energies[f] > gate
            if (voiced) {
                voicedFrames++
                pitchOfFrame(samples, f * hop, frameLen)?.let { pitches.add(it.first) }
                if (runStart < 0) runStart = f
            } else if (runStart >= 0) {
                if (f - runStart >= 3) segments++
                runStart = -1
            }
        }
        if (runStart >= 0 && frameCount - runStart >= 3) segments++

        if (pitches.size < 5) return VoiceResult(speechDetected = false)

        val sorted = pitches.sorted()
        val median = if (sorted.size % 2 == 1) {
            sorted[sorted.size / 2]
        } else {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
        }
        val mean = pitches.average()
        val variance = pitches.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)

        val durationSec = samples.size.toDouble() / SAMPLE_RATE.toDouble()
        val segmentsPerSec = segments.toDouble() / durationSec
        val pauseRatio = 1.0 - voicedFrames.toDouble() / frameCount.toDouble()

        // ---- Presence score: confidence / clarity / steadiness only. ----
        val steadinessScore = (1.0 - (stdDev / 40.0).coerceIn(0.0, 1.0)) * 100.0
        val clarityScore =
            if (voicedFrames > 0) pitches.size.toDouble() / voicedFrames.toDouble() * 100.0 else 0.0
        val paceScore = when {
            segmentsPerSec < 0.8 -> segmentsPerSec / 0.8 * 60.0
            segmentsPerSec <= 4.0 -> 100.0
            else -> (100.0 - (segmentsPerSec - 4.0) * 25.0).coerceAtLeast(20.0)
        }
        val presence =
            (0.4 * steadinessScore + 0.35 * clarityScore + 0.25 * paceScore).roundToInt()
                .coerceIn(0, 100)

        val presenceLine = when {
            presence >= 80 -> "Your voice sounds clear and steady — easy to listen to."
            presence >= 60 -> "Your voice sounds mostly clear, with a natural rhythm."
            presence >= 40 -> "Decent base — the drills below tighten up the weak spots."
            else -> "Rough take. A quieter room plus the drills below will help."
        }

        val pitchNote = when {
            median in 85.0..255.0 -> "Right in the typical adult speaking range (85–255 Hz)."
            median < 85.0 -> "A little below the typical range — speak up from your chest, not your throat."
            else -> "A little above the typical range — the breathing drill helps relax a tight throat."
        }

        val paceNote = when {
            segmentsPerSec < 1.2 -> "Slow and deliberate — good for presence. Watch out for long dead pauses."
            segmentsPerSec <= 4.0 -> "Natural conversational pace — this is the sweet spot."
            else -> "Fast. The pacing drill below trains you to slow down without sounding flat."
        }

        val steadinessNote = when {
            stdDev < 12.0 -> "Very steady — your pitch barely wavers while you talk."
            stdDev < 25.0 -> "Fairly steady, with a little natural movement."
            else -> "Wavers a bit mid-sentence — the humming warm-up smooths this out."
        }

        val weakest = listOf(
            "pace" to paceScore,
            "steadiness" to steadinessScore,
            "clarity" to clarityScore
        ).minByOrNull { it.second }?.first
        val tip = when (weakest) {
            "pace" -> "Try the pacing drill — one slow beat per phrase, no rushing the pauses."
            "steadiness" -> "Do the humming warm-up daily — it trains your pitch to sit still."
            else -> "Project from your chest and open your mouth a little more when you speak."
        }

        return VoiceResult(
            speechDetected = true,
            medianPitchHz = median.toFloat(),
            pitchStdDevHz = stdDev.toFloat(),
            segmentsPerSec = segmentsPerSec.toFloat(),
            pauseRatio = pauseRatio.toFloat().coerceIn(0f, 1f),
            presenceScore = presence,
            presenceLine = presenceLine,
            pitchNote = pitchNote,
            paceNote = paceNote,
            steadinessNote = steadinessNote,
            tip = tip
        )
    }

    /**
     * Fundamental frequency of one frame via normalized autocorrelation
     * with parabolic interpolation. Returns (pitchHz, confidence) or null.
     */
    private fun pitchOfFrame(samples: ShortArray, start: Int, frameLen: Int): Pair<Double, Double>? {
        var mean = 0.0
        for (i in 0 until frameLen) mean += samples[start + i].toDouble()
        mean /= frameLen.toDouble()

        val minLag = SAMPLE_RATE / 350
        val maxLag = SAMPLE_RATE / 75
        val vals = DoubleArray(maxLag + 2)
        var bestLag = -1
        var bestVal = 0.0
        for (lag in minLag..maxLag) {
            var sum = 0.0
            var e1 = 0.0
            var e2 = 0.0
            val n = frameLen - lag
            for (i in 0 until n) {
                val a = samples[start + i].toDouble() - mean
                val b = samples[start + i + lag].toDouble() - mean
                sum += a * b
                e1 += a * a
                e2 += b * b
            }
            val v = if (e1 > 1e-9 && e2 > 1e-9) sum / sqrt(e1 * e2) else 0.0
            vals[lag] = v
            if (v > bestVal) {
                bestVal = v
                bestLag = lag
            }
        }
        if (bestLag <= minLag || bestLag >= maxLag || bestVal < 0.45) return null
        val y0 = vals[bestLag - 1]
        val y2 = vals[bestLag + 1]
        val denom = y0 - 2.0 * bestVal + y2
        val shift = if (abs(denom) > 1e-9) 0.5 * (y0 - y2) / denom else 0.0
        val pitch = SAMPLE_RATE.toDouble() / (bestLag.toDouble() + shift.coerceIn(-1.0, 1.0))
        return pitch to bestVal
    }
}

/** Result of one voice check. Framed as confidence/clarity — never attractiveness. */
data class VoiceResult(
    val speechDetected: Boolean,
    val medianPitchHz: Float = 0f,
    val pitchStdDevHz: Float = 0f,
    val segmentsPerSec: Float = 0f,
    val pauseRatio: Float = 0f,
    val presenceScore: Int = 0,
    val presenceLine: String = "",
    val pitchNote: String = "",
    val paceNote: String = "",
    val steadinessNote: String = "",
    val tip: String = ""
)
