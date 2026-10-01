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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
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
                        micError = "Couldn't access the mic. Try again."
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
            .background(MoggrBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (phase == VoicePhase.RECORDING) stopFlag.set(true)
                    onClose()
                }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = PslText)
                }
                Spacer(Modifier.width(4.dp))
                CapsLabel("VOICE CHECK")
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
    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFBEFE3)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null, tint = PslBlue, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Hear how you\nactually sound.",
                fontFamily = MogSerif,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = PslText,
                lineHeight = 34.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Read one line out loud for 10 seconds. Moggr measures your pitch, " +
                    "pace and steadiness — the things that make a voice sound confident and clear.",
                fontSize = 14.sp,
                color = PslGrey,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(14.dp))
            VoicePrivacyRow()
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "We need the mic to analyze your voice — the recording never leaves your phone.",
                fontSize = 12.sp,
                color = PslGrey,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun VoicePrivacyRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF4EFE7))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = PslBlue, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Processed fully on this device. Nothing is uploaded, saved or shared.",
            fontSize = 13.sp,
            color = PslText,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp
        )
    }
}

// ---------- Denied state ----------

@Composable
private fun VoiceDenied(onOpenSettings: () -> Unit, onClose: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF4EFE7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null, tint = PslGrey, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "No mic, no voice check.",
                fontFamily = MogSerif,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = PslText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "That's totally fine — your call. If you change your mind, allow the microphone " +
                    "in Settings and come back.",
                fontSize = 14.sp,
                color = PslGrey,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = PslDeep),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open settings", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onClose,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text("Not now", color = PslText)
            }
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
    CapsLabel("STEP 1 OF 1 — READ ALOUD")
    Spacer(Modifier.height(8.dp))
    Text(
        "Read this line,",
        fontFamily = MogSerif,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        color = PslText,
        lineHeight = 34.sp
    )
    Text(
        "like you mean it.",
        fontFamily = MogSerif,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        color = PslText,
        lineHeight = 34.sp
    )
    Spacer(Modifier.height(14.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        VoiceLineOption(
            label = "English",
            selected = !useHinglish,
            onClick = { onToggleLine(false) },
            modifier = Modifier.weight(1f)
        )
        VoiceLineOption(
            label = "Hinglish",
            selected = useHinglish,
            onClick = { onToggleLine(true) },
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(12.dp))

    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            CapsLabel(if (useHinglish) "YOUR LINE — HINGLISH" else "YOUR LINE — ENGLISH")
            Spacer(Modifier.height(8.dp))
            Text(
                if (useHinglish) VOICE_LINE_HINGLISH else VOICE_LINE_EN,
                fontFamily = MogSerif,
                fontSize = 20.sp,
                color = PslText,
                lineHeight = 28.sp
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "Sit upright, phone at arm's length, quiet room. 10 seconds — don't rush it.",
        fontSize = 13.sp,
        color = PslGrey,
        lineHeight = 18.sp
    )
    if (micError != null) {
        Spacer(Modifier.height(8.dp))
        Text(micError, fontSize = 13.sp, color = Color(0xFFB3261E), fontWeight = FontWeight.Medium)
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = onStart,
        colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Start recording", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
    if (streakDays > 0) {
        Spacer(Modifier.height(10.dp))
        Text(
            "$streakDays-day drill streak — keep it going.",
            fontSize = 13.sp,
            color = PslGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "The recording is analyzed on your phone and deleted right after. Nothing leaves your device.",
        fontSize = 12.sp,
        color = PslGrey,
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
            .background(if (selected) PslDeep else Color.Transparent)
            .border(1.dp, if (selected) PslDeep else Color(0xFFE2DCD2), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = if (selected) Color.White else PslText
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
    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB3261E))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Recording — ${secondsLeft}s",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = PslText
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                line,
                fontFamily = MogSerif,
                fontSize = 18.sp,
                color = PslText,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            VoiceWaveform(levels = levels, modifier = Modifier.fillMaxWidth().height(96.dp))
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onStop,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text("Stop", color = PslText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun VoiceWaveform(levels: List<Float>, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF4EFE7))
    ) {
        val bars = levels.takeLast(64)
        val w = size.width
        val h = size.height
        if (bars.isEmpty()) {
            drawRect(
                color = Color(0xFFE2DCD2),
                topLeft = Offset(0f, h / 2f - 1.5f),
                size = Size(w, 3f)
            )
            return@Canvas
        }
        val bw = w / 64f
        bars.forEachIndexed { i, v ->
            val bh = (v.coerceIn(0f, 1f) * h * 0.92f).coerceAtLeast(4f)
            drawRect(
                color = Color(0xFFE07856),
                topLeft = Offset(i * bw + bw * 0.2f, (h - bh) / 2f),
                size = Size(bw * 0.6f, bh)
            )
        }
    }
}

// ---------- Analyzing ----------

@Composable
private fun VoiceAnalyzing() {
    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = PslBlue)
            Spacer(Modifier.height(18.dp))
            Text(
                "Analyzing on your phone…",
                fontFamily = MogSerif,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PslText,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Pitch, pace and steadiness — computed on-device. Nothing is uploaded.",
                fontSize = 13.sp,
                color = PslGrey,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
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
    if (result == null || !result.speechDetected) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PslCard),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Couldn't catch enough speech.",
                    fontFamily = MogSerif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PslText,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Move somewhere quieter, hold the phone at arm's length, and speak up a little.",
                    fontSize = 14.sp,
                    color = PslGrey,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Try again", fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    Text(
        "Your voice,",
        fontFamily = MogSerif,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        color = PslText,
        lineHeight = 34.sp
    )
    Text(
        "decoded.",
        fontFamily = MogSerif,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        color = PslText,
        lineHeight = 34.sp
    )
    Spacer(Modifier.height(14.dp))

    // Presence score hero
    Card(
        colors = CardDefaults.cardColors(containerColor = PslDeep),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CapsLabel("PRESENCE — CONFIDENCE & CLARITY")
            Spacer(Modifier.height(8.dp))
            Text(
                "${result.presenceScore}",
                fontFamily = MogSerif,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 64.sp
            )
            Text("/ 100", fontSize = 14.sp, color = Color(0xFFA8A29E))
            Spacer(Modifier.height(8.dp))
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

    VoiceMetricCard(
        title = "Pitch",
        value = "${result.medianPitchHz.toInt()} Hz",
        note = result.pitchNote
    )
    VoiceMetricCard(
        title = "Pace",
        value = "%.1f bursts/sec".format(result.segmentsPerSec),
        note = result.paceNote + " Pauses: ${(result.pauseRatio * 100).toInt()}% of the take."
    )
    VoiceMetricCard(
        title = "Steadiness",
        value = "±${result.pitchStdDevHz.toInt()} Hz",
        note = result.steadinessNote
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBEFE3)),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            CapsLabel("ONE THING TO FIX")
            Spacer(Modifier.height(6.dp))
            Text(result.tip, fontSize = 14.sp, color = PslText, lineHeight = 20.sp)
        }
    }
    Spacer(Modifier.height(12.dp))
    VoicePrivacyRow()
    Spacer(Modifier.height(16.dp))

    Button(
        onClick = onDrills,
        colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Text("Daily voice drills", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
    Spacer(Modifier.height(10.dp))
    OutlinedButton(
        onClick = onRetry,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Record again", color = PslText)
    }
}

@Composable
private fun VoiceMetricCard(title: String, value: String, note: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CapsLabel(title.uppercase())
                Spacer(Modifier.weight(1f))
                Text(
                    value,
                    fontFamily = MogSerif,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PslText
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(note, fontSize = 13.sp, color = PslGrey, lineHeight = 18.sp)
        }
    }
}

