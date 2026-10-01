package com.kurupdevs.moggr.ui

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kurupdevs.moggr.analysis.AnalysisUiState
import com.kurupdevs.moggr.analysis.FaceAnalyzer
import com.kurupdevs.moggr.analysis.FeatureScore
import com.kurupdevs.moggr.analysis.PillarScore
import com.kurupdevs.moggr.analysis.PslReport
import com.kurupdevs.moggr.util.ProfileStore
import com.kurupdevs.moggr.util.ScanHistoryStore
// v2.6-science begin
import com.kurupdevs.moggr.util.TeenMode
import com.kurupdevs.moggr.util.TeenStrings
// v2.6-science end
// v2.6-hinglish begin
import com.kurupdevs.moggr.util.LanguageStore
// v2.6-hinglish end
import com.kurupdevs.moggr.util.UserProfile
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ---------- Analyzing ----------

// v2.6-landmark: staged scan — landmark dots appear in phases over the photo
// ("finding face" → "mapping eyes" → "mapping jaw" → "measuring") with a
// looping scanline sweep. The dots are a stylized loading animation; the real
// measured mesh is drawn on the result screen.
private val ANALYZE_STEPS = listOf(
    "Finding your face",
    "Mapping your eyes",
    "Mapping your jawline",
    "Measuring your ratios"
)

