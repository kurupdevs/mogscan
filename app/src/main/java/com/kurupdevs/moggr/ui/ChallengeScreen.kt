package com.kurupdevs.moggr.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.kurupdevs.moggr.util.ChallengeId
import com.kurupdevs.moggr.util.ChallengeState
import com.kurupdevs.moggr.util.ChallengeStore
import com.kurupdevs.moggr.util.ProfileStore
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_FMT = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.US)

// ---------- entry ----------

@Composable
fun ChallengeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val state = remember(tick) { ChallengeStore.state(context) }
    var tab by remember { mutableIntStateOf(0) }
    val reload = { tick++ }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoggrBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("‹ Back", color = PslBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.weight(1f))
        }
        CapsLabel("CHALLENGE PROGRAMS")
        Spacer(Modifier.height(8.dp))
        Text(
            "Pick a lane,",
            fontFamily = MogSerif,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = PslText,
            lineHeight = 36.sp
        )
        Text(
            "stay consistent.",
            fontFamily = MogSerif,
            fontStyle = FontStyle.Italic,
            fontSize = 32.sp,
            color = PslText,
            lineHeight = 36.sp
        )
        Spacer(Modifier.height(16.dp))

        // Programs / Buddy tabs
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(PslCard)
                .padding(4.dp)
        ) {
            listOf("Programs", "Buddy").forEachIndexed { i, label ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (tab == i) PslDeep else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { tab = i }
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (tab == i) androidx.compose.ui.graphics.Color.White else PslGrey,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        when (tab) {
            0 -> ProgramsPane(state = state, onChanged = reload)
            else -> BuddyPane(state = state, onChanged = reload)
        }
        Spacer(Modifier.height(20.dp))
    }
}

// ---------- programs tab ----------

@Composable
private fun ProgramsPane(state: ChallengeState, onChanged: () -> Unit) {
    val context = LocalContext.current
    val active = state.active
    if (active == null) {
        Text(
            "Two structured runs. Daily photo check-ins, streaks, one freeze per week. All on your phone — nothing leaves the device.",
            fontSize = 14.sp,
            color = PslGrey
        )
        Spacer(Modifier.height(14.dp))
        ChallengeId.entries.forEach { id ->
            ProgramCard(
                id = id,
                onStart = {
                    ChallengeStore.start(context, id)
                    onChanged()
                }
            )
            Spacer(Modifier.height(12.dp))
        }
    } else {
        ActiveChallengeView(state = state, onChanged = onChanged)
    }
}

