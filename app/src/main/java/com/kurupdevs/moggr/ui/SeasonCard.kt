package com.kurupdevs.moggr.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.MogCoral
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.analysis.PslReport
// v2.6-hinglish begin
import com.kurupdevs.moggr.util.LanguageStore
// v2.6-hinglish end

// ---------- Color season data ----------
// Simplified warm/cool season system.
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
fun SeasonResultContent(season: SeasonInfo) {
    // v2.6-hinglish: season name + tagline translated.
    val hi = LanguageStore.isHinglish
    val nameKey = when (season) {
        SeasonAutumn -> "season_name_autumn"
        SeasonWinter -> "season_name_winter"
        SeasonSpring -> "season_name_spring"
        SeasonSummer -> "season_name_summer"
        else -> "season_name_neutral"
    }
    val tagKey = when (season) {
        SeasonAutumn -> "season_tagline_autumn"
        SeasonWinter -> "season_tagline_winter"
        SeasonSpring -> "season_tagline_spring"
        SeasonSummer -> "season_tagline_summer"
        else -> "season_tagline_neutral"
    }
    val seasonName = Strings.s(nameKey, hi).let { if (it == nameKey) season.name else it }
    val seasonTag = Strings.s(tagKey, hi).let { if (it == tagKey) season.tagline else it }
    EqGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                Strings.fmt("season_you_are", hi, "s" to seasonName),
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = EqInk
            )
            Spacer(Modifier.height(4.dp))
            Text(seasonTag, fontSize = 13.sp, color = EqMuted)
            Spacer(Modifier.height(8.dp))
            Text(
                Strings.s("season_tip", hi),
                fontSize = 12.sp,
                color = EqMuted
            )
        }
    }
}

@Composable
fun SwatchTestCard() {
    // v2.6-hinglish: translated body.
    val hi = LanguageStore.isHinglish
    // NOTE: we deliberately do NOT fake a "digital drape" by compositing color
    // swatches onto the user's photo. A composited swatch shifts perceived skin
    // tone and would mislead the read — honesty beats the gimmick. A real cloth
    // in daylight is the honest test, so we teach that instead.
    EqGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(Strings.s("swatch_title", hi), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = EqInk)
            Spacer(Modifier.height(6.dp))
            Text(
                Strings.s("swatch_b0", hi),
                fontSize = 14.sp,
                color = EqInk
            )
            Spacer(Modifier.height(6.dp))
            Text(
                Strings.s("swatch_b1", hi),
                fontSize = 14.sp,
                color = EqInk
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
    // v2.6-hinglish: translated labels.
    val hi = LanguageStore.isHinglish
    val season = seasonFromUndertone(report?.skinUndertone.orEmpty())
    Column(Modifier.fillMaxWidth()) {
        EqSectionLabel(Strings.s("caps_color_season", hi))
        Spacer(Modifier.height(8.dp))
        if (season != null) {
            SeasonResultContent(season)
        } else {
            EqGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    Strings.s("season_locked", hi),
                    fontSize = 14.sp,
                    color = EqInk
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        SwatchTestCard()
    }
}
