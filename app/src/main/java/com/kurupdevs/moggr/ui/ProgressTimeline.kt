package com.kurupdevs.moggr.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.EqPeach
import com.kurupdevs.moggr.ui.theme.EqRose
import com.kurupdevs.moggr.ui.theme.EqSage
import com.kurupdevs.moggr.ui.theme.MogCoral
import com.kurupdevs.moggr.util.ScanHistoryStore
import com.kurupdevs.moggr.util.ScanHistoryStore.ScanEntry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Progress timeline: PSL-over-time graph, per-feature deltas vs the previous
 * scan, a before/after photo slider, and a rescan nudge when the latest scan
 * is stale. Reads only on-device history. Embedded by the routine chunk.
 */
@Composable
fun ProgressTimeline() {
    val context = LocalContext.current
    val history = remember { ScanHistoryStore.load(context) }

    Column(modifier = Modifier.fillMaxWidth()) {
        EqSectionLabel("PROGRESS TIMELINE")
        Spacer(Modifier.height(10.dp))

        if (history.isEmpty()) {
            EqGlassCard(modifier = Modifier.fillMaxWidth(), corner = EqRoundSm) {
                Text(
                    "No scan history yet — your next scans will build a timeline here.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = PslGrey,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return
        }

        PslLineGraph(history)
        Spacer(Modifier.height(14.dp))
        DeltaChips(history)
        Spacer(Modifier.height(14.dp))
        BeforeAfterSlider(history)
        Spacer(Modifier.height(14.dp))
        RescanNudge(history.first())
    }
}

// ---------- PSL line graph ----------

@Composable
private fun PslLineGraph(history: List<ScanEntry>) {
    val ordered = history.sortedBy { it.timestamp }
    val dateFmt = remember { SimpleDateFormat("MM/dd", Locale.US) }
    val density = LocalDensity.current

    EqGlassCard(modifier = Modifier.fillMaxWidth(), corner = EqRoundSm) {
        Column {
            Text("PSL over time", fontFamily = EqSerif, fontSize = 17.sp, color = PslText)
            Spacer(Modifier.height(4.dp))
            Text(
                "${ordered.size} scan${if (ordered.size == 1) "" else "s"} · latest ${String.format(Locale.US, "%.1f", ordered.last().psl)}",
                fontSize = 12.sp,
                color = PslGrey
            )
            Spacer(Modifier.height(8.dp))
            BoxWithConstraints(modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)) {
                val wPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                val hPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                val padL = with(density) { 34.dp.toPx() }
                val padB = with(density) { 22.dp.toPx() }
                val padT = with(density) { 10.dp.toPx() }
                val padR = with(density) { 12.dp.toPx() }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    fun x(i: Int): Float =
                        if (ordered.size == 1) padL + (wPx - padL - padR) / 2f
                        else padL + (wPx - padL - padR) * i / (ordered.size - 1)

                    fun y(psl: Double): Float {
                        val t = ((psl.coerceIn(1.0, 8.0) - 1.0) / 7.0).toFloat()
                        return padT + (1f - t) * (hPx - padT - padB)
                    }

                    // gridlines at 2/4/6/8
                    val gridPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#8A8177")
                        alpha = 120
                        textSize = with(density) { 10.sp.toPx() }
                        isAntiAlias = true
                    }
                    listOf(2.0, 4.0, 6.0, 8.0).forEach { g ->
                        drawLine(
                            EqMuted.copy(alpha = 0.35f),
                            Offset(padL, y(g)), Offset(wPx - padR, y(g)),
                            1.dp.toPx()
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            g.toInt().toString(), 4.dp.toPx(), y(g) + 4.dp.toPx(), gridPaint
                        )
                    }

                    // line + dots
                    val coral = MogCoral
                    if (ordered.size == 1) {
                        drawCircle(coral, 5.dp.toPx(), Offset(x(0), y(ordered[0].psl)))
                    } else {
                        for (i in 0 until ordered.size - 1) {
                            drawLine(
                                coral,
                                Offset(x(i), y(ordered[i].psl)),
                                Offset(x(i + 1), y(ordered[i + 1].psl)),
                                3.dp.toPx()
                            )
                        }
                        ordered.forEachIndexed { i, e ->
                            drawCircle(coral, 4.dp.toPx(), Offset(x(i), y(e.psl)))
                            drawCircle(Color.White, 2.dp.toPx(), Offset(x(i), y(e.psl)))
                        }
                    }

                    // date labels: first + last
                    val labelPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#8A8177")
                        textSize = with(density) { 10.sp.toPx() }
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        dateFmt.format(Date(ordered.first().timestamp)),
                        padL, hPx - 6.dp.toPx(), labelPaint
                    )
                    if (ordered.size > 1) {
                        val lastLabel = dateFmt.format(Date(ordered.last().timestamp))
                        drawContext.canvas.nativeCanvas.drawText(
                            lastLabel,
                            wPx - padR - lastLabel.length * with(density) { 10.sp.toPx() } * 0.55f,
                            hPx - 6.dp.toPx(), labelPaint
                        )
                    }
                }
            }
        }
    }
}

