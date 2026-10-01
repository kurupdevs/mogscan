package com.kurupdevs.moggr.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.kurupdevs.moggr.analysis.PslReport
import java.io.File
import java.util.Locale

private const val CARD_W = 1080
private const val CARD_H = 1920
private const val CREAM: Int = 0xFFE9E2D6.toInt() // v2.7 reskin: greige wash to match the app skin
private const val INK: Int = 0xFF1C1917.toInt()
private const val CORAL: Int = 0xFFE07856.toInt()
private const val GREY: Int = 0xFF78716C.toInt()

/**
 * Renders the shareable 9:16 face card with android.graphics.Canvas
 * (not Compose) and shares it as a PNG via FileProvider. No new permissions.
 */
fun shareFaceCard(context: Context, report: PslReport) {
    val bmp = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    drawFaceCard(canvas, report)
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, "facecard-${System.currentTimeMillis()}.png")
    file.outputStream().use { out ->
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    bmp.recycle()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share your face card"))
}

private fun drawFaceCard(c: android.graphics.Canvas, report: PslReport) {
    val cx = CARD_W / 2f
    c.drawColor(CREAM)

    fun paint(size: Float, color: Int, style: Int = Typeface.NORMAL): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.SERIF, style)
            textAlign = Paint.Align.CENTER
        }

    // Wordmark
    val wordmark = paint(84f, INK, Typeface.BOLD).apply { letterSpacing = 0.35f }
    c.drawText("MOGGR", cx, 220f, wordmark)

    // Coral rule
    val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CORAL }
    c.drawRect(cx - 90f, 262f, cx + 90f, 272f, rule)

    // Score block
    c.drawText("YOUR PSL SCORE", cx, 400f, paint(42f, GREY).apply { letterSpacing = 0.2f })
    c.drawText(
        String.format(Locale.US, "%.1f", report.overallPsl),
        cx, 660f,
        paint(300f, INK, Typeface.BOLD)
    )
    c.drawText(
        pslLabel(report.overallPsl),
        cx, 770f,
        paint(58f, CORAL, Typeface.ITALIC)
    )
    val topPct = if (report.decile > 0) (100 - (report.decile * 10)).toInt()
    else (100 - report.percentile).coerceIn(1, 99)
    c.drawText("Top $topPct%", cx, 860f, paint(52f, INK, Typeface.BOLD))

    // Divider
    val div = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GREY; alpha = 90 }
    c.drawRect(140f, 950f, CARD_W - 140f, 953f, div)

    // Top traits
    c.drawText("TOP TRAITS", cx, 1050f, paint(44f, GREY).apply { letterSpacing = 0.2f })
    val traits = report.topTraits
    traits.forEachIndexed { i, t ->
        val y = 1160f + i * 120f
        val namePaint = paint(54f, INK).apply { textAlign = Paint.Align.LEFT }
        val scorePaint = paint(54f, INK, Typeface.BOLD).apply { textAlign = Paint.Align.RIGHT }
        c.drawText(t.name, 140f, y, namePaint)
        c.drawText(String.format(Locale.US, "%.1f", t.score), CARD_W - 140f, y, scorePaint)
    }
    if (traits.isEmpty()) {
        c.drawText("Scan again to fill this in", cx, 1160f, paint(48f, GREY, Typeface.ITALIC))
    }

    // Face shape line, when measured
    if (report.faceShape.isNotBlank()) {
        c.drawText(
            "Face shape: ${report.faceShape}",
            cx, 1560f,
            paint(46f, GREY, Typeface.ITALIC)
        )
    }

    // Footer watermark
    c.drawText(
        "Moggr · measured on-device · not medical advice",
        cx, 1820f,
        paint(36f, GREY)
    )
}
