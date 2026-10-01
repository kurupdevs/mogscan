package com.kurupdevs.moggr.analysis

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
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
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sign

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

    // v2.6-landmark: full-resolution geometry snapshot + FaceGeo abstraction, so the
    // measurement pipeline can run on user-corrected points, not just ML Kit output.

    /**
     * Full-resolution face geometry captured at scan time: every contour point
     * (not the downsampled overlay mesh) plus the key landmarks, in the source
     * bitmap's pixel space. The verify-points UI edits a copy of this; the
     * corrected copy feeds [recompute].
     */
    data class FaceGeometrySnapshot(
        val contours: Map<Int, List<PointF>>,
        val marks: Map<Int, PointF>,
        val box: Rect,
        val eulerY: Float,
        val eulerX: Float,
        val eulerZ: Float,
        val leftOpenP: Float?,
        val rightOpenP: Float?
    )

    /** analyze() result: the report plus the editable geometry behind it. */
    data class AnalysisResult(
        val report: PslReport,
        val geometry: FaceGeometrySnapshot?
    )

    /**
     * Abstraction over face geometry: ML Kit's [Face] and corrected snapshots
     * both satisfy it, so every measurement runs unchanged on either source.
     */
    interface FaceGeo {
        val box: Rect
        val eulerY: Float
        val eulerX: Float
        val eulerZ: Float
        val leftOpenP: Float?
        val rightOpenP: Float?
        fun landmark(type: Int): PointF?
        fun contourPoints(type: Int): List<PointF>
    }

    private class MlKitFaceGeo(private val f: Face) : FaceGeo {
        override val box: Rect get() = f.boundingBox
        override val eulerY: Float get() = f.headEulerAngleY
        override val eulerX: Float get() = f.headEulerAngleX
        override val eulerZ: Float get() = f.headEulerAngleZ
        override val leftOpenP: Float? get() = f.leftEyeOpenProbability
        override val rightOpenP: Float? get() = f.rightEyeOpenProbability
        override fun landmark(type: Int): PointF? = f.landmark(type)
        override fun contourPoints(type: Int): List<PointF> = f.contourPoints(type)
    }

    /** FaceGeo backed by a (possibly user-corrected) geometry snapshot. */
    class SnapshotFaceGeo(val snapshot: FaceGeometrySnapshot) : FaceGeo {
        override val box: Rect get() = snapshot.box
        override val eulerY: Float get() = snapshot.eulerY
        override val eulerX: Float get() = snapshot.eulerX
        override val eulerZ: Float get() = snapshot.eulerZ
        override val leftOpenP: Float? get() = snapshot.leftOpenP
        override val rightOpenP: Float? get() = snapshot.rightOpenP
        override fun landmark(type: Int): PointF? = snapshot.marks[type]
        override fun contourPoints(type: Int): List<PointF> =
            snapshot.contours[type] ?: emptyList()
    }

    /** Overlay kind for an ML Kit contour type: 0 oval, 1 eye, 2 brow, 3 nose, 4 mouth. */
    fun contourKind(contourType: Int): Int = when (contourType) {
        FaceContour.FACE -> 0
        FaceContour.LEFT_EYE, FaceContour.RIGHT_EYE -> 1
        FaceContour.LEFT_EYEBROW_TOP, FaceContour.RIGHT_EYEBROW_TOP,
        FaceContour.LEFT_EYEBROW_BOTTOM, FaceContour.RIGHT_EYEBROW_BOTTOM -> 2
        FaceContour.NOSE_BRIDGE, FaceContour.NOSE_BOTTOM -> 3
        FaceContour.UPPER_LIP_TOP, FaceContour.UPPER_LIP_BOTTOM,
        FaceContour.LOWER_LIP_TOP, FaceContour.LOWER_LIP_BOTTOM -> 4
        else -> 0
    }

    /** Captures every contour + landmark the measurements read, at full resolution. */
    fun snapshotOf(f: Face): FaceGeometrySnapshot {
        val contourTypes = listOf(
            FaceContour.FACE,
            FaceContour.LEFT_EYEBROW_TOP, FaceContour.RIGHT_EYEBROW_TOP,
            FaceContour.LEFT_EYEBROW_BOTTOM, FaceContour.RIGHT_EYEBROW_BOTTOM,
            FaceContour.LEFT_EYE, FaceContour.RIGHT_EYE,
            FaceContour.NOSE_BRIDGE, FaceContour.NOSE_BOTTOM,
            FaceContour.UPPER_LIP_TOP, FaceContour.UPPER_LIP_BOTTOM,
            FaceContour.LOWER_LIP_TOP, FaceContour.LOWER_LIP_BOTTOM
        )
        val contours = contourTypes.associateWith { t ->
            f.getContour(t)?.points?.map { PointF(it.x, it.y) } ?: emptyList()
        }
        val landmarkTypes = listOf(
            FaceLandmark.LEFT_EYE, FaceLandmark.RIGHT_EYE,
            FaceLandmark.MOUTH_LEFT, FaceLandmark.MOUTH_RIGHT,
            FaceLandmark.LEFT_CHEEK, FaceLandmark.RIGHT_CHEEK,
            FaceLandmark.NOSE_BASE, FaceLandmark.LEFT_EAR, FaceLandmark.RIGHT_EAR
        )
        val marks = landmarkTypes.mapNotNull { t ->
            f.getLandmark(t)?.position?.let { p -> t to PointF(p.x, p.y) }
        }.toMap()
        return FaceGeometrySnapshot(
            contours = contours,
            marks = marks,
            box = Rect(f.boundingBox),
            eulerY = f.headEulerAngleY,
            eulerX = f.headEulerAngleX,
            eulerZ = f.headEulerAngleZ,
            leftOpenP = f.leftEyeOpenProbability,
            rightOpenP = f.rightEyeOpenProbability
        )
    }

    suspend fun analyze(bitmaps: List<Bitmap>): AnalysisResult = withContext(Dispatchers.Default) {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
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
            val primary = primaryPair.second
            AnalysisResult(
                report = buildReport(primary, faces, primaryPair.first),
                geometry = snapshotOf(primary)
            )
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

    private fun noRead(name: String): Pair<FeatureScore, String> =
        unreliable(name) to "couldn't measure from this photo"

    /** Like scoreFromDeviation, but zero deviation across the whole band [lo, hi]. */
    private fun scoreFromBand(v: Float, lo: Float, hi: Float, tol: Float): Double {
        val dev = when {
            v < lo -> lo - v
            v > hi -> v - hi
            else -> 0f
        }
        return scoreFromDeviation(dev, tol)
    }

    /** Signed degrees with one decimal, e.g. "+4.2°" / "-1.5°". */
    private fun fmtDeg(d: Float): String =
        "${if (d >= 0) "+" else ""}${"%.1f".format(d)}°"

    /** Vertical thirds divisions from the face oval: top, brow, nose base, chin. */
    private data class Thirds(val top: Float, val brow: Float, val nose: Float, val chin: Float)

    private fun thirdsOf(face: FaceGeo): Thirds? {
        val oval = face.contourPoints(FaceContour.FACE)
        val browPts = face.contourPoints(FaceContour.LEFT_EYEBROW_TOP) +
            face.contourPoints(FaceContour.RIGHT_EYEBROW_TOP)
        val noseBase = face.landmark(FaceLandmark.NOSE_BASE)
        if (oval.isEmpty() || browPts.isEmpty() || noseBase == null) return null
        return Thirds(
            top = oval.minOf { it.y },
            brow = browPts.map { it.y }.average().toFloat(),
            nose = noseBase.y,
            chin = oval.maxOf { it.y }
        )
    }

    // ================= HARMONY (40%) =================

    private fun symmetry(face: FaceGeo): Pair<FeatureScore, String> {
        val h = face.box.height().toFloat().coerceAtLeast(1f)
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
        if (n == 0) return noRead("Symmetry")
        // Mild penalty: phone cameras exaggerate left-right differences, and raters
        // barely dock for asymmetry unless it's visible at a glance.
        val score = scoreFromDeviation(dev / n, 0.05f)
        val note = when {
            score >= 7 -> "Left and right sides line up very evenly."
            score >= 5 -> "Mostly even — small left-right differences are normal."
            else -> "Some left-right difference; usually reads softer in person than on camera."
        }
        val detail = regionDeviations(face)?.let { (u, m, l) ->
            "upper ${"%.1f".format(u)} · mid ${"%.1f".format(m)} · " +
                "lower ${"%.1f".format(l)} (avg px-dev, lower is better)"
        } ?: "dev ${"%.1f".format(dev / n * 100)}% · ideal ≈ 0%"
        return FeatureScore("Symmetry", score, note) to detail
    }

    /**
     * Per-region left-right mirror deviation in px: within each vertical third,
     * compare mean |x - centerX| of left vs right contour points. Lower = more
     * symmetric. Null when landmarks are missing. Feeds the "why" dialog —
     * the results chunk draws the face map.
     */
    private fun regionDeviations(face: FaceGeo): Triple<Float, Float, Float>? {
        val t = thirdsOf(face) ?: return null
        val cx = (face.box.left + face.box.right) / 2f
        fun devOf(pts: List<PointF>, yLo: Float, yHi: Float): Float? {
            val left = pts.filter { it.y in yLo..yHi && it.x < cx }
            val right = pts.filter { it.y in yLo..yHi && it.x >= cx }
            if (left.size < 3 || right.size < 3) return null
            val ml = left.map { abs(it.x - cx) }.average().toFloat()
            val mr = right.map { abs(it.x - cx) }.average().toFloat()
            return abs(ml - mr)
        }
        val brows = face.contourPoints(FaceContour.LEFT_EYEBROW_TOP) +
            face.contourPoints(FaceContour.RIGHT_EYEBROW_TOP)
        val eyesC = face.contourPoints(FaceContour.LEFT_EYE) +
            face.contourPoints(FaceContour.RIGHT_EYE)
        val noseC = face.contourPoints(FaceContour.NOSE_BRIDGE) +
            face.contourPoints(FaceContour.NOSE_BOTTOM)
        val lipsC = face.contourPoints(FaceContour.UPPER_LIP_TOP) +
            face.contourPoints(FaceContour.LOWER_LIP_BOTTOM)
        val oval = face.contourPoints(FaceContour.FACE)
        val upper = devOf(brows + eyesC, t.top, t.brow) ?: return null
        val mid = devOf(eyesC + noseC, t.brow, t.nose) ?: return null
        val lower = devOf(lipsC + oval, t.nose, t.chin) ?: return null
        return Triple(upper, mid, lower)
    }

    private fun facialThirds(face: FaceGeo): Pair<FeatureScore, String> {
        val t = thirdsOf(face) ?: return noRead("Facial thirds")
        val faceH = (t.chin - t.top).coerceAtLeast(1f)
        val upper = t.brow - t.top
        val mid = t.nose - t.brow
        val lower = t.chin - t.nose
        if (upper <= 0 || mid <= 0 || lower <= 0) {
            return noRead("Facial thirds")
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
        val detail = "${upper.toInt()}/${mid.toInt()}/${lower.toInt()}px · ideal ≈ equal thirds"
        return FeatureScore("Facial thirds", score, note) to detail
    }

    /** Facial fifths: eye width, inter-eye gap and nose width should each be ~1/5 of face width. */
    private fun facialFifths(face: FaceGeo): Pair<FeatureScore, String> {
        val le = face.landmark(FaceLandmark.LEFT_EYE)
        val re = face.landmark(FaceLandmark.RIGHT_EYE)
        val oval = face.contourPoints(FaceContour.FACE)
        val leC = face.contourPoints(FaceContour.LEFT_EYE)
        val reC = face.contourPoints(FaceContour.RIGHT_EYE)
        val nosePts = face.contourPoints(FaceContour.NOSE_BOTTOM)
        if (le == null || re == null || oval.isEmpty() || leC.size < 4 || reC.size < 4 || nosePts.size < 2) {
            return noRead("Facial fifths")
        }
        val h = face.box.height().toFloat().coerceAtLeast(1f)
        val eyeY = (le.y + re.y) / 2f
        val bandW = widthOf(oval.filter { abs(it.y - eyeY) < h * 0.06f })
        if (bandW <= 0) return noRead("Facial fifths")
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
        val detail = "${eyeW.toInt()}/${gap.toInt()}/${noseW.toInt()}px vs ${fifth.toInt()}px each"
        return FeatureScore("Facial fifths", score, note) to detail
    }

    /** Midface ratio = IPD / (brow to upper lip). Community ideal ~1.0 (compact = youthful). */
    private fun midfaceRatio(face: FaceGeo): Pair<FeatureScore, String> {
        val le = face.landmark(FaceLandmark.LEFT_EYE)
        val re = face.landmark(FaceLandmark.RIGHT_EYE)
        val browPts = face.contourPoints(FaceContour.LEFT_EYEBROW_TOP) +
            face.contourPoints(FaceContour.RIGHT_EYEBROW_TOP)
        val lipTop = face.contourPoints(FaceContour.UPPER_LIP_TOP)
        if (le == null || re == null || browPts.isEmpty() || lipTop.isEmpty()) {
            return noRead("Midface ratio")
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
        val detail = "${"%.2f".format(ratio)} · ideal ≈ 1.00"
        return FeatureScore("Midface ratio", score, note) to detail
    }

    /** ESR = interpupillary distance / bizygomatic width. Community ideal 0.45-0.47. */
    private fun eyeSpacing(face: FaceGeo, cheekW: Float): Pair<FeatureScore, String> {
        val le = face.landmark(FaceLandmark.LEFT_EYE)
        val re = face.landmark(FaceLandmark.RIGHT_EYE)
        if (le == null || re == null || cheekW <= 0) {
            return noRead("Eye spacing")
        }
        val ratio = dist(le, re) / cheekW
        val score = scoreFromDeviation(abs(ratio - 0.46f), 0.05f)
        val note = when {
            score >= 6.5 -> "Eye spacing sits right in the ideal ESR band."
            score >= 4.5 -> "Eye spacing is average."
            else -> "Eyes read close-set or wide-set versus the ideal ratio."
        }
        val detail = "${"%.2f".format(ratio)} · ideal ≈ 0.45–0.47"
        return FeatureScore("Eye spacing", score, note) to detail
    }

    /** FWHR = bizygomatic width / midface height. Community sweet spot ~1.8-1.95; both extremes penalized. */
    private fun fwhr(face: FaceGeo, cheekW: Float): Pair<FeatureScore, String> {
        val browPts = face.contourPoints(FaceContour.LEFT_EYEBROW_TOP) +
            face.contourPoints(FaceContour.RIGHT_EYEBROW_TOP)
        val noseBase = face.landmark(FaceLandmark.NOSE_BASE)
        if (cheekW <= 0 || browPts.isEmpty() || noseBase == null) {
            return noRead("FWHR")
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
        val detail = "${"%.2f".format(ratio)} · community ideal ≈ 1.85–2.05"
        return FeatureScore("FWHR", score, note) to detail
    }

    // ================= FEATURES (25%) =================

    private fun eyes(face: FaceGeo): Pair<FeatureScore, String> {
        val leC = face.contourPoints(FaceContour.LEFT_EYE)
        val reC = face.contourPoints(FaceContour.RIGHT_EYE)
        val le = face.landmark(FaceLandmark.LEFT_EYE)
        val re = face.landmark(FaceLandmark.RIGHT_EYE)
        if (leC.size < 4 || reC.size < 4 || le == null || re == null) {
            return noRead("Eyes")
        }
        val eyeW = (widthOf(leC) + widthOf(reC)) / 2f
        val eyeH = (heightOf(leC) + heightOf(reC)) / 2f
        if (eyeW <= 0 || eyeH <= 0) return noRead("Eyes")
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
        val tiltDeg = Math.toDegrees(atan2(avgTilt.toDouble(), 1.0)).toFloat()
        val detail = "gap ${"%.1f".format(gapRatio)}× eye · tilt ${fmtDeg(tiltDeg)} · ideals 1.0× / +3–8°"
        return FeatureScore("Eyes", score, note) to detail
    }

    /**
     * Canthal tilt in degrees: angle of the medial→lateral canthus line vs
     * horizontal, averaged over both eyes. Canthi are approximated from the
     * eye contour's extreme x-points (first/last contour points are adjacent
     * on the closed loop, so min/max x is the correct corner estimate).
     * Positive = lateral corner higher. Community ideal band +3° to +8°.
     */
    private fun canthalTiltDeg(face: FaceGeo): Float? {
        val leC = face.contourPoints(FaceContour.LEFT_EYE)
        val reC = face.contourPoints(FaceContour.RIGHT_EYE)
        if (leC.size < 4 || reC.size < 4) return null
        fun tiltDeg(pts: List<PointF>, isLeft: Boolean): Float {
            val medial = if (isLeft) pts.maxBy { it.x } else pts.minBy { it.x }
            val lateral = if (isLeft) pts.minBy { it.x } else pts.maxBy { it.x }
            val dx = abs(lateral.x - medial.x).coerceAtLeast(1f)
            // Image y grows downward: lateral higher ⇒ medial.y - lateral.y > 0.
            return Math.toDegrees(
                atan2((medial.y - lateral.y).toDouble(), dx.toDouble())
            ).toFloat()
        }
        return (tiltDeg(leC, true) + tiltDeg(reC, false)) / 2f
    }

    private fun canthalTilt(face: FaceGeo): Pair<FeatureScore, String> {
        val tilt = canthalTiltDeg(face) ?: return noRead("Canthal tilt")
        val score = scoreFromBand(tilt, 3f, 8f, 6f)
        val note = when {
            tilt < 0f -> "Negative canthal tilt — lateral corners sit lower than the inner corners."
            score >= 7 -> "Positive canthal tilt right in the community ideal band."
            tilt > 8f -> "Canthal tilt reads steep — past the usual ideal band."
            else -> "Canthal tilt is average."
        }
        val detail = "${fmtDeg(tilt)} · community ideal ≈ +3° to +8° (positive tilt)"
        return FeatureScore("Canthal tilt", score, note) to detail
    }

    /**
     * PFL (palpebral fissure length): eye width relative to face width.
     * Community PFL ~28-32mm vs bizygomatic ~140mm ⇒ ideal ratio 0.20-0.23.
     * Visibility read combines ML Kit eye-open probability with the
     * brow-to-eye gap.
     */
    private fun eyeSizePfl(face: FaceGeo, cheekW: Float): Pair<FeatureScore, String> {
        val leC = face.contourPoints(FaceContour.LEFT_EYE)
        val reC = face.contourPoints(FaceContour.RIGHT_EYE)
        if (leC.size < 4 || reC.size < 4 || cheekW <= 0) return noRead("Eye size (PFL)")
        val eyeW = (widthOf(leC) + widthOf(reC)) / 2f
        val ratio = eyeW / cheekW
        val score = scoreFromBand(ratio, 0.20f, 0.23f, 0.035f)
        val openP = listOfNotNull(face.leftOpenP, face.rightOpenP)
            .takeIf { it.isNotEmpty() }?.average()?.toFloat()
        val lbBot = face.contourPoints(FaceContour.LEFT_EYEBROW_BOTTOM)
        val rbBot = face.contourPoints(FaceContour.RIGHT_EYEBROW_BOTTOM)
        val browGap: Float? = if (lbBot.isNotEmpty() && rbBot.isNotEmpty()) {
            val eyeH = ((heightOf(leC) + heightOf(reC)) / 2f).coerceAtLeast(1f)
            val eyeTopY = (leC.minOf { it.y } + reC.minOf { it.y }) / 2f
            val browBotY = (lbBot.maxOf { it.y } + rbBot.maxOf { it.y }) / 2f
            (eyeTopY - browBotY) / eyeH
        } else null
        val note = when {
            score >= 7 -> "Eye width sits in the community ideal range."
            score >= 5 -> "Eye width is average."
            else -> if (ratio < 0.20f) "Eyes read small relative to face width — brow density frames them best."
            else "Eyes read large relative to face width."
        }
        val mm = (eyeW * 140f / cheekW).toInt()
        val show = when {
            openP == null -> null
            openP >= 0.75f -> "good scleral show"
            openP >= 0.45f -> "some lid cover"
            else -> "heavy lid cover"
        }
        val lid = if (browGap != null && browGap < 0.35f) "upper lid slightly heavy" else null
        val detail = buildString {
            append("${mm}mm est.")
            if (show != null) append(" · $show")
            if (lid != null) append(" · $lid")
        }.toString()
        return FeatureScore("Eye size (PFL)", score, note) to detail
    }

    private fun nose(face: FaceGeo, cheekW: Float): Pair<FeatureScore, String> {
        val noseW = widthOf(face.contourPoints(FaceContour.NOSE_BOTTOM))
        val lipW = widthOf(
            face.contourPoints(FaceContour.UPPER_LIP_BOTTOM) +
                face.contourPoints(FaceContour.LOWER_LIP_TOP)
        )
        if (noseW <= 0 || lipW <= 0 || cheekW <= 0) {
            return noRead("Nose")
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
        // Iris-scaled mm: bizygomatic width ≈ 140mm as reference. Always "est.".
        val mm = (noseW * 140f / cheekW).toInt()
        val detail = "${mm}mm est. · ideal ≈ 32–36mm"
        return FeatureScore("Nose", score, note) to detail
    }

    private fun lips(face: FaceGeo): Pair<FeatureScore, String> {
        val upperPts = face.contourPoints(FaceContour.UPPER_LIP_TOP) +
            face.contourPoints(FaceContour.UPPER_LIP_BOTTOM)
        val lowerPts = face.contourPoints(FaceContour.LOWER_LIP_TOP) +
            face.contourPoints(FaceContour.LOWER_LIP_BOTTOM)
        if (upperPts.size < 4 || lowerPts.size < 4) return noRead("Lips")
        val w = widthOf(upperPts + lowerPts)
        val h = heightOf(upperPts + lowerPts)
        if (w <= 0) return noRead("Lips")
        val fullness = h / w
        val fullnessScore = scoreFromDeviation(abs(fullness - 0.30f), 0.10f)
        // Lower:upper lip height. Community band ~1.4-2.0.
        val upperH = heightOf(upperPts).coerceAtLeast(1f)
        val luRatio = heightOf(lowerPts) / upperH
        val ratioScore = scoreFromDeviation(abs(luRatio - 1.6f), 0.5f)
        val score = fullnessScore * 0.6 + ratioScore * 0.4
        val note = when {
            score >= 7 -> "Good lip fullness and definition."
            score >= 5 -> "Average lip volume."
            else -> "Thinner lips — hydration and lip care help them read fuller."
        }
        val detail = "full ${"%.2f".format(fullness)} · L:U ${"%.1f".format(luRatio)}:1 · ideals 0.30 / 1.6:1"
        return FeatureScore("Lips", score, note) to detail
    }

    /**
     * Skin clarity from the cheek region: Laplacian variance of a grayscale
     * crop, normalized to 0-100 (smooth = low variance = high score), then
     * mapped onto the 1-8 feature scale. Labeled as lighting-sensitive —
     * it is a texture read, not a diagnosis.
     */
    private fun skinClarity(face: FaceGeo, bitmap: Bitmap): Pair<FeatureScore, String> {
        val cheek = face.landmark(FaceLandmark.LEFT_CHEEK)
            ?: face.landmark(FaceLandmark.RIGHT_CHEEK)
            ?: return noRead("Skin clarity")
        val half = (face.box.height() / 12).coerceIn(12, 60)
        val l = (cheek.x - half).toInt().coerceIn(0, bitmap.width - 1)
        val t = (cheek.y - half).toInt().coerceIn(0, bitmap.height - 1)
        val r = (cheek.x + half).toInt().coerceIn(0, bitmap.width)
        val b = (cheek.y + half).toInt().coerceIn(0, bitmap.height)
        if (r - l < 16 || b - t < 16) return noRead("Skin clarity")
        val step = 3
        val cols = (r - l) / step
        val rows = (b - t) / step
        if (cols < 6 || rows < 6) return noRead("Skin clarity")
        val gray = FloatArray(cols * rows)
        for (gy in 0 until rows) {
            for (gx in 0 until cols) {
                val px = bitmap.getPixel(l + gx * step, t + gy * step)
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
        if (n == 0) return noRead("Skin clarity")
        val mean = sum / n
        val variance = (sumSq / n - mean * mean).coerceAtLeast(0.0)
        val texture = (100.0 / (1.0 + variance / 250.0)).coerceIn(0.0, 100.0)
        val score = (1.0 + texture / 100.0 * 7.0).coerceIn(1.0, 8.0)
        val note = when {
            texture >= 70 -> "Skin texture reads smooth in this light."
            texture >= 45 -> "Some texture visible — normal skin, lighting affects this read."
            else -> "Rougher texture read — lighting and photo quality affect this a lot."
        }
        return FeatureScore("Skin clarity", score, note) to
            "texture score ${texture.toInt()}/100 · lighting affects this read"
    }

    // ================= DIMORPHISM (20%) =================

    private fun jawline(face: FaceGeo, cheekW: Float): Pair<FeatureScore, String> {
        val oval = face.contourPoints(FaceContour.FACE)
        if (oval.isEmpty() || cheekW <= 0) {
            return noRead("Jawline")
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
        val detail = "jaw/cheek ${"%.2f".format(ratio)} · ideal ≈ 0.85–0.92"
        return FeatureScore("Jawline", score, note) to detail
    }

    private fun chin(face: FaceGeo, jawW: Float): Pair<FeatureScore, String> {
        val oval = face.contourPoints(FaceContour.FACE)
        if (oval.isEmpty() || jawW <= 0) {
            return noRead("Chin")
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
        val detail = "chin/jaw ${"%.2f".format(ratio)} · ideal ≈ 0.55"
        return FeatureScore("Chin", score, note) to detail
    }

    /** Brows: thicker, lower-set brows read more dimorphic. Coarse ML Kit estimate. */
    private fun brows(face: FaceGeo): Pair<FeatureScore, String> {
        val lbTop = face.contourPoints(FaceContour.LEFT_EYEBROW_TOP)
        val lbBot = face.contourPoints(FaceContour.LEFT_EYEBROW_BOTTOM)
        val rbTop = face.contourPoints(FaceContour.RIGHT_EYEBROW_TOP)
        val rbBot = face.contourPoints(FaceContour.RIGHT_EYEBROW_BOTTOM)
        val leC = face.contourPoints(FaceContour.LEFT_EYE)
        val reC = face.contourPoints(FaceContour.RIGHT_EYE)
        if (lbTop.isEmpty() || lbBot.isEmpty() || rbTop.isEmpty() || rbBot.isEmpty() ||
            leC.size < 4 || reC.size < 4
        ) {
            return noRead("Brows")
        }
        val eyeH = ((heightOf(leC) + heightOf(reC)) / 2f).coerceAtLeast(1f)
        val thick = (heightOf(lbTop + lbBot) + heightOf(rbTop + rbBot)) / 2f / eyeH
        val thickScore = scoreFromDeviation(abs(thick - 0.55f), 0.30f)
        // Setedness: smaller brow-to-eye gap = lower-set = more dimorphic.
        val eyeTopY = (leC.minOf { it.y } + reC.minOf { it.y }) / 2f
        val browBotY = ((lbBot.maxOf { it.y } + rbBot.maxOf { it.y }) / 2f)
        val gap = (eyeTopY - browBotY) / eyeH
        val gapScore = scoreFromDeviation(abs(gap - 0.5f), 0.4f)
        val score = thickScore * 0.6 + gapScore * 0.4
        val note = when {
            score >= 7 -> "Thick, well-set brows — strong eye-area framing."
            score >= 5 -> "Average brows; grooming keeps them intentional."
            else -> "Brows read thin or high-set — growing them thicker is the #1 eye-area lever."
        }
        val detail = "thick ${"%.2f".format(thick)} · gap ${"%.1f".format(gap)} eye-H · ideals 0.55 / 0.5"
        return FeatureScore("Brows", score, note) to detail
    }

    // ================= ANGULARITY (15%) =================

    private fun cheekbones(face: FaceGeo, cheekW: Float, jawW: Float): Pair<FeatureScore, String> {
        if (cheekW <= 0 || jawW <= 0) {
            return noRead("Cheekbones")
        }
        val ratio = cheekW / jawW
        val score = scoreFromDeviation(abs(ratio - 1.12f), 0.12f)
        val note = when {
            score >= 7 -> "Zygos sit visibly wider than the jaw — strong midface structure."
            score >= 5 -> "Average cheekbone projection."
            else -> "Flatter zygo area; leanness brings the most out here."
        }
        val detail = "zygo/jaw ${"%.2f".format(ratio)} · ideal ≈ 1.12"
        return FeatureScore("Cheekbones", score, note) to detail
    }

    /**
     * Jaw frontal angle: angle at the chin between the two jaw (gonion-proxy) points.
     * Community ideal ~84-95 deg for men (sharper = more angular).
     */
    private fun jawFrontalAngle(face: FaceGeo): Pair<FeatureScore, String> {
        val oval = face.contourPoints(FaceContour.FACE)
        if (oval.size < 12) return noRead("Jaw angle")
        val chinY = oval.maxOf { it.y }
        val topY = oval.minOf { it.y }
        val faceH = (chinY - topY).coerceAtLeast(1f)
        val jawY = chinY - faceH * 0.28f
        val jawPts = oval.filter { abs(it.y - jawY) < faceH * 0.06f }
        if (jawPts.size < 2) return noRead("Jaw angle")
        val chinX = oval.filter { it.y >= chinY - faceH * 0.02f }.map { it.x }.average().toFloat()
        val leftJaw = jawPts.minBy { it.x }
        val rightJaw = jawPts.maxBy { it.x }
        val v1x = leftJaw.x - chinX
        val v1y = leftJaw.y - chinY
        val v2x = rightJaw.x - chinX
        val v2y = rightJaw.y - chinY
        val mag = hypot(v1x, v1y) * hypot(v2x, v2y)
        if (mag <= 0f) return noRead("Jaw angle")
        val cos = ((v1x * v2x + v1y * v2y) / mag).coerceIn(-1f, 1f).toDouble()
        val angleDeg = Math.toDegrees(acos(cos)).toFloat()
        val score = scoreFromDeviation(abs(angleDeg - 89f), 8f)
        val note = when {
            score >= 7 -> "Sharp gonial angle — strong dimorphic read."
            score >= 5 -> "Jaw angularity is average."
            else -> "Gonial angle reads soft/round — leanness sharpens this more than anything."
        }
        val detail = "${angleDeg.toInt()}° · community ideal ≈ 84–95°"
        return FeatureScore("Jaw angle", score, note) to detail
    }

    // ================= SIDE PROFILE (15% bonus) =================

    private data class SideRead(val feature: FeatureScore, val detail: String, val ok: Boolean)

    /**
     * Side profile estimate from the most-turned profile photo: how far the chin
     * sits behind the nose tip, relative to face height. Small offset = straight
     * profile / good forward growth. Crude but directionally real.
     * Extras (gonial angle, chin-neck angle, Ricketts E-line note) go only into
     * the detail line — never new pillars.
     */
    private fun sideProfile(faces: List<Face>, primary: Face): SideRead {
        val noPhoto = SideRead(
            unreliable("Side profile").copy(note = "Profile photo wasn't clear enough to assess."),
            "needs a side photo",
            false
        )
        val profile = faces
            .filter { it !== primary && abs(it.headEulerAngleY) > 15f }
            .maxByOrNull { abs(it.headEulerAngleY) }
            ?: return noPhoto
        val contour = profile.contourPoints(FaceContour.FACE)
        if (contour.size < 12) return noPhoto
        val minY = contour.minOf { it.y }
        val maxY = contour.maxOf { it.y }
        val h = (maxY - minY).coerceAtLeast(1f)
        val midY = (minY + maxY) / 2f
        val centerX = contour.map { it.x }.average()
        // Nose tip: farthest point from face center in the middle vertical band.
        val band = contour.filter { abs(it.y - midY) < h * 0.22f }
        if (band.isEmpty()) return noPhoto
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
        val detail = buildString {
            append("chin offset ${"%.2f".format(dev)}h · ideal ≈ 0")
            gonialAngleDeg(contour, noseTip, chin, h)?.let { append(" · gonial ≈ ${it.toInt()}°") }
            chinNeckAngleDeg(contour, noseTip, chin, h)?.let { append(" · chin-neck ≈ ${it.toInt()}°") }
            rickettsLipNote(profile, noseTip, chin, centerX, h)?.let { append(" · $it") }
        }.toString()
        return SideRead(FeatureScore("Side profile", score, note), detail, true)
    }

    /**
     * Gonial angle from the side contour: angle at the jaw corner (gonion,
     * approximated as the most posterior-inferior lower-contour point) between
     * the chin and the ramus. Typical adult range ~120-135°. Null when the
     * contour can't support the read.
     */
    private fun gonialAngleDeg(
        contour: List<PointF>,
        noseTip: PointF,
        chin: PointF,
        h: Float
    ): Float? {
        val cx = contour.map { it.x }.average()
        val p = sign(noseTip.x - cx).toFloat() // +1 when posterior is +x
        if (p == 0f) return null
        val midY = (contour.minOf { it.y } + contour.maxOf { it.y }) / 2f
        val lower = contour.filter { it.y > midY }
        if (lower.size < 6) return null
        val gonion = lower.maxBy { (it.x - cx) * p + (it.y - midY) }
        val ramus = contour
            .filter { it.y < gonion.y - h * 0.04f && (it.x - cx) * p > 0 }
            .maxByOrNull { (it.x - cx) * p - abs(it.y - (gonion.y - h * 0.15f)) * 0.5f }
            ?: return null
        val v1x = chin.x - gonion.x
        val v1y = chin.y - gonion.y
        val v2x = ramus.x - gonion.x
        val v2y = ramus.y - gonion.y
        val mag = hypot(v1x, v1y) * hypot(v2x, v2y)
        if (mag <= 0f) return null
        val cos = ((v1x * v2x + v1y * v2y) / mag).coerceIn(-1f, 1f).toDouble()
        val deg = Math.toDegrees(acos(cos)).toFloat()
        return if (deg in 90f..170f) deg else null
    }

    /**
     * Chin-neck (mentocervical) angle: at the throat dip under the chin,
     * between the chin and the lower neck. Crude on-device estimate.
     */
    private fun chinNeckAngleDeg(
        contour: List<PointF>,
        noseTip: PointF,
        chin: PointF,
        h: Float
    ): Float? {
        val cx = contour.map { it.x }.average()
        val p = sign(noseTip.x - cx).toFloat() // +1 when posterior is +x
        if (p == 0f) return null
        val throat = contour
            .filter { (it.x - cx) * p > h * 0.02f && it.y > chin.y - h * 0.12f && it.y < chin.y + h * 0.25f }
            .maxByOrNull { (it.x - cx) * p }
            ?: return null
        val neckPt = contour
            .filter { (it.x - cx) * p > h * 0.02f && it.y > throat.y + h * 0.03f }
            .maxByOrNull { it.y }
            ?: return null
        val v1x = chin.x - throat.x
        val v1y = chin.y - throat.y
        val v2x = neckPt.x - throat.x
        val v2y = neckPt.y - throat.y
        val mag = hypot(v1x, v1y) * hypot(v2x, v2y)
        if (mag <= 0f) return null
        val cos = ((v1x * v2x + v1y * v2y) / mag).coerceIn(-1f, 1f).toDouble()
        val deg = Math.toDegrees(acos(cos)).toFloat()
        return if (deg in 60f..170f) deg else null
    }

    /**
     * Ricketts E-line: nose tip → chin line. Notes whether the lips sit behind
     * (typical) or ahead of the line. Crude on-device estimate.
     */
    private fun rickettsLipNote(
        profile: Face,
        noseTip: PointF,
        chin: PointF,
        centerX: Double,
        h: Float
    ): String? {
        val lips = profile.contourPoints(FaceContour.UPPER_LIP_TOP) +
            profile.contourPoints(FaceContour.LOWER_LIP_BOTTOM)
        if (lips.size < 4) return null
        val a = sign(noseTip.x - centerX).toFloat()
        if (a == 0f) return null
        val dx = chin.x - noseTip.x
        val dy = chin.y - noseTip.y
        val len = hypot(dx, dy).coerceAtLeast(1f)
        // Signed distance from the E-line; flip so positive = anterior.
        val anteriorPositive = (dy / len) * a >= 0
        val most = lips.maxOf { p ->
            val s = ((p.x - noseTip.x) * dy - (p.y - noseTip.y) * dx) / len
            if (anteriorPositive) s else -s
        }
        return if (most > h * 0.012f) "lips ahead of E-line" else "lips behind E-line"
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
        "Canthal tilt" to listOf(
            "Sleep 7-9 hours — rested eyes read less downturned" to "easy",
            "Thicker, straighter brows visually lift the eye area" to "easy"
        ),
        "Eye size (PFL)" to listOf(
            "PFL is bone — skip products claiming to enlarge eyes; brow density frames the area best" to "easy",
            "A healthy weight sharpens the eye area more than anything topical" to "medium"
        ),
        "Skin clarity" to listOf(
            "Wash your face twice daily and change the pillowcase weekly" to "easy",
            "Don't pick at skin — it scars; sunscreen daily protects texture long-term" to "easy"
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
    private fun lightingNote(face: FaceGeo, bitmap: Bitmap): String? {
        val box = face.box
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
    private fun confidenceOf(face: FaceGeo,
        angles: Int,
        badLight: Boolean,
        badDist: Boolean
    ): Pair<Double, Double> {
        var c = 0.95
        c -= (abs(face.eulerY) / 12f).coerceIn(0f, 1f) * 0.18
        c -= (abs(face.eulerX) / 12f).coerceIn(0f, 1f) * 0.15
        c -= (abs(face.eulerZ) / 10f).coerceIn(0f, 1f) * 0.12
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
    private fun collectMesh(face: FaceGeo,
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
        val box = face.box
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

    /** Forehead width from the face oval at brow level (for face-shape classification). */
    private fun foreheadWidth(face: FaceGeo): Float {
        val oval = face.contourPoints(FaceContour.FACE)
        val browPts = face.contourPoints(FaceContour.LEFT_EYEBROW_TOP) +
            face.contourPoints(FaceContour.RIGHT_EYEBROW_TOP)
        if (oval.isEmpty() || browPts.isEmpty()) return 0f
        val browY = browPts.map { it.y }.average().toFloat()
        val h = (oval.maxOf { it.y } - oval.minOf { it.y }).coerceAtLeast(1f)
        return widthOf(oval.filter { abs(it.y - browY) < h * 0.05f })
    }

    /**
     * Skin undertone from cheek pixels: warm/cool/neutral by red/blue channel
     * ratio. Crude — lighting shifts it; labeled as an estimate in the UI.
     */
    private fun skinUndertone(face: FaceGeo, bitmap: Bitmap): String {
        val pts = listOfNotNull(
            face.landmark(FaceLandmark.LEFT_CHEEK),
            face.landmark(FaceLandmark.RIGHT_CHEEK)
        )
        if (pts.isEmpty()) return ""
        val half = (face.box.height() / 16).coerceIn(6, 48)
        var rSum = 0L
        var bSum = 0L
        var n = 0L
        for (p in pts) {
            var dy = -half
            while (dy <= half) {
                var dx = -half
                while (dx <= half) {
                    val x = (p.x + dx).toInt().coerceIn(0, bitmap.width - 1)
                    val y = (p.y + dy).toInt().coerceIn(0, bitmap.height - 1)
                    val px = bitmap.getPixel(x, y)
                    rSum += ((px shr 16) and 0xFF)
                    bSum += (px and 0xFF)
                    n++
                    dx += 4
                }
                dy += 4
            }
        }
        if (n == 0L || bSum == 0L) return ""
        val ratio = rSum.toDouble() / bSum.toDouble()
        return when {
            ratio > 1.08 -> "Warm"
            ratio < 0.96 -> "Cool"
            else -> "Neutral"
        }
    }

    // ---------- report assembly ----------

    /** The 18 frontal measured features plus the dims other reads need. */
    private data class FrontalRead(
        val features: List<FeatureScore>,
        val details: Map<String, String>,
        val cheekW: Float,
        val faceH: Float,
        val jawW: Float
    )

    /** Photo-quality bits that don't depend on landmark positions. */
    private data class PhotoStatic(
        val photoNotes: List<String>,
        val confidence: Double,
        val uncertainty: Double,
        val anglesRead: Int
    )

    /**
     * Runs the 18 frontal measurements on any face geometry — the ML Kit read
     * or a user-corrected snapshot. Pure: same geometry in, same features out.
     */
    private fun frontalRead(geo: FaceGeo, bitmap: Bitmap): FrontalRead {
        val oval = geo.contourPoints(FaceContour.FACE)
        val lc = geo.landmark(FaceLandmark.LEFT_CHEEK)
        val rc = geo.landmark(FaceLandmark.RIGHT_CHEEK)
        val cheekW = if (lc != null && rc != null) dist(lc, rc) else 0f
        val faceH = if (oval.isNotEmpty()) oval.maxOf { it.y } - oval.minOf { it.y } else 0f
        val jawW = if (oval.isNotEmpty() && cheekW > 0) {
            val topY = oval.minOf { it.y }
            val chinY = oval.maxOf { it.y }
            val fh = (chinY - topY).coerceAtLeast(1f)
            val jawY = chinY - fh * 0.28f
            val jawPts = oval.filter { it.y >= jawY - fh * 0.06f && it.y <= jawY + fh * 0.06f }
            if (jawPts.size >= 2) widthOf(jawPts) else cheekW * 0.8f
        } else 0f

        // --- measureDetails: one "yours vs ideal" line per feature ---
        val details = mutableMapOf<String, String>()
        val feats = mutableListOf<FeatureScore>()
        fun reg(measured: Pair<FeatureScore, String>): FeatureScore {
            details[measured.first.name] = measured.second
            feats.add(measured.first)
            return measured.first
        }

        // --- 18 measured features ---
        val thirds = reg(facialThirds(geo))
        val fifths = reg(facialFifths(geo))
        val midface = reg(midfaceRatio(geo))
        val esr = reg(eyeSpacing(geo, cheekW))
        val fwhrV = reg(fwhr(geo, cheekW))
        val sym = reg(symmetry(geo))
        val eyesV = reg(eyes(geo))
        val canthalV = reg(canthalTilt(geo))
        val eyePflV = reg(eyeSizePfl(geo, cheekW))
        val noseV = reg(nose(geo, cheekW))
        val lipsV = reg(lips(geo))
        val skinV = reg(skinClarity(geo, bitmap))
        val jawV = reg(jawline(geo, cheekW))
        val chinV = reg(chin(geo, jawW))
        val browsV = reg(brows(geo))
        val cheekV = reg(cheekbones(geo, cheekW, jawW))
        val jawAngleV = reg(jawFrontalAngle(geo))

        return FrontalRead(feats.toList(), details.toMap(), cheekW, faceH, jawW)
    }

    /** Photo-quality checks (raters' #1 rule: rate the undistorted photo). */
    private fun photoStatic(geo: FaceGeo, bitmap: Bitmap, angles: Int): PhotoStatic {
        val photoNotes = mutableListOf<String>()
        if (abs(geo.eulerY) > 12f) {
            photoNotes += "Front photo was angled — phone at eye level, 6-8 ft back, face straight at the lens."
        }
        if (abs(geo.eulerX) > 12f) {
            photoNotes += "Chin was tilted up or down — keep the camera level with your eyes."
        }
        if (abs(geo.eulerZ) > 10f) {
            photoNotes += "Head was rolled to one side — keep it straight for a clean read."
        }
        if (angles < 3) {
            photoNotes += "Only $angles of 3 angles read clearly — retake the blurry one in good light."
        }
        // Lighting read from actual face-region pixels.
        val lightNote = lightingNote(geo, bitmap)
        if (lightNote != null) photoNotes += lightNote
        // Distance read from face size relative to the frame.
        val boxArea = geo.box.width().toFloat() * geo.box.height().toFloat()
        val imgArea = bitmap.width.toFloat() * bitmap.height.toFloat()
        val faceFrac = if (imgArea > 0) boxArea / imgArea else 0f
        val badDist = faceFrac < 0.08f || faceFrac > 0.65f
        if (faceFrac < 0.08f) {
            photoNotes += "Face was too small in the frame — shoot from 6-8 ft, not across the room."
        } else if (faceFrac > 0.65f) {
            photoNotes += "Face was too close — back up so the full head fits with margin."
        }
        val (confidence, uncertainty) = confidenceOf(geo, angles, lightNote != null, badDist)
        return PhotoStatic(photoNotes.toList(), confidence, uncertainty, angles)
    }

    /** Assembles the full report from frontal reads + side read + photo statics. */
    private fun assembleReport(
        frontal: FrontalRead,
        sideFeature: FeatureScore,
        sideDetail: String,
        sideOk: Boolean,
        geo: FaceGeo,
        bitmap: Bitmap,
        static: PhotoStatic
    ): PslReport {
        val details = frontal.details.toMutableMap()
        details[sideFeature.name] = sideDetail
        val features = frontal.features + sideFeature

        // --- v2.5 reads: face shape, skin undertone, canthal tilt degrees ---
        val (faceShape, faceShapeNote) =
            FaceShape.classifyFaceShape(frontal.cheekW, frontal.jawW, foreheadWidth(geo), frontal.faceH)
        val undertone = skinUndertone(geo, bitmap)
        val tiltDeg = canthalTiltDeg(geo)?.toDouble() ?: 0.0

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

        // --- Real measured mesh for the scan overlay ---
        val (mesh, faceBox, thirdsY) = collectMesh(geo, bitmap)

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
            "Overall ${"%.1f".format(overall)} PSL (±${"%.1f".format(static.uncertainty)}, ${confidenceLabel(static.confidence)}; " +
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
            anglesRead = static.anglesRead,
            decile = decile,
            percentile = percentile,
            pillars = pillars,
            photoNotes = static.photoNotes,
            failoCount = failos,
            haloCount = halos,
            landmarkMesh = mesh,
            faceBox = faceBox,
            thirdsY = thirdsY,
            confidence = static.confidence,
            uncertainty = static.uncertainty,
            potentialPsl = potentialPsl,
            // v2.5 fields — added to PslReport by the results chunk.
            faceShape = faceShape,
            faceShapeNote = faceShapeNote,
            measureDetails = details,
            skinUndertone = undertone,
            canthalTiltDeg = tiltDeg
        )
    }

    private fun buildReport(face: Face, faces: List<Face>, bitmap: Bitmap): PslReport {
        val geo: FaceGeo = MlKitFaceGeo(face)
        val frontal = frontalRead(geo, bitmap)
        // Side profile is a bonus read, never the base score.
        val side = sideProfile(faces, face)
        val static = photoStatic(geo, bitmap, faces.size)
        return assembleReport(frontal, side.feature, side.detail, side.ok, geo, bitmap, static)
    }

    /**
     * v2.6: re-runs the measurement pipeline on user-corrected landmark geometry.
     * Frontal features, pillars, PSL, mesh and the v2.5 reads are recomputed from
     * [corrected]; the side-profile bonus and photo-quality notes are kept from
     * the original scan (dragging points doesn't change the photo).
     */
    fun recompute(
        original: PslReport,
        corrected: FaceGeometrySnapshot,
        bitmap: Bitmap
    ): PslReport {
        val geo: FaceGeo = SnapshotFaceGeo(corrected)
        val frontal = frontalRead(geo, bitmap)
        val sideFeature = original.features.firstOrNull { it.name == "Side profile" }
            ?: FeatureScore("Side profile", 4.0, "Profile photo wasn't clear enough to assess.")
        val sideDetail = original.measureDetails["Side profile"] ?: "needs a side photo"
        val sideOk = sideDetail != "needs a side photo"
        val static = PhotoStatic(
            original.photoNotes, original.confidence, original.uncertainty, original.anglesRead
        )
        return assembleReport(frontal, sideFeature, sideDetail, sideOk, geo, bitmap, static)
            .copy(timestamp = original.timestamp)
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