@Composable
fun AnalyzingScreen(photo: Bitmap? = null) {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        repeat(ANALYZE_STEPS.size - 1) {
            delay(1100)
            step++
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoggrBg)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            Strings.s("cam_analyzing", LanguageStore.isHinglish),
            fontFamily = MogSerif,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PslText
        )
        Spacer(Modifier.height(6.dp))
        Text(
            ANALYZE_STEPS[step],
            fontSize = 14.sp,
            color = PslBlue,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(24.dp))

        if (photo != null) {
            ScanPhaseCard(photo = photo, step = step)
        } else {
            // PSL radar ring (fallback when the photo isn't available)
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(190.dp)) {
                CircularProgressIndicator(
                    progress = { (step + 1) / ANALYZE_STEPS.size.toFloat() },
                    modifier = Modifier.size(190.dp),
                    color = PslBlue,
                    trackColor = Color(0xFFEDE7DB),
                    strokeWidth = 8.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PSL", fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = PslText)
                    Text(
                        "${step + 1}/4",
                        fontSize = 14.sp,
                        color = PslBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = PslCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ANALYZE_STEPS.forEachIndexed { i, label ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when {
                            i < step -> Text("✓ ", color = PslBlue, fontWeight = FontWeight.Bold)
                            i == step -> CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PslBlue,
                                strokeWidth = 2.5.dp
                            )
                            else -> Text("○ ", color = Color(0xFFD8D0C2))
                        }
                        Spacer(Modifier.padding(4.dp))
                        Text(
                            label,
                            color = if (i <= step) PslText else PslGrey,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * v2.6-landmark: photo card for the analyzing screen. A scanline sweeps the
 * photo on a loop while landmark dots fade in phase by phase: face oval,
 * then eyes, then jawline, then the measuring guides.
 */
@Composable
private fun ScanPhaseCard(photo: Bitmap, step: Int) {
    val coral = Color(0xFFE07856)
    val bw = photo.width.toFloat().coerceAtLeast(1f)
    val bh = photo.height.toFloat().coerceAtLeast(1f)

    val scanTransition = rememberInfiniteTransition(label = "scanline")
    val scanY by scanTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanY"
    )
    val reveals = (0..3).map { i ->
        animateFloatAsState(
            targetValue = if (step >= i) 1f else 0f,
            animationSpec = tween(700),
            label = "phase$i"
        ).value
    }

    // Stylized dot layout in normalized photo space (loading animation only).
    val ovalDots = remember {
        List(28) { i ->
            val a = i / 28f * 2f * PI.toFloat()
            Offset(0.5f + 0.26f * cos(a), 0.46f + 0.34f * sin(a))
        }
    }
    val eyeDots = remember {
        buildList {
            listOf(0.39f, 0.61f).forEach { cx ->
                repeat(8) { k ->
                    val a = k / 8f * 2f * PI.toFloat()
                    add(Offset(cx + 0.030f * cos(a), 0.42f + 0.022f * sin(a)))
                }
            }
        }
    }
    val jawDots = remember {
        List(14) { i ->
            val a = (25f + i * (130f / 13f)) * PI.toFloat() / 180f
            Offset(0.5f + 0.26f * cos(a), 0.46f + 0.34f * sin(a))
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(bw / bh)
            .clip(RoundedCornerShape(24.dp))
    ) {
        Image(
            bitmap = photo.asImageBitmap(),
            contentDescription = "Photo being analyzed",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // scanline sweep
            val y = scanY * h
            val bandH = 64.dp.toPx()
            val top = (y - bandH).coerceAtLeast(0f)
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        coral.copy(alpha = 0f),
                        coral.copy(alpha = 0.16f),
                        coral.copy(alpha = 0.16f),
                        coral.copy(alpha = 0f)
                    )
                ),
                topLeft = Offset(0f, top),
                size = Size(w, (y - top).coerceAtLeast(1f))
            )
            drawLine(coral, Offset(0f, y), Offset(w, y), 3.dp.toPx())

            fun drawDots(dots: List<Offset>, reveal: Float, radius: Float) {
                val n = dots.size
                dots.forEachIndexed { i, o ->
                    val a = ((reveal * (n + 6) - i) / 6f).coerceIn(0f, 1f)
                    if (a > 0f) {
                        drawCircle(coral.copy(alpha = 0.9f * a), radius, Offset(o.x * w, o.y * h))
                    }
                }
            }
            val r = 3.dp.toPx()
            drawDots(ovalDots, reveals[0], r)
            drawDots(eyeDots, reveals[1], r * 1.15f)
            drawDots(jawDots, reveals[2], r * 1.15f)
            // measuring phase: thirds guides fade in
            if (reveals[3] > 0f) {
                val ga = 0.45f * reveals[3]
                listOf(0.36f, 0.58f).forEach { ny ->
                    drawLine(
                        coral.copy(alpha = ga),
                        Offset(w * 0.24f, h * ny),
                        Offset(w * 0.76f, h * ny),
                        1.5.dp.toPx()
                    )
                }
            }
        }
    }
}

// ---------- Results (home style: photo + Overall + stat cards) ----------

@Composable
fun ResultScreen(
    report: PslReport,
    profile: UserProfile?,
    photo: Bitmap?,
    onRescan: () -> Unit,
    onNext: () -> Unit,
    // v2.6-landmark begin
    verifySnapshot: FaceAnalyzer.FaceGeometrySnapshot? = null,
    recomputing: Boolean = false,
    onRecalculate: (FaceAnalyzer.FaceGeometrySnapshot) -> Unit = {}
    // v2.6-landmark end
) {
    val context = LocalContext.current
    // Per-report keys: the guess + reveal run once for each distinct scan.
    // v2.6-landmark: keyed on the photo (stable across point-verify recomputes,
    // which keep the same bitmap) so a recalculation skips straight to the body.
    val scanKeys: Array<Any> = arrayOf(
        report.timestamp,
        photo?.hashCode() ?: 0
    )
    var guessDone by rememberSaveable(*scanKeys) { mutableStateOf(false) }
    var guess by rememberSaveable(*scanKeys) { mutableStateOf(5f) }
    var revealDone by rememberSaveable(*scanKeys) { mutableStateOf(false) }

    // Append to the on-device scan history once per report (deduped by timestamp).
    // v2.6-landmark: keyed on (timestamp, photo) so a point-verify recompute of
    // the same scan doesn't append a duplicate history entry.
    val entryTs = remember(report.timestamp, photo) {
        if (report.timestamp != 0L) report.timestamp else System.currentTimeMillis()
    }
    LaunchedEffect(entryTs) {
        val photoFile = File(context.filesDir, "last_face.jpg")
        ScanHistoryStore.append(
            context,
            report.copy(timestamp = entryTs),
            if (photoFile.exists()) photoFile.absolutePath else null
        )
    }

    if (!guessDone) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MoggrBg),
            contentAlignment = Alignment.Center
        ) {
            GuessDialog(
                guess = guess,
                onGuessChange = { guess = it },
                onConfirm = { guessDone = true }
            )
        }
        return
    }
    // v2.6-landmark begin: verify-points screen sits between reveal and body.
    var verifying by rememberSaveable(*scanKeys) { mutableStateOf(false) }
    if (verifying && verifySnapshot != null && photo != null) {
        VerifyPointsScreen(
            photo = photo,
            snapshot = verifySnapshot,
            busy = recomputing,
            onCancel = { verifying = false },
            onApply = { corrected ->
                onRecalculate(corrected)
                verifying = false
            }
        )
        return
    }
    // v2.6-landmark end
    if (!revealDone) {
        StagedReveal(
            report = report,
            photo = photo,
            guess = guess,
            onDone = { revealDone = true },
            // v2.6-landmark: verify is a step in the staged reveal.
            onVerify = {
                revealDone = true
                verifying = true
            }
        )
        return
    }

    var bodyVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { bodyVisible = true }
    val bodyAlpha by animateFloatAsState(
        targetValue = if (bodyVisible) 1f else 0f,
        animationSpec = tween(400),
        label = "bodyFade"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoggrBg)
            .alpha(bodyAlpha)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            ReportBody(
                report = report,
                profile = profile,
                photo = photo,
                // v2.6-landmark: "Verify points" button on the result screen.
                onVerifyPoints = if (verifySnapshot != null && photo != null) {
                    { verifying = true }
                } else null
            )
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
                    Text(Strings.s("scan_again", LanguageStore.isHinglish), color = PslBlue)
                }
                OutlinedButton(
                    onClick = { shareReport(context, profile, report) },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(Strings.s("share_btn", LanguageStore.isHinglish), color = PslBlue)
                }
            }
        }
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(Strings.s("result_next", LanguageStore.isHinglish), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.White)
        }
    }
}