// ---------- Delta chips vs previous scan ----------

@Composable
private fun DeltaChips(history: List<ScanEntry>) {
    if (history.size < 2) {
        Text(
            "Scan again to see what moved — deltas show up from your second scan.",
            fontSize = 13.sp,
            color = PslGrey
        )
        return
    }
    val latest = history[0]
    val prev = history[1]
    val deltas = latest.features.mapNotNull { (name, score) ->
        val old = prev.features[name] ?: return@mapNotNull null
        val d = score - old
        if (abs(d) < 0.005) null else name to d
    }.sortedByDescending { abs(it.second) }.take(6)

    Column {
        Text("Since last scan", fontFamily = EqSerif, fontSize = 17.sp, color = PslText)
        Spacer(Modifier.height(8.dp))
        if (deltas.isEmpty()) {
            Text("No movement since last scan — same scores across the board.", fontSize = 13.sp, color = PslGrey)
        } else {
            deltas.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (name, d) ->
                        DeltaChip(name, d, Modifier.weight(1f))
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DeltaChip(name: String, delta: Double, modifier: Modifier = Modifier) {
    val up = delta > 0
    val bg = if (up) EqSage else EqRose
    val arrow = if (up) "▲" else "▼"
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "$arrow ${String.format(Locale.US, "%.1f", abs(delta))}",
                fontFamily = EqSerif,
                fontSize = 16.sp,
                color = PslText
            )
            Text(name, fontSize = 11.sp, color = PslGrey, textAlign = TextAlign.Center)
        }
    }
}

// ---------- Before / after slider ----------

@Composable
private fun BeforeAfterSlider(history: List<ScanEntry>) {
    val withPhotos = remember(history) {
        history.filter { it.photoPath != null && File(it.photoPath).exists() }
    }
    val after = withPhotos.firstOrNull()
    val before = withPhotos.lastOrNull()

    Column {
        Text("Before / after", fontFamily = EqSerif, fontSize = 17.sp, color = PslText)
        Spacer(Modifier.height(8.dp))
        if (after == null || before == null || before.timestamp == after.timestamp) {
            EqGlassCard(modifier = Modifier.fillMaxWidth(), corner = EqRoundSm) {
                Text(
                    "Scan photos from two different days will show up here for a side-by-side.",
                    color = PslGrey,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            return
        }

        val beforeBmp = remember(before.photoPath) { BitmapFactory.decodeFile(before.photoPath) }
        val afterBmp = remember(after.photoPath) { BitmapFactory.decodeFile(after.photoPath) }
        if (beforeBmp == null || afterBmp == null) {
            Text("Couldn't load the saved photos for comparison.", fontSize = 13.sp, color = PslGrey)
            return
        }

        var frac by remember { mutableFloatStateOf(0.5f) }
        val density = LocalDensity.current
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(18.dp))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, dx ->
                        change.consume()
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        frac = ((frac * w + dx) / w).coerceIn(0.05f, 0.95f)
                    }
                }
        ) {
            val wPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            // After (right side, full-bleed underneath)
            Image(
                bitmap = afterBmp.asImageBitmap(),
                contentDescription = "Latest scan photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Before (left side, clipped to the divider)
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac)
                    .fillMaxHeight()
                    .clipToBounds()
            ) {
                Image(
                    bitmap = beforeBmp.asImageBitmap(),
                    contentDescription = "Earlier scan photo",
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(with(density) { wPx.toDp() }),
                    contentScale = ContentScale.Crop
                )
            }
            // Divider + handle
            Box(
                modifier = Modifier
                    .offset { IntOffset((frac * wPx).roundToInt(), 0) }
                    .fillMaxHeight()
                    .width(3.dp)
                    .background(Color.White)
            )
            Box(
                modifier = Modifier
                    .offset {
                        val half = with(density) { 22.dp.toPx() }.roundToInt()
                        IntOffset((frac * wPx).roundToInt() - half, 0)
                    }
                    .align(Alignment.CenterStart)
                    .size(44.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("↔", fontSize = 18.sp, color = PslText, fontWeight = FontWeight.Bold)
            }
            // Labels
            Text(
                "BEFORE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Text(
                "AFTER",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text("Drag the divider to compare", fontSize = 12.sp, color = PslGrey, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

// ---------- Rescan nudge ----------

@Composable
private fun RescanNudge(latest: ScanEntry) {
    val days = (System.currentTimeMillis() - latest.timestamp) / 86_400_000L
    if (days <= 14) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(EqRoundSm))
            .background(EqPeach)
            .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(EqRoundSm))
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "It's been $days days",
                    fontFamily = EqSerif,
                    color = PslText,
                    fontSize = 17.sp
                )
                Spacer(Modifier.height(4.dp))
                EqBody(
                    "Grab a fresh scan to keep the timeline honest — same lighting, same angle.",
                    size = 13
                )
            }
        }
    }
}
