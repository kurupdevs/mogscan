package com.kurupdevs.moggr.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kurupdevs.moggr.R
import com.kurupdevs.moggr.analysis.FaceAnalyzer
import com.kurupdevs.moggr.analysis.PslReport
import com.kurupdevs.moggr.coach.CoachClient
import com.kurupdevs.moggr.util.ChallengeStore
import com.kurupdevs.moggr.util.CoachMemory
import com.kurupdevs.moggr.util.LanguageStore
import com.kurupdevs.moggr.util.ROUTINE_TASKS
import com.kurupdevs.moggr.util.RoutineStore
import com.kurupdevs.moggr.util.ScanHistoryStore
import com.kurupdevs.moggr.util.TeenMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class ChatMsg(
    val isUser: Boolean,
    val text: String,
    val time: Long = System.currentTimeMillis(),
    val isPhotoScan: Boolean = false
)

// ---- reference-matched palette (Gemini-style pastel) ----
private val GInk = Color(0xFF1F1B16)
private val GMuted = Color(0xFF8E867B)
private val GFaint = Color(0xFFB9B2A6)
private val GLavCard = Color(0xFFE9E0F8)
private val GBlue1 = Color(0xFF2E7CF6)
private val GBlue2 = Color(0xFF6FA8FA)
private val GGradA = Color(0xFF7C5CFC)
private val GGradB = Color(0xFF3B82F6)
private val GCardShadow = Color(0x14000000)

private fun geminiBg(): Brush = Brush.linearGradient(
    colors = listOf(
        Color(0xFFFFF9F2),
        Color(0xFFEAF1FD),
        Color(0xFFF1EAFB)
    )
)

private fun sparkleBrush(): Brush = Brush.linearGradient(
    colors = listOf(GGradA, GGradB)
)

private fun timeFmt(ts: Long): String =
    Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))

private fun dayFmt(d: LocalDate): String =
    d.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US))

private fun coachFallback(hi: Boolean): String = Strings.s("coach_fallback", hi)

// ---------------------------------------------------------------------------
// Root: home <-> chat + overlays
// ---------------------------------------------------------------------------

