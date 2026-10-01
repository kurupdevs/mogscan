package com.kurupdevs.moggr.analysis

import android.graphics.Bitmap
import android.graphics.PointF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.hypot

/**
 * On-device PSL analysis. Runs ML Kit face detection (bundled model, no network,
 * no API key) on the three captured photos and derives facial-structure metrics
 * from landmarks + contours, mapped onto the real 1.0-8.0 PSL scale used by the
 * looksmaxxing community (LTN / MTN / HTN / Chadlite / Chad tiers).
 */
object FaceAnalyzer {

    suspend fun analyze(bitmaps: List<Bitmap>): PslReport = withContext(Dispatchers.Default) {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .build()
        val detector = FaceDetection.getClient(options)
        try {
            val faces = bitmaps.mapNotNull { bmp ->
                try {
                    detector.process(InputImage.fromBitmap(bmp, 0)).await()
                        .maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                } catch (_: Exception) {
                    null
                }
            }
            if (faces.isEmpty()) {
                throw IllegalStateException(
                    "No face found in any photo. Retake with better lighting and a plain background."
                )
            }
            // Primary face = most frontal detection (usually the front-angle photo).
            val primary = faces.minByOrNull { abs(it.headEulerAngleY) }!!
            buildReport(primary, faces)
        } finally {
            detector.close()
        }
    }

    // ---------- geometry helpers ----------

    private fun dist(a: PointF, b: PointF): Float = hypot(a.x - b.x, a.y - b.y)

    private fun Face.landmark(type: Int): PointF? = getLandmark(type)?.position

    private fun Face.contourPoints(type: Int): List<PointF> =
        getContour(type)?.points ?: emptyList()

    private fun widthOf(pts: List<PointF>): Float =
        if (pts.size < 2) 0f else pts.maxOf { it.x } - pts.minOf { it.x }

    private fun heightOf(pts: List<PointF>): Float =
        if (pts.size < 2) 0f else pts.maxOf { it.y } - pts.minOf { it.y }

    /** 8.0 at zero deviation, 4.5 at one tolerance, 1.0 at two tolerances. */
    private fun scoreFromDeviation(dev: Float, tolerance: Float): Double =
        (8.0 - (dev / tolerance).coerceIn(0f, 2f) * 3.5).coerceIn(1.0, 8.0)

    // ---------- feature metrics ----------

    private fun symmetry(face: Face): FeatureScore {
        val h = face.boundingBox.height().toFloat().coerceAtLeast(1f)
        var dev = 0f
        var n = 0
        fun pair(a: PointF?, b: PointF?) {
            if (a != null && b != null) {
                dev += abs(a.y - b.y) / h
                n++
            }
        }
        pair(face.landmark(FaceLandmark.LEFT_EYE), face.landmark(FaceLandmark.RIGHT_EYE))
        pair(face.landmark(FaceLandmark.MOUTH_LEFT), face.landmark(FaceLandmark.MOUTH_RIGHT))
        pair(face.landmark(FaceLandmark.LEFT_CHEEK), face.landmark(FaceLandmark.RIGHT_CHEEK))
        val lw = widthOf(face.contourPoints(FaceContour.LEFT_EYE))
        val rw = widthOf(face.contourPoints(FaceContour.RIGHT_EYE))
        if (lw > 0 && rw > 0) {
            dev += abs(lw - rw) / h
            n++
        }
        if (n == 0) return FeatureScore("Symmetry", 4.0, "Couldn't measure reliably from this photo.")
        val score = scoreFromDeviation(dev / n, 0.03f)
        val note = when {
            score >= 7 -> "Left and right sides line up very evenly."
            score >= 5 -> "Mostly even, with small left-right differences."
            else -> "Noticeable left-right differences — very common and very improvable."
        }
        return FeatureScore("Symmetry", score, note)
    }

    private fun jawline(face: Face, cheekW: Float): FeatureScore {
        val oval = face.contourPoints(FaceContour.FACE)
        if (oval.isEmpty() || cheekW <= 0) {
            return FeatureScore("Jawline", 4.0, "Couldn't measure reliably from this photo.")
        }
        val topY = oval.minOf { it.y }
        val chinY = oval.maxOf { it.y }
        val faceH = (chinY - topY).coerceAtLeast(1f)
        val jawY = chinY - faceH * 0.28f
        val jawPts = oval.filter { it.y >= jawY - faceH * 0.06f && it.y <= jawY + faceH * 0.06f }
        val jawW = if (jawPts.size >= 2) widthOf(jawPts) else cheekW * 0.8f
        val ratio = jawW / cheekW
        val score = scoreFromDeviation(abs(ratio - 0.90f), 0.12f)
        val note = when {
            score >= 7 -> "Wide, well-defined jaw relative to cheekbones."
            score >= 5 -> "Decent jaw width — definition is the main lever."
            else -> "Narrower jaw line; width and definition can both be built up."
        }
        return FeatureScore("Jawline", score, note)
    }

