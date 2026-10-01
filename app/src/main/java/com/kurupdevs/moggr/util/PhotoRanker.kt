package com.kurupdevs.moggr.util

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * Ranks candidate capture bitmaps best-first for analysis.
 *
 * Lightweight on-device scoring per bitmap: face presence (ML Kit, fast mode),
 * face size fraction, pose (yaw/pitch closeness to 0), brightness in a sane
 * range, and sharpness (Laplacian variance). Bitmaps with no detectable face
 * rank last. The camera UI builds on top of these indices.
 *
 * Suspend: call from a background coroutine, never the main thread.
 */
object PhotoRanker {

    suspend fun rank(bitmaps: List<Bitmap>): List<Int> = withContext(Dispatchers.Default) {
        if (bitmaps.isEmpty()) return@withContext emptyList()
        if (bitmaps.size == 1) return@withContext listOf(0)
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .build()
        val detector = FaceDetection.getClient(options)
        try {
            bitmaps.mapIndexed { idx, bmp -> idx to scoreBitmap(bmp, detector) }
                .sortedByDescending { it.second }
                .map { it.first }
        } finally {
            detector.close()
        }
    }

    private suspend fun scoreBitmap(bmp: Bitmap, detector: FaceDetector): Double {
        if (bmp.isRecycled || bmp.width < 32 || bmp.height < 32) return -2.0
        val face = try {
            detector.process(InputImage.fromBitmap(bmp, 0)).await()
                .maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
        } catch (_: Exception) {
            null
        } ?: return -1.0 // No face: ranks last, above corrupt bitmaps.
        var s = 2.0 // Face found.
        val frac = (face.boundingBox.width().toFloat() * face.boundingBox.height() /
            (bmp.width.toFloat() * bmp.height)).coerceIn(0f, 1f)
        s += sizeScore(frac) * 1.5
        val posePenalty = (abs(face.headEulerAngleY) / 40f).coerceIn(0f, 1f) * 0.6f +
            (abs(face.headEulerAngleX) / 40f).coerceIn(0f, 1f) * 0.4f
        s += (1f - posePenalty).toDouble()
        s += brightnessScore(meanLuminance(bmp, face.boundingBox)) * 0.8
        s += sharpnessScore(bmp)
        return s
    }

    /** Ideal face fraction ~0.12-0.55 of the frame. */
    private fun sizeScore(frac: Float): Double = when {
        frac < 0.12f -> (frac / 0.12f).coerceIn(0f, 1f).toDouble()
        frac <= 0.55f -> 1.0
        else -> (1f - (frac - 0.55f) / 0.45f).coerceIn(0f, 1f).toDouble()
    }

    /** Ideal mean luminance ~70-190; falls off toward too-dark / blown-out. */
    private fun brightnessScore(mean: Double): Double = when {
        mean < 70 -> (mean / 70.0).coerceIn(0.0, 1.0)
        mean <= 190 -> 1.0
        else -> (1.0 - (mean - 190) / 65.0).coerceIn(0.0, 1.0)
    }

    private fun meanLuminance(bmp: Bitmap, box: Rect): Double {
        val l = box.left.coerceIn(0, bmp.width - 1)
        val t = box.top.coerceIn(0, bmp.height - 1)
        val r = box.right.coerceIn(0, bmp.width)
        val b = box.bottom.coerceIn(0, bmp.height)
        if (r <= l || b <= t) return 128.0
        var sum = 0L
        var n = 0L
        var y = t
        while (y < b) {
            var x = l
            while (x < r) {
                val px = bmp.getPixel(x, y)
                sum += (0.299 * ((px shr 16) and 0xFF) +
                    0.587 * ((px shr 8) and 0xFF) +
                    0.114 * (px and 0xFF)).toLong()
                n++
                x += 16
            }
            y += 16
        }
        return if (n == 0L) 128.0 else sum.toDouble() / n
    }

    /** Laplacian variance on a downsampled grayscale grid, mapped to 0..1. */
    private fun sharpnessScore(bmp: Bitmap): Double {
        val w = bmp.width
        val h = bmp.height
        val step = 8
        val cols = w / step
        val rows = h / step
        if (cols < 8 || rows < 8) return 0.5
        val gray = FloatArray(cols * rows)
        for (gy in 0 until rows) {
            for (gx in 0 until cols) {
                val px = bmp.getPixel(
                    (gx * step).coerceIn(0, w - 1),
                    (gy * step).coerceIn(0, h - 1)
                )
                gray[gy * cols + gx] = 0.299f * ((px shr 16) and 0xFF) +
                    0.587f * ((px shr 8) and 0xFF) + 0.114f * (px and 0xFF)
            }
        }
        var sum = 0.0
        var sumSq = 0.0
        var n = 0
        for (gy in 1 until rows - 1) {
            for (gx in 1 until cols - 1) {
                val i = gy * cols + gx
                val lap = 4f * gray[i] - gray[i - 1] - gray[i + 1] -
                    gray[i - cols] - gray[i + cols]
                sum += lap
                sumSq += lap * lap
                n++
            }
        }
        if (n == 0) return 0.5
        val mean = sum / n
        val variance = (sumSq / n - mean * mean).coerceAtLeast(0.0)
        return (variance / (variance + 300.0)).coerceIn(0.0, 1.0)
    }
}