@Composable
fun CoachScreen(
    report: PslReport?,
    userName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val hi = LanguageStore.isHinglish
    var screen by remember { mutableStateOf("home") }
    var showChallenges by remember { mutableStateOf(false) }
    var showVoice by remember { mutableStateOf(false) }
    var showFlows by remember { mutableStateOf(false) }
    var activeFlow by remember { mutableStateOf<GuidedFlow?>(null) }
    var vibe by remember { mutableStateOf(CoachMemory.getVibe(context)) }
    var pendingPrompt by remember { mutableStateOf<String?>(null) }
    val name = userName.trim()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(geminiBg())
    ) {
        if (screen == "home") {
            CoachHome(
                name = name,
                hi = hi,
                onMenu = onBack,
                onBell = { showChallenges = true },
                onSearch = { screen = "chat" },
                onSeeAll = { showChallenges = true },
                onUpcoming = { showChallenges = true },
                onActivity = { screen = "chat" }
            )
        } else {
            CoachChat(
                report = report,
                name = name,
                hi = hi,
                vibe = vibe,
                onVibe = { v ->
                    CoachMemory.saveVibe(context, v)
                    vibe = v
                },
                pendingPrompt = pendingPrompt,
                onPromptConsumed = { pendingPrompt = null },
                onHome = { screen = "home" },
                onVoice = { showVoice = true },
                onOpenFlows = { showFlows = true }
            )
        }

        if (showChallenges) ChallengeScreen(onBack = { showChallenges = false })
        if (showVoice) VoiceCheckScreen(onClose = { showVoice = false })

        if (showFlows) {
            Dialog(onDismissRequest = { showFlows = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(20.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (hi) "Coach tools" else "Coach tools",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = GInk,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { showFlows = false }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Filled.Close, contentDescription = null, tint = GMuted)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        GUIDED_FLOWS.forEach { flow ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF6F3EE))
                                    .clickable {
                                        showFlows = false
                                        activeFlow = flow
                                    }
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Text(flow.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GInk)
                                    Spacer(Modifier.height(2.dp))
                                    Text(flow.subtitle, fontSize = 13.sp, color = GMuted)
                                }
                            }
                        }
                    }
                }
            }
        }

        activeFlow?.let { flow ->
            GuidedFlowDialog(
                flow = flow,
                onDismiss = { activeFlow = null },
                onSend = { msg ->
                    activeFlow = null
                    pendingPrompt = msg
                    screen = "chat"
                }
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Home — "Hello" assistant screen (reference 1)
// ---------------------------------------------------------------------------

private data class ActivityItem(
    val icon: String,
    val title: String,
    val subtitle: String,
    val time: String
)

@Composable
private fun CoachHome(
    name: String,
    hi: Boolean,
    onMenu: () -> Unit,
    onBell: () -> Unit,
    onSearch: () -> Unit,
    onSeeAll: () -> Unit,
    onUpcoming: () -> Unit,
    onActivity: () -> Unit
) {
    val context = LocalContext.current
    val initial = name.firstOrNull()?.uppercase() ?: "M"

    // Real upcoming: active challenge, else daily check-in nudge.
    val chState = remember { ChallengeStore.state(context) }
    val active = chState.active
    val upcomingTitle = if (active != null) "${active.title} ›" else if (hi) "Daily check-in ›" else "Daily check-in ›"
    val upcomingLines = if (active != null) {
        listOf(dayFmt(LocalDate.now()), "Today", "Day ${chState.dayIndex} of ${active.days} · check-in due")
    } else {
        listOf(
            dayFmt(LocalDate.now()), "Today",
            if (hi) "Routine + photo log karo" else "Log your routine + photo"
        )
    }
    val upcomingImg = R.drawable.coach_upcoming

    // Real recent activity.
    val scans = remember { ScanHistoryStore.load(context) }
    val lastScan = scans.firstOrNull()
    val checkin = remember { CoachMemory.todayCheckin(context) }
    val streak = remember { RoutineStore.load(context).streak }
    val activity = buildList {
        lastScan?.let {
            add(
                ActivityItem(
                    icon = "◍",
                    title = if (hi) "Face scan" else "Face scan",
                    subtitle = "${String.format(Locale.US, "%.1f", it.psl)} PSL · ${it.tier}",
                    time = timeFmt(it.timestamp)
                )
            )
        }
        checkin?.let {
            add(
                ActivityItem(
                    icon = "☀",
                    title = if (hi) "Morning check-in" else "Morning check-in",
                    subtitle = "${it.sleep} sleep · ${it.focus}",
                    time = if (hi) "Aaj" else "Today"
                )
            )
        }
        if (streak > 0) {
            add(
                ActivityItem(
                    icon = "🔥",
                    title = if (hi) "Routine streak" else "Routine streak",
                    subtitle = "$streak ${if (hi) "din lagatar" else "days and counting"}",
                    time = if (hi) "Aaj" else "Today"
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
            Spacer(Modifier.height(10.dp))
            // Top bar: menu | bell + avatar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIcon(icon = Icons.Filled.Menu, onClick = onMenu)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleIcon(icon = Icons.Filled.Notifications, onClick = onBell)
                    Spacer(Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(sparkleBrush()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(initial, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = if (name.isNotBlank()) {
                    if (hi) "Namaste, $name" else "Hello, $name"
                } else {
                    if (hi) "Namaste" else "Hello"
                },
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = GInk
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (hi) "Aaj main tumhari kaise help karun?" else "How can I support you today?",
                fontSize = 15.sp,
                color = GMuted
            )
            Spacer(Modifier.height(22.dp))
            // Search bar -> opens chat
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(50), spotColor = GCardShadow)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .clickable(onClick = onSearch)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(sparkleBrush()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✦", fontSize = 22.sp, color = Color.White)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "looksmax assistant",
                        fontSize = 16.sp,
                        color = GInk,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF4F1EC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = GInk)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (hi) "Aane wala" else "Your Upcoming", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = GInk)
                Text(
                    if (hi) "Sab dekho" else "See All",
                    fontSize = 13.sp,
                    color = GMuted,
                    modifier = Modifier.clickable(onClick = onSeeAll)
                )
            }
            Spacer(Modifier.height(10.dp))
            // Upcoming card (lavender)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(GLavCard)
                    .clickable(onClick = onUpcoming)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(upcomingTitle, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = GInk)
                        Spacer(Modifier.height(10.dp))
                        upcomingLines.forEach { line ->
                            Text(line, fontSize = 14.sp, color = GInk.copy(alpha = 0.85f))
                            Spacer(Modifier.height(2.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Image(
                        painter = painterResource(upcomingImg),
                        contentDescription = null,
                        modifier = Modifier
                            .width(104.dp)
                            .height(128.dp)
                            .clip(RoundedCornerShape(22.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(if (hi) "Recent activity" else "Recent Activity", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = GInk)
            Spacer(Modifier.height(10.dp))
            if (activity.isEmpty()) {
                WhiteActivityCard(
                    item = ActivityItem(
                        "✦",
                        if (hi) "Abhi kuch nahi" else "Nothing yet",
                        if (hi) "Scan karke shuru karo" else "Start with a face scan",
                        ""
                    ),
                    onClick = onActivity
                )
            } else {
                activity.take(3).forEach { item ->
                    WhiteActivityCard(item = item, onClick = onActivity)
                    Spacer(Modifier.height(10.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
}

@Composable
private fun CircleIcon(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .shadow(4.dp, CircleShape, spotColor = GCardShadow)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = GInk, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun WhiteActivityCard(item: ActivityItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(26.dp), spotColor = GCardShadow)
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.icon, fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        item.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (item.subtitle.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(item.subtitle, fontSize = 14.sp, color = GMuted, modifier = Modifier.padding(start = 32.dp))
                }
                if (item.time.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(item.time, fontSize = 13.sp, color = GFaint)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Chat — Gemini-style looksmax assistant (reference 2)
// ---------------------------------------------------------------------------

@Composable
private fun CoachChat(
    report: PslReport?,
    name: String,
    hi: Boolean,
    vibe: String,
    onVibe: (String) -> Unit,
    pendingPrompt: String?,
    onPromptConsumed: () -> Unit,
    onHome: () -> Unit,
    onVoice: () -> Unit,
    onOpenFlows: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val teen = TeenMode.isTeen(context)

    var showCheckin by remember { mutableStateOf(false) }
    var celebration by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showVibeDialog by remember { mutableStateOf(false) }
    var showCrisisLink by remember { mutableStateOf(false) }
    var crisisExpanded by remember { mutableStateOf(false) }

    fun hasDistressWords(text: String): Boolean {
        val lower = text.lowercase(Locale.US)
        return listOf("suicid", "kill myself", "self-harm", "selfharm", "want to die", "hopeless")
            .any { lower.contains(it) }
    }

    val metricsCtx = remember(report) {
        report?.let {
            CoachClient.reportContext(
                it.overallPsl,
                it.features.map { f -> f.name to f.score },
                it.anglesRead,
                decile = it.decile,
                tier = pslLabel(it.overallPsl),
                failos = it.failoCount,
                halos = it.haloCount,
                pillars = it.pillars.map { p -> p.name to p.score }
            )
        } ?: "No scan yet — user hasn't completed a face scan."
    }
    var checkinNonce by remember { mutableStateOf(0) }
    val baseSystem = remember(report, vibe, checkinNonce, hi) {
        CoachClient.systemPrompt(
            name.ifBlank { "there" },
            metricsCtx,
            CoachMemory.getMemoryContext(context),
            teen = teen,
            hinglish = hi
        )
    }

    val greeting = remember(report, hi) {
        val n = name.ifBlank { if (hi) "bhai" else "there" }
        if (hi) {
            if (report != null) "Yo $n — scan me ${String.format(Locale.US, "%.1f", report.overallPsl)} PSL aaya. Looks se related kuch bhi puch: jawline, skin, hair, posture, photos."
            else "Yo $n — looks se related kuch bhi puch: jawline, skin, hair, posture, photos."
        } else {
            if (report != null) "Yo $n — I see your scan: ${String.format(Locale.US, "%.1f", report.overallPsl)} PSL. Ask me anything about looks: jawline, skin, hair, posture, photos."
            else "Yo $n — ask me anything about looks: jawline, skin, hair, posture, photos."
        }
    }

    var messages by remember { mutableStateOf(listOf(ChatMsg(false, greeting))) }
    var input by remember { mutableStateOf("") }
    var waiting by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    LaunchedEffect(Unit) {
        if (!CoachMemory.checkinCardShownToday(context)) {
            CoachMemory.markCheckinCardShown(context)
            showCheckin = true
        }
        val streak = RoutineStore.load(context).streak
        val streakKey = when {
            streak >= 30 && !CoachMemory.hasSeenCelebration(context, "streak_30") -> "streak_30"
            streak >= 7 && !CoachMemory.hasSeenCelebration(context, "streak_7") -> "streak_7"
            else -> null
        }
        if (streakKey != null && celebration == null) {
            CoachMemory.markCelebration(context, streakKey)
            celebration = streakKey to if (streakKey == "streak_30")
                "30-day streak — a whole month locked in. Keep the streak alive."
            else
                "7-day streak — a full week of showing up. Keep the streak alive."
        }
    }

    LaunchedEffect(report) {
        val rep = report ?: return@LaunchedEffect
        val weakest = rep.features.sortedBy { it.score }.take(3).map { it.name }
        val isBest = CoachMemory.saveReportSummary(
            context,
            CoachMemory.ReportSummary(
                psl = rep.overallPsl,
                tier = pslLabel(rep.overallPsl),
                weakest = weakest,
                date = LocalDate.now().toString()
            )
        )
        if (isBest) {
            val pslStr = String.format(Locale.US, "%.1f", rep.overallPsl)
            val key = "pb_$pslStr"
            CoachMemory.markCelebration(context, key)
            celebration = key to "New all-time best: $pslStr PSL — the work is paying off. Keep the streak alive."
        }
    }

    fun history(): List<Pair<Boolean, String>> = messages.map { it.isUser to it.text }

    fun sendText(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || waiting) return
        if (hasDistressWords(clean)) showCrisisLink = true
        val h = history()
        messages = messages + ChatMsg(true, clean)
        input = ""
        waiting = true
        CoachClient.ask(baseSystem, h, clean) { reply ->
            waiting = false
            messages = messages + ChatMsg(false, reply ?: coachFallback(hi))
            if (reply == null) showCrisisLink = true
        }
    }

    // Guided-flow prompt arriving from the home screen.
    LaunchedEffect(pendingPrompt) {
        pendingPrompt?.let {
            onPromptConsumed()
            sendText(it)
        }
    }

    fun sendPhoto(uri: Uri) {
        if (waiting) return
        val h = history()
        messages = messages + ChatMsg(true, "Break down this photo.", isPhotoScan = true)
        waiting = true
        scope.launch(Dispatchers.Default) {
            try {
                val bmp = decodeDownscaled(context, uri)
                val rep = FaceAnalyzer.analyze(listOf(bmp)).report
                val photoCtx = "FRESH PHOTO SCAN done on-device just now (single photo): " +
                    CoachClient.reportContext(
                        rep.overallPsl,
                        rep.features.map { f -> f.name to f.score },
                        1,
                        decile = rep.decile,
                        tier = pslLabel(rep.overallPsl),
                        failos = rep.failoCount,
                        halos = rep.haloCount,
                        pillars = rep.pillars.map { p -> p.name to p.score }
                    )
                val sys = baseSystem + "\n\nThe user just sent an additional photo, " +
                    "scanned on-device: $photoCtx Analyze THIS photo scan now — full " +
                    "breakdown: verdict, why, top 3 fixes, what not to worry about. " +
                    "Note: this is a single-photo read, so treat it as a limited estimate — " +
                    "say so briefly."
                CoachClient.ask(
                    sys, h,
                    "I just sent a photo — give me your full analysis of it."
                ) { reply ->
                    waiting = false
                    messages = messages + ChatMsg(false, reply ?: coachFallback(hi))
                    if (reply == null) showCrisisLink = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    waiting = false
                    messages = messages + ChatMsg(
                        false,
                        if (hi) "Photo padh nahi paya — dusri try karo." else "Couldn't read that photo — try another one."
                    )
                }
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) sendPhoto(uri)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header: back | sparkle + Looksmax AI | vibe(speaker)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleIcon(icon = Icons.Filled.ChevronLeft, onClick = onHome)
            Spacer(Modifier.width(12.dp))
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(sparkleBrush()),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✦", fontSize = 14.sp, color = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                Text("Looksmax AI", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = GInk)
            }
            Spacer(Modifier.width(12.dp))
            CircleIcon(icon = Icons.Filled.Explore, onClick = onOpenFlows)
            Spacer(Modifier.width(8.dp))
            CircleIcon(icon = Icons.Filled.VolumeUp, onClick = { showVibeDialog = true })
        }

        // Real streak card (blue, reference style)
        StreakCard(hi = hi, onTip = { sendText(if (hi) "Aaj ke liye ek quick looksmax tip de." else "Give me one quick looksmax tip for today.") })
        Spacer(Modifier.height(6.dp))

        if (showCheckin) {
            MorningCheckinCard(
                onDone = { sleep, puff, focus ->
                    CoachMemory.logCheckin(context, CoachMemory.Checkin(sleep, puff, focus))
                    showCheckin = false
                    checkinNonce++
                },
                onSkip = { showCheckin = false }
            )
        }
        celebration?.let { (_, text) ->
            CelebrationBanner(text = text, onDismiss = { celebration = null })
        }

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 10.dp)
        ) {
            items(messages) { msg ->
                if (msg.isUser) UserBubble(msg = msg, name = name)
                else AiMessage(msg = msg)
            }
            if (waiting) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SparkleAvatar(size = 30)
                        Spacer(Modifier.width(10.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = GGradA,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (hi) "Soch raha hai…" else "Thinking…",
                            color = GMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        if (showCrisisLink) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                CrisisLinkButton(expanded = crisisExpanded) {
                    crisisExpanded = !crisisExpanded
                }
                if (crisisExpanded) {
                    CrisisCard()
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // Input pill: + photo | text | mic | gradient send
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .shadow(8.dp, RoundedCornerShape(50), spotColor = GCardShadow)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF4F1EC))
                        .clickable(enabled = !waiting) { photoPicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = GInk, modifier = Modifier.size(22.dp))
                }
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("looksmax agent", color = GFaint, fontSize = 15.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = GInk,
                        unfocusedTextColor = GInk,
                        cursorColor = GGradA
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendText(input) }),
                    singleLine = false,
                    maxLines = 4
                )
                IconButton(onClick = onVoice, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Filled.Mic, contentDescription = null, tint = GMuted, modifier = Modifier.size(22.dp))
                }
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(sparkleBrush())
                        .clickable(enabled = !waiting && input.isNotBlank()) { sendText(input) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("➤", fontSize = 20.sp, color = Color.White)
                }
            }
        }
        Text(
            if (hi) "Photo tumhare phone pe hi scan hoti hai." else "Photos are scanned on-device only.",
            fontSize = 11.sp,
            color = GFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )
    }

    if (showVibeDialog) {
        Dialog(onDismissRequest = { showVibeDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (hi) "Coach ka style" else "Coach vibe",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = GInk,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showVibeDialog = false }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = GMuted)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (hi) "Coach tumse kaise baat karega." else "How your coach talks to you.",
                        fontSize = 13.sp,
                        color = GMuted
                    )
                    Spacer(Modifier.height(14.dp))
                    VibeSegmentedControl(
                        vibe = vibe,
                        onVibe = { v ->
                            onVibe(v)
                            showVibeDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SparkleAvatar(size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(sparkleBrush()),
        contentAlignment = Alignment.Center
    ) {
        Text("✦", fontSize = (size * 0.55).sp, color = Color.White)
    }
}

@Composable
private fun UserBubble(msg: ChatMsg, name: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .shadow(3.dp, RoundedCornerShape(22.dp), spotColor = GCardShadow)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White)
                .padding(14.dp)
        ) {
            Column {
                if (msg.isPhotoScan) {
                    Text("PHOTO SCAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GGradA)
                    Spacer(Modifier.height(4.dp))
                }
                Text(msg.text, color = GInk, fontSize = 15.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFFE4DDF5)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.firstOrNull()?.uppercase() ?: "M",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = GInk
            )
        }
    }
}

@Composable
private fun AiMessage(msg: ChatMsg) {
    Column {
        Text(
            timeFmt(msg.time),
            fontSize = 12.sp,
            color = GFaint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Top) {
            SparkleAvatar(size = 30)
            Spacer(Modifier.width(10.dp))
            Text(
                msg.text,
                color = GInk,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Blue streak card (reference style) — real routine/scan data
// ---------------------------------------------------------------------------

@Composable
private fun StreakCard(hi: Boolean, onTip: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val routine = remember { RoutineStore.load(context) }
    val scans = remember { ScanHistoryStore.load(context) }
    val totalTasks = ROUTINE_TASKS.size
    val doneTasks = routine.done.size.coerceAtMost(totalTasks)
    val scanCount = scans.size
    var liked by remember { mutableStateOf<Boolean?>(null) }

    val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US))
    val summary = "${routine.streak}-day streak · $doneTasks/$totalTasks tasks today · $scanCount scans"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(28.dp), spotColor = GCardShadow)
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.linearGradient(listOf(GBlue1, GBlue2)))
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (hi) "Routine Streak" else "Routine Streak",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(dateStr, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${routine.streak}",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (hi) "din ki streak" else "day streak",
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (hi) "Roz thoda-thoda. Glow compound hota hai." else "Small steps daily. The glow compounds.",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.35f))
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StreakMiniCard(
                        label = if (hi) "Aaj" else "Today",
                        value = "$doneTasks/$totalTasks",
                        sub = if (hi) "Tasks done" else "Tasks done",
                        progress = if (totalTasks > 0) doneTasks / totalTasks.toFloat() else 0f,
                        modifier = Modifier.weight(1f)
                    )
                    StreakMiniCard(
                        label = if (hi) "Scans" else "Scans",
                        value = "$scanCount",
                        sub = if (hi) "Total scans" else "Total scans",
                        progress = (scanCount / 10f).coerceIn(0f, 1f),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Reaction row — working: like/dislike, refresh = new tip, copy = copy summary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ReactionBtn(
                icon = Icons.Filled.ThumbUp,
                active = liked == true,
                onClick = { liked = if (liked == true) null else true }
            )
            ReactionBtn(
                icon = Icons.Filled.ThumbDown,
                active = liked == false,
                onClick = { liked = if (liked == false) null else false }
            )
            ReactionBtn(icon = Icons.Filled.Refresh, active = false, onClick = onTip)
            Spacer(Modifier.weight(1f))
            ReactionBtn(
                icon = Icons.Filled.ContentCopy,
                active = false,
                onClick = { clipboard.setText(AnnotatedString(summary)) }
            )
        }
    }
}

@Composable
private fun StreakMiniCard(
    label: String,
    value: String,
    sub: String,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(14.dp)
    ) {
        Column {
            Text(label, fontSize = 12.sp, color = GMuted)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GInk)
            Spacer(Modifier.height(8.dp))
            Text(sub, fontSize = 11.sp, color = GMuted)
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = GBlue1,
                trackColor = Color(0xFFE4E9F2)
            )
        }
    }
}

@Composable
private fun ReactionBtn(icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (active) Color(0xFFE4DDF5) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (active) GGradA else GFaint, modifier = Modifier.size(19.dp))
    }
}

private fun decodeDownscaled(context: Context, uri: Uri): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    }
    var sample = 1
    while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
    return context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts)
    } ?: throw IllegalStateException("Couldn't read that photo.")
}