    private fun cheekbones(face: Face, cheekW: Float, jawW: Float): FeatureScore {
        if (cheekW <= 0 || jawW <= 0) {
            return FeatureScore("Cheekbones", 4.0, "Couldn't measure reliably from this photo.")
        }
        val ratio = cheekW / jawW
        val score = scoreFromDeviation(abs(ratio - 1.12f), 0.12f)
        val note = when {
            score >= 7 -> "Cheekbones sit visibly wider than the jaw — strong midface structure."
            score >= 5 -> "Average cheekbone projection."
            else -> "Flatter midface; leanness brings the most out here."
        }
        return FeatureScore("Cheekbones", score, note)
    }

    private fun facialThirds(face: Face): FeatureScore {
        val oval = face.contourPoints(FaceContour.FACE)
        val browPts = face.contourPoints(FaceContour.LEFT_EYEBROW_TOP) +
            face.contourPoints(FaceContour.RIGHT_EYEBROW_TOP)
        val noseBase = face.landmark(FaceLandmark.NOSE_BASE)
        if (oval.isEmpty() || browPts.isEmpty() || noseBase == null) {
            return FeatureScore("Facial thirds", 4.0, "Couldn't measure reliably from this photo.")
        }
        val topY = oval.minOf { it.y }
        val chinY = oval.maxOf { it.y }
        val faceH = (chinY - topY).coerceAtLeast(1f)
        val browY = browPts.map { it.y }.average().toFloat()
        val upper = browY - topY
        val mid = noseBase.y - browY
        val lower = chinY - noseBase.y
        if (upper <= 0 || mid <= 0 || lower <= 0) {
            return FeatureScore("Facial thirds", 4.0, "Couldn't measure reliably from this photo.")
        }
        val avg = (upper + mid + lower) / 3f
        val dev = (abs(upper - avg) + abs(mid - avg) + abs(lower - avg)) / 3f / faceH
        val score = scoreFromDeviation(dev, 0.05f)
        val longest = listOf("forehead" to upper, "midface" to mid, "lower third" to lower).maxBy { it.second }.first
        val note = when {
            score >= 7 -> "Forehead, midface and lower third are nicely balanced."
            score >= 5 -> "Fairly balanced thirds."
            else -> "The $longest runs long — hairstyle and framing can rebalance it visually."
        }
        return FeatureScore("Facial thirds", score, note)
    }

    private fun eyes(face: Face): FeatureScore {
        val leC = face.contourPoints(FaceContour.LEFT_EYE)
        val reC = face.contourPoints(FaceContour.RIGHT_EYE)
        val le = face.landmark(FaceLandmark.LEFT_EYE)
        val re = face.landmark(FaceLandmark.RIGHT_EYE)
        if (leC.size < 4 || reC.size < 4 || le == null || re == null) {
            return FeatureScore("Eyes", 4.0, "Couldn't measure reliably from this photo.")
        }
        val eyeW = (widthOf(leC) + widthOf(reC)) / 2f
        if (eyeW <= 0) return FeatureScore("Eyes", 4.0, "Couldn't measure reliably from this photo.")
        // Gap between the eyes, in eye-widths. Ideal ~= 1.0.
        val gapRatio = (dist(le, re) - eyeW) / eyeW
        val spacingScore = scoreFromDeviation(abs(gapRatio - 1.0f), 0.35f)
        // Canthal tilt: lateral corner higher than medial corner = positive tilt (good).
        fun tilt(pts: List<PointF>, isLeft: Boolean): Float {
            val medial = if (isLeft) pts.maxBy { it.x } else pts.minBy { it.x }
            val lateral = if (isLeft) pts.minBy { it.x } else pts.maxBy { it.x }
            return (medial.y - lateral.y) / eyeW
        }
        val avgTilt = (tilt(leC, true) + tilt(reC, false)) / 2f
        val tiltScore = scoreFromDeviation(abs(avgTilt - 0.06f), 0.10f)
        val score = (spacingScore * 0.6 + tiltScore * 0.4)
        val note = when {
            avgTilt >= 0.02f && spacingScore >= 6 -> "Good spacing with a positive canthal tilt."
            avgTilt < -0.02f -> "Slight negative canthal tilt — the main thing dragging the eye area down."
            else -> "Average eye area; spacing and tilt are both workable."
        }
        return FeatureScore("Eyes", score, note)
    }