@Composable
private fun ProgramCard(id: ChallengeId, onStart: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(PslBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${id.days}",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        id.title,
                        fontFamily = MogSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = PslText
                    )
                    Text(id.tagline, fontSize = 13.sp, color = PslGrey)
                }
            }
            Spacer(Modifier.height(12.dp))
            CapsLabel("DAILY TASKS")
            Spacer(Modifier.height(6.dp))
            ChallengeStore.tasksForDay(id, 1).forEach { t ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text("• ", color = PslBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("${t.title} — ${t.detail}", fontSize = 13.sp, color = PslText)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (id.days == 7) "+ a rotating focus task each day · 1 streak freeze"
                else "Tasks rotate across skin / sleep / hair / posture · 4 streak freezes",
                fontSize = 12.sp,
                color = PslGrey,
                fontStyle = FontStyle.Italic
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = PslDeep),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Start ${id.title}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ---------- active challenge ----------

@Composable
private fun ActiveChallengeView(state: ChallengeState, onChanged: () -> Unit) {
    val context = LocalContext.current
    val active = state.active ?: return
    val start = state.start ?: LocalDate.now()
    val today = LocalDate.now()
    val doneCount = state.checkins.size
    var showQuit by remember { mutableStateOf(false) }

    // Streak header
    Card(
        colors = CardDefaults.cardColors(containerColor = PslDeep),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            CapsLabelLight("${active.title.uppercase()}")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${state.streak}",
                    fontFamily = MogSerif,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = androidx.compose.ui.graphics.Color.White,
                    lineHeight = 64.sp
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.padding(bottom = 8.dp)) {
                    Text(
                        "day streak",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    Text(
                        if (state.broken) "streak broken — revive it below"
                        else "day ${state.dayIndex.coerceAtMost(active.days)} of ${active.days}",
                        fontSize = 12.sp,
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { doneCount / active.days.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PslBlue,
                trackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "$doneCount/${active.days} check-ins",
                fontSize = 12.sp,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val todayCovered = today in state.checkins.keys || today in state.frozen
                OutlinedButton(
                    onClick = {
                        if (ChallengeStore.useFreeze(context)) onChanged()
                    },
                    enabled = state.freezesAvailable > 0 && !todayCovered,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Freeze (${state.freezesAvailable})",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (state.reviveAvailable) {
                    Button(
                        onClick = {
                            if (ChallengeStore.revive(context)) onChanged()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Revive streak", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
            if (state.reviveAvailable) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Revive is one-time per challenge and resets your freezes.",
                    fontSize = 11.sp,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }

    if (state.dayIndex > active.days) {
        Spacer(Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFECFDF3)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    "Challenge complete.",
                    fontFamily = MogSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = PslText
                )
                Text(
                    "Final streak: ${state.streak} days. Line up your day-1 and final photos — that's the real report card.",
                    fontSize = 13.sp,
                    color = PslGrey
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Progress strip: photo thumbnails, filmstrip style
    if (state.checkins.isNotEmpty()) {
        CapsLabel("PROGRESS STRIP")
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(
                (1..active.days).mapNotNull { day ->
                    val date = start.plusDays((day - 1).toLong())
                    state.checkins[date]?.let { day to it }
                }
            ) { (day, file) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val bmp = remember(file.absolutePath) {
                        BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = "Day $day check-in",
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PslCard)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Day $day", fontSize = 11.sp, color = PslGrey, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // Day list
    CapsLabel("DAY BY DAY")
    Spacer(Modifier.height(8.dp))
    (1..active.days).forEach { day ->
        val date = start.plusDays((day - 1).toLong())
        DayRow(
            id = active,
            day = day,
            date = date,
            isToday = date == today,
            checkedIn = date in state.checkins.keys,
            frozenDay = date in state.frozen,
            photoFile = state.checkins[date],
            onChanged = onChanged
        )
        Spacer(Modifier.height(8.dp))
    }

    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = {
                ChallengeStore.start(context, active)
                onChanged()
            },
            shape = RoundedCornerShape(50),
            modifier = Modifier.weight(1f)
        ) {
            Text("Restart", color = PslBlue, fontSize = 13.sp)
        }
        TextButton(onClick = { showQuit = true }, modifier = Modifier.weight(1f)) {
            Text("Quit challenge", color = PslGrey, fontSize = 13.sp)
        }
    }

    if (showQuit) {
        AlertDialog(
            onDismissRequest = { showQuit = false },
            title = { Text("Quit this challenge?", fontWeight = FontWeight.Bold, color = PslText) },
            text = { Text("Your check-in photos stay on your phone. Streak resets.", color = PslGrey, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    ChallengeStore.quit(context)
                    showQuit = false
                    onChanged()
                }) { Text("Quit", color = androidx.compose.ui.graphics.Color(0xFFB42318), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showQuit = false }) { Text("Keep going", color = PslBlue) }
            }
        )
    }
}

@Composable
private fun CapsLabelLight(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.65f),
        letterSpacing = 2.sp
    )
}

@Composable
private fun DayRow(
    id: ChallengeId,
    day: Int,
    date: LocalDate,
    isToday: Boolean,
    checkedIn: Boolean,
    frozenDay: Boolean,
    photoFile: File?,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    var expanded by remember(day, id) { mutableStateOf(isToday) }
    var showPicker by remember { mutableStateOf(false) }
    var done by remember(day, id) {
        mutableStateOf(ChallengeStore.doneTasks(context, id, date))
    }
    val tasks = remember(day, id) { ChallengeStore.tasksForDay(id, day) }
    val isPast = date.isBefore(LocalDate.now())

    val statusColor = when {
        checkedIn -> androidx.compose.ui.graphics.Color(0xFF12B76A)
        frozenDay -> PslBlue
        isToday -> PslBlue
        else -> PslGrey
    }
    val statusText = when {
        checkedIn -> "checked in"
        frozenDay -> "frozen"
        isToday -> "today"
        isPast -> "missed"
        else -> "upcoming"
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) androidx.compose.ui.graphics.Color.White else PslCard
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isToday) 4.dp else 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isToday) PslDeep else androidx.compose.ui.graphics.Color(0xFFEDE7DB)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$day",
                        color = if (isToday) androidx.compose.ui.graphics.Color.White else PslText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Day $day",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = PslText
                    )
                    Text(date.format(DAY_FMT), fontSize = 12.sp, color = PslGrey)
                }
                if (checkedIn && photoFile != null) {
                    val bmp = remember(photoFile.absolutePath) {
                        BitmapFactory.decodeFile(photoFile.absolutePath)?.asImageBitmap()
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = "Day $day photo",
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
                Text(
                    statusText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = statusColor
                )
            }

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                tasks.forEach { t ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                done = ChallengeStore.toggleTask(context, id, date, t.id)
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = t.id in done,
                            onCheckedChange = {
                                done = ChallengeStore.toggleTask(context, id, date, t.id)
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = androidx.compose.ui.graphics.Color(0xFF12B76A),
                                uncheckedColor = PslGrey
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PslText)
                            Text(t.detail, fontSize = 12.sp, color = PslGrey)
                        }
                    }
                }
                if (isToday && !checkedIn) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showPicker = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Daily photo check-in", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        "Saved only on this phone — never uploaded.",
                        fontSize = 11.sp,
                        color = PslGrey,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }
            }
        }
    }

    if (showPicker) {
        CheckinPickerDialog(
            id = id,
            date = date,
            onDone = {
                showPicker = false
                onChanged()
            },
            onDismiss = { showPicker = false }
        )
    }
}