// ---------- Daily drills (streak-tracked) ----------

@Composable
private fun VoiceDrills(
    streakDays: Int,
    doneToday: Boolean,
    onDrillDone: () -> Unit
) {
    Text(
        "Daily voice",
        fontFamily = MogSerif,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        color = PslText,
        lineHeight = 34.sp
    )
    Text(
        "drills.",
        fontFamily = MogSerif,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        color = PslText,
        lineHeight = 34.sp
    )
    Spacer(Modifier.height(10.dp))
    Text(
        if (doneToday) "Today's drill is done — streak safe."
        else "One drill a day keeps the streak alive.",
        fontSize = 14.sp,
        color = PslGrey
    )
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Timer, contentDescription = null, tint = PslBlue, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            if (streakDays > 0) "$streakDays-day streak" else "No streak yet — start today",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = PslText
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
    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = PslText)
            Text(desc, fontSize = 13.sp, color = PslGrey, lineHeight = 18.sp)
            Spacer(Modifier.height(12.dp))
            body()
        }
    }
}

/** 4-7-8 breathing: inhale 4s, hold 7s, exhale 8s — 3 rounds. */
@Composable
private fun BreathingDrillCard(onDrillDone: () -> Unit) {
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
        title = "Breathing 4-7-8",
        desc = "Calms the throat and steadies the voice. Inhale 4s, hold 7s, exhale 8s — 3 rounds."
    ) {
        when (step) {
            "" -> VoiceDrillStartButton("Start breathing drill") { step = "INHALE"; secs = 4; round = 1 }
            "DONE" -> VoiceDrillDoneRow()
            else -> {
                val label = when (step) {
                    "INHALE" -> "Breathe in through your nose"
                    "HOLD" -> "Hold"
                    else -> "Slow exhale through your mouth"
                }
                val total = when (step) {
                    "INHALE" -> 4
                    "HOLD" -> 7
                    else -> 8
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Round $round / 3",
                        fontSize = 12.sp,
                        color = PslGrey,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PslText, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "$secs",
                        fontFamily = MogSerif,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = PslBlue
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { 1f - secs.toFloat() / total.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = PslBlue,
                        trackColor = Color(0xFFEDE6D8)
                    )
                }
            }
        }
    }
}