    private fun nose(face: Face): FeatureScore {
        val noseW = widthOf(face.contourPoints(FaceContour.NOSE_BOTTOM))
        val lipW = widthOf(
            face.contourPoints(FaceContour.UPPER_LIP_BOTTOM) +
                face.contourPoints(FaceContour.LOWER_LIP_TOP)
        )
        if (noseW <= 0 || lipW <= 0) {
            return FeatureScore("Nose", 4.0, "Couldn't measure reliably from this photo.")
        }
        // Ideal: mouth ~1.5x nose width.
        val ratio = lipW / noseW
        val score = scoreFromDeviation(abs(ratio - 1.5f), 0.3f)
        val note = when {
            score >= 7 -> "Nose width sits in good proportion to the mouth."
            score >= 5 -> "Nose proportions are average."
            else -> "Nose reads wide relative to the mouth — framing and angles matter most here."
        }
        return FeatureScore("Nose", score, note)
    }

    private fun lips(face: Face): FeatureScore {
        val pts = face.contourPoints(FaceContour.UPPER_LIP_TOP) +
            face.contourPoints(FaceContour.UPPER_LIP_BOTTOM) +
            face.contourPoints(FaceContour.LOWER_LIP_TOP) +
            face.contourPoints(FaceContour.LOWER_LIP_BOTTOM)
        if (pts.size < 4) return FeatureScore("Lips", 4.0, "Couldn't measure reliably from this photo.")
        val w = widthOf(pts)
        val h = heightOf(pts)
        if (w <= 0) return FeatureScore("Lips", 4.0, "Couldn't measure reliably from this photo.")
        val fullness = h / w
        val score = scoreFromDeviation(abs(fullness - 0.30f), 0.10f)
        val note = when {
            score >= 7 -> "Good lip fullness and definition."
            score >= 5 -> "Average lip volume."
            else -> "Thinner lips — hydration and lip care help them read fuller."
        }
        return FeatureScore("Lips", score, note)
    }

    private fun chin(face: Face, jawW: Float): FeatureScore {
        val oval = face.contourPoints(FaceContour.FACE)
        if (oval.isEmpty() || jawW <= 0) {
            return FeatureScore("Chin", 4.0, "Couldn't measure reliably from this photo.")
        }
        val chinY = oval.maxOf { it.y }
        val topY = oval.minOf { it.y }
        val faceH = (chinY - topY).coerceAtLeast(1f)
        val chinPts = oval.filter { it.y >= chinY - faceH * 0.09f }
        val chinW = if (chinPts.size >= 2) widthOf(chinPts) else jawW * 0.5f
        val ratio = chinW / jawW
        val score = scoreFromDeviation(abs(ratio - 0.55f), 0.12f)
        val note = when {
            score >= 7 -> "Chin width balances well with the jaw."
            score >= 5 -> "Average chin shape."
            else -> "Chin reads narrow or weak relative to the jaw."
        }
        return FeatureScore("Chin", score, note)
    }

    /** FWHR = bizygomatic width / midface height. Community ideal ~1.8-1.95. */
    private fun fwhr(face: Face, cheekW: Float): FeatureScore {
        val browPts = face.contourPoints(FaceContour.LEFT_EYEBROW_TOP) +
            face.contourPoints(FaceContour.RIGHT_EYEBROW_TOP)
        val noseBase = face.landmark(FaceLandmark.NOSE_BASE)
        if (cheekW <= 0 || browPts.isEmpty() || noseBase == null) {
            return FeatureScore("FWHR", 4.0, "Couldn't measure reliably from this photo.")
        }
        val browY = browPts.map { it.y }.average().toFloat()
        val midfaceH = (noseBase.y - browY).coerceAtLeast(1f)
        val ratio = cheekW / midfaceH
        val score = scoreFromDeviation(abs(ratio - 1.87f), 0.22f)
        val note = when {
            score >= 6.5 -> "FWHR near the community ideal — wide, masculine midface."
            score >= 4.5 -> "FWHR in the average range."
            else -> "FWHR off the ideal band — leanness and angles influence this most."
        }
        return FeatureScore("FWHR", score, note)
    }