// ---------- daily photo check-in: camera or gallery, app-private only ----------

@Composable
private fun CheckinPickerDialog(
    id: ChallengeId,
    date: LocalDate,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pendingUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            pendingUri?.let { uri ->
                decodeDownscaled(context, uri, 1024)?.let { bmp ->
                    ChallengeStore.saveCheckinPhoto(context, id, date, bmp)
                    bmp.recycle()
                    ChallengeStore.recordCheckin(context, id, date)
                    onDone()
                    return@rememberLauncherForActivityResult
                }
            }
        }
        onDismiss()
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            decodeDownscaled(context, uri, 1024)?.let { bmp ->
                ChallengeStore.saveCheckinPhoto(context, id, date, bmp)
                bmp.recycle()
                ChallengeStore.recordCheckin(context, id, date)
                onDone()
                return@rememberLauncherForActivityResult
            }
        }
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Check-in photo", fontWeight = FontWeight.Bold, color = PslText) },
        text = {
            Column {
                Text("How do you want to take today's photo?", color = PslGrey, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val tmp = File(
                            File(context.cacheDir, "shared").apply { mkdirs() },
                            "checkin-tmp.jpg"
                        ).apply { createNewFile() }
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            tmp
                        )
                        pendingUri = uri
                        cameraLauncher.launch(uri)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PslDeep),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Take photo", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pick from gallery", color = PslBlue, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = PslGrey) }
        }
    )
}

