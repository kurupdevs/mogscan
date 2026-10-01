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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import com.kurupdevs.moggr.ui.theme.EqGreigeDeep
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqInkSoft
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.EqPillDark
import com.kurupdevs.moggr.ui.theme.MogCoral
import com.kurupdevs.moggr.util.ChallengeId
import com.kurupdevs.moggr.util.ChallengeState
import com.kurupdevs.moggr.util.ChallengeStore
import com.kurupdevs.moggr.util.LanguageStore
import com.kurupdevs.moggr.util.ProfileStore
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DAY_FMT = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.US)

/** v2.7-hinglish: challenge titles mapped by enum (util stays English-only). */
private fun challengeTitleFor(id: ChallengeId, hi: Boolean): String {
    val key = when (id) {
        ChallengeId.DEBLOAT7 -> "ch_t_debloat"
        ChallengeId.GLOWUP30 -> "ch_t_glowup"
    }
    return Strings.s(key, hi).let { if (it == key) id.title else it }
}

private fun challengeTaglineFor(id: ChallengeId, hi: Boolean): String {
    val key = when (id) {
        ChallengeId.DEBLOAT7 -> "ch_tag_debloat"
        ChallengeId.GLOWUP30 -> "ch_tag_glowup"
    }
    return Strings.s(key, hi).let { if (it == key) id.tagline else it }
}

// ---------- entry ----------