/** 45-second humming warm-up. */
@Composable
private fun HummingDrillCard(onDrillDone: () -> Unit) {
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
        title = "Humming warm-up",
        desc = "Hum at a comfortable pitch for 45s — feel the buzz on your lips. Smooths out pitch wobble."
    ) {
        when {
            done -> VoiceDrillDoneRow()
            running -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = PslBlue, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Hmmmmm…",
                            fontFamily = MogSerif,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = PslText
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("${secsLeft}s left", fontSize = 15.sp, color = PslGrey, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { 1f - secsLeft.toFloat() / 45f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = PslBlue,
                        trackColor = Color(0xFFEDE6D8)
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { running = false },
                        shape = RoundedCornerShape(50)
                    ) { Text("Pause", color = PslText) }
                }
            }
            else -> VoiceDrillStartButton(if (secsLeft < 45) "Resume humming" else "Start humming") { running = true }
        }
    }
}

/** Read the line to a slow 50 BPM pulse — 16 beats. */
@Composable
private fun PacingDrillCard(onDrillDone: () -> Unit) {
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
        title = "Pacing drill",
        desc = "Read the line below to the slow pulse — one phrase per beat. Don't rush the pauses."
    ) {
        Text(
            VOICE_LINE_EN,
            fontFamily = MogSerif,
            fontSize = 16.sp,
            color = PslText,
            lineHeight = 24.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF4EFE7))
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
                            .background(PslBlue),
                        contentAlignment = Alignment.Center
                    ) {}
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (beat > 0) "Beat $beat / 16" else "Get ready…",
                        fontSize = 14.sp,
                        color = PslGrey,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { running = false; beat = 0 },
                        shape = RoundedCornerShape(50)
                    ) { Text("Stop", color = PslText) }
                }
            }
            else -> VoiceDrillStartButton("Start pacing drill") { beat = 0; running = true }
        }
    }
}

@Composable
private fun VoiceDrillStartButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = PslDeep),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VoiceDrillDoneRow() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Color(0xFFE9F5E9))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Done — streak updated", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2E7D32))
    }
}
