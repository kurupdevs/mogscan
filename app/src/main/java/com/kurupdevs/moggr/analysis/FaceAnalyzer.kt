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
import kotlin.math.acos
import kotlin.math.exp
import kotlin.math.hypot

/**
 * On-device PSL analysis. Runs ML Kit face detection (bundled model, no network,
 * no API key) on the captured photos and derives facial-structure metrics from
 * landmarks + contours, mapped onto the real 1.0-8.0 PSL scale used by the
 * looksmaxxing community (LTN / MTN / HTN / Chadlite / Chad / Gigachad tiers).
 *
 * Rating methodology mirrors how experienced community raters score a face:
 * - Four pillars: Harmony 40% / Features 25% / Dimorphism 20% / Angularity 15%.
 *   Harmony (proportions) dominates; no single ratio decides the score.
 * - Every ratio is scored as distance-from-community-ideal with forgiving bands,
 *   never more-is-better. Tier-1 extremes are NOT the ideal.
 * - Failo/halo gating: major deviations (score < 4) cap the tier. Chadlite+
 *   requires zero major deviations plus standout features.
 * - Side profile is a 15% bonus read on top of the frontal score, never the base.
 * - Photo-quality checks: angled/tilted captures get flagged before scoring,
 *   because raters' #1 rule is "rate the undistorted neutral photo".
 *
 * Honesty: all ideals are looksmaxxing-community conventions (PSL wikis,
 * looksmax.org rating guides, rater consensus) — NOT validated attractiveness
 * science. The report labels them as community-benchmark estimates.
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
            val pairs = bitmaps.mapNotNull { bmp ->
                try {
                    val face = detector.process(InputImage.fromBitmap(bmp, 0)).await()
                        .maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                    if (face != null) bmp to face else null
                } catch (_: Exception) {
                    null
                }
            }
            if (pairs.isEmpty()) {
                throw IllegalStateException(
                    "No face found in any photo. Retake with better lighting and a plain background."
                )
            }
            val faces = pairs.map { it.second }
            // Primary face = most frontal detection (usually the front-angle photo).
            val primaryPair = pairs.minByOrNull { abs(it.second.headEulerAngleY) }!!
            buildReport(primaryPair.second, faces, primaryPair.first)
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

    private fun weighted(parts: List<Pair<FeatureScore, Double>>): Double {
        val w = parts.sumOf { it.second }
        return if (w <= 0) 4.0 else parts.sumOf { it.first.score * it.second } / w
    }

    /** Standard-normal CDF (Abramowitz & Stegun). PSL 4 = 50th percentile, 1 SD per point. */
    private fun normalCdf(x: Double): Double {
        val t = 1.0 / (1.0 + 0.2316419 * abs(x))
        val d = 0.3989423 * exp(-x * x / 2.0)
        var p = d * t * (0.3193815 + t * (-0.3565638 + t * (1.781478 + t * (-1.821256 + t * 1.330274))))
        p = 1.0 - p
        return if (x > 0) p else 1.0 - p
    }

    private fun unreliable(name: String) =
        FeatureScore(name, 4.0, "Couldn't measure reliably from this photo.")

    // ================= HARMONY (40%) =================

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
        if (n == 0) return unreliable("Symmetry")
        // Mild penalty: phone cameras exaggerate left-right differences, and raters
        // barely dock for asymmetry unless it's visible at a glance.
        val score = scoreFromDeviation(dev / n, 0.05f)
        val note = when {
            score >= 7 -> "Left and right sides line up very evenly."
            score >= 5 -> "Mostly even — small left-right differences are normal."
            else -> "Some left-right difference; usually reads softer in person than on camera."
        }
        return FeatureScore("Symmetry", score, note)
    }

    private fun facialThirds(face: Face): FeatureScore {
        val oval = face.contourPoints(FaceContour.FACE)
        val browPts = face.contourPoints(FaceContour.LEFT_EYEBROW_TOP) +
            face.contourPoints(FaceContour.RIGHT_EYEBROW_TOP)
        val noseBase = face.landmark(FaceLandmark.NOSE_BASE)
        if (oval.isEmpty() || browPts.isEmpty() || noseBase == null) {
            return unreliable("Facial thirds")
        }
        val topY = oval.minOf { it.y }
        val chinY = oval.maxOf { it.y }
        val faceH = (chinY - topY).coerceAtLeast(1f)
        val browY = browPts.map { it.y }.average().toFloat()
        val upper = browY - topY
        val mid = noseBase.y - browY
        val lower = chinY - noseBase.y
        if (upper <= 0 || mid <= 0 || lower <= 0) {
            return unreliable("Facial thirds")
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

    /** Facial fifths: eye width, inter-eye gap and nose width should each be ~1/5 of face width. */
    private fun facialFifths(face: Face): FeatureScore {
        val le = face.landmark(FaceLandmark.LEFT_EYE)
        val re = face.landmark(FaceLandmark.RIGHT_EYE)
        val oval = face.contourPoints(FaceContour.FACE)
        val leC = face.contourPoints(FaceContour.LEFT_EYE)
        val reC = face.contourPoints(FaceContour.RIGHT_EYE)
        val nosePts = face.contourPoints(FaceContour.NOSE_BOTTOM)
        if (le == null || re == null || oval.isEmpty() || leC.size < 4 || reC.size < 4 || nosePts.size < 2) {
            return unreliable("Facial fifths")
        }
        val h = face.boundingBox.height().toFloat().coerceAtLeast(1f)
        val eyeY = (le.y + re.y) / 2f
        val bandW = widthOf(oval.filter { abs(it.y - eyeY) < h * 0.06f })
        if (bandW <= 0) return unreliable("Facial fifths")
        val eyeW = (widthOf(leC) + widthOf(reC)) / 2f
        val gap = (dist(le, re) - eyeW).coerceAtLeast(0f)
        val noseW = widthOf(nosePts)
        val fifth = bandW / 5f
        val dev = (abs(eyeW - fifth) + abs(gap - fifth) + abs(noseW - fifth)) / 3f / fifth
        val score = scoreFromDeviation(dev, 0.28f)
        val note = when {
            score >= 7 -> "Eye width, spacing and nose width divide the face into clean fifths."
            score >= 5 -> "Fifths are roughly balanced."
            else -> "Fifths run uneven — a side part and face-framing hair rebalance it visually."
        }
        return FeatureScore("Facial fifths", score, note)
    }

    /** Midface ratio = IPD / (brow to upper lip). Community ideal ~1.0 (compact = youthful). */
    private fun midfaceRatio(face: Face): FeatureScore {
        val le = face.landmark(FaceLandmark.LEFT_EYE)
        val re = face.landmark(FaceLandmark.RIGHT_EYE)
        val browPts = face.contourPoints(FaceContour.LEFT_EYEBROW_TOP) +
            face.contourPoints(FaceContour.RIGHT_EYEBROW_TOP)
        val lipTop = face.contourPoints(FaceContour.UPPER_LIP_TOP)
        if (le == null || re == null || browPts.isEmpty() || lipTop.isEmpty()) {
            return unreliable("Midface ratio")
        }
        val browY = browPts.map { it.y }.average().toFloat()
        val lipY = lipTop.minOf { it.y }
        val midH = (lipY - browY).coerceAtLeast(1f)
        val ratio = dist(le, re) / midH
        val score = scoreFromDeviation(abs(ratio - 1.0f), 0.18f)
        val note = when {
            score >= 7 -> "Compact midface — reads youthful and harmonious."
            score >= 5 -> "Midface length is average."
            else -> if (ratio > 1.0f) "Midface runs long — keep hair volume on the sides, not on top."
            else "Midface reads short — some height on top balances it."
        }
        return FeatureScore("Midface ratio", score, note)
    }

    /** ESR = interpupillary distance / bizygomatic width. Community ideal 0.45-0.47. */
    private fun eyeSpacing(face: Face, cheekW: Float): FeatureScore {
        val le = face.landmark(FaceLandmark.LEFT_EYE)
        val re = face.landmark(FaceLandmark.RIGHT_EYE)
        if (le == null || re == null || cheekW <= 0) {
            return unreliable("Eye spacing")
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

    /** FWHR = bizygomatic width / midface height. Community sweet spot ~1.8-1.95; both extremes penalized. */
    private fun fwhr(face: Face, cheekW: Float): FeatureScore {
        val browPts = face.contourPoints(FaceContour.LEFT_EYEBROW_TOP) +
            face.contourPoints(FaceContour.RIGHT_EYEBROW_TOP)
        val noseBase = face.landmark(FaceLandmark.NOSE_BASE)
        if (cheekW <= 0 || browPts.isEmpty() || noseBase == null) {
            return unreliable("FWHR")
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

    // ================= FEATURES (25%) =================

    private fun eyes(face: Face): FeatureScore {
        val leC = face.contourPoints(FaceContour.LEFT_EYE)
        val reC = face.contourPoints(FaceContour.RIGHT_EYE)
        val le = face.landmark(FaceLandmark.LEFT_EYE)
        val re = face.landmark(FaceLandmark.RIGHT_EYE)
        if (leC.size < 4 || reC.size < 4 || le == null || re == null) {
            return unreliable("Eyes")
        }
        val eyeW = (widthOf(leC) + widthOf(reC)) / 2f
        val eyeH = (heightOf(leC) + heightOf(reC)) / 2f
        if (eyeW <= 0 || eyeH <= 0) return unreliable("Eyes")
        // Gap between the eyes, in eye-widths. Ideal ~= 1.0 (one-eye-apart rule).
        val gapRatio = (dist(le, re) - eyeW) / eyeW
        val spacingScore = scoreFromDeviation(abs(gapRatio - 1.0f), 0.35f)
        // Canthal tilt: lateral corner higher than medial corner = positive tilt.
        // Community target ~+4 deg (tan ~= 0.07); negative tilt is a failo.
        fun tilt(pts: List<PointF>, isLeft: Boolean): Float {
            val medial = if (isLeft) pts.maxBy { it.x } else pts.minBy { it.x }
            val lateral = if (isLeft) pts.minBy { it.x } else pts.maxBy { it.x }
            return (medial.y - lateral.y) / eyeW
        }
        val avgTilt = (tilt(leC, true) + tilt(reC, false)) / 2f
        val tiltScore = scoreFromDeviation(abs(avgTilt - 0.07f), 0.09f)
        // Eye aspect ratio (width/height). Community ideal ~2.8-3.6 for men.
        val aspectScore = scoreFromDeviation(abs(eyeW / eyeH - 3.1f), 0.7f)
        val score = spacingScore * 0.5 + tiltScore * 0.3 + aspectScore * 0.2
        val note = when {
            avgTilt < -0.02f -> "Negative canthal tilt — the main thing dragging the eye area down."
            spacingScore >= 6 && tiltScore >= 6 -> "Good spacing with a positive canthal tilt."
            else -> "Average eye area; spacing, tilt and shape are all workable."
        }
        return FeatureScore("Eyes", score, note)
    }

    private fun nose(face: Face, cheekW: Float): FeatureScore {
        val noseW = widthOf(face.contourPoints(FaceContour.NOSE_BOTTOM))
        val lipW = widthOf(
            face.contourPoints(FaceContour.UPPER_LIP_BOTTOM) +
                face.contourPoints(FaceContour.LOWER_LIP_TOP)
        )
        if (noseW <= 0 || lipW <= 0 || cheekW <= 0) {
            return unreliable("Nose")
        }
        // Mouth ~1.5x nose width; nose width ~22-26% of face width.
        val ratioScore = scoreFromDeviation(abs(lipW / noseW - 1.5f), 0.3f)
        val widthScore = scoreFromDeviation(abs(noseW / cheekW - 0.24f), 0.05f)
        val score = ratioScore * 0.6 + widthScore * 0.4
        val note = when {
            score >= 7 -> "Nose width sits in good proportion to the mouth and face."
            score >= 5 -> "Nose proportions are average."
            else -> "Nose reads wide relative to the mouth — alar base width is the main read; framing and angles matter most here."
        }
        return FeatureScore("Nose", score, note)
    }

    private fun lips(face: Face): FeatureScore {
        val upperPts = face.contourPoints(FaceContour.UPPER_LIP_TOP) +
            face.contourPoints(FaceContour.UPPER_LIP_BOTTOM)
        val lowerPts = face.contourPoints(FaceContour.LOWER_LIP_TOP) +
            face.contourPoints(FaceContour.LOWER_LIP_BOTTOM)
        if (upperPts.size < 4 || lowerPts.size < 4) return unreliable("Lips")
        val w = widthOf(upperPts + lowerPts)
        val h = heightOf(upperPts + lowerPts)
        if (w <= 0) return unreliable("Lips")
        val fullness = h / w
        val fullnessScore = scoreFromDeviation(abs(fullness - 0.30f), 0.10f)
        // Lower:upper lip height. Community band ~1.4-2.0.
        val upperH = heightOf(upperPts).coerceAtLeast(1f)
        val ratioScore = scoreFromDeviation(abs(heightOf(lowerPts) / upperH - 1.6f), 0.5f)
        val score = fullnessScore * 0.6 + ratioScore * 0.4
        val note = when {
            score >= 7 -> "Good lip fullness and definition."
            score >= 5 -> "Average lip volume."
            else -> "Thinner lips — hydration and lip care help them read fuller."
        }
        return FeatureScore("Lips", score, note)
    }

    // ================= DIMORPHISM (20%) =================

    private fun jawline(face: Face, cheekW: Float): FeatureScore {
        val oval = face.contourPoints(FaceContour.FACE)
        if (oval.isEmpty() || cheekW <= 0) {
            return unreliable("Jawline")
        }
        val topY = oval.minOf { it.y }
        val chinY = oval.maxOf { it.y }
        val faceH = (chinY - topY).coerceAtLeast(1f)
        val jawY = chinY - faceH * 0.28f
        val jawPts = oval.filter { it.y >= jawY - faceH * 0.06f && it.y <= jawY + faceH * 0.06f }
        val jawW = if (jawPts.size >= 2) widthOf(jawPts) else cheekW * 0.8f
        // Bigonial / bizygomatic. Community ideal 0.85-0.92 (wider jaw = more dimorphic).
        val ratio = jawW / cheekW
        val score = scoreFromDeviation(abs(ratio - 0.88f), 0.12f)
        val note = when {
            score >= 7 -> "Wide, well-defined jaw relative to cheekbones — good gonial width."
            score >= 5 -> "Decent jaw width — definition is the main lever."
            else -> "Narrower jaw line with a soft gonial read; leanness brings out what bone is there."
        }
        return FeatureScore("Jawline", score, note)
    }

    private fun chin(face: Face, jawW: Float): FeatureScore {
        val oval = face.contourPoints(FaceContour.FACE)
        if (oval.isEmpty() || jawW <= 0) {
            return unreliable("Chin")
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
            else -> "Chin reads narrow or recessed relative to the jaw — chin projection is the main read."
        }
        return FeatureScore("Chin", score, note)
    }

    /** Brows: thicker, lower-set brows read more dimorphic. Coarse ML Kit estimate. */
    private fun brows(face: Face): FeatureScore {
        val lbTop = face.contourPoints(FaceContour.LEFT_EYEBROW_TOP)
        val lbBot = face.contourPoints(FaceContour.LEFT_EYEBROW_BOTTOM)
        val rbTop = face.contourPoints(FaceContour.RIGHT_EYEBROW_TOP)
        val rbBot = face.contourPoints(FaceContour.RIGHT_EYEBROW_BOTTOM)
        val leC = face.contourPoints(FaceContour.LEFT_EYE)
        val reC = face.contourPoints(FaceContour.RIGHT_EYE)
        if (lbTop.isEmpty() || lbBot.isEmpty() || rbTop.isEmpty() || rbBot.isEmpty() ||
            leC.size < 4 || reC.size < 4
        ) {
            return unreliable("Brows")
        }
        val eyeH = ((heightOf(leC) + heightOf(reC)) / 2f).coerceAtLeast(1f)
        val thick = (heightOf(lbTop + lbBot) + heightOf(rbTop + rbBot)) / 2f / eyeH
        val thickScore = scoreFromDeviation(abs(thick - 0.55f), 0.30f)
        // Setedness: smaller brow-to-eye gap = lower-set = more dimorphic.
        val eyeTopY = (leC.minOf { it.y } + reC.minOf { it.y }) / 2f
        val browBotY = ((lbBot.maxOf { it.y } + rbBot.maxOf { it.y }) / 2f)
        val gapScore = scoreFromDeviation(abs((eyeTopY - browBotY) / eyeH - 0.5f), 0.4f)
        val score = thickScore * 0.6 + gapScore * 0.4
        val note = when {
            score >= 7 -> "Thick, well-set brows — strong eye-area framing."
            score >= 5 -> "Average brows; grooming keeps them intentional."
            else -> "Brows read thin or high-set — growing them thicker is the #1 eye-area lever."
        }
        return FeatureScore("Brows", score, note)
    }

    // ================= ANGULARITY (15%) =================

    private fun cheekbones(face: Face, cheekW: Float, jawW: Float): FeatureScore {
        if (cheekW <= 0 || jawW <= 0) {
            return unreliable("Cheekbones")
        }
        val ratio = cheekW / jawW
        val score = scoreFromDeviation(abs(ratio - 1.12f), 0.12f)
        val note = when {
            score >= 7 -> "Zygos sit visibly wider than the jaw — strong midface structure."
            score >= 5 -> "Average cheekbone projection."
            else -> "Flatter zygo area; leanness brings the most out here."
        }
        return FeatureScore("Cheekbones", score, note)
    }

    /**
     * Jaw frontal angle: angle at the chin between the two jaw (gonion-proxy) points.
     * Community ideal ~84-95 deg for men (sharper = more angular).
     */
    private fun jawFrontalAngle(face: Face): FeatureScore {
        val oval = face.contourPoints(FaceContour.FACE)
        if (oval.size < 12) return unreliable("Jaw angle")
        val chinY = oval.maxOf { it.y }
        val topY = oval.minOf { it.y }
        val faceH = (chinY - topY).coerceAtLeast(1f)
        val jawY = chinY - faceH * 0.28f
        val jawPts = oval.filter { abs(it.y - jawY) < faceH * 0.06f }
        if (jawPts.size < 2) return unreliable("Jaw angle")
        val chinX = oval.filter { it.y >= chinY - faceH * 0.02f }.map { it.x }.average().toFloat()
        val leftJaw = jawPts.minBy { it.x }
        val rightJaw = jawPts.maxBy { it.x }
        val v1x = leftJaw.x - chinX
        val v1y = leftJaw.y - chinY
        val v2x = rightJaw.x - chinX
        val v2y = rightJaw.y - chinY
        val mag = hypot(v1x, v1y) * hypot(v2x, v2y)
        if (mag <= 0f) return unreliable("Jaw angle")
        val cos = ((v1x * v2x + v1y * v2y) / mag).coerceIn(-1f, 1f).toDouble()
        val angleDeg = Math.toDegrees(acos(cos)).toFloat()
        val score = scoreFromDeviation(abs(angleDeg - 89f), 8f)
        val note = when {
            score >= 7 -> "Sharp gonial angle — strong dimorphic read."
            score >= 5 -> "Jaw angularity is average."
            else -> "Gonial angle reads soft/round — leanness sharpens this more than anything."
        }
        return FeatureScore("Jaw angle", score, note)
    }

    // ================= SIDE PROFILE (15% bonus) =================

    /**
     * Side profile estimate from the most-turned profile photo: how far the chin
     * sits behind the nose tip, relative to face height. Small offset = straight
     * profile / good forward growth. Crude but directionally real. Returns the
     * score plus whether a usable profile photo existed.
     */
    private fun sideProfile(faces: List<Face>, primary: Face): Pair<FeatureScore, Boolean> {
        val fallback = unreliable("Side profile")
            .copy(note = "Profile photo wasn't clear enough to assess.")
        val profile = faces
            .filter { it !== primary && abs(it.headEulerAngleY) > 15f }
            .maxByOrNull { abs(it.headEulerAngleY) }
            ?: return fallback to false
        val contour = profile.contourPoints(FaceContour.FACE)
        if (contour.size < 12) return fallback to false
        val minY = contour.minOf { it.y }
        val maxY = contour.maxOf { it.y }
        val h = (maxY - minY).coerceAtLeast(1f)
        val midY = (minY + maxY) / 2f
        val centerX = contour.map { it.x }.average()
        // Nose tip: farthest point from face center in the middle vertical band.
        val band = contour.filter { abs(it.y - midY) < h * 0.22f }
        if (band.isEmpty()) return fallback to false
        val noseTip = band.maxBy { abs(it.x - centerX) }
        val chin = contour.maxBy { it.y }
        val dev = abs(noseTip.x - chin.x) / h
        val score = scoreFromDeviation(dev, 0.055f)
        val angleCaveat = if (abs(profile.headEulerAngleY) < 45f) {
            " (3/4-angle estimate — a true 90° side photo sharpens this)"
        } else ""
        val note = when {
            score >= 6.5 -> "Straight profile — chin projection lines up under the nose, maxilla reads forward.$angleCaveat"
            score >= 4.5 -> "Profile is average; slight recession or projection.$angleCaveat"
            else -> "Chin reads recessed behind the nose — weak chin projection / flat maxilla read; posture and photo angle help most.$angleCaveat"
        }
        return FeatureScore("Side profile", score, note) to true
    }

    // ---------- advice database (softmaxxing only) ----------

    private val ADVICE: Map<String, List<Pair<String, String>>> = mapOf(
        "Jawline" to listOf(
            "Keep a healthy weight with balanced food and regular movement — no crash diets; a leaner face shows more jaw definition" to "hard",
            "Chin tucks daily to fix forward head posture that hides the jawline" to "easy",
            "A short boxed beard or stubble along the jaw adds definition overnight" to "easy",
            "Beard density routine: 3-5 min daily massage along the jawline and cheeks for blood flow, protein-rich food, 7-9h sleep — visible gains in 4-6 weeks" to "medium"
        ),
        "Jaw angle" to listOf(
            "A healthy weight shows more angularity — never crash diet, definition follows overall leanness, not products" to "hard",
            "Bodyweight neck curls build neck thickness that frames the jaw" to "medium"
        ),
        "Cheekbones" to listOf(
            "A healthy weight hollows the cheeks — never crash diet; it follows overall leanness, not products" to "hard",
            "Cut salty food and drink more water — less facial bloat reads sharper" to "easy"
        ),
        "Symmetry" to listOf(
            "Sleep on your back so one side of your face isn't pressed all night" to "easy",
            "Fix forward head posture — wall chin tucks, 2 minutes daily" to "medium",
            "Stop resting your chin or cheek on your hand" to "easy"
        ),
        "Facial thirds" to listOf(
            "Get a fringe or textured top if your forehead runs long" to "easy",
            "Avoid tall hairstyles if your midface runs long — keep volume on the sides" to "easy"
        ),
        "Facial fifths" to listOf(
            "A side part instead of a middle part rebalances uneven fifths" to "easy",
            "Face-framing layers draw the eye away from width imbalances" to "easy"
        ),
        "Midface ratio" to listOf(
            "Long midface: keep hair volume on the sides, not stacked on top" to "easy",
            "Short midface: some height on top restores balance" to "easy"
        ),
        "Eyes" to listOf(
            "Sleep 7-9 hours — under-eye bags drag the whole eye area down" to "medium",
            "Grow brows thicker and trim strays; they frame the eyes" to "easy",
            "Cold spoon or cold compress in the morning for puffiness" to "easy"
        ),
        "Brows" to listOf(
            "Grow them thicker, trim strays — community raters tend to rate the eye area first" to "easy",
            "Keep the shape straight and natural; avoid arched ends" to "easy"
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
            "A healthy weight reveals chin definition — never crash diet" to "hard",
            "Fix forward head posture — chin tucks make the chin read stronger" to "easy",
            "A defined short beard or stubble visually projects the chin and squares the lower third" to "easy"
        ),
        "FWHR" to listOf(
            "FWHR reads best at a healthy weight — never crash diet for it" to "hard",
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

    /** Rebuilds the 4 pillar scores from any feature list (same weights as the live read). */
    private fun pillarScores(features: List<FeatureScore>): List<PillarScore> {
        fun byName(n: String) = features.firstOrNull { it.name == n } ?: unreliable(n)
        val harmony = weighted(
            listOf(
                byName("Facial thirds") to 0.25, byName("Facial fifths") to 0.12,
                byName("Midface ratio") to 0.17, byName("Eye spacing") to 0.17,
                byName("FWHR") to 0.13, byName("Symmetry") to 0.16
            )
        )
        val featuresP = weighted(
            listOf(byName("Eyes") to 0.45, byName("Nose") to 0.30, byName("Lips") to 0.25)
        )
        val dimorphism = weighted(
            listOf(byName("Jawline") to 0.40, byName("Chin") to 0.35, byName("Brows") to 0.25)
        )
        val angular = weighted(
            listOf(byName("Cheekbones") to 0.55, byName("Jaw angle") to 0.45)
        )
        return listOf(
            PillarScore("Harmony", harmony, "How well your proportions fit together — the #1 thing raters score."),
            PillarScore("Features", featuresP, "Eyes, nose and lips on their own merits."),
            PillarScore("Dimorphism", dimorphism, "Masculine structure — jaw, chin, brow."),
            PillarScore("Angularity", angular, "Sharpness vs softness — leanness lives here.")
        )
    }

    /**
     * Full score pipeline from a feature list: pillar weighting, side-profile
     * bonus blend, then failo/halo gating (community tier logic).
     * Failo = major deviation (< 4). Halo = standout feature (>= 7).
     */
    private fun overallFromFeatures(features: List<FeatureScore>, sideOk: Boolean): Double {
        val pillars = pillarScores(features)
        val frontal = pillars[0].score * 0.40 + pillars[1].score * 0.25 +
            pillars[2].score * 0.20 + pillars[3].score * 0.15
        var overall = if (sideOk) {
            val side = features.firstOrNull { it.name == "Side profile" }?.score ?: 4.0
            frontal * 0.85 + side * 0.15
        } else {
            frontal
        }
        val failos = features.count { it.score < 4.0 }
        val halos = features.count { it.score >= 7.0 }
        return when {
            failos >= 3 -> minOf(overall, 4.9)
            failos == 2 -> minOf(overall, 5.4)
            failos == 1 -> minOf(overall, 5.9)
            halos == 0 -> minOf(overall, 5.9)
            else -> overall
        }.coerceIn(1.0, 8.0)
    }

    /** Mean luminance of the face region; flags dark or blown-out captures. */
    private fun lightingNote(face: Face, bitmap: Bitmap): String? {
        val box = face.boundingBox
        val l = box.left.coerceIn(0, bitmap.width - 1)
        val t = box.top.coerceIn(0, bitmap.height - 1)
        val r = box.right.coerceIn(0, bitmap.width)
        val b = box.bottom.coerceIn(0, bitmap.height)
        if (r <= l || b <= t) return null
        var sum = 0L
        var n = 0
        var y = t
        while (y < b) {
            var x = l
            while (x < r) {
                val px = bitmap.getPixel(x, y)
                sum += (0.299 * ((px shr 16) and 0xFF) +
                    0.587 * ((px shr 8) and 0xFF) +
                    0.114 * (px and 0xFF)).toLong()
                n++
                x += 14
            }
            y += 14
        }
        if (n == 0) return null
        val mean = sum.toDouble() / n
        return when {
            mean < 55 -> "Photo was too dark — face the light source, never shoot against it."
            mean > 205 -> "Photo was overexposed — soften the light for a truer read."
            else -> null
        }
    }

    /**
     * Confidence 0..1 plus ± uncertainty, derived from pose angles, lighting,
     * framing distance and how many of the 3 angles read cleanly.
     */
    private fun confidenceOf(
        face: Face,
        angles: Int,
        badLight: Boolean,
        badDist: Boolean
    ): Pair<Double, Double> {
        var c = 0.95
        c -= (abs(face.headEulerAngleY) / 12f).coerceIn(0f, 1f) * 0.18
        c -= (abs(face.headEulerAngleX) / 12f).coerceIn(0f, 1f) * 0.15
        c -= (abs(face.headEulerAngleZ) / 10f).coerceIn(0f, 1f) * 0.12
        if (badLight) c -= 0.20
        if (badDist) c -= 0.15
        c -= (3 - angles).coerceIn(0, 3) * 0.08
        c = c.coerceIn(0.40, 0.97)
        val uncertainty = ((1 - c) * 1.4 + 0.12).coerceIn(0.1, 1.0)
        return c to (kotlin.math.round(uncertainty * 10) / 10.0)
    }

    private fun confidenceLabel(c: Double): String = when {
        c >= 0.85 -> "high confidence"
        c >= 0.65 -> "medium confidence"
        else -> "low confidence — retake in better light"
    }

    /**
     * Collects the real measured face mesh for the scan overlay: face oval,
     * eyes, brows, nose, mouth contours plus ear/cheek landmarks, all
     * normalized to 0..1 in the source bitmap's space. Also returns the
     * normalized face bounding box and thirds guide y-positions.
     */
    private fun collectMesh(
        face: Face,
        bitmap: Bitmap
    ): Triple<List<LandmarkPt>, List<Float>, List<Float>> {
        val w = bitmap.width.toFloat().coerceAtLeast(1f)
        val h = bitmap.height.toFloat().coerceAtLeast(1f)
        val mesh = mutableListOf<LandmarkPt>()
        fun addPts(pts: List<PointF>, kind: Int) {
            pts.filterIndexed { i, _ -> i % 2 == 0 }.take(36).forEach { p ->
                mesh.add(
                    LandmarkPt(
                        (p.x / w).coerceIn(0f, 1f),
                        (p.y / h).coerceIn(0f, 1f),
                        kind
                    )
                )
            }
        }
        addPts(face.contourPoints(FaceContour.FACE), 0)
        addPts(
            face.contourPoints(FaceContour.LEFT_EYE) + face.contourPoints(FaceContour.RIGHT_EYE),
            1
        )
        addPts(
            face.contourPoints(FaceContour.LEFT_EYEBROW_TOP) +
                face.contourPoints(FaceContour.RIGHT_EYEBROW_TOP),
            2
        )
        addPts(
            face.contourPoints(FaceContour.NOSE_BRIDGE) + face.contourPoints(FaceContour.NOSE_BOTTOM),
            3
        )
        addPts(
            face.contourPoints(FaceContour.UPPER_LIP_TOP) +
                face.contourPoints(FaceContour.LOWER_LIP_BOTTOM),
            4
        )
        listOf(
            face.landmark(FaceLandmark.LEFT_EAR), face.landmark(FaceLandmark.RIGHT_EAR),
            face.landmark(FaceLandmark.LEFT_CHEEK), face.landmark(FaceLandmark.RIGHT_CHEEK)
        ).forEach { p ->
            if (p != null) mesh.add(
                LandmarkPt((p.x / w).coerceIn(0f, 1f), (p.y / h).coerceIn(0f, 1f), 5)
            )
        }
        val box = face.boundingBox
        val faceBox = listOf(box.left / w, box.top / h, box.right / w, box.bottom / h)
            .map { it.coerceIn(0f, 1f) }
        val oval = face.contourPoints(FaceContour.FACE)
        val thirdsY = if (oval.isNotEmpty()) {
            val browPts = face.contourPoints(FaceContour.LEFT_EYEBROW_TOP) +
                face.contourPoints(FaceContour.RIGHT_EYEBROW_TOP)
            val browY = if (browPts.isNotEmpty()) browPts.map { it.y }.average().toFloat()
            else oval.minOf { it.y }
            val noseY = face.landmark(FaceLandmark.NOSE_BASE)?.y ?: browY
            listOf(oval.minOf { it.y } / h, browY / h, noseY / h, oval.maxOf { it.y } / h)
                .map { it.coerceIn(0f, 1f) }
        } else emptyList()
        return Triple(mesh.take(170), faceBox, thirdsY)
    }

    // ---------- report assembly ----------

    private fun buildReport(face: Face, faces: List<Face>, bitmap: Bitmap): PslReport {
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

        // --- 15 measured features ---
        val thirds = facialThirds(face)
        val fifths = facialFifths(face)
        val midface = midfaceRatio(face)
        val esr = eyeSpacing(face, cheekW)
        val fwhrV = fwhr(face, cheekW)
        val sym = symmetry(face)
        val eyesV = eyes(face)
        val noseV = nose(face, cheekW)
        val lipsV = lips(face)
        val jawV = jawline(face, cheekW)
        val chinV = chin(face, jawW)
        val browsV = brows(face)
        val cheekV = cheekbones(face, cheekW, jawW)
        val jawAngleV = jawFrontalAngle(face)

        // Side profile is a bonus read, never the base score.
        val (sideV, sideOk) = sideProfile(faces, face)

        val features = listOf(
            thirds, fifths, midface, esr, fwhrV, sym,
            eyesV, noseV, lipsV,
            jawV, chinV, browsV,
            cheekV, jawAngleV,
            sideV
        )

        val pillars = pillarScores(features)
        val overall = overallFromFeatures(features, sideOk)

        // --- Softmaxx ceiling: a grounded potential, not a flat +1.5 ---
        // Every non-bone feature below 6.0 is assumed to close 35% of its gap
        // to 6.0 through consistent softmaxxing (grooming, leanness, styling —
        // presentation, not bone). Same pillars, same gating, recomputed.
        val improved = features.map { f ->
            if (f.name != "Side profile" && f.score < 6.0)
                f.copy(score = minOf(f.score + (6.0 - f.score) * 0.35, 6.6))
            else f
        }
        val potentialPsl = overallFromFeatures(improved, sideOk).coerceAtMost(8.0)

        // --- Failo/halo counts for the summary line ---
        val failos = features.count { it.score < 4.0 }
        val halos = features.count { it.score >= 7.0 }

        val decile = (overall + 1.0).coerceIn(1.0, 10.0)
        val percentile = (normalCdf(overall - 4.0) * 100).toInt().coerceIn(1, 99)
        val overall100 = ((overall - 1.0) / 7.0 * 100).toInt().coerceIn(0, 100)

        // --- Photo-quality checks (raters' #1 rule: rate the undistorted photo) ---
        val photoNotes = mutableListOf<String>()
        if (abs(face.headEulerAngleY) > 12f) {
            photoNotes += "Front photo was angled — phone at eye level, 6-8 ft back, face straight at the lens."
        }
        if (abs(face.headEulerAngleX) > 12f) {
            photoNotes += "Chin was tilted up or down — keep the camera level with your eyes."
        }
        if (abs(face.headEulerAngleZ) > 10f) {
            photoNotes += "Head was rolled to one side — keep it straight for a clean read."
        }
        if (faces.size < 3) {
            photoNotes += "Only ${faces.size} of 3 angles read clearly — retake the blurry one in good light."
        }
        // Lighting read from actual face-region pixels.
        val lightNote = lightingNote(face, bitmap)
        if (lightNote != null) photoNotes += lightNote
        // Distance read from face size relative to the frame.
        val boxArea = face.boundingBox.width().toFloat() * face.boundingBox.height().toFloat()
        val imgArea = bitmap.width.toFloat() * bitmap.height.toFloat()
        val faceFrac = if (imgArea > 0) boxArea / imgArea else 0f
        val badDist = faceFrac < 0.08f || faceFrac > 0.65f
        if (faceFrac < 0.08f) {
            photoNotes += "Face was too small in the frame — shoot from 6-8 ft, not across the room."
        } else if (faceFrac > 0.65f) {
            photoNotes += "Face was too close — back up so the full head fits with margin."
        }
        val (confidence, uncertainty) = confidenceOf(face, faces.size, lightNote != null, badDist)

        // --- Real measured mesh for the scan overlay ---
        val (mesh, faceBox, thirdsY) = collectMesh(face, bitmap)

        val ranked = features.sortedByDescending { it.score }
        val strengths = ranked.filter { it.score >= 6.0 }.take(3)
            .ifEmpty { ranked.take(2) }
            .map { "${it.name} (${"%.1f".format(it.score)}) — ${it.note}" }

        val weakest = ranked.takeLast(3)
        val improvements = buildList {
            // Raters' highest-ROI lever first: fixing the presentation beats optimizing bone.
            if (overall < 5.0) {
                add(
                    Improvement(
                        "Biggest lever",
                        "Healthy weight and low bloat — community raters treat leanness as the biggest softmaxx lever; never crash diet",
                        "hard"
                    )
                )
            }
            // Beard growth routine for low-dimorphism reads: safe mechanical + nutrition method.
            if (pillars[2].score < 5.0) {
                add(
                    Improvement(
                        "Beard density",
                        "Daily 3-5 min massage along jaw and cheeks for blood flow, protein-rich food, 7-9h sleep, keep the skin underneath clean — first gains in 4-6 weeks, real coverage in 3-6 months",
                        "medium"
                    )
                )
            }
            // Hyoid/under-chin drill: safe exercise method for a tighter throat-jaw angle.
            if (weakest.any { it.name == "Jawline" || it.name == "Chin" }) {
                add(
                    Improvement(
                        "Under-chin",
                        "Tongue pressed to the palate + slow, wide jaw opens (3x10, twice daily) tightens the under-chin area — stop if the jaw clicks or aches",
                        "easy"
                    )
                )
            }
            addAll(
                weakest.flatMap { f ->
                    (ADVICE[f.name] ?: emptyList()).take(2).map { (method, effort) ->
                        Improvement(f.name, method, effort)
                    }
                }
            )
        }.take(6) + Improvement(
            "Fragrance",
            "1-2 sprays on pulse points (neck sides, wrists) — less is more; fresh or citrus for daytime, woody or spicy for evenings",
            "easy"
        )

        val best = ranked.first()
        val worst = ranked.last()
        val summary = "Your ${best.name.lowercase()} is your strongest asset right now. " +
            "Biggest wins come from ${worst.name.lowercase()} — " +
            "${improvements.firstOrNull()?.method?.lowercase() ?: "consistent softmaxxing"}. " +
            "Overall ${"%.1f".format(overall)} PSL (±${"%.1f".format(uncertainty)}, ${confidenceLabel(confidence)}; " +
            "≈${"%.1f".format(decile)}/10): " +
            "${pslLabel(overall).lowercase()}. $failos negative points dragging, $halos halos carrying. " +
            "Photo-dependent estimate — lighting and angle change the read."

        return PslReport(
            overallPsl = overall,
            overall100 = overall100,
            features = features,
            strengths = strengths,
            improvements = improvements,
            summary = summary,
            anglesRead = faces.size,
            decile = decile,
            percentile = percentile,
            pillars = pillars,
            photoNotes = photoNotes,
            failoCount = failos,
            haloCount = halos,
            landmarkMesh = mesh,
            faceBox = faceBox,
            thirdsY = thirdsY,
            confidence = confidence,
            uncertainty = uncertainty,
            potentialPsl = potentialPsl
        )
    }

    /** Community PSL tiers (1.0-8.0 scale). Brutally honest, no sugarcoating. */
    private fun pslLabel(psl: Double): String = when {
        psl >= 7.75 -> "Gigachad — near-mythical"
        psl >= 7.0 -> "Chad"
        psl >= 6.0 -> "Chadlite"
        psl >= 5.0 -> "HTN — High Tier Normie"
        psl >= 3.0 -> "MTN — Mid Tier Normie"
        psl >= 1.4 -> "LTN — Low Tier Normie"
        else -> "Sub-5 — maximum ascension potential"
    }
}