    /** ESR = interpupillary distance / bizygomatic width. Community ideal 0.45-0.47. */
    private fun eyeSpacing(face: Face, cheekW: Float): FeatureScore {
        val le = face.landmark(FaceLandmark.LEFT_EYE)
        val re = face.landmark(FaceLandmark.RIGHT_EYE)
        if (le == null || re == null || cheekW <= 0) {
            return FeatureScore("Eye spacing", 4.0, "Couldn't measure reliably from this photo.")
        }
        val ratio = dist(le, re) / cheekW
        val score = scoreFromDeviation(abs(ratio - 0.46f), 0.05f)
        val note = when {
            score >= 6.5 -> "Eye spacing sits right in the ideal ESR band."
            score >= 4.5 -> "Eye spacing is average."
            else -> "Eyes read close-set or wide-set versus the ideal ratio."
        }
        return FeatureScore("Eye spacing", score, note)
    }

    /**
     * Side profile estimate from the most-turned profile photo: how far the chin
     * sits behind the nose tip, relative to face height. Small offset = straight
     * profile / good forward growth. Crude but directionally real.
     */
    private fun sideProfile(faces: List<Face>, primary: Face): FeatureScore {
        val profile = faces
            .filter { it !== primary && abs(it.headEulerAngleY) > 15f }
            .maxByOrNull { abs(it.headEulerAngleY) }
            ?: return FeatureScore("Side profile", 4.0, "Profile photo wasn't clear enough to assess.")
        val contour = profile.contourPoints(FaceContour.FACE)
        if (contour.size < 12) {
            return FeatureScore("Side profile", 4.0, "Profile photo wasn't clear enough to assess.")
        }
        val minY = contour.minOf { it.y }
        val maxY = contour.maxOf { it.y }
        val h = (maxY - minY).coerceAtLeast(1f)
        val midY = (minY + maxY) / 2f
        val centerX = contour.map { it.x }.average()
        // Nose tip: farthest point from face center in the middle vertical band.
        val band = contour.filter { abs(it.y - midY) < h * 0.22f }
        if (band.isEmpty()) {
            return FeatureScore("Side profile", 4.0, "Profile photo wasn't clear enough to assess.")
        }
        val noseTip = band.maxBy { abs(it.x - centerX) }
        val chin = contour.maxBy { it.y }
        val dev = abs(noseTip.x - chin.x) / h
        val score = scoreFromDeviation(dev, 0.055f)
        val note = when {
            score >= 6.5 -> "Straight profile — chin sits well under the nose, good forward growth."
            score >= 4.5 -> "Profile is average; slight recession or projection."
            else -> "Chin reads recessed behind the nose — posture and photo angle help most."
        }
        return FeatureScore("Side profile", score, note)
    }

    // ---------- advice database (softmaxxing only) ----------

    private val ADVICE: Map<String, List<Pair<String, String>>> = mapOf(
        "Jawline" to listOf(
            "Drop body fat through diet and training — leanness defines the jaw more than anything" to "hard",
            "Chin tucks daily to fix forward head posture that hides the jawline" to "easy",
            "A short boxed beard or stubble along the jaw adds definition overnight" to "easy"
        ),
        "Cheekbones" to listOf(
            "Lower body fat — hollow cheeks show through leanness, not products" to "hard",
            "Cut salty food and drink more water — less facial bloat reads sharper" to "easy"
        ),
        "Symmetry" to listOf(
            "Sleep on your back so one side of your face isn't pressed all night" to "easy",
            "Chew evenly on both sides instead of favoring one" to "easy",
            "Fix forward head posture — wall chin tucks, 2 minutes daily" to "medium",
            "Stop resting your chin or cheek on your hand" to "easy"
        ),
        "Facial thirds" to listOf(
            "Get a fringe or textured top if your forehead runs long" to "easy",
            "Avoid tall hairstyles if your midface runs long — keep volume on the sides" to "easy"
        ),
        "Eyes" to listOf(
            "Sleep 7-9 hours — under-eye bags drag the whole eye area down" to "medium",
            "Groom and shape your eyebrows; they frame the eyes" to "easy",
            "Cold spoon or cold compress in the morning for puffiness" to "easy"
        ),
        "Nose" to listOf(
            "Face-framing hairstyle to balance nose prominence" to "easy",
            "Shoot photos slightly above eye level with soft front light" to "easy"
        ),
        "Lips" to listOf(
            "Lip balm daily plus water — hydrated lips read fuller" to "easy",
            "Gentle lip scrub once a week to remove dead skin" to "easy"
        ),
        "Chin" to listOf(
            "Lower body fat to reveal chin definition" to "hard",
            "Fix forward head posture — chin tucks make the chin read stronger" to "easy"
        ),
        "FWHR" to listOf(
            "Drop body fat — FWHR reads best on a lean midface" to "hard",
            "Hairstyle with height on top can balance a wide or narrow read" to "easy"
        ),
        "Eye spacing" to listOf(
            "Nothing changes spacing — but groomed, thicker brows make any spacing look intentional" to "easy",
            "Avoid middle parts if eyes read close-set; side parts add balance" to "easy"
        ),
        "Side profile" to listOf(
            "Chin tucks against a wall, 2 minutes daily — posture changes the profile more than anything" to "easy",
            "Side-profile photos: chin slightly down, jaw pushed a touch forward" to "easy"
        )
    )

