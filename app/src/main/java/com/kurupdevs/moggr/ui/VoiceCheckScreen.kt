package com.kurupdevs.moggr.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kurupdevs.moggr.ui.theme.EqGreigeDeep
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqLavender
import com.kurupdevs.moggr.ui.theme.EqLine
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.EqPeach
import com.kurupdevs.moggr.ui.theme.EqPillDark
import com.kurupdevs.moggr.ui.theme.EqSage
import com.kurupdevs.moggr.ui.theme.MogCoral
import com.kurupdevs.moggr.util.LanguageStore
import com.kurupdevs.moggr.util.VoiceAnalyzer
import com.kurupdevs.moggr.util.VoiceResult
import com.kurupdevs.moggr.util.VoiceStore
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class VoicePhase {
    EXPLAINER, PROMPT, RECORDING, ANALYZING, RESULTS, DRILLS, DENIED
}

private const val VOICE_LINE_EN =
    "Good morning. I speak slowly, steady and clear — and every word I say lands exactly where I want it to."
private const val VOICE_LINE_HINGLISH =
    "Namaste. Main aaram se, saaf aur steady bolta hoon — meri har baat poore confidence ke saath utarti hai."

/**
 * On-device voice check: pitch, pace, steadiness + daily drills.
 * The mic permission is requested only here, only after the explainer,
 * and the recording is analyzed in memory — it never leaves the phone.
 */
