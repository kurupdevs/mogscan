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
data class PhotoGrade(
    val score: Int,
    val issues: List<String>,
    val pass: Boolean
)

object PhotoQuality {

    private const val PASS_AT = 65

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
    private const val OFF_CENTER_ABOVE = 0.4 // normalized distance of face center from frame center
    private const val ANGLE_ABOVE = 15f      // yaw/pitch degrees

    fun grade(bitmap: Bitmap): PhotoGrade {
        val issues = mutableListOf<String>()
        var score = 100

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
            score -= 25
            issues += "Too dark — face a window"
        } else if (mean > BRIGHT_ABOVE) {
            score -= 20
            issues += "Too bright — harsh light on your face"
        } else if (std < FLAT_STD_BELOW) {
            score -= 12
            issues += "Flat light — needs more contrast"
        }

        // --- sharpness: Laplacian variance on the center crop (face zone) ---
        val lapVar = laplacianVarianceCenter(grey, gw, gh)
        if (lapVar < BLUR_BELOW) {
            score -= 30
            issues += "Hold still — photo is blurry"
        }

        // --- face geometry via ML Kit (fast mode, no landmarks needed) ---
        val face = detectLargestFace(bitmap)
        if (face == null) {
            score -= 45
            issues += "No face found — retake with your face in frame"
        } else {
            val w = bitmap.width.toFloat()
            val h = bitmap.height.toFloat()
            val box = face.boundingBox
            val frac = (box.width() * box.height()) / (w * h)
            if (frac < FACE_MIN_FRAC) {
                score -= 20
                issues += "Move closer"
            }
            val dx = (box.exactCenterX() - w / 2f) / (w / 2f)
            val dy = (box.exactCenterY() - h / 2f) / (h / 2f)
            if (hypot(dx, dy) > OFF_CENTER_ABOVE) {
                score -= 10
                issues += "Center your face in the frame"
            }
            if (abs(face.headEulerAngleY) > ANGLE_ABOVE || abs(face.headEulerAngleX) > ANGLE_ABOVE) {
                score -= 10
                issues += "Look straight at the camera"
            }
        }

        val final = score.coerceIn(0, 100)
        return PhotoGrade(final, issues, final >= PASS_AT)
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
