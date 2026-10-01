package com.kurupdevs.moggr.ui

import android.content.Context
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/**
 * v3.0 light design language — clean, airy, premium.
 * Warm white background, bold dark type, photo cards, sage tracker.
 * (Reference: light wellness app — greeting header, inspiration carousel,
 *  activity-progress panel, icon+label bottom bar.)
 */

// ---------- palette ----------

val LtBg = Color(0xFFFCFBF8)
val LtInk = Color(0xFF1B1917)
val LtSoft = Color(0xFF57534E)
val LtMuted = Color(0xFFA8A29E)
val LtLine = Color(0xFFECE7DC)
val LtPill = Color(0xFFF2EEE7)
val LtCard = Color(0xFFFFFFFF)
val LtSage = Color(0xFF8CA69D)
val LtSageDeep = Color(0xFF6B847B)
val LtCheckBadge = Color(0xFF2E2A26)

/** App-wide background — Coach-style blue-white gradient. */
val AppBg: Brush = Brush.linearGradient(
    colors = listOf(Color(0xFFFFF9F2), Color(0xFFEAF1FD), Color(0xFFF1EAFB))
)

// profile (pink) palette — Ayush's profile brand
val PkBg = Color(0xFFF6DADA)
val PkInk = Color(0xFF2B2320)
val PkMuted = Color(0xFF8D7B74)
val PkCoral = Color(0xFFE07856)
val PkCoralDeep = Color(0xFFC25E3C)
val PkLine = Color(0xFFF1E3E3)
val PkPill = Color(0xFFF3D3C8)

val LtSerif = FontFamily.Serif

// ---------- helpers ----------

/** "Good morning" / "Good afternoon" / "Good evening" by local hour. */
fun daypartGreeting(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (h) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}

/** "Week of Jan 12-18" for the current week (Sun-Sat like the reference). */
fun weekLabel(): String {
    val today = LocalDate.now()
    val dow = today.dayOfWeek.value % 7 // Sun=0
    val sunday = today.minusDays(dow.toLong())
    val saturday = sunday.plusDays(6)
    val fmt = java.time.format.DateTimeFormatter.ofPattern("MMM d", Locale.US)
    return "Week of ${sunday.format(fmt)}-${saturday.format(java.time.format.DateTimeFormatter.ofPattern("d", Locale.US))}"
}

/** One cell of the weekly tracker. */
data class LtDay(val label: String, val state: DayState)

enum class DayState { DONE, PENDING, FUTURE }

/** Last 7 days (Sun..Sat) of routine activity, read straight from RoutineStore prefs. */
fun lastWeekActivity(context: Context): List<LtDay> {
    val p = context.getSharedPreferences("moggr_routine", Context.MODE_PRIVATE)
    val today = LocalDate.now()
    val dow = today.dayOfWeek.value % 7 // Sun=0..Sat=6
    val sunday = today.minusDays(dow.toLong())
    return (0..6).map { i ->
        val d = sunday.plusDays(i.toLong())
        val doneSet = p.getStringSet("done_${d}", emptySet()).orEmpty()
        val state = when {
            doneSet.isNotEmpty() -> DayState.DONE
            d.isEqual(today) -> DayState.PENDING
            d.isAfter(today) -> DayState.FUTURE
            else -> DayState.PENDING
        }
        LtDay(
            label = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US),
            state = state
        )
    }
}

// ---------- type ----------

/** Centered serif greeting, e.g. "Good morning, Ayush." */
@Composable
fun LtGreeting(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        fontFamily = LtSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        color = LtInk,
        textAlign = TextAlign.Center
    )
}

@Composable
fun LtSub(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        fontSize = 15.sp,
        color = LtMuted,
        textAlign = TextAlign.Center
    )
}

/** Left-aligned bold section title + optional grey subtitle. */
@Composable
fun LtSectionTitle(title: String, sub: String? = null, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = LtInk
        )
        if (sub != null) {
            Spacer(Modifier.height(4.dp))
            Text(text = sub, fontSize = 14.sp, lineHeight = 20.sp, color = LtMuted)
        }
    }
}

// ---------- cards ----------

/** White card with hairline border + soft shadow. */
@Composable
fun LtWhiteCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0x1A3A2E1A))
            .clip(RoundedCornerShape(20.dp))
            .background(LtCard)
            .padding(18.dp)
    ) { content() }
}

/**
 * Photo inspiration card — rounded photo, bottom scrim, white title + body,
 * chevron on the right edge. Reference "Breathing" card style.
 */
@Composable
fun LtPhotoCard(
    imageRes: Int,
    title: String,
    body: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(300.dp)
            .height(195.dp)
            .shadow(10.dp, RoundedCornerShape(20.dp), spotColor = Color(0x263A2E1A))
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(imageRes),
            contentDescription = title,
            modifier = Modifier.fillMaxWidth().height(195.dp),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(195.dp)
                .background(
                    Brush.verticalGradient(
                        0.35f to Color.Transparent,
                        1f to Color(0xB31B1917)
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
                .padding(end = 44.dp)
        ) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = Color.White.copy(alpha = 0.92f),
                maxLines = 2
            )
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(30.dp)
        )
    }
}

// ---------- segmented control ----------

/** Grey pill with a white sliding active segment — reference style. */
@Composable
fun LtSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(LtPill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { i, label ->
            val active = i == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(if (active) 4.dp else 0.dp, RoundedCornerShape(50))
                    .clip(RoundedCornerShape(50))
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (active) LtInk else LtMuted
                )
            }
        }
    }
}

// ---------- weekly tracker ----------

/**
 * Sage panel with the 7-day strip — reference "Mood" panel style.
 * Check badge = done, warning triangle = missed/today-pending, dot = future.
 */
@Composable
fun LtWeekPanel(
    title: String,
    days: List<LtDay>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(LtSage)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { day ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(40.dp)
                ) {
                    Text(
                        text = day.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(8.dp))
                    when (day.state) {
                        DayState.DONE -> Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(LtCheckBadge),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "done",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DayState.PENDING -> Icon(
                            Icons.Filled.PriorityHigh,
                            contentDescription = "pending",
                            tint = LtCheckBadge,
                            modifier = Modifier.size(26.dp)
                        )
                        DayState.FUTURE -> Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    }
}

// ---------- buttons ----------

/** Dark pill CTA (reference bottom-bar active / strong actions). */
@Composable
fun LtDarkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(LtInk)
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
    }
}

/** Coral pill CTA (brand). */
@Composable
fun LtCoralButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(PkCoral)
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
    }
}

// ---------- bottom bar ----------

/** Fixed white bottom bar, hairline top border, icon + label items — reference style. */
@Composable
fun LtBottomBar(
    items: List<Pair<String, ImageVector>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // v3.3: floating white bar, pink-gradient pill for the active tab — mockup style.
    val activeGrad = Brush.horizontalGradient(listOf(Color(0xFFFF6A62), Color(0xFFF8579B)))
    val idleGray = Color(0xFF9AA0A6)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 10.dp)
            .shadow(12.dp, RoundedCornerShape(30.dp), spotColor = Color(0x26000000))
            .clip(RoundedCornerShape(30.dp))
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { i, (label, icon) ->
                val active = i == selected
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .then(if (active) Modifier.background(activeGrad) else Modifier)
                        .clickable { onSelect(i) }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (active) Color.White else idleGray,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (active) Color.White else idleGray
                    )
                }
            }
        }
    }
}
