package com.kurupdevs.moggr.ui

import android.graphics.Bitmap
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import com.kurupdevs.moggr.util.PlanStore
import com.kurupdevs.moggr.util.ReportStore
import com.kurupdevs.moggr.util.RoutineStore
import com.kurupdevs.moggr.util.RoutineTask
import com.kurupdevs.moggr.util.TaskCategory
import com.kurupdevs.moggr.util.UserProfile
import kotlinx.coroutines.delay

// ---------- Main tabs ----------

private data class TabDef(val label: String, val icon: ImageVector)

@Composable
fun MainTabs(
    report: PslReport?,
    profile: UserProfile?,
    frontPhoto: Bitmap?,
    userName: String,
    onRescan: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        TabDef("Home", Icons.Filled.Home),
        TabDef("Method", Icons.Filled.School),
        TabDef("Coach", Icons.Filled.Chat),
        TabDef("Routine", Icons.Filled.Checklist)
    )
    Box(modifier = Modifier.fillMaxSize().background(MoggrBg)) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .shadow(16.dp, RoundedCornerShape(50))
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.88f))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { idx, t ->
                        val selected = tab == idx
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) PslDeep else Color.Transparent)
                                .clickable { tab = idx }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                t.icon,
                                contentDescription = t.label,
                                tint = if (selected) Color.White else PslGrey,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                t.label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else PslGrey
                            )
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
            when (tab) {
                0 -> HomeTab(report = report, profile = profile, photo = frontPhoto, onRescan = onRescan)
                1 -> MethodScreen(report = report)
                2 -> CoachScreen(report = report, userName = userName, onBack = { tab = 0 })
                3 -> RoutineScreen()
            }
        }
    }
    }
}

// ---------- Home: saved face + ratings ----------

