package com.kurupdevs.moggr.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.analysis.AnalysisUiState
import com.kurupdevs.moggr.analysis.PslReport
import com.kurupdevs.moggr.util.ProfileStore
import com.kurupdevs.moggr.util.UserProfile
import kotlinx.coroutines.delay
import java.util.Locale

// ---------- Analyzing ----------

private val ANALYZE_STEPS = listOf(
    "Mapping your facial geometry…",
    "Scoring every feature…",
    "Calculating your PSL tier…",
    "Building your ascension roadmap…"
)

@Composable
fun AnalyzingScreen() {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        repeat(ANALYZE_STEPS.size - 1) {
            delay(1100)
            step++
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PslBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Text(
            "Analyzing your face",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Mapping your facial geometry…",
            fontSize = 14.sp,
            color = PslGrey
        )
        Spacer(Modifier.height(40.dp))

        // PSL radar ring
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(190.dp)) {
            CircularProgressIndicator(
                progress = { (step + 1) / ANALYZE_STEPS.size.toFloat() },
                modifier = Modifier.size(190.dp),
                color = PslBlue,
                trackColor = Color(0xFF1E2A38),
                strokeWidth = 8.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PSL", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(
                    "${step + 1}/4",
                    fontSize = 14.sp,
                    color = PslBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(40.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = PslCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ANALYZE_STEPS.forEachIndexed { i, label ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when {
                            i < step -> Text("✓ ", color = PslBlue, fontWeight = FontWeight.Bold)
                            i == step -> CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PslBlue,
                                strokeWidth = 2.5.dp
                            )
                            else -> Text("○ ", color = Color(0xFF3A3A3A))
                        }
                        Spacer(Modifier.padding(4.dp))
                        Text(
                            label,
                            color = if (i <= step) Color.White else PslGrey,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// ---------- Results ----------

@Composable
fun ResultScreen(
    report: PslReport,
    profile: UserProfile?,
    onRescan: () -> Unit,
    onAskCoach: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PslBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "You Will Ascend By",
            fontSize = 13.sp,
            color = PslGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (profile != null) "${profile.name}'s PSL report" else "Your PSL report",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Read from ${report.anglesRead} of 3 angles · 100% on-device · free forever",
            fontSize = 12.sp,
            color = PslGrey
        )
        if (profile != null) {
            val age = ProfileStore.ageYears(profile.dobMillis)
            if (age in 1..20) {
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2A38)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "You're $age — your growth plates may still be open, so posture, " +
                            "sleep and habits move the needle even more for you right now.",
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        // Hero score
        Card(
            colors = CardDefaults.cardColors(containerColor = PslCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    String.format(Locale.US, "%.1f", report.overallPsl),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PslBlue
                )
                Text(
                    "≈ ${String.format(Locale.US, "%.1f", report.decile)}/10 decile · ~${ordinal(report.percentile)} percentile",
                    fontSize = 16.sp,
                    color = PslGrey,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                Text(pslLabel(report.overallPsl), fontSize = 15.sp, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${report.failoCount} failos · ${report.haloCount} halos",
                    fontSize = 12.sp,
                    color = PslGrey
                )
            }
        }

        if (report.photoNotes.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2113)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Photo check", fontWeight = FontWeight.SemiBold, color = Color(0xFFFBBF24), fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    report.photoNotes.forEach { n ->
                        Text("⚠ $n", fontSize = 13.sp, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

        if (report.pillars.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionTitle("The 4 Pillars")
            Text(
                "How experienced raters actually score a face — harmony first",
                fontSize = 13.sp,
                color = PslGrey
            )
            Spacer(Modifier.height(10.dp))
            report.pillars.forEach { p ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PslCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(p.name, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 16.sp)
                            Text(
                                String.format(Locale.US, "%.1f", p.score),
                                fontWeight = FontWeight.Bold,
                                color = scoreColor(p.score),
                                fontSize = 18.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (p.score / 8.0).toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = scoreColor(p.score),
                            trackColor = Color(0xFF2A2A2A)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(p.note, fontSize = 13.sp, color = PslGrey)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionTitle("Facial Geometry Analysis")
        Spacer(Modifier.height(10.dp))
        report.features.forEach { f ->
            Card(
                colors = CardDefaults.cardColors(containerColor = PslCard),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(f.name, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 16.sp)
                        Text(
                            String.format(Locale.US, "%.1f", f.score),
                            fontWeight = FontWeight.Bold,
                            color = scoreColor(f.score),
                            fontSize = 18.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (f.score / 8.0).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = scoreColor(f.score),
                        trackColor = Color(0xFF2A2A2A)
                    )
                    if (f.note.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(f.note, fontSize = 13.sp, color = PslGrey)
                    }
                }
            }
        }

        if (report.strengths.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionTitle("Halos")
            Text(
                "Your best features, in order of importance",
                fontSize = 13.sp,
                color = PslGrey
            )
            Spacer(Modifier.height(10.dp))
            report.strengths.forEach { s ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF12261A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(Modifier.padding(12.dp)) {
                        Text("✓ ", color = Color(0xFF4ADE80), fontWeight = FontWeight.Bold)
                        Text(s, color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }

        if (report.improvements.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionTitle("Your Ascension Roadmap")
            Text(
                "Concrete steps for your weakest features — no surgery, ever",
                fontSize = 13.sp,
                color = PslGrey
            )
            Spacer(Modifier.height(10.dp))
            report.improvements.forEach { im ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PslCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(im.area, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 15.sp)
                            EffortChip(im.effort)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(im.method, fontSize = 14.sp, color = PslGrey)
                    }
                }
            }
        }

        if (report.summary.isNotBlank()) {
            Spacer(Modifier.height(24.dp))
            SectionTitle("Summary")
            Spacer(Modifier.height(8.dp))
            Text(report.summary, color = Color.White, fontSize = 15.sp)
        }

        Spacer(Modifier.height(28.dp))
        // Bottom options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRescan,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Scan again", color = Color.White)
            }
            Button(
                onClick = { shareReport(context, profile, report) },
                colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Share report", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onAskCoach,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2A38)),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Ask Moggr Coach", color = PslBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Community-benchmark estimate, not a medical measurement — PSL ratios are " +
                "looksmaxxing-community conventions, not validated science. Scores come from " +
                "facial geometry measured on your phone — no photo ever leaves your device. " +
                "Same lighting, same angle = comparable results. " +
                "Free forever: no paywall, no unlock fees.",
            fontSize = 12.sp,
            color = PslGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
    }
}

private fun shareReport(context: android.content.Context, profile: UserProfile?, report: PslReport) {
    val top = report.features.sortedByDescending { it.score }.take(3)
        .joinToString(", ") { "${it.name} ${String.format(Locale.US, "%.1f", it.score)}" }
    val text = buildString {
        append("My Moggr PSL report: ")
        append(String.format(Locale.US, "%.1f", report.overallPsl))
        append(" PSL (≈")
        append(String.format(Locale.US, "%.1f", report.decile))
        append("/10, ")
        append(pslLabel(report.overallPsl))
        append("). Top features: ")
        append(top)
        append(". Full breakdown free on Moggr — no paywall.")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share your PSL report"))
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        color = PslBlue
    )
}

@Composable
private fun EffortChip(effort: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PslBlue.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            effort.replaceFirstChar { it.uppercase() },
            fontSize = 11.sp,
            color = PslBlue,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun scoreColor(score: Double): Color = when {
    score >= 7.0 -> Color(0xFF4ADE80)
    score >= 5.0 -> Color(0xFFFACC15)
    else -> Color(0xFFF87171)
}

fun pslLabel(psl: Double): String = when {
    psl >= 7.75 -> "Gigachad — near-mythical"
    psl >= 7.0 -> "Chad"
    psl >= 6.0 -> "Chadlite"
    psl >= 5.0 -> "HTN — High Tier Normie"
    psl >= 3.0 -> "MTN — Mid Tier Normie"
    psl >= 1.4 -> "LTN — Low Tier Normie"
    else -> "Sub-5 — maximum ascension potential"
}

private fun ordinal(n: Int): String = when {
    n % 100 in 11..13 -> "${n}th"
    n % 10 == 1 -> "${n}st"
    n % 10 == 2 -> "${n}nd"
    n % 10 == 3 -> "${n}rd"
    else -> "${n}th"
}

// ---------- Error ----------

@Composable
fun AnalysisErrorState(state: AnalysisUiState.Error, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PslBlack.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PslCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Analysis failed", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(state.message, textAlign = TextAlign.Center, fontSize = 14.sp, color = PslGrey)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Back to camera") }
            }
        }
    }
}
