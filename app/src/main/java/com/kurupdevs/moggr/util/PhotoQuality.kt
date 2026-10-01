package com.kurupdevs.moggr.util

import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.hypot

/**
 * On-device photo quality grading for face-scan captures.
 *
 * Runs entirely on the device: luma/contrast stats, Laplacian sharpness,
 * and a fast ML Kit face check (bounding box + head angles). No network,
 * no photo leaves the phone.
 *
 * IMPORTANT: [grade] blocks (ML Kit detection) — always call it off the
 * main thread (e.g. Dispatchers.Default).
 */
// v2.6-photogate begin
/** Machine-readable reason a photo failed the gate, paired with a per-issue fix line. */
enum class PhotoIssueCode {
    DARK, BRIGHT, FLAT_LIGHT, BLURRY, NO_FACE, TOO_FAR, TOO_CLOSE, OFF_CENTER, ANGLE
}

/** One failed quality check: what went wrong ([title]) and how to fix it ([fix]). */
data class PhotoIssue(
    val code: PhotoIssueCode,
    val title: String,
    val fix: String
)
// v2.6-photogate end

data class PhotoGrade(
    val score: Int,
    val issues: List<String>,
    val pass: Boolean,
    // v2.6-photogate begin
    val details: List<PhotoIssue> = emptyList()
    // v2.6-photogate end
)

object PhotoQuality {

    // v2.6-photogate begin
    private const val PASS_AT = 55 // hard-gate threshold: block bad photos, not decent ones
    // v2.6-photogate end

    // Luma/contrast bands (0-255).
    private const val DARK_BELOW = 60.0
    private const val BRIGHT_ABOVE = 200.0
    private const val FLAT_STD_BELOW = 22.0

    /**
     * Laplacian variance threshold for "blurry", measured on a ~256px grey
     * center crop. FaceAnalyzer has no Laplacian logic of its own — its
     * photo checks are Euler-angle based (12° yaw/pitch flags) — so this is
     * calibrated from the standard rule of thumb for this kernel/scale:
     * crisp phone selfies land well above 100, motion-blurred ones under ~50.
     */
    private const val BLUR_BELOW = 80.0

    private const val FACE_MIN_FRAC = 0.25   // face must cover >= 25% of the frame
    // v2.6-photogate begin
    private const val FACE_MAX_FRAC = 0.85   // face covering >= 85% of the frame = too close
    // v2.6-photogate end
    private const val OFF_CENTER_ABOVE = 0.4 // normalized distance of face center from frame center
    private const val ANGLE_ABOVE = 15f      // yaw/pitch degrees

    fun grade(bitmap: Bitmap): PhotoGrade {
        val issues = mutableListOf<String>()
        var score = 100
        // v2.6-photogate begin
        val details = mutableListOf<PhotoIssue>()
        fun flag(code: PhotoIssueCode, title: String, fix: String, penalty: Int) {
            issues += "$title — $fix"
            details += PhotoIssue(code, title, fix)
            score -= penalty
        }
        // v2.6-photogate end

        // --- luma + contrast on a small grey copy ---
        val (grey, gw, gh) = greyDownscale(bitmap, 256)
        var sum = 0.0
        var sumSq = 0.0
        for (v in grey) {
            sum += v
            sumSq += v * v
        }
        val n = grey.size.coerceAtLeast(1)
        val mean = sum / n
        val std = kotlin.math.sqrt((sumSq / n - mean * mean).coerceAtLeast(0.0))

        if (mean < DARK_BELOW) {
            // v2.6-photogate begin
            flag(PhotoIssueCode.DARK, "Lighting too dim", "face a window or step into brighter light", 25)
            // v2.6-photogate end
        } else if (mean > BRIGHT_ABOVE) {
            // v2.6-photogate begin
            flag(PhotoIssueCode.BRIGHT, "Lighting too harsh", "move out of direct sun — find softer light", 20)
            // v2.6-photogate end
        } else if (std < FLAT_STD_BELOW) {
            // v2.6-photogate begin
            flag(PhotoIssueCode.FLAT_LIGHT, "Flat lighting", "move near a window for more contrast", 12)
            // v2.6-photogate end
        }

        // --- sharpness: Laplacian variance on the center crop (face zone) ---
        val lapVar = laplacianVarianceCenter(grey, gw, gh)
        if (lapVar < BLUR_BELOW) {
            // v2.6-photogate begin
            flag(PhotoIssueCode.BLURRY, "Too blurry", "hold still for 1 second and try again", 30)
            // v2.6-photogate end
        }

        // --- face geometry via ML Kit (fast mode, no landmarks needed) ---
        val face = detectLargestFace(bitmap)
        if (face == null) {
            // v2.6-photogate begin
            flag(PhotoIssueCode.NO_FACE, "No face found", "retake with your face in the frame", 45)
            // v2.6-photogate end
        } else {
            val w = bitmap.width.toFloat()
            val h = bitmap.height.toFloat()
            val box = face.boundingBox
            val frac = (box.width() * box.height()) / (w * h)
            if (frac < FACE_MIN_FRAC) {
                // v2.6-photogate begin
                flag(PhotoIssueCode.TOO_FAR, "You're too far", "move a little closer", 20)
                // v2.6-photogate end
            // v2.6-photogate begin
            } else if (frac > FACE_MAX_FRAC) {
                flag(PhotoIssueCode.TOO_CLOSE, "Too close", "hold the phone at arm's length", 20)
            // v2.6-photogate end
            }
            val dx = (box.exactCenterX() - w / 2f) / (w / 2f)
            val dy = (box.exactCenterY() - h / 2f) / (h / 2f)
            if (hypot(dx, dy) > OFF_CENTER_ABOVE) {
                // v2.6-photogate begin
                flag(PhotoIssueCode.OFF_CENTER, "Face is off-center", "line your face up with the frame guides", 10)
                // v2.6-photogate end
            }
            if (abs(face.headEulerAngleY) > ANGLE_ABOVE || abs(face.headEulerAngleX) > ANGLE_ABOVE) {
                // v2.6-photogate begin
                flag(PhotoIssueCode.ANGLE, "Face not straight", "look directly at the camera", 10)
                // v2.6-photogate end
            }
        }

        val final = score.coerceIn(0, 100)
        // v2.6-photogate begin
        return PhotoGrade(final, issues, final >= PASS_AT, details)
        // v2.6-photogate end
    }

