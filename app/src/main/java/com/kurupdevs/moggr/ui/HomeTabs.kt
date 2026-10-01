package com.kurupdevs.moggr.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import com.kurupdevs.moggr.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.analysis.PslReport
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqLavender
import com.kurupdevs.moggr.ui.theme.EqLine
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.EqPeach
import com.kurupdevs.moggr.ui.theme.EqPillDark
import com.kurupdevs.moggr.ui.theme.EqRose
import com.kurupdevs.moggr.ui.theme.EqSage
import com.kurupdevs.moggr.ui.theme.EqTeal
import com.kurupdevs.moggr.ui.theme.MogCoral
import com.kurupdevs.moggr.util.PlanStore
import com.kurupdevs.moggr.util.ReportStore
import com.kurupdevs.moggr.util.RoutineStore
import com.kurupdevs.moggr.util.RoutineTask
import com.kurupdevs.moggr.util.TaskCategory
import com.kurupdevs.moggr.util.UserProfile
import com.kurupdevs.moggr.util.VoiceStore
import kotlinx.coroutines.delay

// v2.6-hinglish begin: app language (English / Hinglish) is global Compose state.
import com.kurupdevs.moggr.util.LanguageStore
// v2.6-hinglish end

// ---------- Main tabs ----------

private data class TabDef(val key: String, val icon: ImageVector)

@Composable
fun MainTabs(
    report: PslReport?,
    profile: UserProfile?,
    frontPhoto: Bitmap?,
    userName: String,
    onRescan: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    // v2.6-hinglish: hi flips the whole tab UI; reading the state recomposes.
    val hi = LanguageStore.isHinglish
    // v2.6-challenges: overlay toggle
    var showChallenges by remember { mutableStateOf(false) }
    val tabs = listOf(
        TabDef("home", Icons.Filled.Home),
        TabDef("method", Icons.Filled.School),
        TabDef("coach", Icons.Filled.Chat),
        TabDef("routine", Icons.Filled.Checklist)
    )
    Box(modifier = Modifier.fillMaxSize().background(MoggrBg)) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            // v2.7: floating greige-glass nav bar, 24dp corners, icon+label items
            if (!showChallenges) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .shadow(16.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.72f))
                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { idx, t ->
                        val selected = tab == idx
                        // v2.6-hinglish: translated tab label.
                        val label = Strings.s("tab_${t.key}", hi)
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (selected) EqPillDark else Color.Transparent)
                                .clickable { tab = idx }
                                .padding(horizontal = 13.dp, vertical = 7.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                t.icon,
                                contentDescription = label,
                                tint = if (selected) Color.White else EqMuted,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.height(1.dp))
                            Text(
                                label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else EqMuted
                            )
                        }
                    }
                }
            }
            }
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            // v2.6-challenges: overlay screen, no nav-graph changes needed
            if (showChallenges) {
                ChallengeScreen(onBack = { showChallenges = false })
            } else {
                when (tab) {
                    0 -> HomeTab(
                        report = report,
                        profile = profile,
                        photo = frontPhoto,
                        onRescan = onRescan,
                        onChallenges = { showChallenges = true },
                        onGoTab = { tab = it }
                    )
                    1 -> MethodScreen(report = report, onGoTab = { tab = it }, onRescan = onRescan)
                    // v2.6-hinglish: CoachScreen reads LanguageStore.isHinglish itself.
                    2 -> CoachScreen(report = report, userName = userName, onBack = { tab = 0 })
                    3 -> RoutineScreen(onChallenges = { showChallenges = true })
                }
            }
        }
    }
    }
}

// ---------- v2.6 challenges: shared promo card ----------

@Composable
private fun ChallengesPromoCard(onChallenges: () -> Unit) {
    EqPastelTile(
        title = "7-day debloat. 30-day glow-up.",
        subtitle = "Daily photo check-ins, streaks, freezes + buddy mode.",
        chip = "CHALLENGES",
        tileColor = EqPeach,
        onClick = onChallenges,
        modifier = Modifier.fillMaxWidth()
    )
}

// ---------- Home: saved face + ratings ----------

// v2.6-hinglish begin: EN/HI language toggle — one tap, visible in the Home header.
// v2.6-hinglish end
@Composable
fun LanguageToggle(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val hi = LanguageStore.isHinglish
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.85f))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(false to "EN", true to "HI").forEach { (isHi, label) ->
            val selected = hi == isHi
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) PslBlue else Color.Transparent)
                    .clickable { LanguageStore.set(context, isHi) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else PslGrey
                )
            }
        }
    }
}

