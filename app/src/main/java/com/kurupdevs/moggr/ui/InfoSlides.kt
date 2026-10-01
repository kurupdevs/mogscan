package com.kurupdevs.moggr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqLine
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.MogCoral

private data class HBar(val label: String, val pct: Int)

/**
 * Educational onboarding slides shown after Get Started, before the questionnaire.
 * Original copy and visuals — same beats as the genre, none of the source artwork.
 */
@Composable
fun InfoSlidesScreen(onDone: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    val total = 6

    Column(
        Modifier
            .fillMaxSize()
            .background(eqBackgroundBrush())
            .padding(20.dp)
    ) {
        // Top visual card
        EqGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when (index) {
                    0 -> HaloVisual()
                    1 -> BarsVisual(
                        title = "LOOKS INFLUENCE EVERYTHING",
                        bars = listOf(
                            HBar("Dating", 80),
                            HBar("Popularity", 65),
                            HBar("Career", 35),
                            HBar("Income", 30)
                        )
                    )
                    2 -> EmblemVisual(top = "TOP 20%", bottom = "gets most of the attention")
                    3 -> BarsVisual(
                        title = "HOW COUPLES MEET TODAY",
                        bars = listOf(
                            HBar("Dating apps", 61),
                            HBar("Via friends", 14),
                            HBar("At work", 9),
                            HBar("At a bar", 5)
                        )
                    )
                    4 -> LikesVisual()
                    else -> EmblemVisual(top = "LOOKS =", bottom = "the highest leverage")
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Bottom card
        EqGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val (cardTitle, cardText) = when (index) {
                    0 -> "Perception Is\nEverything" to
                        "It's human nature — the halo effect means you're treated differently based on how you look."
                    1 -> "The Brutal\nTruth" to
                        "Most people don't realize how much attractiveness quietly shapes their life."
                    2 -> "The Loneliness\nEpidemic" to
                        "Social media forces everyone to compete with the top 20%. Not close? You're left behind."
                    3 -> "Modern\nDating World" to
                        "Most couples meet on dating apps today — and looks are the first filter."
                    4 -> "The Dating\nMarket Reality" to
                        "Without standing out, it's an uphill battle. A small top slice gets almost all the attention."
                    else -> "The Highest\nLeverage Solution" to
                        "For attraction, it was never money or status first — looks spark primal desire. Let's measure yours."
                }
                EqHeadline(cardTitle, size = 30, align = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                Text(
                    cardText,
                    fontSize = 15.sp,
                    color = EqMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                Spacer(Modifier.height(24.dp))
                EqCoralPillButton(
                    text = "Next",
                    onClick = { if (index < total - 1) index++ else onDone() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )
            }
        }
    }
}

@Composable
private fun HaloVisual() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "THE HALO EFFECT",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = EqInk,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "People assume attractive people are smarter, kinder and more successful.",
            fontSize = 13.sp,
            color = EqMuted,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HaloCard(mark = "✓", markColor = Color(0xFF12B76A), label = "attractive", sub = "seen as smart\nkind · successful")
            HaloCard(mark = "✕", markColor = Color(0xFFF04438), label = "unattractive", sub = "assumed\nthe opposite")
        }
    }
}

@Composable
private fun HaloCard(mark: String, markColor: Color, label: String, sub: String) {
    Column(
        modifier = Modifier
            .border(1.dp, EqLine, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(mark, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = markColor)
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = EqInk)
        Spacer(Modifier.height(4.dp))
        Text(sub, fontSize = 12.sp, color = EqMuted, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BarsVisual(title: String, bars: List<HBar>) {
    Column {
        Text(
            title,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = EqInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        bars.forEach { bar ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                Text(
                    bar.label,
                    fontSize = 13.sp,
                    color = EqInk,
                    modifier = Modifier.width(88.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(EqLine.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(bar.pct / 100f)
                            .height(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MogCoral)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("${bar.pct}%", fontSize = 13.sp, color = EqInk, modifier = Modifier.width(44.dp))
            }
        }
    }
}

@Composable
private fun EmblemVisual(top: String, bottom: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            top,
            fontSize = 44.sp,
            fontWeight = FontWeight.ExtraBold,
            color = EqInk,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            bottom,
            fontSize = 16.sp,
            color = EqMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LikesVisual() {
    // Avg likes per week by attractiveness decile — simple vertical bars
    val values = listOf(0, 0, 0, 0, 0, 1, 4, 8, 17, 39)
    val max = 39f
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "AVG LIKES / WEEK",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = EqInk
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.height(170.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            values.forEachIndexed { i, v ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    if (v > 0) {
                        Text("$v", fontSize = 10.sp, color = EqInk)
                    } else {
                        Spacer(Modifier.height(14.dp))
                    }
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height((8 + (v / max) * 120).dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (i >= 8) MogCoral else EqLine.copy(alpha = 0.7f))
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("${(i + 1) * 10}%", fontSize = 8.sp, color = EqMuted)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("ATTRACTIVENESS SCORE", fontSize = 12.sp, color = EqMuted)
    }
}