    // ---------- internals ----------

    private fun greyDownscale(src: Bitmap, maxW: Int): Triple<FloatArray, Int, Int> {
        val scale = (src.width / maxW.toFloat()).coerceAtLeast(1f)
        val w = (src.width / scale).toInt().coerceAtLeast(8)
        val h = (src.height / scale).toInt().coerceAtLeast(8)
        val small = Bitmap.createScaledBitmap(src, w, h, true)
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)
        if (small !== src) small.recycle()
        val grey = FloatArray(w * h)
        for (i in px.indices) {
            val p = px[i]
            grey[i] = 0.299f * ((p shr 16) and 0xFF) +
                0.587f * ((p shr 8) and 0xFF) +
                0.114f * (p and 0xFF)
        }
        return Triple(grey, w, h)
    }

    /** Variance of the 4-neighbour Laplacian response over the center 60% of the frame. */
    private fun laplacianVarianceCenter(g: FloatArray, w: Int, h: Int): Double {
        val x0 = (w * 0.2f).toInt().coerceAtLeast(1)
        val x1 = (w * 0.8f).toInt().coerceAtMost(w - 1)
        val y0 = (h * 0.2f).toInt().coerceAtLeast(1)
        val y1 = (h * 0.8f).toInt().coerceAtMost(h - 1)
        var sum = 0.0
        var sumSq = 0.0
        var n = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                val i = y * w + x
                val v = (g[i - 1] + g[i + 1] + g[i - w] + g[i + w] - 4f * g[i]).toDouble()
                sum += v
                sumSq += v * v
                n++
            }
        }
        if (n == 0) return 0.0
        val mean = sum / n
        return (sumSq / n - mean * mean).coerceAtLeast(0.0)
    }

    private fun detectLargestFace(bitmap: Bitmap): Face? {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
        val detector = FaceDetection.getClient(options)
        return try {
            awaitTask(detector.process(InputImage.fromBitmap(bitmap, 0)), 8000)
                ?.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
        } catch (_: Exception) {
            null
        } finally {
            try {
                detector.close()
            } catch (_: Exception) {
            }
        }
    }

    /** Blocks the calling thread until the ML Kit Task completes (or times out). */
    private fun <T> awaitTask(task: Task<T>, timeoutMs: Long): T? {
        val latch = CountDownLatch(1)
        var result: T? = null
        var failure: Exception? = null
        task.addOnSuccessListener { r -> result = r; latch.countDown() }
            .addOnFailureListener { e -> failure = e; latch.countDown() }
        latch.await(timeoutMs, TimeUnit.MILLISECONDS)
        failure?.let { throw it }
        return result
    }
}
