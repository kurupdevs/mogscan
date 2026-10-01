package com.kurupdevs.moggr.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqLavender
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.EqPeach
import com.kurupdevs.moggr.ui.theme.EqRose
import com.kurupdevs.moggr.ui.theme.EqSage
import com.kurupdevs.moggr.ui.theme.MogCoralDark

private data class MythCard(
    val myth: String,
    val honest: String,
    val why: String
)

/** Myth-buster cards for the Method tab — honest science, Gen Z tone, no lecture. */
private val MYTHS: List<MythCard> = listOf(
    MythCard(
        myth = "The golden ratio decides your face",
        honest = "Nah — it's not a validated beauty metric, and Moggr doesn't use it.",
        why = "The 1.618 ratio shows up in forum posts way more than in actual research. " +
            "Symmetry and harmony read as attractive; no magic number predicts them. " +
            "We score real measured ratios instead of chasing a golden constant."
    ),
    MythCard(
        myth = "Chewing gum grows your jaw",
        honest = "Chewing trains jaw muscles, not bone — the muscle works harder, the bone stays the bone.",
        why = "Masseter hypertrophy is real: resistance training can thicken the jaw muscles a bit. " +
            "But adult mandible shape doesn't change from chewing. Leanness and skin do more " +
            "for the lower-third read than any gum routine."
    ),
    MythCard(
        myth = "Mewing reshapes adult bones",
        honest = "Mewing won't reshape adult bones — but good tongue posture and straight posture genuinely help.",
        why = "No solid studies show adult bone change from tongue posture. What it does give you: " +
            "a cleaner resting face, less under-chin softness, and better profile photos. " +
            "Posture and habits, not bone claims."
    ),
    MythCard(
        myth = "One photo is your final score",
        honest = "One photo isn't a verdict — lighting, angle, and lens change the score.",
        why = "Wide-angle front cameras distort faces; harsh light can sharpen or hide features. " +
            "The same setup gives comparable results, so compare scans against each other, " +
            "never against a single photo."
    )
)

// v2.7: myth-buster cards as rotating pastel tiles.
private val MYTH_TILE_COLORS = listOf(EqLavender, EqSage, EqPeach, EqRose)

@Composable
private fun MythBusterCard(card: MythCard, tileColor: Color) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(tileColor)
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Text(
            "MYTH: ${card.myth}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MogCoralDark,
            letterSpacing = 0.5.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            card.honest,
            fontFamily = EqSerif,
            fontSize = 17.sp,
            color = EqInk,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (expanded) "why — hide" else "why it matters",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontStyle = FontStyle.Italic,
                color = EqMuted
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(Modifier.height(6.dp))
                EqBody(card.why)
            }
        }
    }
}

/** All four myth-buster cards. Drop inside the Method tab. */
@Composable
fun ScienceCards() {
    Column(modifier = Modifier.fillMaxWidth()) {
        MYTHS.forEachIndexed { idx, myth ->
            MythBusterCard(myth, MYTH_TILE_COLORS[idx % MYTH_TILE_COLORS.size])
        }
    }
}