@Composable
fun VoiceCheckScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hi = LanguageStore.isHinglish

    var phase by remember { mutableStateOf(VoicePhase.EXPLAINER) }
    var useHinglish by remember { mutableStateOf(false) }
    var micError by remember { mutableStateOf<String?>(null) }
    var recordedData by remember { mutableStateOf<ShortArray?>(null) }
    var lastResult by remember { mutableStateOf<VoiceResult?>(null) }
    var voiceStreak by remember { mutableStateOf(VoiceStore.load(context)) }

    // Recording machinery
    val stopFlag = remember { AtomicBoolean(false) }
    val levelBuffer = remember { Collections.synchronizedList(mutableListOf<Float>()) }
    var levels by remember { mutableStateOf(listOf<Float>()) }
    var secondsLeft by remember { mutableIntStateOf(VoiceAnalyzer.RECORD_SECONDS) }
    var recordJob by remember { mutableStateOf<Job?>(null) }

    fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) phase = VoicePhase.PROMPT else phase = VoicePhase.DENIED
    }

    fun startRecording() {
        micError = null
        stopFlag.set(false)
        levelBuffer.clear()
        secondsLeft = VoiceAnalyzer.RECORD_SECONDS
        recordJob?.cancel()
        phase = VoicePhase.RECORDING
        recordJob = scope.launch {
            try {
                val data = VoiceAnalyzer.record(
                    VoiceAnalyzer.RECORD_SECONDS,
                    isCancelled = { stopFlag.get() },
                    onLevel = { levelBuffer.add(it) }
                )
                recordedData = data
                phase = when {
                    stopFlag.get() -> VoicePhase.PROMPT
                    data == null -> {
                        micError = Strings.s("v_mic_error", hi)
                        VoicePhase.PROMPT
                    }
                    else -> VoicePhase.ANALYZING
                }
            } catch (_: CancellationException) {
                // screen closed mid-recording — recorder already released itself
            }
        }
    }

    fun stopRecording() {
        stopFlag.set(true)
    }

    // Live waveform ticker while recording
    LaunchedEffect(phase) {
        if (phase == VoicePhase.RECORDING) {
            while (true) {
                delay(200L)
                levels = synchronized(levelBuffer) { levelBuffer.toList() }
            }
        }
    }

    // Recording countdown
    LaunchedEffect(phase) {
        if (phase == VoicePhase.RECORDING) {
            secondsLeft = VoiceAnalyzer.RECORD_SECONDS
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--
            }
        }
    }

    // Run the analysis once recording finishes
    LaunchedEffect(phase) {
        if (phase == VoicePhase.ANALYZING) {
            val data = recordedData
            lastResult = withContext(Dispatchers.Default) {
                if (data == null) VoiceResult(speechDetected = false)
                else VoiceAnalyzer.analyze(data)
            }
            recordedData = null // drop the audio the moment analysis is done
            phase = VoicePhase.RESULTS
        }
    }

    fun onDrillDone() {
        voiceStreak = VoiceStore.markDrillDone(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(eqBackgroundBrush())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EqPillButton(
                    text = Strings.s("v_back", hi),
                    onClick = {
                        if (phase == VoicePhase.RECORDING) stopFlag.set(true)
                        onClose()
                    }
                )
                Spacer(Modifier.width(12.dp))
                EqSectionLabel(Strings.s("v_caps", hi))
            }
            Spacer(Modifier.height(12.dp))

            when (phase) {
                VoicePhase.EXPLAINER -> VoiceExplainer(
                    onContinue = {
                        if (hasMicPermission()) phase = VoicePhase.PROMPT
                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onClose = onClose
                )
                VoicePhase.DENIED -> VoiceDenied(onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }, onClose = onClose)
                VoicePhase.PROMPT -> VoicePrompt(
                    useHinglish = useHinglish,
                    onToggleLine = { useHinglish = it },
                    micError = micError,
                    streakDays = voiceStreak.streak,
                    onStart = { startRecording() }
                )
                VoicePhase.RECORDING -> VoiceRecording(
                    line = if (useHinglish) VOICE_LINE_HINGLISH else VOICE_LINE_EN,
                    levels = levels,
                    secondsLeft = secondsLeft,
                    onStop = { stopRecording() }
                )
                VoicePhase.ANALYZING -> VoiceAnalyzing()
                VoicePhase.RESULTS -> VoiceResults(
                    result = lastResult,
                    onRetry = { phase = VoicePhase.PROMPT },
                    onDrills = { phase = VoicePhase.DRILLS }
                )
                VoicePhase.DRILLS -> VoiceDrills(
                    streakDays = voiceStreak.streak,
                    doneToday = voiceStreak.doneToday,
                    onDrillDone = { onDrillDone() }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---------- Explainer: shown BEFORE the permission request ----------

@Composable
private fun VoiceExplainer(onContinue: () -> Unit, onClose: () -> Unit) {
    val hi = LanguageStore.isHinglish
    EqGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(EqPeach),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null, tint = MogCoral, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            EqHeadline(Strings.s("v_expl_head", hi), size = 30)
            Spacer(Modifier.height(10.dp))
            EqBody(Strings.s("v_expl_body", hi))
            Spacer(Modifier.height(14.dp))
            VoicePrivacyRow()
            Spacer(Modifier.height(18.dp))
            EqCoralPillButton(
                text = Strings.s("v_expl_continue", hi),
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                Strings.s("v_expl_micnote", hi),
                fontSize = 12.sp,
                color = EqMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VoicePrivacyRow() {
    val hi = LanguageStore.isHinglish
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(EqSage)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = MogCoral, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            Strings.s("v_expl_privacy", hi),
            fontSize = 13.sp,
            color = EqInk,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp
        )
    }
}

// ---------- Denied state ----------

@Composable
private fun VoiceDenied(onOpenSettings: () -> Unit, onClose: () -> Unit) {
    val hi = LanguageStore.isHinglish
    EqGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(EqGreigeDeep),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null, tint = EqMuted, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            EqHeadline(Strings.s("v_denied_head", hi), size = 24, align = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            EqBody(Strings.s("v_denied_body", hi), modifier = Modifier.fillMaxWidth(), size = 14)
            Spacer(Modifier.height(18.dp))
            EqDarkPillButton(
                text = Strings.s("v_denied_settings", hi),
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            EqPillButton(
                text = Strings.s("v_denied_notnow", hi),
                onClick = onClose,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ---------- Prompt: pick a line, then record ----------

@Composable
private fun VoicePrompt(
    useHinglish: Boolean,
    onToggleLine: (Boolean) -> Unit,
    micError: String?,
    streakDays: Int,
    onStart: () -> Unit
) {
    val hi = LanguageStore.isHinglish
    EqSectionLabel(Strings.s("v_prompt_caps", hi))
    Spacer(Modifier.height(8.dp))
    EqHeadline(Strings.s("v_prompt_head1", hi), size = 30)
    EqHeadline(Strings.s("v_prompt_head2", hi), size = 30)
    Spacer(Modifier.height(14.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        VoiceLineOption(
            label = Strings.s("v_opt_english", hi),
            selected = !useHinglish,
            onClick = { onToggleLine(false) },
            modifier = Modifier.weight(1f)
        )
        VoiceLineOption(
            label = Strings.s("v_opt_hinglish", hi),
            selected = useHinglish,
            onClick = { onToggleLine(true) },
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(12.dp))

    EqGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            EqSectionLabel(Strings.s(if (useHinglish) "v_line_hi" else "v_line_en", hi))
            Spacer(Modifier.height(8.dp))
            Text(
                if (useHinglish) VOICE_LINE_HINGLISH else VOICE_LINE_EN,
                fontFamily = EqSerif,
                fontSize = 20.sp,
                color = EqInk,
                lineHeight = 28.sp
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    EqBody(Strings.s("v_prompt_tip", hi), size = 13)
    if (micError != null) {
        Spacer(Modifier.height(8.dp))
        Text(micError, fontSize = 13.sp, color = Color(0xFFB3261E), fontWeight = FontWeight.Medium)
    }
    Spacer(Modifier.height(16.dp))
    EqCoralPillButton(
        text = Strings.s("v_start_rec", hi),
        onClick = onStart,
        modifier = Modifier.fillMaxWidth()
    )
    if (streakDays > 0) {
        Spacer(Modifier.height(10.dp))
        Text(
            Strings.fmt("v_streak_keep", hi, "n" to "$streakDays"),
            fontSize = 13.sp,
            color = EqMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Spacer(Modifier.height(10.dp))
    Text(
        Strings.s("v_rec_privacy", hi),
        fontSize = 12.sp,
        color = EqMuted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun VoiceLineOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) EqPillDark else Color.Transparent)
            .border(1.dp, if (selected) EqPillDark else EqLine, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = if (selected) Color.White else EqInk
        )
    }
}

// ---------- Recording with live waveform ----------

@Composable
private fun VoiceRecording(
    line: String,
    levels: List<Float>,
    secondsLeft: Int,
    onStop: () -> Unit
) {
    val hi = LanguageStore.isHinglish
    EqGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB3261E))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    Strings.fmt("v_rec_title", hi, "s" to "$secondsLeft"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = EqInk
                )
            }
            Spacer(Modifier.height(12.dp))
            EqHeadline(line, size = 18, align = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            VoiceWaveform(levels = levels, modifier = Modifier.fillMaxWidth().height(96.dp))
            Spacer(Modifier.height(16.dp))
            EqPillButton(
                text = Strings.s("v_stop", hi),
                onClick = onStop,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VoiceWaveform(levels: List<Float>, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(EqGreigeDeep)
    ) {
        val bars = levels.takeLast(64)
        val w = size.width
        val h = size.height
        if (bars.isEmpty()) {
            drawRect(
                color = EqLine,
                topLeft = Offset(0f, h / 2f - 1.5f),
                size = Size(w, 3f)
            )
            return@Canvas
        }
        val bw = w / 64f
        bars.forEachIndexed { i, v ->
            val bh = (v.coerceIn(0f, 1f) * h * 0.92f).coerceAtLeast(4f)
            drawRect(
                color = MogCoral,
                topLeft = Offset(i * bw + bw * 0.2f, (h - bh) / 2f),
                size = Size(bw * 0.6f, bh)
            )
        }
    }
}

// ---------- Analyzing ----------

@Composable
private fun VoiceAnalyzing() {
    val hi = LanguageStore.isHinglish
    EqGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = MogCoral)
            Spacer(Modifier.height(18.dp))
            EqHeadline(Strings.s("v_analyzing_head", hi), size = 22, align = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            EqBody(Strings.s("v_analyzing_body", hi), size = 13)
        }
    }
}

// ---------- Results ----------

@Composable
private fun VoiceResults(
    result: VoiceResult?,
    onRetry: () -> Unit,
    onDrills: () -> Unit
) {
    val hi = LanguageStore.isHinglish
    if (result == null || !result.speechDetected) {
        EqGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                EqHeadline(Strings.s("v_nospeech_head", hi), size = 24, align = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                EqBody(Strings.s("v_nospeech_body", hi), size = 14)
                Spacer(Modifier.height(16.dp))
                EqCoralPillButton(
                    text = Strings.s("v_try_again", hi),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        return
    }

    EqHeadline(Strings.s("v_results_head1", hi), size = 30)
    EqHeadline(Strings.s("v_results_head2", hi), size = 30)
    Spacer(Modifier.height(14.dp))

    // Presence score hero
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(EqRound))
            .background(EqPillDark)
            .padding(22.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CapsLabelLightVoice(Strings.s("v_presence_caps", hi))
            Spacer(Modifier.height(8.dp))
            Text(
                "${result.presenceScore}",
                fontFamily = EqSerif,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 64.sp
            )
            Text(Strings.s("v_per_100", hi), fontSize = 14.sp, color = EqMuted)
            Spacer(Modifier.height(8.dp))
            // NOTE: presenceLine comes from VoiceAnalyzer (util) and stays English.
            Text(
                result.presenceLine,
                fontSize = 14.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
    Spacer(Modifier.height(12.dp))

    // NOTE: metric notes (pitchNote/paceNote/steadinessNote/tip) come from
    // VoiceAnalyzer (util) and stay English.
    EqPastelTile(
        title = Strings.s("v_metric_pitch", hi),
        subtitle = result.pitchNote,
        chip = Strings.fmt("v_hz", hi, "n" to "${result.medianPitchHz.toInt()}"),
        tileColor = EqLavender,
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )
    EqPastelTile(
        title = Strings.s("v_metric_pace", hi),
        subtitle = result.paceNote + " " + Strings.fmt(
            "v_pace_suffix", hi, "p" to "${(result.pauseRatio * 100).toInt()}"
        ),
        chip = Strings.fmt(
            "v_pace_value", hi,
            "n" to "%.1f".format(result.segmentsPerSec)
        ),
        tileColor = EqSage,
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )
    EqPastelTile(
        title = Strings.s("v_metric_steady", hi),
        subtitle = result.steadinessNote,
        chip = Strings.fmt("v_hz_pm", hi, "n" to "${result.pitchStdDevHz.toInt()}"),
        tileColor = EqPeach,
        onClick = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(EqRoundSm))
            .background(EqPeach)
            .padding(18.dp)
    ) {
        Column {
            EqSectionLabel(Strings.s("v_fix_caps", hi))
            Spacer(Modifier.height(6.dp))
            Text(result.tip, fontSize = 14.sp, color = EqInk, lineHeight = 20.sp)
        }
    }
    Spacer(Modifier.height(12.dp))
    VoicePrivacyRow()
    Spacer(Modifier.height(16.dp))

    EqCoralPillButton(
        text = Strings.s("v_drills_btn", hi),
        onClick = onDrills,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(10.dp))
    EqPillButton(
        text = Strings.s("v_record_again", hi),
        onClick = onRetry,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun CapsLabelLightVoice(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color.White.copy(alpha = 0.65f),
        letterSpacing = 2.sp,
        textAlign = TextAlign.Center
    )
}

// ---------- Daily drills (streak-tracked) ----------

@Composable
private fun VoiceDrills(
    streakDays: Int,
    doneToday: Boolean,
    onDrillDone: () -> Unit
) {
    val hi = LanguageStore.isHinglish
    EqHeadline(Strings.s("v_drills_head1", hi), size = 30)
    EqHeadline(Strings.s("v_drills_head2", hi), size = 30)
    Spacer(Modifier.height(10.dp))
    EqBody(
        Strings.s(if (doneToday) "v_drills_done_today" else "v_drills_not_done", hi),
        size = 14
    )
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Timer, contentDescription = null, tint = MogCoral, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            if (streakDays > 0) Strings.fmt("v_drill_streak", hi, "n" to "$streakDays")
            else Strings.s("v_no_streak", hi),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = EqInk
        )
    }
    Spacer(Modifier.height(14.dp))

    BreathingDrillCard(onDrillDone = onDrillDone)
    HummingDrillCard(onDrillDone = onDrillDone)
    PacingDrillCard(onDrillDone = onDrillDone)
}

@Composable
private fun DrillShell(
    title: String,
    desc: String,
    body: @Composable () -> Unit
) {
    EqGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Column {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = EqInk)
            EqBody(desc, size = 13)
            Spacer(Modifier.height(12.dp))
            body()
        }
    }
}

/** 4-7-8 breathing: inhale 4s, hold 7s, exhale 8s — 3 rounds. */
@Composable
private fun BreathingDrillCard(onDrillDone: () -> Unit) {
    val hi = LanguageStore.isHinglish
    var step by remember { mutableStateOf("") } // "", INHALE, HOLD, EXHALE, DONE
    var secs by remember { mutableIntStateOf(0) }
    var round by remember { mutableIntStateOf(1) }

    LaunchedEffect(step) {
        if (step == "INHALE" || step == "HOLD" || step == "EXHALE") {
            while (secs > 0) {
                delay(1000L)
                secs--
            }
            when (step) {
                "INHALE" -> { step = "HOLD"; secs = 7 }
                "HOLD" -> { step = "EXHALE"; secs = 8 }
                "EXHALE" -> {
                    if (round < 3) {
                        round++
                        step = "INHALE"
                        secs = 4
                    } else {
                        step = "DONE"
                        onDrillDone()
                    }
                }
            }
        }
    }

    DrillShell(
        title = Strings.s("v_breath_title", hi),
        desc = Strings.s("v_breath_desc", hi)
    ) {
        when (step) {
            "" -> VoiceDrillStartButton(Strings.s("v_breath_start", hi)) { step = "INHALE"; secs = 4; round = 1 }
            "DONE" -> VoiceDrillDoneRow()
            else -> {
                val label = when (step) {
                    "INHALE" -> Strings.s("v_inhale", hi)
                    "HOLD" -> Strings.s("v_hold", hi)
                    else -> Strings.s("v_exhale", hi)
                }
                val total = when (step) {
                    "INHALE" -> 4
                    "HOLD" -> 7
                    else -> 8
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        Strings.fmt("v_round", hi, "r" to "$round"),
                        fontSize = 12.sp,
                        color = EqMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EqInk, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$secs",
                        fontFamily = EqSerif,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = MogCoral
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { 1f - secs.toFloat() / total.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MogCoral,
                        trackColor = EqGreigeDeep
                    )
                }
            }
        }
    }
}

/** 45-second humming warm-up. */
@Composable
private fun HummingDrillCard(onDrillDone: () -> Unit) {
    val hi = LanguageStore.isHinglish
    var secsLeft by remember { mutableIntStateOf(45) }
    var running by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running && secsLeft > 0) {
            delay(1000L)
            secsLeft--
        }
        if (running && secsLeft == 0) {
            running = false
            if (!done) {
                done = true
                onDrillDone()
            }
        }
    }

    DrillShell(
        title = Strings.s("v_hum_title", hi),
        desc = Strings.s("v_hum_desc", hi)
    ) {
        when {
            done -> VoiceDrillDoneRow()
            running -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = MogCoral, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            Strings.s("v_hum_text", hi),
                            fontFamily = EqSerif,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = EqInk
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        Strings.fmt("v_secs_left", hi, "s" to "$secsLeft"),
                        fontSize = 15.sp,
                        color = EqMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { 1f - secsLeft.toFloat() / 45f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MogCoral,
                        trackColor = EqGreigeDeep
                    )
                    Spacer(Modifier.height(10.dp))
                    EqPillButton(
                        text = Strings.s("v_pause", hi),
                        onClick = { running = false }
                    )
                }
            }
            else -> VoiceDrillStartButton(
                if (secsLeft < 45) Strings.s("v_hum_resume", hi) else Strings.s("v_hum_start", hi)
            ) { running = true }
        }
    }
}

/** Read the line to a slow 50 BPM pulse — 16 beats. */
@Composable
private fun PacingDrillCard(onDrillDone: () -> Unit) {
    val hi = LanguageStore.isHinglish
    var beat by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    val dotSize by animateDpAsState(
        targetValue = if (running && beat % 2 == 1) 64.dp else 36.dp,
        animationSpec = tween(450),
        label = "pulse"
    )

    LaunchedEffect(running) {
        if (running) {
            repeat(16) { i ->
                beat = i + 1
                delay(1200L) // 50 BPM
            }
            running = false
            beat = 0
            if (!done) {
                done = true
                onDrillDone()
            }
        }
    }

    DrillShell(
        title = Strings.s("v_pace_title", hi),
        desc = Strings.s("v_pace_desc", hi)
    ) {
        Text(
            VOICE_LINE_EN,
            fontFamily = EqSerif,
            fontSize = 16.sp,
            color = EqInk,
            lineHeight = 24.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(EqGreigeDeep)
                .padding(14.dp)
        )
        Spacer(Modifier.height(12.dp))
        when {
            done -> VoiceDrillDoneRow()
            running -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .size(dotSize)
                            .clip(CircleShape)
                            .background(MogCoral),
                        contentAlignment = Alignment.Center
                    ) {}
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (beat > 0) Strings.fmt("v_beat", hi, "b" to "$beat")
                        else Strings.s("v_get_ready", hi),
                        fontSize = 14.sp,
                        color = EqMuted,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    EqPillButton(
                        text = Strings.s("v_stop", hi),
                        onClick = { running = false; beat = 0 }
                    )
                }
            }
            else -> VoiceDrillStartButton(Strings.s("v_pace_start", hi)) { beat = 0; running = true }
        }
    }
}

@Composable
private fun VoiceDrillStartButton(label: String, onClick: () -> Unit) {
    EqDarkPillButton(
        text = label,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun VoiceDrillDoneRow() {
    val hi = LanguageStore.isHinglish
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(EqSage)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            Strings.s("v_drill_done", hi),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF2E7D32)
        )
    }
}
