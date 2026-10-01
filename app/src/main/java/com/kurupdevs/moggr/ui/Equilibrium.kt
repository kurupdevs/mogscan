package com.kurupdevs.moggr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.ui.theme.EqGreige
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqInkSoft
import com.kurupdevs.moggr.ui.theme.EqLine
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.EqPillDark
import com.kurupdevs.moggr.ui.theme.EqTeal
import com.kurupdevs.moggr.ui.theme.MogCoral

/**
 * v2.7 shared design language — equilibrium-style calm.
 * Original implementation, same visual language only.
 *
 * Use these everywhere instead of raw Cards/Buttons so the whole app
 * shares one skin: greige background, serif display type, frosted-glass
 * cards, pill CTAs, pastel tiles, 24dp rounding, letter-spaced labels.
 */

val EqSerif = FontFamily.Serif
val EqRound = 24.dp
val EqRoundSm = 18.dp

/** Letter-spaced small-caps section label, e.g. "TODAYS THOUGHTS". */
@Composable
fun EqSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 2.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = EqMuted
        )
    )
}

/** Large serif display headline. */
@Composable
fun EqHeadline(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = EqInk,
    size: Int = 32,
    align: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        modifier = modifier,
        fontFamily = EqSerif,
        fontWeight = FontWeight.Normal,
        fontSize = size.sp,
        lineHeight = (size + 6).sp,
        color = color,
        textAlign = align
    )
}

/** Frosted-glass card: translucent warm white over greige, soft border.
 * v3.0: solid clean-white card, hairline border, soft shadow (reference style). */
@Composable
fun EqGlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = EqRound,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(corner), spotColor = Color(0x1A3A2E1A))
            .clip(RoundedCornerShape(corner))
            .background(Color.White)
            .border(1.dp, LtLine, RoundedCornerShape(corner))
            .padding(18.dp),
        contentAlignment = Alignment.TopStart
    ) { content() }
}

/** Light pill button ("Start Now" style). */
@Composable
fun EqPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (enabled) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.4f))
            .border(1.dp, EqLine.copy(alpha = 0.6f), RoundedCornerShape(50))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = if (enabled) EqInk else EqMuted
        )
    }
}

/** Dark pill button (secondary / strong CTA). */
@Composable
fun EqDarkPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (enabled) EqPillDark else EqMuted.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Color.White
        )
    }
}

/** Teal pill button (secondary CTA, reference style). */
@Composable
fun EqTealPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (enabled) EqTeal else EqMuted.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Color.White
        )
    }
}

/** Coral pill button (brand primary CTA). */
@Composable
fun EqCoralPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (enabled) MogCoral else EqMuted.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Color.White
        )
    }
}

/**
 * Pastel feature tile — title + subtitle + duration chip, tappable.
 * Colors: pass EqLavender / EqSage / EqPeach / EqRose / EqSky etc.
 */
@Composable
fun EqPastelTile(
    title: String,
    subtitle: String,
    chip: String,
    tileColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(EqRoundSm))
            .background(tileColor)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = title,
                fontFamily = EqSerif,
                fontSize = 17.sp,
                lineHeight = 22.sp,
                color = EqInk
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = EqInkSoft
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(text = chip, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = EqInk)
            }
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(EqPillDark),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "›", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Date badge card ("15 / Sept" style). */
@Composable
fun EqDateBadge(day: String, month: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.6f))
            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = day, fontFamily = EqSerif, fontSize = 22.sp, color = EqInk)
        Text(
            text = month,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
            color = EqMuted
        )
    }
}

/** Soft divider line. */
@Composable
fun EqDivider(modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(EqLine.copy(alpha = 0.7f))
    )
}

/** Screen background: v3.0 clean warm-white wash (reference style). */
fun eqBackgroundBrush() = Brush.verticalGradient(
    listOf(Color(0xFFFCFBF8), Color(0xFFF4EEE3))
)

/** Convenience: muted body text. */
@Composable
fun EqBody(text: String, modifier: Modifier = Modifier, size: Int = 14) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = size.sp,
        lineHeight = (size + 6).sp,
        color = EqInkSoft
    )
}

/** Frosted dark-glass card for full-bleed photo backgrounds (reference screen-2 style).
 * Translucent white over a dark scrim; content is expected to use white text. */
@Composable
fun EqDarkGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(28.dp))
            .padding(20.dp),
        contentAlignment = Alignment.TopStart
    ) {
        content()
    }
}

/** Frosted translucent pill button for photo backgrounds. */
@Composable
fun EqFrostPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.20f))
            .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Color.White
        )
    }
}
