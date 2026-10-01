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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import com.kurupdevs.moggr.util.ScanHistoryStore
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
    onRescan: () -> Unit,
    onOpenProfile: () -> Unit = {}
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
    Box(modifier = Modifier.fillMaxSize().background(AppBg)) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            // v3.0: fixed white bottom bar, icon + label — reference style.
            if (!showChallenges) {
                LtBottomBar(
                    items = tabs.map { Strings.s("tab_${it.key}", hi) to it.icon },
                    selected = tab,
                    onSelect = { tab = it }
                )
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
                        onGoTab = { tab = it },
                        onOpenProfile = onOpenProfile
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
            .background(LtPill)
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(false to "EN", true to "HI").forEach { (isHi, label) ->
            val selected = hi == isHi
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) LtInk else Color.Transparent)
                    .clickable { LanguageStore.set(context, isHi) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else LtMuted
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
    onGoTab: (Int) -> Unit,
    onOpenProfile: () -> Unit
) {
    val context = LocalContext.current
    // v2.6-hinglish: current app language.
    val hi = LanguageStore.isHinglish
    // v2.6-voice: voice check overlay toggle
    var showVoice by remember { mutableStateOf(false) }

    if (report == null) {
        LightWelcome(onRescan = onRescan)
        return
    }

    // v3.0: light wellness home — reference style.
    val displayName = profile?.name?.takeIf { it.isNotBlank() }
    val greeting = buildString {
        append(daypartGreeting())
        if (displayName != null) append(", $displayName")
        append(".")
    }
    val routineState = remember { RoutineStore.load(context) }
    var seg by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().background(AppBg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StreakPill(streak = routineState.streak)
                LanguageToggle()
            }
            Spacer(Modifier.height(22.dp))
            LtGreeting(greeting)
            Spacer(Modifier.height(6.dp))
            LtSub(weekLabel())
            Spacer(Modifier.height(20.dp))

            ScoreCard(report = report, profile = profile, onRescan = onRescan)
            Spacer(Modifier.height(26.dp))

            LtSectionTitle(title = if (hi) "Aaj ki inspiration" else "Today's inspiration")
            Spacer(Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(end = 20.dp)
            ) {
                item {
                    LtPhotoCard(
                        imageRes = R.drawable.inspire_standtall,
                        title = if (hi) "Seedhe khade raho" else "Stand tall",
                        body = if (hi) "2 minute me posture fix karo." else "Fix your posture in 2 minutes flat.",
                        onClick = { onGoTab(3) }
                    )
                }
                item {
                    LtPhotoCard(
                        imageRes = R.drawable.inspire_debloat,
                        title = if (hi) "Morning reset" else "Morning reset",
                        body = if (hi) "Subah debloat, shaam tak glow." else "Debloat before noon, glow by evening.",
                        onClick = onChallenges
                    )
                }
                item {
                    LtPhotoCard(
                        imageRes = R.drawable.inspire_glowbasics,
                        title = if (hi) "Glow basics" else "Glow basics",
                        body = if (hi) "Skin pehle. Baaki sab baad me." else "Skin first. Everything else follows.",
                        onClick = { onGoTab(3) }
                    )
                }
            }
            Spacer(Modifier.height(26.dp))

            LtSectionTitle(
                title = if (hi) "Meri activity progress" else "My activity progress",
                sub = if (hi) "Apna week dekho. Tumhari practice, tumhare pauses, sab ek jagah."
                else "Look back on your week. Your practice, your pauses, all in one place."
            )
            Spacer(Modifier.height(12.dp))
            LtSegmented(
                options = listOf(
                    if (hi) "Routine" else "Routine",
                    if (hi) "Scans" else "Scans",
                    if (hi) "Voice" else "Voice"
                ),
                selected = seg,
                onSelect = { seg = it }
            )
            Spacer(Modifier.height(12.dp))
            when (seg) {
                0 -> LtWeekPanel(
                    title = if (hi) "Routine" else "Routine",
                    days = lastWeekActivity(context)
                )
                1 -> LtWeekPanel(
                    title = if (hi) "Scans" else "Scans",
                    days = lastWeekScans(context)
                )
                else -> LtWeekPanel(
                    title = if (hi) "Voice" else "Voice",
                    days = lastWeekVoice(context)
                )
            }
            Spacer(Modifier.height(26.dp))

            ChallengesPromoCard(onChallenges = onChallenges)
            Spacer(Modifier.height(8.dp))
            VoiceCheckCard(onOpen = { showVoice = true })
            Spacer(Modifier.height(16.dp))

            // v3.1: developer card moved to the very bottom.
            DeveloperCard(onOpen = onOpenProfile)
            Spacer(Modifier.height(28.dp))
        }
        if (showVoice) {
            VoiceCheckScreen(onClose = { showVoice = false })
        }
    }
}