@Composable
fun ChallengeScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val state = remember(tick) { ChallengeStore.state(context) }
    var tab by remember { mutableIntStateOf(0) }
    val reload: () -> Unit = { tick++ }
    val hi = LanguageStore.isHinglish

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(eqBackgroundBrush())
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EqPillButton(text = Strings.s("ch_back", hi), onClick = onBack)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        EqSectionLabel(Strings.s("ch_caps", hi))
        Spacer(Modifier.height(8.dp))
        EqHeadline(Strings.s("ch_head1", hi))
        EqHeadline(Strings.s("ch_head2", hi))
        Spacer(Modifier.height(16.dp))

        // Programs / Buddy tabs
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f))
                .padding(4.dp)
        ) {
            listOf(Strings.s("ch_tab_programs", hi), Strings.s("ch_tab_buddy", hi)).forEachIndexed { i, label ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (tab == i) EqPillDark else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { tab = i }
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        color = if (tab == i) androidx.compose.ui.graphics.Color.White else EqMuted,
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
    val hi = LanguageStore.isHinglish
    val active = state.active
    if (active == null) {
        EqBody(Strings.s("ch_intro", hi))
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
    val hi = LanguageStore.isHinglish
    EqGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MogCoral),
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
                        challengeTitleFor(id, hi),
                        fontFamily = EqSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = EqInk
                    )
                    Text(challengeTaglineFor(id, hi), fontSize = 13.sp, color = EqMuted)
                }
            }
            Spacer(Modifier.height(12.dp))
            EqSectionLabel(Strings.s("ch_daily_tasks", hi))
            Spacer(Modifier.height(6.dp))
            // NOTE: task titles/details come from ChallengeStore (util) and stay English.
            ChallengeStore.tasksForDay(id, 1).forEach { t ->
                Row(Modifier.padding(vertical = 3.dp)) {
                    Text("• ", color = MogCoral, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("${t.title} — ${t.detail}", fontSize = 13.sp, color = EqInk)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                Strings.s(if (id.days == 7) "ch_foot7" else "ch_foot30", hi),
                fontSize = 12.sp,
                color = EqMuted,
                fontStyle = FontStyle.Italic
            )
            Spacer(Modifier.height(12.dp))
            EqCoralPillButton(
                text = Strings.fmt("ch_start", hi, "t" to challengeTitleFor(id, hi)),
                onClick = onStart,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------- active challenge ----------

@Composable
private fun ActiveChallengeView(state: ChallengeState, onChanged: () -> Unit) {
    val context = LocalContext.current
    val hi = LanguageStore.isHinglish
    val active = state.active ?: return
    val start = state.start ?: LocalDate.now()
    val today = LocalDate.now()
    val doneCount = state.checkins.size
    var showQuit by remember { mutableStateOf(false) }

    // Streak header
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(EqRound))
            .background(EqPillDark)
            .padding(20.dp)
    ) {
        Column {
            CapsLabelLight(challengeTitleFor(active, hi).uppercase())
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${state.streak}",
                    fontFamily = EqSerif,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = androidx.compose.ui.graphics.Color.White,
                    lineHeight = 64.sp
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.padding(bottom = 8.dp)) {
                    Text(
                        Strings.s("ch_day_streak", hi),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                    Text(
                        if (state.broken) Strings.s("ch_broken", hi)
                        else Strings.fmt(
                            "ch_day_of", hi,
                            "di" to "${state.dayIndex.coerceAtMost(active.days)}",
                            "d" to "${active.days}"
                        ),
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
                color = MogCoral,
                trackColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                Strings.fmt("ch_checkins", hi, "c" to "$doneCount", "d" to "${active.days}"),
                fontSize = 12.sp,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val todayCovered = today in state.checkins.keys || today in state.frozen
                EqPillButton(
                    text = Strings.fmt("ch_freeze", hi, "n" to "${state.freezesAvailable}"),
                    onClick = {
                        if (ChallengeStore.useFreeze(context)) onChanged()
                    },
                    enabled = state.freezesAvailable > 0 && !todayCovered,
                    modifier = Modifier.weight(1f)
                )
                if (state.reviveAvailable) {
                    EqCoralPillButton(
                        text = Strings.s("ch_revive", hi),
                        onClick = {
                            if (ChallengeStore.revive(context)) onChanged()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (state.reviveAvailable) {
                Spacer(Modifier.height(6.dp))
                Text(
                    Strings.s("ch_revive_note", hi),
                    fontSize = 11.sp,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }

    if (state.dayIndex > active.days) {
        Spacer(Modifier.height(12.dp))
        EqGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                EqHeadline(Strings.s("ch_complete", hi), size = 22)
                Spacer(Modifier.height(6.dp))
                EqBody(
                    Strings.fmt("ch_complete_body", hi, "s" to "${state.streak}"),
                    size = 13
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // Progress strip: photo thumbnails, filmstrip style
    if (state.checkins.isNotEmpty()) {
        EqSectionLabel(Strings.s("ch_progress_strip", hi))
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
                            contentDescription = Strings.fmt("ch_day", hi, "d" to "$day"),
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
                                .background(EqGreigeDeep)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        Strings.fmt("ch_day", hi, "d" to "$day"),
                        fontSize = 11.sp,
                        color = EqMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    // Day list
    EqSectionLabel(Strings.s("ch_day_by_day", hi))
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
        EqPillButton(
            text = Strings.s("ch_restart", hi),
            onClick = {
                ChallengeStore.start(context, active)
                onChanged()
            },
            modifier = Modifier.weight(1f)
        )
        EqPillButton(
            text = Strings.s("ch_quit", hi),
            onClick = { showQuit = true },
            modifier = Modifier.weight(1f)
        )
    }

    if (showQuit) {
        AlertDialog(
            onDismissRequest = { showQuit = false },
            title = { Text(Strings.s("ch_quit_title", hi), fontWeight = FontWeight.Bold, color = EqInk) },
            text = { Text(Strings.s("ch_quit_body", hi), color = EqInkSoft, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    ChallengeStore.quit(context)
                    showQuit = false
                    onChanged()
                }) { Text(Strings.s("ch_quit_yes", hi), color = androidx.compose.ui.graphics.Color(0xFFB42318), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showQuit = false }) { Text(Strings.s("ch_quit_no", hi), color = MogCoral) }
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
    val hi = LanguageStore.isHinglish
    var expanded by remember(day, id) { mutableStateOf(isToday) }
    var showPicker by remember { mutableStateOf(false) }
    var done by remember(day, id) {
        mutableStateOf(ChallengeStore.doneTasks(context, id, date))
    }
    val tasks = remember(day, id) { ChallengeStore.tasksForDay(id, day) }
    val isPast = date.isBefore(LocalDate.now())

    val statusColor = when {
        checkedIn -> androidx.compose.ui.graphics.Color(0xFF12B76A)
        frozenDay -> MogCoral
        isToday -> MogCoral
        else -> EqMuted
    }
    val statusText = when {
        checkedIn -> Strings.s("ch_st_checked", hi)
        frozenDay -> Strings.s("ch_st_frozen", hi)
        isToday -> Strings.s("ch_st_today", hi)
        isPast -> Strings.s("ch_st_missed", hi)
        else -> Strings.s("ch_st_upcoming", hi)
    }

    EqGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isToday) EqPillDark else EqGreigeDeep),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$day",
                        color = if (isToday) androidx.compose.ui.graphics.Color.White else EqInk,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        Strings.fmt("ch_day", hi, "d" to "$day"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = EqInk
                    )
                    Text(date.format(DAY_FMT), fontSize = 12.sp, color = EqMuted)
                }
                if (checkedIn && photoFile != null) {
                    val bmp = remember(photoFile.absolutePath) {
                        BitmapFactory.decodeFile(photoFile.absolutePath)?.asImageBitmap()
                    }
                    if (bmp != null) {
                        Image(
                            bitmap = bmp,
                            contentDescription = Strings.fmt("ch_day", hi, "d" to "$day"),
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
                // NOTE: task titles/details come from ChallengeStore (util) and stay English.
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
                                uncheckedColor = EqMuted
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = EqInk)
                            Text(t.detail, fontSize = 12.sp, color = EqMuted)
                        }
                    }
                }
                if (isToday && !checkedIn) {
                    Spacer(Modifier.height(8.dp))
                    EqCoralPillButton(
                        text = Strings.s("ch_daily_checkin", hi),
                        onClick = { showPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        Strings.s("ch_ondevice", hi),
                        fontSize = 11.sp,
                        color = EqMuted,
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
    val hi = LanguageStore.isHinglish
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
        title = { Text(Strings.s("ch_picker_title", hi), fontWeight = FontWeight.Bold, color = EqInk) },
        text = {
            Column {
                Text(Strings.s("ch_picker_how", hi), color = EqInkSoft, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                EqDarkPillButton(
                    text = Strings.s("ch_take_photo", hi),
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
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                EqPillButton(
                    text = Strings.s("ch_pick_gallery", hi),
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.s("ch_cancel", hi), color = EqMuted) }
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
    val hi = LanguageStore.isHinglish
    var codeInput by remember { mutableStateOf("") }
    var pairError by remember { mutableStateOf(false) }
    var buddyStreakInput by remember(state.buddyStreak) {
        mutableStateOf(if (state.buddyStreak > 0) "${state.buddyStreak}" else "")
    }

    EqBody(Strings.s("ch_buddy_intro", hi))
    Spacer(Modifier.height(14.dp))

    // Your code
    EqGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            EqSectionLabel(Strings.s("ch_your_code", hi))
            Spacer(Modifier.height(8.dp))
            EqHeadline(state.myCode, size = 40)
            Spacer(Modifier.height(4.dp))
            EqBody(Strings.s("ch_code_sub", hi), size = 13)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                ChallengeStore.regenerateCode(context)
                onChanged()
            }) {
                Text(Strings.s("ch_regen", hi), color = MogCoral, fontSize = 13.sp)
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // Pair with buddy
    EqGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            EqSectionLabel(Strings.s("ch_pair_title", hi))
            Spacer(Modifier.height(8.dp))
            if (state.buddyCode == null) {
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = {
                        codeInput = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(6)
                        pairError = false
                    },
                    label = { Text(Strings.s("ch_code_label", hi)) },
                    singleLine = true,
                    isError = pairError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (pairError) {
                    Text(
                        Strings.s("ch_code_error", hi),
                        fontSize = 12.sp,
                        color = androidx.compose.ui.graphics.Color(0xFFB42318),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                EqCoralPillButton(
                    text = Strings.s("ch_pair_up", hi),
                    onClick = {
                        pairError = !ChallengeStore.pairBuddy(context, codeInput)
                        if (!pairError) {
                            codeInput = ""
                            onChanged()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            Strings.fmt("ch_paired", hi, "c" to (state.buddyCode ?: "")),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = EqInk,
                            letterSpacing = 2.sp
                        )
                        Text(
                            Strings.fmt("ch_their_streak", hi, "n" to "${state.buddyStreak}"),
                            fontSize = 13.sp,
                            color = EqMuted
                        )
                    }
                    TextButton(onClick = {
                        ChallengeStore.unpairBuddy(context)
                        onChanged()
                    }) {
                        Text(Strings.s("ch_unpair", hi), color = EqMuted, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    // Buddy streak (honor system) + compare
    EqGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            EqSectionLabel(Strings.s("ch_buddy_streak", hi))
            Spacer(Modifier.height(8.dp))
            EqBody(Strings.s("ch_honor", hi), size = 13)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = buddyStreakInput,
                    onValueChange = {
                        buddyStreakInput = it.filter { c -> c.isDigit() }.take(3)
                    },
                    label = { Text(Strings.s("ch_days_label", hi)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(10.dp))
                EqDarkPillButton(
                    text = Strings.s("ch_save", hi),
                    onClick = {
                        ChallengeStore.setBuddyStreak(
                            context,
                            buddyStreakInput.toIntOrNull() ?: 0
                        )
                        onChanged()
                    }
                )
            }
            Spacer(Modifier.height(14.dp))
            EqCoralPillButton(
                text = Strings.s("ch_share_card", hi),
                onClick = {
                    val profile = ProfileStore.load(context)
                    val myName = profile?.name?.takeIf { it.isNotBlank() } ?: "You"
                    val challengeTitle = state.active?.let { challengeTitleFor(it, hi) }
                        ?: Strings.s("ch_streak_fallback", hi)
                    shareStreakCard(
                        context = context,
                        myName = myName,
                        myStreak = state.streak,
                        buddyTag = state.buddyCode ?: "Buddy",
                        buddyStreak = state.buddyStreak,
                        challengeTitle = challengeTitle,
                        hi = hi
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
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
    challengeTitle: String,
    hi: Boolean
) {
    val bmp = Bitmap.createBitmap(SC_CARD_W, SC_CARD_H, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    drawStreakCard(canvas, myName, myStreak, buddyTag, buddyStreak, challengeTitle, hi)
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
    context.startActivity(Intent.createChooser(intent, Strings.s("ch_sc_share", hi)))
}

private fun drawStreakCard(
    c: android.graphics.Canvas,
    myName: String,
    myStreak: Int,
    buddyTag: String,
    buddyStreak: Int,
    challengeTitle: String,
    hi: Boolean
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

    c.drawText(Strings.s("ch_sc_title", hi), cx, 400f, paint(42f, SC_GREY).apply { letterSpacing = 0.2f })
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
    val daysLabel = Strings.s("ch_sc_days", hi)
    c.drawText(daysLabel, leftX, 1030f, paint(44f, SC_GREY))
    c.drawText(daysLabel, rightX, 1030f, paint(44f, SC_GREY))

    // Divider
    val div = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SC_GREY; alpha = 90 }
    c.drawRect(140f, 1150f, SC_CARD_W - 140f, 1153f, div)

    // Verdict
    val diff = myStreak - buddyStreak
    val verdict = when {
        diff > 0 -> Strings.fmt("ch_sc_up", hi, "d" to "$diff")
        diff < 0 -> Strings.fmt("ch_sc_down", hi, "d" to "${-diff}")
        else -> Strings.s("ch_sc_even", hi)
    }
    c.drawText(verdict, cx, 1300f, paint(52f, SC_INK, Typeface.BOLD))

    // How-to line
    val howTo = paint(42f, SC_GREY).apply { letterSpacing = 0.05f }
    c.drawText(Strings.s("ch_sc_how", hi), cx, 1440f, howTo)
    c.drawText(Strings.s("ch_sc_honor", hi), cx, 1510f, howTo)

    // Footer watermark
    c.drawText(
        Strings.s("ch_sc_footer", hi),
        cx, 1820f,
        paint(36f, SC_GREY)
    )
}