    // ---------- report assembly ----------

    private fun buildReport(face: Face, faces: List<Face>): PslReport {
        val oval = face.contourPoints(FaceContour.FACE)
        val lc = face.landmark(FaceLandmark.LEFT_CHEEK)
        val rc = face.landmark(FaceLandmark.RIGHT_CHEEK)
        val cheekW = if (lc != null && rc != null) dist(lc, rc) else 0f
        val jawW = if (oval.isNotEmpty() && cheekW > 0) {
            val topY = oval.minOf { it.y }
            val chinY = oval.maxOf { it.y }
            val faceH = (chinY - topY).coerceAtLeast(1f)
            val jawY = chinY - faceH * 0.28f
            val jawPts = oval.filter { it.y >= jawY - faceH * 0.06f && it.y <= jawY + faceH * 0.06f }
            if (jawPts.size >= 2) widthOf(jawPts) else cheekW * 0.8f
        } else 0f

        val scored = listOf(
            symmetry(face) to 1.2,
            jawline(face, cheekW) to 1.3,
            cheekbones(face, cheekW, jawW) to 1.2,
            facialThirds(face) to 1.0,
            eyes(face) to 1.2,
            eyeSpacing(face, cheekW) to 1.0,
            fwhr(face, cheekW) to 1.0,
            nose(face) to 0.8,
            lips(face) to 0.7,
            chin(face, jawW) to 1.0,
            sideProfile(faces, face) to 1.1
        )
        val features = scored.map { it.first }
        val totalWeight = scored.sumOf { it.second }
        val overall = (scored.sumOf { it.first.score * it.second } / totalWeight)
            .coerceIn(1.0, 8.0)
        val overall100 = ((overall - 1.0) / 7.0 * 100).toInt().coerceIn(0, 100)

        val ranked = features.sortedByDescending { it.score }
        val strengths = ranked.filter { it.score >= 6.0 }.take(3)
            .ifEmpty { ranked.take(2) }
            .map { "${it.name} (${"%.1f".format(it.score)}) — ${it.note}" }

        val weakest = ranked.takeLast(3)
        val improvements = weakest.flatMap { f ->
            (ADVICE[f.name] ?: emptyList()).take(2).map { (method, effort) ->
                Improvement(f.name, method, effort)
            }
        }.take(6)

        val best = ranked.first()
        val worst = ranked.last()
        val summary = "Your ${best.name.lowercase()} is your strongest asset right now. " +
            "The biggest wins will come from ${worst.name.lowercase()} — " +
            "${improvements.firstOrNull()?.method?.lowercase() ?: "consistent softmaxxing"}. " +
            "Overall you sit at ${"%.1f".format(overall)} PSL: ${pslLabel(overall).lowercase()}."

        return PslReport(
            overallPsl = overall,
            overall100 = overall100,
            features = features,
            strengths = strengths,
            improvements = improvements,
            summary = summary,
            anglesRead = faces.size
        )
    }

    /** Community PSL tiers (1.0-8.0 scale). Brutally honest, no sugarcoating. */
    private fun pslLabel(psl: Double): String = when {
        psl >= 7.5 -> "Gigachad range — near-mythical"
        psl >= 6.9 -> "Chad"
        psl >= 6.0 -> "Chadlite"
        psl >= 5.1 -> "HTN — High Tier Normie"
        psl >= 2.8 -> "MTN — Mid Tier Normie"
        psl >= 1.4 -> "LTN — Low Tier Normie"
        else -> "Sub-5 — maximum ascension potential"
    }
}