/** First-run welcome — light, centered, reference-clean. */
@Composable
private fun LightWelcome(onRescan: () -> Unit) {
    val hi = LanguageStore.isHinglish
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            LanguageToggle()
        }
        Spacer(Modifier.height(36.dp))
        LtGreeting(if (hi) "Apna best face pao." else "Find your best face.")
        Spacer(Modifier.height(8.dp))
        LtSub(
            if (hi) "3-angle AI scan. 100% phone pe. Free forever."
            else "3-angle AI scan. 100% on-device. Free forever."
        )
        Spacer(Modifier.height(28.dp))
        LtPhotoCard(
            imageRes = R.drawable.inspire_posture,
            title = if (hi) "Scan se shuru karo" else "Start with a scan",
            body = if (hi) "3 photo, 2 minute, poora face report." else "3 photos, 2 minutes, your full face report.",
            onClick = onRescan,
            modifier = Modifier.width(340.dp).height(210.dp)
        )
        Spacer(Modifier.height(28.dp))
        LtDarkButton(
            text = if (hi) "Scan shuru karo" else "Start your scan",
            onClick = onRescan,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** Compact score hero card. */
@Composable
private fun ScoreCard(
    report: PslReport,
    profile: UserProfile?,
    onRescan: () -> Unit
) {
    val context = LocalContext.current
    val hi = LanguageStore.isHinglish
    LtWhiteCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (hi) "TUMHARA SCORE" else "YOUR SCORE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                        color = LtMuted
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f", report.overallPsl),
                        fontFamily = LtSerif,
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        color = LtInk
                    )
                    Text(
                        text = "${pslTierShort(report.overallPsl)} · Top ${report.percentile}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PkCoralDeep
                    )
                }
                LtDarkButton(
                    text = if (hi) "Rescan" else "Rescan",
                    onClick = onRescan
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (hi) "Report share karo" else "Share report",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = LtMuted,
                modifier = Modifier.clickable { shareReport(context, profile, report) }
            )
        }
    }
}

/** Streak flame pill for the home header. */
@Composable
private fun StreakPill(streak: Int) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(LtPill)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🔥", fontSize = 15.sp)
        Spacer(Modifier.width(6.dp))
        Text(
            text = "$streak",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = LtInk
        )
    }
}

/**
 * v3.0 developer option — extra pink card, "DEVELOPER" label on top,
 * "Ayush" in a nice serif style below, tappable → profile.
 */
@Composable
private fun DeveloperCard(onOpen: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = Color(0x1A3A2E1A))
            .clip(RoundedCornerShape(20.dp))
            .background(PkBg)
            .clickable(onClick = onOpen)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.dev_avatar),
                contentDescription = "Developer",
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "DEVELOPER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp,
                    color = PkMuted
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Ayush",
                    fontFamily = LtSerif,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = PkInk
                )
            }
            Text(
                text = "View profile →",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PkCoralDeep
            )
        }
    }
}

/** Scan days this week (Sun..Sat) from the on-device scan history. */
fun lastWeekScans(context: android.content.Context): List<LtDay> {
    val today = java.time.LocalDate.now()
    val dow = today.dayOfWeek.value % 7
    val sunday = today.minusDays(dow.toLong())
    val scanned = try {
        ScanHistoryStore.load(context).map {
            java.time.Instant.ofEpochMilli(it.timestamp)
                .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        }.toSet()
    } catch (_: Exception) { emptySet() }
    return (0..6).map { i ->
        val d = sunday.plusDays(i.toLong())
        val state = when {
            d in scanned -> DayState.DONE
            d.isEqual(today) -> DayState.PENDING
            d.isAfter(today) -> DayState.FUTURE
            else -> DayState.PENDING
        }
        LtDay(
            d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.US),
            state
        )
    }
}

/** Voice drill days this week — only the real recorded drill day counts. */
fun lastWeekVoice(context: android.content.Context): List<LtDay> {
    val today = java.time.LocalDate.now()
    val dow = today.dayOfWeek.value % 7
    val sunday = today.minusDays(dow.toLong())
    val lastDrill = try {
        context.getSharedPreferences("moggr_voice", android.content.Context.MODE_PRIVATE)
            .getString("last_drill", null)?.let { java.time.LocalDate.parse(it) }
    } catch (_: Exception) { null }
    return (0..6).map { i ->
        val d = sunday.plusDays(i.toLong())
        val state = when {
            d == lastDrill -> DayState.DONE
            d.isEqual(today) -> DayState.PENDING
            d.isAfter(today) -> DayState.FUTURE
            else -> DayState.PENDING
        }
        LtDay(
            d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.US),
            state
        )
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
            .background(AppBg)
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
            .background(AppBg)
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
