package com.kurupdevs.moggr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.analysis.PslReport

// ---------- Color season data ----------
// Simplified warm/cool season system with desi-friendly palettes.
// The measure chunk sets report.skinUndertone ("Warm"/"Cool"/"Neutral"/"").

data class SeasonInfo(
    val name: String,
    val tagline: String,
    /** label → swatch color */
    val colors: List<Pair<String, Color>>
)

val SeasonAutumn = SeasonInfo(
    "Autumn",
    "Warm, deep, earthy — you glow in rich tones.",
    listOf(
        "Mustard" to Color(0xFFD9A62E),
        "Olive" to Color(0xFF6B7F3E),
        "Rust" to Color(0xFFB5542D),
        "Deep teal" to Color(0xFF1F6E6B)
    )
)

val SeasonWinter = SeasonInfo(
    "Winter",
    "Cool, bold, high-contrast — black and jewel tones are yours.",
    listOf(
        "Black" to Color(0xFF141414),
        "White" to Color(0xFFFFFFFF),
        "Royal blue" to Color(0xFF2743B8),
        "Emerald" to Color(0xFF0F7B5F)
    )
)

val SeasonSpring = SeasonInfo(
    "Spring",
    "Warm, bright, fresh — light punchy colors lift you.",
    listOf(
        "Coral" to Color(0xFFFF7F6B),
        "Turmeric yellow" to Color(0xFFE8B93C),
        "Peach" to Color(0xFFFFC9A3),
        "Leaf green" to Color(0xFF7CB05A)
    )
)

val SeasonSummer = SeasonInfo(
    "Summer",
    "Cool, soft, muted — powdery pastels suit you best.",
    listOf(
        "Rose" to Color(0xFFE8A0B4),
        "Lavender" to Color(0xFFB9A7E6),
        "Powder blue" to Color(0xFFA9CBE8),
        "Sage" to Color(0xFF9DB89A)
    )
)

val SeasonNeutral = SeasonInfo(
    "In-between",
    "You sit between warm and cool — borrow from both sides and wear what you already get compliments in.",
    listOf(
        "Olive" to Color(0xFF6B7F3E),
        "Teal" to Color(0xFF2E8B8B),
        "Maroon" to Color(0xFF7B2D3B),
        "Navy" to Color(0xFF2C3E6B)
    )
)

/** Season from a scan-read undertone alone (used by the embedded Method-tab card). */
fun seasonFromUndertone(undertone: String): SeasonInfo? = when (undertone.trim().lowercase()) {
    "warm" -> SeasonAutumn
    "cool" -> SeasonWinter
    "neutral" -> SeasonNeutral
    else -> null
}

/**
 * Season from the 3-question quiz. The scan's skinUndertone counts as a bonus vote.
 * Strong leans (3+) land on the deep seasons, soft leans on the bright ones —
 * a deliberate simplification, stated as such in the UI.
 */
fun seasonFromVotes(warmVotes: Int, coolVotes: Int, skinUndertone: String): SeasonInfo {
    var warm = warmVotes
    var cool = coolVotes
    when (skinUndertone.trim().lowercase()) {
        "warm" -> warm += 1
        "cool" -> cool += 1
    }
    return when {
        warm > cool -> if (warm >= 3) SeasonAutumn else SeasonSpring
        cool > warm -> if (cool >= 3) SeasonWinter else SeasonSummer
        else -> SeasonNeutral
    }
}

@Composable
private fun SeasonPaletteDots(season: SeasonInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        season.colors.forEach { (label, color) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, PslGrey.copy(alpha = 0.4f), CircleShape)
                )
                Spacer(Modifier.height(4.dp))
                Text(label, fontSize = 11.sp, color = PslGrey)
            }
        }
    }
}

@Composable
fun SeasonResultContent(season: SeasonInfo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("You are ${season.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PslText)
            Spacer(Modifier.height(4.dp))
            Text(season.tagline, fontSize = 13.sp, color = PslGrey)
            Spacer(Modifier.height(12.dp))
            Text("DESI PALETTE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PslBlue)
            Spacer(Modifier.height(8.dp))
            SeasonPaletteDots(season)
            Spacer(Modifier.height(8.dp))
            Text(
                "Start with one piece in your palette near your face — a tee, a kurta, a shirt. Watch how your skin looks in daylight.",
                fontSize = 12.sp,
                color = PslGrey
            )
        }
    }
}

@Composable
fun SwatchTestCard() {
    // NOTE: we deliberately do NOT fake a "digital drape" by compositing color
    // swatches onto the user's photo. A composited swatch shifts perceived skin
    // tone and would mislead the read — honesty beats the gimmick. A real cloth
    // in daylight is the honest test, so we teach that instead.
    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Swatch test — the honest drape", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PslText)
            Spacer(Modifier.height(6.dp))
            Text(
                "Stand in daylight facing a window. Hold a solid-color cloth under your chin — " +
                    "mustard or yellow for the warm test, grey or blue for the cool test.",
                fontSize = 14.sp,
                color = PslText
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Watch your jawline shadows and your skin: if the shadows sharpen and your skin " +
                    "looks alive, that temperature suits you. If your skin looks dull or grey, it's not yours. " +
                    "Two cloths, two minutes — no app filter can fake this.",
                fontSize = 14.sp,
                color = PslText
            )
        }
    }
}

/**
 * Embedded season card for the Method tab (wired in by the routine chunk).
 * Uses the scan's skinUndertone only — the full quiz lives in the Guides tab.
 */
@Composable
fun SeasonCard(report: PslReport?) {
    val season = seasonFromUndertone(report?.skinUndertone.orEmpty())
    Column(Modifier.fillMaxWidth()) {
        CapsLabel("YOUR COLOR SEASON")
        Spacer(Modifier.height(8.dp))
        if (season != null) {
            SeasonResultContent(season)
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = PslCard),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "No undertone read yet — take the 3-question season quiz in the Guides tab to unlock your palette.",
                    fontSize = 14.sp,
                    color = PslText,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        SwatchTestCard()
    }
}
