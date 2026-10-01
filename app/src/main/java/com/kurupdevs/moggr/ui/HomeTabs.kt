package com.kurupdevs.moggr.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.analysis.PslReport
import com.kurupdevs.moggr.util.ROUTINE_TASKS
import com.kurupdevs.moggr.util.RoutineStore
import com.kurupdevs.moggr.util.UserProfile
import java.util.Locale

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
    Scaffold(
        containerColor = PslBlack,
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                tabs.forEachIndexed { idx, t ->
                    NavigationBarItem(
                        selected = tab == idx,
                        onClick = { tab = idx },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label, fontSize = 12.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PslBlue,
                            selectedTextColor = PslBlue,
                            unselectedIconColor = PslGrey,
                            unselectedTextColor = PslGrey,
                            indicatorColor = PslBlue.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(PslBlack)
        ) {
            when (tab) {
                0 -> HomeTab(report = report, profile = profile, photo = frontPhoto, onRescan = onRescan)
                1 -> MethodScreen()
                2 -> CoachScreen(report = report, userName = userName, onBack = { tab = 0 })
                3 -> RoutineScreen()
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
                .background(PslBlack)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "No scan saved yet",
                color = PslText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Take your 3-angle scan once — your face and report stay saved here.",
                color = PslGrey,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRescan,
                colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(54.dp)
            ) {
                Text("Scan now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PslBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        ReportBody(report = report, profile = profile, photo = photo)
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
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Scan again", color = PslBlue)
            }
            Button(
                onClick = { shareReport(context, profile, report) },
                colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Share", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ---------- Method: how the rating works ----------

@Composable
private fun MethodScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PslBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Method", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = PslBlue)
        Spacer(Modifier.height(4.dp))
        Text(
            "Exactly how Moggr turns your photos into a PSL score. No black box.",
            fontSize = 14.sp,
            color = PslGrey
        )
        Spacer(Modifier.height(18.dp))

        MethodSection("How scoring works") {
            Text(
                "Your face is measured 15 ways from your 3 photos. Each measurement is scored " +
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

        MethodSection("The 15 measurements") {
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
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = PslBlue)
            Spacer(Modifier.height(8.dp))
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

// ---------- Routine: daily softmaxx checklist ----------

@Composable
private fun RoutineScreen() {
    val context = LocalContext.current
    var state by remember { mutableStateOf(RoutineStore.load(context)) }
    val doneCount = state.done.size
    val total = ROUTINE_TASKS.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PslBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text("Routine", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = PslBlue)
        Spacer(Modifier.height(4.dp))
        Text(
            "Small daily habits that stack up. Tick them off every day.",
            fontSize = 14.sp,
            color = PslGrey
        )
        Spacer(Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = PslCard),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "${state.streak}",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PslText
                    )
                    Text("day streak", fontSize = 14.sp, color = PslGrey)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$doneCount/$total today",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PslText
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { doneCount / total.toFloat() },
                        modifier = Modifier
                            .width(140.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = PslBlue,
                        trackColor = Color(0xFFE4E9F2)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        ROUTINE_TASKS.forEach { task ->
            val checked = task.id in state.done
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
                        onCheckedChange = { state = RoutineStore.toggle(context, task.id) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF12B76A),
                            uncheckedColor = PslGrey
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            task.title,
                            color = PslText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(task.detail, color = PslGrey, fontSize = 13.sp)
                    }
                }
            }
        }
        if (doneCount >= total) {
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
        Spacer(Modifier.height(8.dp))
        Text(
            String.format(Locale.US, "Tip: rescan every few weeks under the same lighting to track real change."),
            fontSize = 12.sp,
            color = PslGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
    }
}