// ---------- Guess-then-reveal ----------

@Composable
private fun GuessDialog(
    guess: Float,
    onGuessChange: (Float) -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PslCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CapsLabel("BEFORE WE SHOW YOU")
                Spacer(Modifier.height(8.dp))
                Text(
                    "What PSL do you think you are?",
                    fontFamily = MogSerif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PslText,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Be honest — no wrong answers. The scan decides.",
                    fontSize = 13.sp,
                    color = PslGrey,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    String.format(Locale.US, "%.1f", guess),
                    fontFamily = MogSerif,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = PslBlue
                )
                Slider(
                    value = guess,
                    onValueChange = {
                        onGuessChange(((it * 2).roundToInt() / 2f).coerceIn(1f, 8f))
                    },
                    valueRange = 1f..8f,
                    steps = 13,
                    colors = SliderDefaults.colors(
                        thumbColor = PslBlue,
                        activeTrackColor = PslBlue,
                        inactiveTrackColor = Color(0xFFEDE7DB)
                    )
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("1.0", fontSize = 12.sp, color = PslGrey)
                    Spacer(Modifier.weight(1f))
                    Text("8.0", fontSize = 12.sp, color = PslGrey)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text("LOCK IT IN", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Staged rating reveal, <4s total, tap anywhere to skip:
 * scan sweep → landmark dots → PSL count-up → tier slam → guess/gap line.
 */
@Composable
private fun StagedReveal(
    report: PslReport,
    photo: Bitmap?,
    guess: Float,
    onDone: () -> Unit,
    // v2.6-landmark: verify offered as a step of the staged reveal.
    onVerify: () -> Unit = {}
) {
    var stage by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(800); stage = 1   // sweep done
        delay(600); stage = 2   // dots in
        delay(1100); stage = 3  // count-up done
        delay(650); stage = 4   // tier slammed
        delay(2200); onDone()   // gap line + verify step shown → body
    }

    val sweep by animateFloatAsState(1f, tween(800), label = "sweep")
    val dotsIn by animateFloatAsState(
        targetValue = if (stage >= 1) 1f else 0f,
        animationSpec = tween(500),
        label = "dots"
    )
    val count by animateFloatAsState(
        targetValue = if (stage >= 2) report.overallPsl.toFloat() else 0f,
        animationSpec = tween(1000),
        label = "count"
    )
    val tierScale by animateFloatAsState(
        targetValue = if (stage >= 3) 1f else 1.3f,
        animationSpec = spring(dampingRatio = 0.45f),
        label = "tierScale"
    )
    val tierAlpha by animateFloatAsState(
        targetValue = if (stage >= 3) 1f else 0f,
        animationSpec = tween(250),
        label = "tierAlpha"
    )
    val gapAlpha by animateFloatAsState(
        targetValue = if (stage >= 4) 1f else 0f,
        animationSpec = tween(400),
        label = "gapAlpha"
    )

    val dots = remember(report) { revealDots(report) }
    val potential = (report.overallPsl + 1.3).coerceAtMost(8.0)
    val coral = Color(0xFFE07856)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MoggrBg)
            .clickable { onDone() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(28.dp))
            ) {
                if (photo != null) {
                    Image(
                        bitmap = photo.asImageBitmap(),
                        contentDescription = "Your scan photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // ContentScale.Crop math so dots land on the face.
                    val bw = photo.width.toFloat()
                    val bh = photo.height.toFloat()
                    val cw = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                    val ch = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                    val scale = maxOf(cw / bw, ch / bh)
                    val dx = (cw - bw * scale) / 2f
                    val dy = (ch - bh * scale) / 2f
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (stage == 0) {
                            val y = sweep * size.height
                            val top = (y - 44.dp.toPx()).coerceAtLeast(0f)
                            drawRect(
                                coral.copy(alpha = 0.16f),
                                topLeft = Offset(0f, top),
                                size = Size(size.width, (y - top).coerceAtLeast(0f))
                            )
                            drawLine(coral, Offset(0f, y), Offset(size.width, y), 3.dp.toPx())
                        } else {
                            val n = (dotsIn * dots.size).toInt()
                            dots.take(n).forEach { (fx, fy) ->
                                drawCircle(
                                    coral,
                                    3.dp.toPx(),
                                    Offset(fx * bw * scale + dx, fy * bh * scale + dy)
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PslCard)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            CapsLabel("MEASURED PSL")
            Spacer(Modifier.height(4.dp))
            Text(
                String.format(Locale.US, "%.1f", count),
                fontFamily = MogSerif,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = PslText
            )
            Text(
                pslLabel(report.overallPsl),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = PslBlue,
                modifier = Modifier
                    .alpha(tierAlpha)
                    .graphicsLayer {
                        scaleX = tierScale
                        scaleY = tierScale
                    }
            )
            Spacer(Modifier.height(12.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.alpha(gapAlpha)
            ) {
                Text(
                    "You guessed ${String.format(Locale.US, "%.1f", guess)} · measured ${String.format(Locale.US, "%.1f", report.overallPsl)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PslText,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${String.format(Locale.US, "%.1f", report.overallPsl)} → ${String.format(Locale.US, "%.1f", potential)} potential · " +
                        // v2.6-science begin: softer framing for teen mode
                        TeenStrings.movesLine(TeenMode.isTeen(LocalContext.current)),
                        // v2.6-science end
                    fontSize = 14.sp,
                    color = PslGrey,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(28.dp))
            // v2.6-landmark: verify step — offered once the score is revealed.
            if (stage >= 4) {
                OutlinedButton(
                    onClick = onVerify,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.alpha(gapAlpha)
                ) {
                    Text(
                        "Verify the AI's points",
                        color = PslBlue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            Text("tap to skip", fontSize = 12.sp, color = PslGrey)
        }
    }
}

/** Dots for the reveal: real mesh when available, decorative grid otherwise. */
private fun revealDots(report: PslReport): List<Pair<Float, Float>> {
    val mesh = report.landmarkMesh.filter { it.kind != 0 }
    if (mesh.isNotEmpty()) {
        return mesh.filterIndexed { i, _ -> i % 2 == 0 }.take(30).map { it.x to it.y }
    }
    val pts = mutableListOf<Pair<Float, Float>>()
    for (i in 0..6) for (j in 0..8) {
        pts.add((0.2f + 0.6f * i / 6) to (0.12f + 0.76f * j / 8))
    }
    return pts.take(30)
}

/** Face-mapping scan overlay drawn from the REAL measured ML Kit mesh.
 *  Falls back to the decorative grid for pre-1.7 saved reports. */
@Composable
private fun ScanOverlay(photo: Bitmap?, report: PslReport) {
    val mesh = report.landmarkMesh
    if (mesh.isEmpty() || photo == null || photo.width == 0 || photo.height == 0) {
        DecorativeScanOverlay()
        return
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val bw = photo.width.toFloat()
        val bh = photo.height.toFloat()
        val cw = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val ch = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        // ContentScale.Crop math: bitmap -> box mapping
        val scale = maxOf(cw / bw, ch / bh)
        val dx = (cw - bw * scale) / 2f
        val dy = (ch - bh * scale) / 2f
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Color(0xFFE07856) // coral accent
            val sw = 3.dp.toPx()
            val l = 26.dp.toPx()
            // corner brackets
            drawLine(c, Offset(0f, l), Offset(0f, 0f), sw)
            drawLine(c, Offset(0f, 0f), Offset(l, 0f), sw)
            drawLine(c, Offset(cw - l, 0f), Offset(cw, 0f), sw)
            drawLine(c, Offset(cw, 0f), Offset(cw, l), sw)
            drawLine(c, Offset(0f, ch - l), Offset(0f, ch), sw)
            drawLine(c, Offset(0f, ch), Offset(l, ch), sw)
            drawLine(c, Offset(cw - l, ch), Offset(cw, ch), sw)
            drawLine(c, Offset(cw, ch), Offset(cw, ch - l), sw)
            fun mapX(nx: Float) = nx * bw * scale + dx
            fun mapY(ny: Float) = ny * bh * scale + dy
            // measured face oval
            val ovalPts = mesh.filter { it.kind == 0 }
                .map { Offset(mapX(it.x), mapY(it.y)) }
            if (ovalPts.size >= 2) {
                for (i in ovalPts.indices) {
                    drawLine(
                        c.copy(alpha = 0.8f),
                        ovalPts[i], ovalPts[(i + 1) % ovalPts.size],
                        2.dp.toPx()
                    )
                }
            }
            // measured landmark dots (eyes bigger)
            mesh.filter { it.kind != 0 }.forEach { p ->
                val r = if (p.kind == 1) 3.2.dp.toPx() else 2.2.dp.toPx()
                drawCircle(c.copy(alpha = 0.9f), r, Offset(mapX(p.x), mapY(p.y)))
            }
            // measured thirds guides across the face box
            if (report.thirdsY.size == 4 && report.faceBox.size == 4) {
                val left = mapX(report.faceBox[0])
                val right = mapX(report.faceBox[2])
                listOf(report.thirdsY[1], report.thirdsY[2]).forEach { ny ->
                    drawLine(
                        c.copy(alpha = 0.45f),
                        Offset(left, mapY(ny)), Offset(right, mapY(ny)),
                        1.5.dp.toPx()
                    )
                }
            }
        }
    }
}

/** Old decorative grid, kept for pre-1.7 saved reports that have no mesh. */
@Composable
private fun DecorativeScanOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val c = Color(0xFF38BDF8)
        val sw = 3.dp.toPx()
        val l = 26.dp.toPx()
        // corner brackets
        drawLine(c, Offset(0f, l), Offset(0f, 0f), sw)
        drawLine(c, Offset(0f, 0f), Offset(l, 0f), sw)
        drawLine(c, Offset(w - l, 0f), Offset(w, 0f), sw)
        drawLine(c, Offset(w, 0f), Offset(w, l), sw)
        drawLine(c, Offset(0f, h - l), Offset(0f, h), sw)
        drawLine(c, Offset(0f, h), Offset(l, h), sw)
        drawLine(c, Offset(w - l, h), Offset(w, h), sw)
        drawLine(c, Offset(w, h), Offset(w, h - l), sw)
        // landmark-style dot grid
        val cols = 7
        val rows = 9
        for (i in 0..cols) {
            for (j in 0..rows) {
                val x = w * 0.14f + (w * 0.72f) * i / cols
                val y = h * 0.10f + (h * 0.80f) * j / rows
                drawCircle(c.copy(alpha = 0.30f), radius = 2.dp.toPx(), center = Offset(x, y))
            }
        }
        // horizontal scan line
        drawLine(
            c.copy(alpha = 0.55f),
            Offset(0f, h * 0.52f), Offset(w, h * 0.52f),
            2.dp.toPx()
        )
        // thirds guides
        drawLine(c.copy(alpha = 0.25f), Offset(0f, h / 3f), Offset(w, h / 3f), 1.dp.toPx())
        drawLine(c.copy(alpha = 0.25f), Offset(0f, 2f * h / 3f), Offset(w, 2f * h / 3f), 1.dp.toPx())
    }
}

@Composable
fun ReportBody(
    report: PslReport,
    profile: UserProfile?,
    photo: Bitmap?,
    // v2.6-landmark: null hides the button (e.g. pre-v2.6 saved reports).
    onVerifyPoints: (() -> Unit)? = null
) {
    // v2.6-hinglish: report headers follow the app language.
    val hi = LanguageStore.isHinglish
    Column(modifier = Modifier.fillMaxWidth()) {
        var faceMapOn by remember { mutableStateOf(false) }
        var selectedFeature by remember { mutableStateOf<FeatureScore?>(null) }
        val context = LocalContext.current
        photo?.let {
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .align(Alignment.CenterHorizontally)
            ) {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Your scan photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                ScanOverlay(it, report)
                if (faceMapOn) FaceMapGuides()
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { faceMapOn = !faceMapOn }
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    Strings.s("face_map", hi),
                    color = PslText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = faceMapOn,
                    onCheckedChange = { faceMapOn = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PslBlue,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFE2DCD2)
                    )
                )
            }
            if (faceMapOn) {
                Spacer(Modifier.height(2.dp))
                Text(
                    Strings.s("sub_face_map", hi),
                    fontSize = 12.sp,
                    color = PslGrey,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(14.dp))
        }
        // v2.6-landmark begin
        if (onVerifyPoints != null) {
            TextButton(
                onClick = onVerifyPoints,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    "Verify the AI's points",
                    color = PslBlue,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
        // v2.6-landmark end
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CapsLabel(text = Strings.s("caps_face_report", hi))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (profile != null) Strings.fmt("score_of", hi, "n" to profile.name) else Strings.s("your_score", hi),
            fontFamily = MogSerif,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = PslText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Read from ${report.anglesRead} of 3 angles · 100% on-device · free forever",
            fontSize = 12.sp,
            color = PslGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        if (report.confidence > 0) {
            Spacer(Modifier.height(4.dp))
            val confLabel = when {
                report.confidence >= 0.85 -> "high confidence"
                report.confidence >= 0.65 -> "medium confidence"
                else -> "low confidence"
            }
            Text(
                text = "±${"%.1f".format(Locale.US, report.uncertainty)} · $confLabel",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = PslText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(14.dp))

        val potential = if (report.potentialPsl > 0) report.potentialPsl
        else (report.overallPsl + 1.5).coerceAtMost(8.0)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeroCard(
                title = "PSL",
                score = report.overallPsl,
                sub = pslTierShort(report.overallPsl),
                barColor = scoreColor(report.overallPsl),
                modifier = Modifier.weight(1f),
                featured = true
            )
            HeroCard(
                title = "POTENTIAL",
                score = potential,
                sub = pslTierShort(potential) + " est.",
                barColor = Color(0xFF12B76A),
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Top ${100 - report.percentile}% of faces (est.) · ${report.failoCount} negative points · ${report.haloCount} halos",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = PslText,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "potential assumes weak areas close ~a third of the gap — est.",
            fontSize = 11.sp,
            color = PslGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { shareFaceCard(context, report) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text("Share face card", color = PslBlue, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(18.dp))
        TraitMeters(report)

        if (report.photoNotes.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF6E7)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Photo check", fontWeight = FontWeight.SemiBold, color = Color(0xFFB54708), fontSize = 15.sp)
                    Spacer(Modifier.height(6.dp))
                    report.photoNotes.forEach { n ->
                        Text("• $n", fontSize = 13.sp, color = PslText)
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        SectionTitle(Strings.s("sec_feature_scores", hi))
        Text(
            Strings.s("sub_feature_scores", hi),
            fontSize = 13.sp,
            color = PslGrey
        )
        Spacer(Modifier.height(10.dp))
        report.features.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { f -> FeatureCard(f, Modifier.weight(1f), onClick = { selectedFeature = f }) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        if (report.pillars.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            SectionTitle("The 4 pillars")
            Text(
                "How experienced raters actually weigh a face — harmony first",
                fontSize = 13.sp,
                color = PslGrey
            )
            Spacer(Modifier.height(10.dp))
            report.pillars.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { p -> PillarCard(p, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        if (report.strengths.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            SectionTitle(Strings.s("sec_positives", hi))
            Text(Strings.s("sub_positives", hi), fontSize = 13.sp, color = PslGrey)
            Spacer(Modifier.height(10.dp))
            report.strengths.forEach { s ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF3)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(Modifier.padding(12.dp)) {
                        Text("✓ ", color = Color(0xFF12B76A), fontWeight = FontWeight.Bold)
                        Text(s, color = PslText, fontSize = 14.sp)
                    }
                }
            }
        }

        run {
            // Only meaningful failos (< 5.0) — never pad the list with decent scores.
            val negatives = report.features.sortedBy { it.score }.filter { it.score < 5.0 }.take(3)
            Spacer(Modifier.height(10.dp))
            SectionTitle(Strings.s("sec_negatives", hi))
            if (negatives.isNotEmpty()) {
                Text(Strings.s("sub_negatives", hi), fontSize = 13.sp, color = PslGrey)
                Spacer(Modifier.height(10.dp))
                negatives.forEach { f ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3F2)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(Modifier.padding(12.dp)) {
                            Text("✕ ", color = Color(0xFFF04438), fontWeight = FontWeight.Bold)
                            Text(
                                "${f.name} (${"%.1f".format(Locale.US, f.score)}) — ${f.note}",
                                color = PslText,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF3)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp)) {
                        Text("✓ ", color = Color(0xFF12B76A), fontWeight = FontWeight.Bold)
                        Text(
                            "No major failos — nothing is dragging your score down right now.",
                            color = PslText,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        if (report.improvements.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            SectionTitle(Strings.s("sec_ascension", hi))
            Text(
                Strings.s("sub_ascension", hi),
                fontSize = 13.sp,
                color = PslGrey
            )
            Spacer(Modifier.height(10.dp))
            report.improvements.forEachIndexed { i, im ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PslCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${i + 1}. ${im.area}",
                                fontWeight = FontWeight.SemiBold,
                                color = PslText,
                                fontSize = 15.sp
                            )
                            EffortChip(im.effort)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(im.method, fontSize = 14.sp, color = PslGrey)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF3)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Projected: ${pslTierShort(potential)} — if every step sticks",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF067647),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                )
            }
        }

        if (report.summary.isNotBlank()) {
            Spacer(Modifier.height(18.dp))
            SectionTitle(Strings.s("sec_summary", hi))
            Spacer(Modifier.height(8.dp))
            Text(report.summary, color = PslText, fontSize = 15.sp)
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Community-benchmark estimate, not a medical measurement — PSL ratios are " +
                "looksmaxxing-community conventions, not validated science. Scores come from " +
                "facial geometry measured on your phone — no photo ever leaves your device. " +
                "Same lighting, same angle = comparable results. " +
                "Free forever: no paywall, no unlock fees.",
            fontSize = 12.sp,
            color = PslGrey,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        selectedFeature?.let { f ->
            WhyThisScoreDialog(
                feature = f,
                report = report,
                onDismiss = { selectedFeature = null }
            )
        }
    }
}

/** Proportional face-map guides: thirds, fifths and the center symmetry axis. */
@Composable
private fun FaceMapGuides() {
    val density = LocalDensity.current
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val guide = Color(0xFFE07856).copy(alpha = 0.65f)
        // horizontal thirds
        listOf(1f / 3f, 2f / 3f).forEach { fy ->
            drawLine(guide, Offset(0f, h * fy), Offset(w, h * fy), 1.5.dp.toPx())
        }
        // vertical fifths
        listOf(0.2f, 0.4f, 0.6f, 0.8f).forEach { fx ->
            drawLine(
                guide.copy(alpha = 0.45f),
                Offset(w * fx, 0f), Offset(w * fx, h),
                1.dp.toPx()
            )
        }
        // center symmetry axis, dashed
        drawLine(
            Color(0xFFE07856),
            Offset(w / 2f, 0f), Offset(w / 2f, h),
            2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        )
        // small labels
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#1C1917")
            alpha = 160
            textSize = with(density) { 11.sp.toPx() }
            isAntiAlias = true
        }
        val native = drawContext.canvas.nativeCanvas
        native.drawText("⅓", 8.dp.toPx(), h / 3f - 6.dp.toPx(), paint)
        native.drawText("⅔", 8.dp.toPx(), 2f * h / 3f - 6.dp.toPx(), paint)
        native.drawText("axis", w / 2f + 8.dp.toPx(), 22.dp.toPx(), paint)
    }
}

@Composable
private fun HeroCard(
    title: String,
    score: Double,
    sub: String,
    barColor: Color,
    modifier: Modifier = Modifier,
    featured: Boolean = false
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (featured) Color.Transparent else PslCard
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.background(
                brush = if (featured) {
                    Brush.horizontalGradient(listOf(Color(0xFF2A2119), PslDeep))
                } else {
                    Brush.verticalGradient(listOf(PslCard, PslCard))
                },
                shape = RoundedCornerShape(18.dp)
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                val titleColor = if (featured) Color.White.copy(alpha = 0.85f) else PslBlue
                val scoreColorTxt = if (featured) Color.White else PslText
                val subColor = if (featured) Color.White.copy(alpha = 0.7f) else PslGrey
                Text(
                    title,
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        String.format(Locale.US, "%.1f", score),
                        fontFamily = MogSerif,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColorTxt
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("• $sub", fontSize = 13.sp, color = subColor)
                }
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { (score / 8.0).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (featured) PslBlue else barColor,
                    trackColor = if (featured) Color.White.copy(alpha = 0.25f) else Color(0xFFEDE7DB)
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(f: FeatureScore, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                f.name.uppercase(),
                color = PslBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    String.format(Locale.US, "%.1f", f.score),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = PslText
                )
                Spacer(Modifier.width(6.dp))
                Text("• ${featLabel(f.score)}", fontSize = 12.sp, color = PslGrey)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (f.score / 8.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = scoreColor(f.score),
                trackColor = Color(0xFFEDE7DB)
            )
        }
    }
}

@Composable
private fun PillarCard(p: PillarScore, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                p.name.uppercase(),
                color = PslBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                String.format(Locale.US, "%.1f", p.score),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PslText
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (p.score / 8.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = scoreColor(p.score),
                trackColor = Color(0xFFEDE7DB)
            )
            if (p.note.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(p.note, fontSize = 12.sp, color = PslGrey)
            }
        }
    }
}

private fun featLabel(score: Double): String = when {
    score >= 7.0 -> "Excellent"
    score >= 6.0 -> "Good"
    score >= 4.5 -> "Average"
    score >= 3.0 -> "Below avg"
    else -> "Weak"
}

/** Short tier name, shared with the history store and timeline. */
fun pslTierShort(psl: Double): String = when {
    psl >= 7.75 -> "Gigachad"
    psl >= 7.0 -> "Chad"
    psl >= 6.0 -> "Chadlite"
    psl >= 5.0 -> "HTN"
    psl >= 3.0 -> "MTN"
    psl >= 1.4 -> "LTN"
    else -> "Sub-5"
}

fun shareReport(context: android.content.Context, profile: UserProfile?, report: PslReport) {
    val top = report.features.sortedByDescending { it.score }.take(3)
        .joinToString(", ") { "${it.name} ${String.format(Locale.US, "%.1f", it.score)}" }
    val text = buildString {
        append("My Moggr PSL report: ")
        append(String.format(Locale.US, "%.1f", report.overallPsl))
        append(" PSL (≈")
        append(String.format(Locale.US, "%.1f", report.decile))
        append("/10, ")
        append(pslLabel(report.overallPsl))
        append("). Top features: ")
        append(top)
        append(". Full breakdown free on Moggr — no paywall.")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share your PSL report"))
}

@Composable
private fun SectionTitle(text: String) {
    CapsLabel(text)
}

@Composable
private fun EffortChip(effort: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PslBlue.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            effort.replaceFirstChar { it.uppercase() },
            fontSize = 11.sp,
            color = PslBlue,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

private fun scoreColor(score: Double): Color = when {
    score >= 7.0 -> Color(0xFF12B76A)
    score >= 5.0 -> Color(0xFFF79009)
    else -> Color(0xFFF04438)
}

fun pslLabel(psl: Double): String = when {
    psl >= 7.75 -> "Gigachad — near-mythical"
    psl >= 7.0 -> "Chad"
    psl >= 6.0 -> "Chadlite"
    psl >= 5.0 -> "HTN — High Tier Normie"
    psl >= 3.0 -> "MTN — Mid Tier Normie"
    psl >= 1.4 -> "LTN — Low Tier Normie"
    else -> "Sub-5 — maximum ascension potential"
}

private fun ordinal(n: Int): String = when {
    n % 100 in 11..13 -> "${n}th"
    n % 10 == 1 -> "${n}st"
    n % 10 == 2 -> "${n}nd"
    n % 10 == 3 -> "${n}rd"
    else -> "${n}th"
}

// ---------- Error ----------

@Composable
fun AnalysisErrorState(state: AnalysisUiState.Error, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1917).copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PslCard),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Analysis failed", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PslText)
                Spacer(Modifier.height(8.dp))
                Text(state.message, textAlign = TextAlign.Center, fontSize = 14.sp, color = PslGrey)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Back to camera") }
            }
        }
    }
}