/** v2.6-hinglish: routine task title/detail/counter-unit in the current language. */
internal fun taskTitle(task: RoutineTask, hi: Boolean): String {
    val key = "task_${task.id}_t"
    val v = Strings.s(key, hi)
    return if (v == key) task.title else v
}

internal fun taskDetail(task: RoutineTask, hi: Boolean): String {
    val key = "task_${task.id}_d"
    val v = Strings.s(key, hi)
    return if (v == key) task.detail else v
}

internal fun taskUnit(task: RoutineTask, hi: Boolean): String {
    if (task.counterUnit.isBlank()) return ""
    val key = "task_${task.id}_u"
    val v = Strings.s(key, hi)
    return if (v == key) task.counterUnit else v
}

internal fun categoryLabel(cat: TaskCategory, hi: Boolean): String {
    val key = "cat_${cat.key}"
    val v = Strings.s(key, hi)
    return if (v == key) cat.label else v
}

@Composable
private fun HomeTab(
    report: PslReport?,
    profile: UserProfile?,
    photo: Bitmap?,
    onRescan: () -> Unit,
    onChallenges: () -> Unit,
    onGoTab: (Int) -> Unit
) {
    val context = LocalContext.current
    // v2.6-hinglish: current app language.
    val hi = LanguageStore.isHinglish
    // v2.6-voice: voice check overlay toggle
    var showVoice by remember { mutableStateOf(false) }
    if (report == null) {
        // v2.9: full-bleed photo welcome, reference screen-1 style.
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.moggr_hero_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.10f),
                                Color.Black.copy(alpha = 0.70f)
                            )
                        )
                    )
            )
            LanguageToggle(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.height(56.dp))
                Column {
                    Text(
                        if (hi) "Apna best face\npao." else "Find your best\nface.",
                        fontFamily = EqSerif,
                        fontSize = 46.sp,
                        lineHeight = 52.sp,
                        color = Color.White
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        Strings.s("home_hero_sub", hi),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.22f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(50))
                        .clickable { onRescan() }
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        Strings.s("home_start_scan", hi),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
        return
        // v2.6-hinglish end
    }
    // v2.9: full-bleed photo home with frosted glass, reference screen-2 style.
    Box(modifier = Modifier.fillMaxSize()) {
        if (photo != null) {
            Image(
                bitmap = photo.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(40.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.moggr_home_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.65f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            val daypart = remember {
                when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
                    in 5..11 -> "morning"
                    in 12..16 -> "afternoon"
                    in 17..21 -> "evening"
                    else -> "night"
                }
            }
            EqDarkGlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (photo != null) {
                        Image(
                            bitmap = photo.asImageBitmap(),
                            contentDescription = "You",
                            modifier = Modifier.size(48.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (profile?.name?.firstOrNull()?.uppercase() ?: "M"),
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            Strings.fmt("greet_good", hi, "d" to Strings.s("greet_$daypart", hi)),
                            fontSize = 13.sp, color = Color.White.copy(alpha = 0.65f)
                        )
                        Text(
                            if (hi) "Aaj ka glow kaisa hai?" else "How's your glow today?",
                            fontFamily = EqSerif, fontStyle = FontStyle.Italic,
                            fontSize = 18.sp, color = Color.White
                        )
                    }
                    LanguageToggle()
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                if (hi) "AAJ KA FOCUS" else "TODAY'S FOCUS",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = Color.White.copy(alpha = 0.55f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                Strings.s("home_know1", hi),
                fontFamily = EqSerif, fontSize = 34.sp, color = Color.White, lineHeight = 38.sp
            )
            Text(
                Strings.s("home_know2", hi),
                fontFamily = EqSerif, fontStyle = FontStyle.Italic,
                fontSize = 34.sp, color = Color.White, lineHeight = 38.sp
            )
            Spacer(Modifier.height(18.dp))

            var showBreakdown by remember { mutableStateOf(false) }
            val cal = remember { java.util.Calendar.getInstance() }
            EqDarkGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (hi) "TUMHARA SCORE" else "YOUR SCORE",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                            color = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.weight(1f)
                        )
                        EqDateBadge(
                            day = cal.get(java.util.Calendar.DAY_OF_MONTH).toString(),
                            month = java.text.SimpleDateFormat("MMM", java.util.Locale.US).format(cal.time)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            String.format(java.util.Locale.US, "%.1f", report.overallPsl),
                            fontFamily = EqSerif, fontSize = 52.sp, color = Color.White, lineHeight = 54.sp
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.padding(bottom = 8.dp)) {
                            Text(pslTierShort(report.overallPsl), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Text("PSL · 1–8 scale", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        report.summary,
                        fontSize = 13.sp, lineHeight = 19.sp,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { showBreakdown = !showBreakdown }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (showBreakdown) (if (hi) "Chhupao" else "Hide breakdown")
                            else (if (hi) "Poora breakdown" else "Full breakdown"),
                            color = EqPillDark, fontWeight = FontWeight.Bold, fontSize = 15.sp
                        )
                    }
                }
            }
            if (showBreakdown) {
                Spacer(Modifier.height(12.dp))
                ReportBody(report = report, profile = profile, photo = photo)
            }
            Spacer(Modifier.height(20.dp))

            Text(
                if (hi) "QUICK ACTIONS" else "QUICK ACTIONS",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = Color.White.copy(alpha = 0.55f)
            )
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                EqPastelTile(
                    title = if (hi) "Naya Scan" else "New Scan",
                    subtitle = if (hi) "3 angle" else "3 angles",
                    chip = if (hi) "Scan" else "Scan",
                    tileColor = EqLavender, onClick = onRescan, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                EqPastelTile(
                    title = if (hi) "Coach" else "Coach",
                    subtitle = if (hi) "Kuch bhi puchho" else "Ask anything",
                    chip = if (hi) "Chat" else "Chat",
                    tileColor = EqSage, onClick = { onGoTab(2) }, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                EqPastelTile(
                    title = if (hi) "Routine" else "Routine",
                    subtitle = if (hi) "Aaj ke tasks" else "Today's tasks",
                    chip = if (hi) "Kholo" else "Open",
                    tileColor = EqPeach, onClick = { onGoTab(3) }, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                EqPastelTile(
                    title = if (hi) "Voice" else "Voice",
                    subtitle = if (hi) "10-sec check" else "10-sec check",
                    chip = if (hi) "Check" else "Check",
                    tileColor = EqRose, onClick = { showVoice = true }, modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                Strings.s("caps_progress", hi),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = Color.White.copy(alpha = 0.55f)
            )
            Spacer(Modifier.height(10.dp))
            ProgressTimeline()
            Spacer(Modifier.height(14.dp))
            SleepMiniCard()
            DebloatMorningMini()
            Spacer(Modifier.height(14.dp))
            ChallengesPromoCard(onChallenges = onChallenges)
            Spacer(Modifier.height(6.dp))
            VoiceCheckCard(onOpen = { showVoice = true })
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EqFrostPillButton(
                    text = Strings.s("scan_again", hi),
                    onClick = onRescan,
                    modifier = Modifier.weight(1f)
                )
                EqFrostPillButton(
                    text = Strings.s("share_btn", hi),
                    onClick = { shareReport(context, profile, report) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(20.dp))
        }
        if (showVoice) {
            VoiceCheckScreen(onClose = { showVoice = false })
        }
    }
}

// ---------- Voice check card (opens overlay) ----------

@Composable
private fun VoiceCheckCard(onOpen: () -> Unit) {
    val context = LocalContext.current
    val streak = remember { VoiceStore.load(context).streak }
    EqGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EqSectionLabel("VOICE CHECK")
                Spacer(Modifier.weight(1f))
                if (streak > 0) {
                    Text(
                        "$streak-day streak",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MogCoral
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Your voice, steady and clear",
                fontFamily = EqSerif,
                fontSize = 22.sp,
                color = PslText
            )
            Spacer(Modifier.height(4.dp))
            EqBody("10-second on-device check — pitch, pace, steadiness. The mic never leaves your phone.")
            Spacer(Modifier.height(12.dp))
            EqCoralPillButton(
                text = "Check my voice",
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------- Method: how the rating works ----------

@Composable
private fun MethodScreen(
    report: PslReport?,
    onGoTab: (Int) -> Unit,
    onRescan: () -> Unit
) {
    val measuredCount = report?.features?.size ?: 15
    val hi = LanguageStore.isHinglish
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoggrBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // v2.8: header with back chevron, reference style.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f))
                    .clickable { onGoTab(0) },
                contentAlignment = Alignment.Center
            ) {
                Text("‹", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = EqInk)
            }
            Spacer(Modifier.width(12.dp))
            Text(
                if (hi) "Scoring Kaise Hoti Hai" else "How Scoring Works",
                fontFamily = EqSerif, fontSize = 22.sp, color = EqInk,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(16.dp))

        SeasonCard(report)
        Spacer(Modifier.height(12.dp))

        EqGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    if (hi) "Method" else "The Method",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp, color = EqInk
                )
                Spacer(Modifier.height(4.dp))
                EqBody(Strings.fmt("scoring_body", hi, "n" to "$measuredCount"), size = 13)
            }
        }
        Spacer(Modifier.height(18.dp))

        EqSectionLabel(if (hi) "TUMHARE PILLARS" else "YOUR PILLARS")
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            EqPastelTile(
                title = Strings.s("pillar_harmony", hi),
                subtitle = Strings.s("pillar_harmony_d", hi),
                chip = "40%", tileColor = EqLavender, onClick = {}, modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            EqPastelTile(
                title = Strings.s("pillar_features", hi),
                subtitle = Strings.s("pillar_features_d", hi),
                chip = "25%", tileColor = EqSage, onClick = {}, modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            EqPastelTile(
                title = Strings.s("pillar_dimorphism", hi),
                subtitle = Strings.s("pillar_dimorphism_d", hi),
                chip = "20%", tileColor = EqPeach, onClick = {}, modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            EqPastelTile(
                title = Strings.s("pillar_angularity", hi),
                subtitle = Strings.s("pillar_angularity_d", hi),
                chip = "15%", tileColor = EqRose, onClick = {}, modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(18.dp))

        EqSectionLabel(Strings.fmt("sec_measurements", hi, "n" to "$measuredCount"))
        Spacer(Modifier.height(10.dp))
        EqGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                val cites = listOf(
                    "fwhr" to "Weston et al. 2007",
                    "esr" to "Farkas 1994",
                    "pfh" to "Farkas 1994",
                    "jaw_frontal" to "Moggr landmark mesh",
                    "cheek" to "Moggr landmark mesh",
                    "chin" to "Moggr landmark mesh",
                    "midface" to "Farkas 1994",
                    "nasal" to "Farkas 1994",
                    "lips" to "Farkas 1994",
                    "jaw_side" to "Moggr landmark mesh",
                    "ramus" to "Moggr landmark mesh",
                    "gonial" to "Moggr landmark mesh",
                    "dorsal" to "Moggr landmark mesh",
                    "projection" to "Moggr landmark mesh",
                    "symmetry" to "Rhodes 2006"
                )
                for (i in 0 until 15) {
                    val name = Strings.s("m_$i", hi)
                    val cite = cites.getOrNull(i)?.second ?: ""
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, color = EqInk, fontSize = 14.sp)
                            Text(cite, color = EqMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))

        EqSectionLabel(Strings.s("sec_tiers", hi))
        Spacer(Modifier.height(10.dp))
        EqGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                val tierScores = listOf("7.75+", "7.0+", "6.0+", "5.0+", "3.0+", "1.4+", "< 1.4")
                tierScores.forEachIndexed { i, score ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(score, color = EqInk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(Strings.s("tier_desc_$i", hi), color = EqMuted, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(Strings.s("tiers_note", hi), fontSize = 12.sp, color = EqMuted)
            }
        }
        Spacer(Modifier.height(12.dp))

        EqGlassCard(modifier = Modifier.fillMaxWidth(), corner = EqRoundSm) {
            GuidesSection()
        }
        Spacer(Modifier.height(18.dp))

        EqSectionLabel(Strings.s("caps_accurate", hi))
        Spacer(Modifier.height(10.dp))
        EqGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                for (i in 0 until 5) {
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text("• ", color = EqTeal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(Strings.s("scan_tip_$i", hi), fontSize = 14.sp, color = EqInk)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))

        EqSectionLabel(Strings.s("caps_honest", hi))
        Spacer(Modifier.height(10.dp))
        EqGlassCard(modifier = Modifier.fillMaxWidth()) {
            EqBody(Strings.s("honest_body", hi))
        }
        Spacer(Modifier.height(20.dp))

        // v2.8: bottom CTAs, reference style.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EqTealPillButton(
                text = if (hi) "Coach se puchho" else "Ask Coach",
                onClick = { onGoTab(2) },
                modifier = Modifier.weight(1f)
            )
            EqDarkPillButton(
                text = Strings.s("scan_again", hi),
                onClick = onRescan,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(20.dp))
    }
    // v2.6-hinglish end
}

@Composable
private fun MethodSection(title: String, content: @Composable () -> Unit) {
    EqGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        corner = EqRoundSm
    ) {
        Column {
            EqSectionLabel(title)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun PillarRow(name: String, weight: String, desc: String) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, color = PslText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(weight, color = PslBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.height(2.dp))
        Text(desc, fontSize = 13.sp, color = PslGrey)
    }
}

// ---------- Routine: 90-day plan + daily softmaxx ----------

@Composable
private fun RoutineScreen(onChallenges: () -> Unit) {
    val context = LocalContext.current
    val report = remember { ReportStore.loadReport(context) }
    var routineState by remember { mutableStateOf(RoutineStore.load(context)) }
    val planDay = remember { PlanStore.planDay(context) }
    val plan = remember { PlanStore.buildPlan(report) }
    val phase = plan.phases[PlanStore.phaseForDay(planDay).coerceIn(0, 2)]
    val tasks = remember(planDay, routineState.done) {
        PlanStore.todayTasks(report, planDay, routineState.done)
    }
    val doneCount = tasks.count { it.id in routineState.done }
    val onToggle: (String) -> Unit = { id -> routineState = RoutineStore.toggle(context, id) }
    // v2.6-hinglish begin: routine screen translations.
    val hi = LanguageStore.isHinglish
    val phaseTitleKey = "phase_t_${phase.title.lowercase()}"
    val phaseGoalKey = "phase_g_${phase.title.lowercase()}"
    val phaseTitle = Strings.s(phaseTitleKey, hi).let { if (it == phaseTitleKey) phase.title else it }
    val phaseGoal = Strings.s(phaseGoalKey, hi).let { if (it == phaseGoalKey) phase.goal else it }
    // Weekly focus strings are ordered phase_w0..phase_w9 across the three phases.
    val weekOffset = when (phase.index) { 0 -> 0; 1 -> 4; else -> 7 }
    val weekIdx = (((planDay - 1) % 30) / 7).coerceIn(0, phase.weeklyFocus.size - 1)
    val weekKey = "phase_w${weekOffset + weekIdx}"
    val weekText = Strings.s(weekKey, hi).let { if (it == weekKey) phase.weeklyFocus[weekIdx] else it }
    // v2.6-hinglish end

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoggrBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        EqSectionLabel(Strings.s("caps_90", hi))
        Spacer(Modifier.height(8.dp))
        EqHeadline(
            text = Strings.s("plan_head1", hi),
            size = 32
        )
        Text(
            Strings.s("plan_head2", hi),
            fontFamily = EqSerif,
            fontStyle = FontStyle.Italic,
            fontSize = 32.sp,
            color = PslText,
            lineHeight = 36.sp
        )
        Spacer(Modifier.height(8.dp))
        EqBody(Strings.s("plan_sub", hi))
        Spacer(Modifier.height(16.dp))

        // Phase header card
        EqGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EqDateBadge(day = "$planDay", month = "DAY")
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            Strings.fmt(
                                "day_phase", hi,
                                "d" to "$planDay", "p" to "${phase.index + 1}", "t" to phaseTitle
                            ),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PslText
                        )
                        Spacer(Modifier.height(4.dp))
                        EqBody(phaseGoal, size = 13)
                    }
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { planDay / 90f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MogCoral,
                    trackColor = EqLine
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    Strings.fmt("this_week", hi, "w" to weekText),
                    fontSize = 12.sp,
                    color = PslGrey
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Streak flame row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔥", fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                Strings.fmt("streak_days", hi, "n" to "${routineState.streak}"),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PslText
            )
            Spacer(Modifier.weight(1f))
            Text(
                Strings.fmt("today_count", hi, "d" to "$doneCount", "t" to "${tasks.size}"),
                fontSize = 14.sp,
                color = PslGrey
            )
        }

        Spacer(Modifier.height(14.dp))
        // v2.6-challenges entry point from the Routine tab
        ChallengesPromoCard(onChallenges = onChallenges)
        Spacer(Modifier.height(14.dp))
        CapsLabel(Strings.s("caps_today", hi))
        Spacer(Modifier.height(4.dp))

        TaskCategory.entries.forEach { cat ->
            val group = tasks.filter { it.category == cat }
            if (group.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                CapsLabel(categoryLabel(cat, hi).uppercase())
                Spacer(Modifier.height(4.dp))
                group.forEach { task ->
                    SmartTaskRow(
                        task = task,
                        checked = task.id in routineState.done,
                        count = routineState.counters[task.id] ?: 0,
                        onToggle = { onToggle(task.id) },
                        onBump = { d ->
                            routineState = RoutineStore.bumpCounter(context, task.id, d)
                        },
                        hi = hi
                    )
                }
            }
        }

        if (tasks.isNotEmpty() && doneCount >= tasks.size) {
            Spacer(Modifier.height(12.dp))
            Text(
                Strings.s("all_done", hi),
                color = Color(0xFF067647),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(10.dp))
        PostureTrackCard(done = routineState.done, onToggle = onToggle)
        DebloatCheckinCard()
        ChewingCard(done = routineState.done, onToggle = onToggle)
        SleepTrackerCard()
        MonthlyRecapCard(report = report)

        Spacer(Modifier.height(8.dp))
        Text(
            Strings.s("tip_rescan", hi),
            fontSize = 12.sp,
            color = PslGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun SmartTaskRow(
    task: RoutineTask,
    checked: Boolean,
    count: Int,
    onToggle: () -> Unit,
    onBump: (Int) -> Unit,
    hi: Boolean
) {
    // v2.7: glass task row; sage pastel when done
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) EqSage else Color.White.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF12B76A),
                    uncheckedColor = PslGrey
                )
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    taskTitle(task, hi),
                    color = PslText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                EqBody(taskDetail(task, hi), size = 13)
            }
            if (task.counterTarget > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TaskCounterBtn("−") { onBump(-1) }
                        Text(
                            "$count/${task.counterTarget}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PslText,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        TaskCounterBtn("+") { onBump(1) }
                    }
                    val unit = taskUnit(task, hi)
                    if (unit.isNotBlank()) {
                        Text(unit, fontSize = 10.sp, color = PslGrey)
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCounterBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(EqLine.copy(alpha = 0.55f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PslText)
    }
}

// ---------- Chewing: 10-min gum timer ----------

@Composable
private fun ChewingCard(
    done: Set<String>,
    onToggle: (String) -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(10 * 60) }
    var running by remember { mutableStateOf(false) }
    var marked by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running && secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
        if (secondsLeft == 0 && running) {
            running = false
            if (!marked) {
                marked = true
                onToggle("chew_gum")
            }
        }
    }

    val mm = secondsLeft / 60
    val ss = (secondsLeft % 60).toString().padStart(2, '0')
    val logged = marked || "chew_gum" in done
    // v2.6-hinglish: gum timer translations.
    val hi = LanguageStore.isHinglish

    EqGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column {
            EqSectionLabel(Strings.s("jaw_caps", hi))
            Spacer(Modifier.height(6.dp))
            Text(
                Strings.s("jaw_title", hi),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PslText
            )
            EqBody(Strings.s("jaw_detail", hi), size = 13)
            Spacer(Modifier.height(12.dp))
            Text(
                "$mm:$ss",
                fontFamily = EqSerif,
                fontSize = 46.sp,
                color = PslDeep,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 1f - secondsLeft / 600f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MogCoral,
                trackColor = EqLine
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EqDarkPillButton(
                    text = if (running) Strings.s("jaw_pause", hi)
                    else if (secondsLeft < 600) Strings.s("jaw_resume", hi)
                    else Strings.s("jaw_start", hi),
                    onClick = { running = !running },
                    modifier = Modifier.weight(1f)
                )
                EqPillButton(
                    text = Strings.s("jaw_reset", hi),
                    onClick = { running = false; secondsLeft = 600; marked = false },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                Strings.s("jaw_tmj", hi),
                fontSize = 12.sp,
                color = Color(0xFFB42318)
            )
            if (logged) {
                Spacer(Modifier.height(4.dp))
                Text(
                    Strings.s("jaw_logged", hi),
                    fontSize = 12.sp,
                    color = Color(0xFF067647),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