@Composable
private fun HomeTab(
    report: PslReport?,
    profile: UserProfile?,
    photo: Bitmap?,
    onRescan: () -> Unit
) {
    val context = LocalContext.current
    if (report == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MoggrBg)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CapsLabel("WELCOME TO MOGGR")
            Spacer(Modifier.height(14.dp))
            Text(
                "Know your face.\nOwn your look.",
                fontFamily = MogSerif,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = PslText,
                textAlign = TextAlign.Center,
                lineHeight = 42.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Take your 3-angle scan once — your face and report stay saved here.",
                color = PslGrey,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onRescan,
                colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(56.dp)
            ) {
                Text("Start your scan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoggrBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Greeting header, reference style
        val daypart = remember {
            when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
                in 5..11 -> "morning"
                in 12..16 -> "afternoon"
                in 17..21 -> "evening"
                else -> "night"
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (photo != null) {
                Image(
                    bitmap = photo.asImageBitmap(),
                    contentDescription = "You",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PslDeep),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (profile?.name?.firstOrNull()?.uppercase() ?: "M"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Good $daypart,", fontSize = 13.sp, color = PslGrey)
                Text(
                    if (!profile?.name.isNullOrBlank()) "${profile!!.name} — how's the ascension?"
                    else "How's the ascension going?",
                    fontFamily = MogSerif,
                    fontStyle = FontStyle.Italic,
                    fontSize = 18.sp,
                    color = PslText
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Know your face,",
            fontFamily = MogSerif,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = PslText,
            lineHeight = 38.sp
        )
        Text(
            "own your rating.",
            fontFamily = MogSerif,
            fontStyle = FontStyle.Italic,
            fontSize = 34.sp,
            color = PslText,
            lineHeight = 38.sp
        )
        Spacer(Modifier.height(18.dp))
        ReportBody(report = report, profile = profile, photo = photo)
        Spacer(Modifier.height(20.dp))
        CapsLabel("PROGRESS")
        Spacer(Modifier.height(10.dp))
        ProgressTimeline()
        Spacer(Modifier.height(14.dp))
        SleepMiniCard()
        DebloatMorningMini()
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRescan,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("Scan again", color = PslBlue)
            }
            Button(
                onClick = { shareReport(context, profile, report) },
                colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(50)
            ) {
                Text("Share", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ---------- Method: how the rating works ----------

@Composable
private fun MethodScreen(report: PslReport?) {
    val measuredCount = report?.features?.size ?: 15
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoggrBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        CapsLabel("HOW IT WORKS")
        Spacer(Modifier.height(8.dp))
        Text(
            "The method,",
            fontFamily = MogSerif,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = PslText,
            lineHeight = 36.sp
        )
        Text(
            "no black box.",
            fontFamily = MogSerif,
            fontStyle = FontStyle.Italic,
            fontSize = 32.sp,
            color = PslText,
            lineHeight = 36.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Exactly how Moggr turns your photos into a PSL score.",
            fontSize = 14.sp,
            color = PslGrey
        )
        Spacer(Modifier.height(18.dp))

        SeasonCard(report)
        Spacer(Modifier.height(12.dp))

        MethodSection("How scoring works") {
            Text(
                "Your face is measured $measuredCount ways from your 3 photos. Each measurement is scored " +
                    "1–8 by distance from the community ideal, then grouped into 4 pillars. " +
                    "The pillars combine into your overall PSL. Big deviations cap your tier — " +
                    "one weak area drags the whole score, like community raters tend to judge.",
                fontSize = 14.sp,
                color = PslText
            )
        }

        MethodSection("The 4 pillars") {
            PillarRow("Harmony", "40%", "How well everything fits together — symmetry, thirds, fifths, midface balance. Raters notice this first.")
            PillarRow("Features", "25%", "The individual pieces — eyes, nose, lips, brows, cheekbones.")
            PillarRow("Dimorphism", "20%", "Structure cues — jaw width, chin, brow area.")
            PillarRow("Angularity", "15%", "Sharp vs soft — jaw angle, cheek definition.")
        }

        MethodSection("The $measuredCount measurements") {
            val items = listOf(
                "Symmetry" to "Left-right balance of the whole face.",
                "Facial thirds" to "Forehead, midface and lower face in balance.",
                "Facial fifths" to "Face width split into five equal eye-widths.",
                "Midface ratio" to "How compact the middle of the face is.",
                "Eye spacing (ESR)" to "Distance between the eyes vs face width.",
                "FWHR" to "Face width vs height.",
                "Eyes" to "Shape, tilt and openness.",
                "Nose" to "Width and proportion against the face.",
                "Lips" to "Fullness and width balance.",
                "Jawline" to "Width and definition.",
                "Chin" to "Projection and width.",
                "Brows" to "Density and shape framing the eyes.",
                "Cheekbones" to "Width and prominence.",
                "Jaw angle" to "Frontal sharpness of the jaw corners.",
                "Side profile" to "Chin projection read from your side photo."
            )
            items.forEach { (name, desc) ->
                Row(Modifier.padding(vertical = 5.dp)) {
                    Text("• ", color = PslBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        buildString { append(name); append(" — "); append(desc) },
                        fontSize = 14.sp,
                        color = PslText
                    )
                }
            }
        }

        MethodSection("The tiers") {
            val tiers = listOf(
                "7.75+" to "Gigachad — near-mythical",
                "7.0+" to "Chad",
                "6.0+" to "Chadlite",
                "5.0+" to "HTN — High Tier Normie",
                "3.0+" to "MTN — Mid Tier Normie",
                "1.4+" to "LTN — Low Tier Normie",
                "< 1.4" to "Sub-5 — maximum ascension potential"
            )
            tiers.forEach { (score, tier) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(score, color = PslText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(tier, color = PslGrey, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Tiers are looksmaxxing-community conventions, not medical grades.",
                fontSize = 12.sp,
                color = PslGrey
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = PslCard),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                GuidesSection()
            }
        }

        MethodSection("For an accurate scan") {
            val tips = listOf(
                "Neutral expression, mouth closed, no smile.",
                "Camera at eye level, arm's length away.",
                "Even lighting on the face — no harsh shadows.",
                "Hair off the forehead, nothing covering the jaw.",
                "Look straight ahead for front, full 90° turn for profiles."
            )
            tips.forEach { tip ->
                Row(Modifier.padding(vertical = 4.dp)) {
                    Text("• ", color = PslBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(tip, fontSize = 14.sp, color = PslText)
                }
            }
        }

        MethodSection("Honest limits") {
            Text(
                "PSL ratios are forum conventions, not validated science. Scores shift with " +
                    "lighting, pose, lens and expression — same setup gives comparable results. " +
                    "A number from your camera is a starting point for the stuff you control " +
                    "(skin, hair, fitness, style, posture), not a verdict on your worth.",
                fontSize = 14.sp,
                color = PslText
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun MethodSection(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            CapsLabel(title)
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
private fun RoutineScreen() {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoggrBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        CapsLabel("90-DAY ASCENSION")
        Spacer(Modifier.height(8.dp))
        Text(
            "The plan,",
            fontFamily = MogSerif,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = PslText,
            lineHeight = 36.sp
        )
        Text(
            "day by day.",
            fontFamily = MogSerif,
            fontStyle = FontStyle.Italic,
            fontSize = 32.sp,
            color = PslText,
            lineHeight = 36.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Built from your scan — your weakest areas get the most reps.",
            fontSize = 14.sp,
            color = PslGrey
        )
        Spacer(Modifier.height(16.dp))

        // Phase header card
        Card(
            colors = CardDefaults.cardColors(containerColor = PslCard),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "Day $planDay / 90 · Phase ${phase.index + 1}: ${phase.title}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PslText
                )
                Spacer(Modifier.height(4.dp))
                Text(phase.goal, fontSize = 13.sp, color = PslGrey)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { planDay / 90f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = PslBlue,
                    trackColor = Color(0xFFEDE7DB)
                )
                Spacer(Modifier.height(8.dp))
                val weekIdx = (((planDay - 1) % 30) / 7).coerceIn(0, phase.weeklyFocus.size - 1)
                Text(
                    "This week: ${phase.weeklyFocus[weekIdx]}",
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
                "${routineState.streak}-day streak",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = PslText
            )
            Spacer(Modifier.weight(1f))
            Text("$doneCount/${tasks.size} today", fontSize = 14.sp, color = PslGrey)
        }

        Spacer(Modifier.height(14.dp))
        CapsLabel("TODAY'S PLAN")
        Spacer(Modifier.height(4.dp))

        TaskCategory.entries.forEach { cat ->
            val group = tasks.filter { it.category == cat }
            if (group.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                CapsLabel(cat.label.uppercase())
                Spacer(Modifier.height(4.dp))
                group.forEach { task ->
                    SmartTaskRow(
                        task = task,
                        checked = task.id in routineState.done,
                        count = routineState.counters[task.id] ?: 0,
                        onToggle = { onToggle(task.id) },
                        onBump = { d ->
                            routineState = RoutineStore.bumpCounter(context, task.id, d)
                        }
                    )
                }
            }
        }

        if (tasks.isNotEmpty() && doneCount >= tasks.size) {
            Spacer(Modifier.height(12.dp))
            Text(
                "All done today. Consistency is the whole game.",
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
            "Tip: rescan every few weeks under the same lighting to track real change.",
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
    onBump: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (checked) Color(0xFFECFDF3) else PslCard
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                    task.title,
                    color = PslText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(task.detail, color = PslGrey, fontSize = 13.sp)
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
                    if (task.counterUnit.isNotBlank()) {
                        Text(task.counterUnit, fontSize = 10.sp, color = PslGrey)
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
            .background(Color(0xFFEDE7DB))
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

    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            CapsLabel("JAW SESSION")
            Spacer(Modifier.height(6.dp))
            Text(
                "10-min gum timer",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = PslText
            )
            Text(
                "Chew evenly on both sides — not just your strong side.",
                fontSize = 13.sp,
                color = PslGrey
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "$mm:$ss",
                fontSize = 46.sp,
                fontWeight = FontWeight.ExtraBold,
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
                color = PslBlue,
                trackColor = Color(0xFFEDE7DB)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { running = !running },
                    colors = ButtonDefaults.buttonColors(containerColor = PslDeep),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        if (running) "Pause" else if (secondsLeft < 600) "Resume" else "Start",
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = { running = false; secondsLeft = 600; marked = false },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Reset", color = PslBlue)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Stop if your jaw clicks or hurts — pushing through pain is how TMJ starts.",
                fontSize = 12.sp,
                color = Color(0xFFB42318)
            )
            if (logged) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Logged for today — nice work.",
                    fontSize = 12.sp,
                    color = Color(0xFF067647),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
