package com.kurupdevs.moggr.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.kurupdevs.moggr.analysis.PslReport
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqLine
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.MogCoral

/** Animated 0–100 trait meters shown in the report body. */
@Composable
fun TraitMeters(report: PslReport) {
    Column(modifier = Modifier.fillMaxWidth()) {
        EqSectionLabel("Trait meters")
        Spacer(Modifier.height(10.dp))

        MeterRow("Overall", (report.overallPsl / 8.0 * 100).toFloat())

        val potentialPct = minOf(
            100f,
            (report.overallPsl / 8.0 * 100 + 10 + report.haloCount * 2).toFloat()
        )
        MeterRow(
            label = "Potential",
            pct = potentialPct,
            caption = "estimated ceiling with consistent softmaxxing — not a promise"
        )

        dimorphismPct(report)?.let { MeterRow("Dimorphism", it) }
        traitPct(report, "Jawline", "Jaw width")?.let { MeterRow("Jawline", it) }
        traitPct(report, "Cheekbones")?.let { MeterRow("Cheekbones", it) }
        // Only shown when the measure chunk actually scores skin.
        traitPct(report, "Skin clarity")?.let { MeterRow("Skin", it) }
    }
}

@Composable
private fun MeterRow(label: String, pct: Float, caption: String? = null) {
    val anim by animateFloatAsState(
        targetValue = pct.coerceIn(0f, 100f) / 100f,
        animationSpec = tween(durationMillis = 900),
        label = "meter_$label"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                color = EqInk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${(anim * 100).toInt()}",
                color = EqInk,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        // v2.7 reskin: greige track, coral fill.
        LinearProgressIndicator(
            progress = { anim },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MogCoral,
            trackColor = EqLine
        )
        if (caption != null) {
            Spacer(Modifier.height(4.dp))
            Text(caption, fontSize = 11.sp, color = EqMuted)
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** First present feature among the given names, as a 0–100 percentage. Null if none measured. */
private fun traitPct(report: PslReport, vararg names: String): Float? {
    val f = names.firstNotNullOfOrNull { n -> report.features.find { it.name == n } }
        ?: return null
    return (f.score / 8.0 * 100).toFloat()
}

/** Dimorphism = average of the masculine-structure features that were measured. */
private fun dimorphismPct(report: PslReport): Float? {
    val scores = listOf("FWHR", "Brows", "Jawline", "Chin")
        .mapNotNull { n -> report.features.find { it.name == n }?.score }
    if (scores.isEmpty()) return null
    return (scores.average() / 8.0 * 100).toFloat()
}