/** Downscales to maxDim on the long edge so gallery/camera shots stay light. */
private fun decodeDownscaled(context: Context, uri: Uri, maxDim: Int): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { ins ->
            BitmapFactory.decodeStream(ins, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        while (longest / (sample * 2) >= maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { ins ->
            BitmapFactory.decodeStream(ins, null, opts)
        }
    } catch (_: Exception) {
        null
    }
}

// ---------- buddy tab: local-only, no accounts, no servers ----------

@Composable
private fun BuddyPane(state: ChallengeState, onChanged: () -> Unit) {
    val context = LocalContext.current
    var codeInput by remember { mutableStateOf("") }
    var pairError by remember { mutableStateOf(false) }
    var buddyStreakInput by remember(state.buddyStreak) {
        mutableStateOf(if (state.buddyStreak > 0) "${state.buddyStreak}" else "")
    }

    Text(
        "Run it with a friend. No accounts, no servers — each phone tracks its own streak. Your buddy sends you their code, you pair it here.",
        fontSize = 14.sp,
        color = PslGrey
    )
    Spacer(Modifier.height(14.dp))

    // Your code
    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            CapsLabel("YOUR BUDDY CODE")
            Spacer(Modifier.height(8.dp))
            Text(
                state.myCode,
                fontFamily = MogSerif,
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PslText,
                letterSpacing = 6.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Send this to your buddy. They enter it on their phone.",
                fontSize = 13.sp,
                color = PslGrey
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                ChallengeStore.regenerateCode(context)
                onChanged()
            }) {
                Text("Regenerate code", color = PslBlue, fontSize = 13.sp)
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // Pair with buddy
    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            CapsLabel("PAIR WITH BUDDY")
            Spacer(Modifier.height(8.dp))
            if (state.buddyCode == null) {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = {
                        codeInput = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(6)
                        pairError = false
                    },
                    label = { Text("Buddy's 6-char code") },
                    singleLine = true,
                    isError = pairError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (pairError) {
                    Text(
                        "That code didn't work — check it with your buddy.",
                        fontSize = 12.sp,
                        color = androidx.compose.ui.graphics.Color(0xFFB42318),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        pairError = !ChallengeStore.pairBuddy(context, codeInput)
                        if (!pairError) {
                            codeInput = ""
                            onChanged()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PslDeep),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Pair up", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Paired with ${state.buddyCode}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = PslText,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "Their streak: ${state.buddyStreak} days",
                            fontSize = 13.sp,
                            color = PslGrey
                        )
                    }
                    TextButton(onClick = {
                        ChallengeStore.unpairBuddy(context)
                        onChanged()
                    }) {
                        Text("Unpair", color = PslGrey, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // Buddy streak (honor system) + compare
    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            CapsLabel("BUDDY'S STREAK")
            Spacer(Modifier.height(8.dp))
            Text(
                "Your buddy types their own streak number — honor system, no verification. Keep it honest.",
                fontSize = 13.sp,
                color = PslGrey
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = buddyStreakInput,
                    onValueChange = {
                        buddyStreakInput = it.filter { c -> c.isDigit() }.take(3)
                    },
                    label = { Text("Days") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = {
                        ChallengeStore.setBuddyStreak(
                            context,
                            buddyStreakInput.toIntOrNull() ?: 0
                        )
                        onChanged()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    val profile = ProfileStore.load(context)
                    val myName = profile?.name?.takeIf { it.isNotBlank() } ?: "You"
                    val challengeTitle = state.active?.title ?: "Streak"
                    shareStreakCard(
                        context = context,
                        myName = myName,
                        myStreak = state.streak,
                        buddyTag = state.buddyCode ?: "Buddy",
                        buddyStreak = state.buddyStreak,
                        challengeTitle = challengeTitle
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PslDeep),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Compare & share card", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ---------- shareable streak showdown card (android.graphics.Canvas) ----------

private const val SC_CARD_W = 1080
private const val SC_CARD_H = 1920
private const val SC_CREAM: Int = 0xFFFAF7F1.toInt()
private const val SC_INK: Int = 0xFF1C1917.toInt()
private const val SC_CORAL: Int = 0xFFE07856.toInt()
private const val SC_GREY: Int = 0xFF78716C.toInt()

/**
 * Renders a shareable streak showdown card (buddy streak is self-reported,
 * never verified) and shares it as a PNG via FileProvider. No new permissions.
 */
fun shareStreakCard(
    context: Context,
    myName: String,
    myStreak: Int,
    buddyTag: String,
    buddyStreak: Int,
    challengeTitle: String
) {
    val bmp = Bitmap.createBitmap(SC_CARD_W, SC_CARD_H, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    drawStreakCard(canvas, myName, myStreak, buddyTag, buddyStreak, challengeTitle)
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    val file = File(dir, "streakcard-${System.currentTimeMillis()}.png")
    file.outputStream().use { out ->
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    bmp.recycle()
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share your streak card"))
}

private fun drawStreakCard(
    c: android.graphics.Canvas,
    myName: String,
    myStreak: Int,
    buddyTag: String,
    buddyStreak: Int,
    challengeTitle: String
) {
    val cx = SC_CARD_W / 2f
    c.drawColor(SC_CREAM)

    fun paint(size: Float, color: Int, style: Int = Typeface.NORMAL): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.SERIF, style)
            textAlign = Paint.Align.CENTER
        }

    // Wordmark
    val wordmark = paint(84f, SC_INK, Typeface.BOLD).apply { letterSpacing = 0.35f }
    c.drawText("MOGGR", cx, 220f, wordmark)
    val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SC_CORAL }
    c.drawRect(cx - 90f, 262f, cx + 90f, 272f, rule)

    c.drawText("STREAK SHOWDOWN", cx, 400f, paint(42f, SC_GREY).apply { letterSpacing = 0.2f })
    c.drawText(
        challengeTitle,
        cx, 480f,
        paint(56f, SC_INK, Typeface.ITALIC)
    )

    // Side-by-side streaks
    val leftX = SC_CARD_W * 0.28f
    val rightX = SC_CARD_W * 0.72f
    c.drawText(myName.uppercase(), leftX, 720f, paint(40f, SC_GREY).apply { letterSpacing = 0.15f })
    c.drawText(buddyTag.uppercase(), rightX, 720f, paint(40f, SC_GREY).apply { letterSpacing = 0.15f })
    c.drawText("$myStreak", leftX, 950f, paint(260f, SC_CORAL, Typeface.BOLD))
    c.drawText("$buddyStreak", rightX, 950f, paint(260f, SC_INK, Typeface.BOLD))
    c.drawText("days", leftX, 1030f, paint(44f, SC_GREY))
    c.drawText("days", rightX, 1030f, paint(44f, SC_GREY))

    // Divider
    val div = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SC_GREY; alpha = 90 }
    c.drawRect(140f, 1150f, SC_CARD_W - 140f, 1153f, div)

    // Verdict
    val diff = myStreak - buddyStreak
    val verdict = when {
        diff > 0 -> "You're up by $diff — hold the gap."
        diff < 0 -> "Down by ${-diff} — chase mode on."
        else -> "Dead even. Photo finish."
    }
    c.drawText(verdict, cx, 1300f, paint(52f, SC_INK, Typeface.BOLD))

    // How-to line
    val howTo = paint(42f, SC_GREY).apply { letterSpacing = 0.05f }
    c.drawText("Daily photo check-ins.", cx, 1440f, howTo)
    c.drawText("Buddy streaks are self-reported — honor system.", cx, 1510f, howTo)

    // Footer watermark
    c.drawText(
        "Moggr · streaks tracked on-device · not medical advice",
        cx, 1820f,
        paint(36f, SC_GREY)
    )
}
