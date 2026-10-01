package com.kurupdevs.moggr.ui

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.analysis.FaceAnalyzer
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.MogCoral
import com.kurupdevs.moggr.ui.theme.MogCoralDark
import kotlin.math.hypot

/**
 * v2.6 "Verify the AI's points": the scan's landmark markers drawn over the
 * user's photo on a Canvas. Every marker is draggable (generous 24dp touch
 * targets); dragging updates the point, and "Recalculate" feeds the corrected
 * geometry back into the analyzer for a fresh measurement + PSL.
 *
 * Honest framing throughout: the markers were placed by the on-device scan —
 * this screen only lets the user nudge the ones that missed.
 */

private fun initialPositions(
    photoW: Int,
    photoH: Int,
    snap: FaceAnalyzer.FaceGeometrySnapshot
): Map<String, Offset> {
    val w = photoW.toFloat().coerceAtLeast(1f)
    val h = photoH.toFloat().coerceAtLeast(1f)
    val m = mutableMapOf<String, Offset>()
    snap.contours.forEach { (type, pts) ->
        pts.forEachIndexed { i, p ->
            m["c$type:$i"] = Offset((p.x / w).coerceIn(0f, 1f), (p.y / h).coerceIn(0f, 1f))
        }
    }
    snap.marks.forEach { (type, p) ->
        m["m$type"] = Offset((p.x / w).coerceIn(0f, 1f), (p.y / h).coerceIn(0f, 1f))
    }
    return m
}

private fun correctedSnapshot(
    photoW: Int,
    photoH: Int,
    snap: FaceAnalyzer.FaceGeometrySnapshot,
    pts: Map<String, Offset>
): FaceAnalyzer.FaceGeometrySnapshot {
    val w = photoW.toFloat().coerceAtLeast(1f)
    val h = photoH.toFloat().coerceAtLeast(1f)
    fun toPx(o: Offset) = PointF(
        (o.x * w).coerceIn(0f, w),
        (o.y * h).coerceIn(0f, h)
    )
    val contours = snap.contours.mapValues { (type, list) ->
        list.mapIndexed { i, p ->
            pts["c$type:$i"]?.let { toPx(it) } ?: PointF(p.x, p.y)
        }
    }
    val marks = snap.marks.mapValues { (type, p) ->
        pts["m$type"]?.let { toPx(it) } ?: PointF(p.x, p.y)
    }
    return snap.copy(contours = contours, marks = marks)
}

@Composable
fun VerifyPointsScreen(
    photo: Bitmap,
    snapshot: FaceAnalyzer.FaceGeometrySnapshot,
    busy: Boolean,
    onCancel: () -> Unit,
    onApply: (FaceAnalyzer.FaceGeometrySnapshot) -> Unit
) {
    val bw = photo.width.coerceAtLeast(1)
    val bh = photo.height.coerceAtLeast(1)
    // SnapshotStateMap: stable reference, so the drag gesture always hit-tests
    // the latest positions (a plain state-held Map would go stale mid-gesture).
    val pts = remember(snapshot) {
        mutableStateMapOf<String, Offset>().apply { putAll(initialPositions(bw, bh, snapshot)) }
    }
    var dirty by remember(snapshot) { mutableStateOf(false) }
    var activeKey by remember { mutableStateOf<String?>(null) }

    // Draw order: face oval first, then brows / eyes / nose / mouth, marks last.
    val contourGroups = remember(snapshot) {
        snapshot.contours.entries.sortedBy { FaceAnalyzer.contourKind(it.key) }
    }
    val markEntries = remember(snapshot) { snapshot.marks.entries.toList() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(eqBackgroundBrush())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = EqMuted, fontSize = 15.sp)
            }
            Spacer(Modifier.weight(1f))
            if (dirty) {
                TextButton(
                    onClick = {
                        pts.clear()
                        pts.putAll(initialPositions(bw, bh, snapshot))
                        dirty = false
                        activeKey = null
                    }
                ) {
                    Text("Undo changes", color = MogCoralDark, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        EqSectionLabel("Verify the AI's points")
        Spacer(Modifier.height(6.dp))
        EqHeadline("Check the markers", size = 26, color = EqInk)
        Spacer(Modifier.height(4.dp))
        EqBody(
            "The scan placed these markers itself — drag any that missed the mark, then recalculate.",
            size = 13
        )
        Spacer(Modifier.height(12.dp))

        // No scroll here: the drag gesture owns all touch inside the editor.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(bw.toFloat() / bh.toFloat())
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Image(
                    bitmap = photo.asImageBitmap(),
                    contentDescription = "Your scan photo with landmark markers",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(busy) {
                        if (busy) return@pointerInput
                        detectDragGestures(
                            onDragStart = { down ->
                                val hitR = 24.dp.toPx()
                                var best: String? = null
                                var bestD = hitR
                                pts.forEach { (k, o) ->
                                    val px = Offset(o.x * size.width, o.y * size.height)
                                    val d = hypot(px.x - down.x, px.y - down.y)
                                    if (d <= bestD) {
                                        bestD = d
                                        best = k
                                    }
                                }
                                activeKey = best
                            },
                            onDragEnd = { activeKey = null },
                            onDragCancel = { activeKey = null },
                            onDrag = { change, _ ->
                                val key = activeKey
                                if (key != null) {
                                    change.consume()
                                    val nx = (change.position.x / size.width).coerceIn(0f, 1f)
                                    val ny = (change.position.y / size.height).coerceIn(0f, 1f)
                                    pts[key] = Offset(nx, ny)
                                    dirty = true
                                }
                            }
                        )
                    }
            ) {
                val cw = size.width
                val ch = size.height
                fun map(o: Offset) = Offset(o.x * cw, o.y * ch)
                // v2.7 reskin: markers use the brand coral; drag logic untouched.
                val coral = MogCoral
                // faint contour polylines so the face structure reads at a glance
                contourGroups.forEach { (type, list) ->
                    val mapped = list.mapIndexed { i, _ ->
                        pts["c$type:$i"]?.let { map(it) }
                    }.filterNotNull()
                    if (mapped.size >= 2) {
                        for (i in mapped.indices) {
                            val a = mapped[i]
                            val b = mapped[(i + 1) % mapped.size]
                            // brows and nose stay open; the rest read as closed loops
                            val kind = FaceAnalyzer.contourKind(type)
                            if (i < mapped.size - 1 || (kind != 2 && kind != 3)) {
                                drawLine(coral.copy(alpha = 0.30f), a, b, 1.5.dp.toPx())
                            }
                        }
                    }
                }
                // markers
                fun drawMarker(center: Offset, big: Boolean) {
                    if (big) {
                        drawCircle(coral.copy(alpha = 0.25f), 24.dp.toPx() / 2f, center)
                    }
                    drawCircle(Color.White, (if (big) 7.dp else 4.5.dp).toPx(), center)
                    drawCircle(coral, (if (big) 5.5.dp else 3.2.dp).toPx(), center)
                }
                contourGroups.forEach { (type, list) ->
                    list.forEachIndexed { i, _ ->
                        val o = pts["c$type:$i"] ?: return@forEachIndexed
                        drawMarker(map(o), activeKey == "c$type:$i")
                    }
                }
                markEntries.forEach { (type, _) ->
                    val o = pts["m$type"] ?: return@forEach
                    drawMarker(map(o), activeKey == "m$type")
                }
            }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "${pts.size} markers · drag with your finger — targets are fingertip-sized",
            fontSize = 12.sp,
            color = EqMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        // v2.7 reskin: coral pill CTA matching EqCoralPillButton's look, with the
        // busy spinner kept; enable logic identical to the old Button.
        val ctaEnabled = dirty && !busy
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(if (ctaEnabled) MogCoral else EqMuted.copy(alpha = 0.35f))
                .clickable(enabled = ctaEnabled) { onApply(correctedSnapshot(bw, bh, snapshot, pts)) }
                .padding(horizontal = 26.dp, vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            if (busy) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Recalculating…", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                Text(
                    if (dirty) "Recalculate my score" else "Move a marker to recalculate",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Recalculating re-runs the same on-device measurements on your corrected points — nothing leaves your phone.",
            fontSize = 12.sp,
            color = EqMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
    }
}
