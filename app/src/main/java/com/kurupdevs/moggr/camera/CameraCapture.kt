package com.kurupdevs.moggr.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
// v2.6-photogate begin
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
// v2.6-photogate end
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.kurupdevs.moggr.ui.BestPicPicker
// v2.7 reskin: shared equilibrium-style components + theme colors.
import com.kurupdevs.moggr.ui.EqBody
import com.kurupdevs.moggr.ui.EqCoralPillButton
import com.kurupdevs.moggr.ui.EqGlassCard
import com.kurupdevs.moggr.ui.EqHeadline
import com.kurupdevs.moggr.ui.EqSectionLabel
import com.kurupdevs.moggr.ui.AppBg
import com.kurupdevs.moggr.ui.GhostOverlay
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqLine
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.EqPillDark
import com.kurupdevs.moggr.ui.theme.MogCoral
import com.kurupdevs.moggr.ui.theme.MogCoralDark
import com.kurupdevs.moggr.ui.loadGhostBitmap
import com.kurupdevs.moggr.util.PhotoGrade
import com.kurupdevs.moggr.util.PhotoQuality
// v2.6-hinglish begin
import com.kurupdevs.moggr.util.LanguageStore
import com.kurupdevs.moggr.ui.Strings
// v2.6-hinglish end
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

/** The three required scan angles, in capture order. */
enum class CaptureAngle(val title: String, val guide: String) {
    FRONT(
        "1 of 3 — Front",
        "Face the camera straight on. Neutral expression, chin level, hair off your forehead."
    ),
    LEFT(
        "2 of 3 — Left profile",
        "Turn your head to YOUR left, so the camera sees your left cheek, jawline and nose profile."
    ),
    RIGHT(
        "3 of 3 — Right profile",
        "Turn your head to YOUR right, so the camera sees your right cheek, jawline and nose profile."
    )
}

/**
 * CameraX-based capture flow. Guides the user through the three angles,
 * shows live preview with a face frame overlay, grades each capture
 * on-device (quality gate), and returns the three bitmaps once all angles
 * are captured.
 *
 * Extras: live low-light hint from preview frames, ghost overlay of the
 * last scan for rescans, and a "Rank my pics" gallery picker.
 */
@Composable
fun CameraCapture(
    onAnalyze: (front: Bitmap, left: Bitmap, right: Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val angles = remember { CaptureAngle.entries.toList() }
    val scope = rememberCoroutineScope()
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    var step by remember { mutableIntStateOf(0) }
    val shots = remember { mutableStateMapOf<CaptureAngle, Bitmap>() }
    var capturing by remember { mutableStateOf(false) }
    var grading by remember { mutableStateOf(false) }
    var captureError by remember { mutableStateOf<String?>(null) }
    var lowLight by remember { mutableStateOf(false) }

    // --- quality gate state ---
    var pendingAngle by remember { mutableStateOf<CaptureAngle?>(null) }
    var pendingShot by remember { mutableStateOf<Bitmap?>(null) }
    var pendingGrade by remember { mutableStateOf<PhotoGrade?>(null) }
    // v2.6-photogate begin
    var failStreak by remember { mutableIntStateOf(0) } // consecutive gate failures this session
    // --- gallery hard-gate state (Best Pic Picker path) ---
    var galleryBmp by remember { mutableStateOf<Bitmap?>(null) }
    var galleryGrade by remember { mutableStateOf<PhotoGrade?>(null) }
    var galleryGrading by remember { mutableStateOf(false) }
    // v2.6-photogate end

    // --- ghost + picker state ---
    var ghostOn by remember { mutableStateOf(false) }
    var ghostBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPicker by remember { mutableStateOf(false) }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Lightweight lighting sampler: averages the Y plane of every 30th
    // preview frame (~1/sec). Cheap by design — samples every 32nd byte,
    // closes the proxy immediately, never runs ML Kit here.
    val lightAnalyzer = remember {
        var frames = 0
        var darkStreak = 0
        ImageAnalysis.Analyzer { proxy ->
            try {
                frames++
                if (frames % 30 == 0) {
                    val buf = proxy.planes.getOrNull(0)?.buffer
                    if (buf != null) {
                        val dup = buf.duplicate()
                        var sum = 0L
                        var n = 0
                        val remaining = dup.remaining()
                        var i = 0
                        while (i < remaining) {
                            sum += (dup.get(i).toInt() and 0xFF)
                            n++
                            i += 32
                        }
                        val avg = if (n > 0) sum / n else 255L
                        if (avg < 55) darkStreak++ else darkStreak = 0
                        val dark = darkStreak >= 3
                        mainExecutor.execute { lowLight = dark }
                    }
                }
            } catch (_: Exception) {
                // never let the sampler break the preview
            } finally {
                proxy.close()
            }
        }
    }
    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    DisposableEffect(lifecycleOwner) {
        var provider: ProcessCameraProvider? = null
        val listener = Runnable {
            provider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }
            imageAnalysis.setAnalyzer(cameraExecutor, lightAnalyzer)
            try {
                provider?.unbindAll()
                provider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            } catch (_: Exception) {
                captureError = "Could not start the camera on this device."
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            imageAnalysis.clearAnalyzer()
            provider?.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    // Load the ghost bitmap once (last scan photo, downscaled).
    LaunchedEffect(Unit) {
        ghostBitmap = loadGhostBitmap(context)
    }

    fun clearPending() {
        pendingAngle = null
        pendingShot = null
        pendingGrade = null
    }

    fun acceptPending() {
        val angle = pendingAngle
        val bmp = pendingShot
        if (angle != null && bmp != null) {
            shots[angle] = bmp
            if (step < angles.lastIndex) step++
        }
        // v2.6-photogate begin: a passing shot breaks the failure streak
        failStreak = 0
        // v2.6-photogate end
        clearPending()
    }

    // v2.6-photogate begin
    /** Runs a gallery-picked photo through the same hard gate before scoring. */
    fun gradePickedPhoto(bmp: Bitmap) {
        galleryBmp = bmp
        galleryGrading = true
        galleryGrade = null
        scope.launch(Dispatchers.Default) {
            val g = try {
                PhotoQuality.grade(bmp)
            } catch (_: Exception) {
                // fail-open: never trap the user on a grading hiccup
                PhotoGrade(70, emptyList(), true)
            }
            galleryGrading = false
            if (g.pass) {
                onAnalyze(bmp, bmp, bmp)
                galleryBmp = null
            } else {
                failStreak++ // consecutive failure counter drives the "use anyway" escape hatch
                galleryGrade = g
            }
        }
    }
    // v2.6-photogate end

    fun takePhoto() {
        if (capturing || grading || pendingGrade != null) return
        capturing = true
        captureError = null
        val angle = angles[step]
        val file = File(context.cacheDir, "moggr_${angle.name.lowercase()}_${System.currentTimeMillis()}.jpg")
        val output = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            output,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    // sec-hardening: the bitmap lives in memory from here on, so the
                    // temp capture file is no longer needed. Delete best-effort —
                    // never break the capture flow (covers the decode-failure path too).
                    try {
                        file.delete()
                    } catch (_: Exception) {
                    }
                    capturing = false
                    if (bmp != null) {
                        // Quality gate: grade off the main thread, then show the sheet.
                        grading = true
                        scope.launch(Dispatchers.Default) {
                            val grade = try {
                                PhotoQuality.grade(bmp)
                            } catch (_: Exception) {
                                // fail-open: never trap the user on a grading hiccup
                                PhotoGrade(70, emptyList(), true)
                            }
                            pendingAngle = angle
                            pendingShot = bmp
                            // v2.6-photogate begin: hard gate — count consecutive failures
                            if (!grade.pass) failStreak++
                            // v2.6-photogate end
                            pendingGrade = grade
                            grading = false
                        }
                    } else {
                        captureError = "Photo failed to save. Try again."
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    captureError = "Capture failed: ${exc.message}"
                    capturing = false
                }
            }
        )
    }

    fun retake(angle: CaptureAngle) {
        clearPending()
        shots.remove(angle)
        step = angles.indexOf(angle)
        captureError = null
    }

    // Passing shots auto-continue after ~1.2s (chip shows with a retake link).
    val shownGrade = pendingGrade
    LaunchedEffect(shownGrade) {
        if (shownGrade != null && shownGrade.pass) {
            delay(1200)
            acceptPending()
        }
    }

    val allDone = shots.size == angles.size
    val grade = pendingGrade
    val shot = pendingShot

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top bar: titles + ghost toggle + rank-my-pics
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    EqHeadline(
                        text = "Moggr Face Scan",
                        size = 24,
                        color = EqInk
                    )
                    EqSectionLabel("your privacy is our priority")
                }
                if (ghostBitmap != null) {
                    TopPill(
                        text = "Ghost",
                        active = ghostOn,
                        onClick = { ghostOn = !ghostOn }
                    )
                    Spacer(Modifier.width(8.dp))
                }
                TopPill(text = "Rank my pics", onClick = { showPicker = true })
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (allDone) "All angles captured"
                        else angles[step].title.replace("—", "-"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EqInk,
                    modifier = Modifier.weight(1f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    angles.forEachIndexed { i, _ ->
                        val filled = allDone || i <= step
                        Box(
                            modifier = Modifier
                                .width(42.dp)
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (filled) MogCoral else EqLine)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            if (!allDone) {
                // v2.7 reskin: glass guide card.
                EqGlassCard(modifier = Modifier.fillMaxWidth()) {
                    EqBody(angles[step].guide)
                }
                Spacer(Modifier.height(12.dp))
            }

            // Preview with face frame overlay (+ ghost, low-light pill)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .shadow(14.dp, RoundedCornerShape(28.dp), spotColor = Color(0x20000000))
                    .clip(RoundedCornerShape(28.dp))
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
                // ghost of the last scan, behind the framing guides
                GhostOverlay(
                    bitmap = ghostBitmap,
                    visible = ghostOn
                )
                // face frame overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.72f)
                        .align(Alignment.Center)
                        .border(
                            BorderStroke(2.dp, MogCoral),
                            RoundedCornerShape(120.dp)
                        )
                )
                if (lowLight) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                "Low light — face a window",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                if (capturing || grading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            if (grading) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Checking quality…",
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            captureError?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MogCoralDark,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // Thumbnails of captured angles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                angles.forEach { angle ->
                    val bmp = shots[angle]
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.55f))
                            .border(
                                BorderStroke(
                                    2.dp,
                                    if (bmp != null) MogCoral
                                    else EqLine
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable(enabled = bmp != null) { retake(angle) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (bmp != null) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "${angle.name} photo, tap to retake",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = angle.name.take(1),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EqMuted
                            )
                        }
                    }
                }
            }
            Text(
                text = "Tap a photo to retake it",
                fontSize = 13.sp,
                color = EqMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(EqLine)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(shots.size / 3f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(MogCoral)
                )
            }

            Spacer(Modifier.weight(1f))

            if (allDone) {
                // v2.7 reskin: coral pill CTA.
                EqCoralPillButton(
                    text = Strings.s("cam_analyze", LanguageStore.isHinglish),
                    onClick = {
                        onAnalyze(shots[CaptureAngle.FRONT]!!, shots[CaptureAngle.LEFT]!!, shots[CaptureAngle.RIGHT]!!)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // v2.8 restyle: circular coral shutter like the mockup.
                // Logic untouched: same takePhoto() call, same enabled gate.
                val shutterReady = !capturing && !grading && grade == null
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .shadow(
                                12.dp, CircleShape,
                                spotColor = MogCoral.copy(alpha = 0.45f)
                            )
                            .clip(CircleShape)
                            .background(
                                brush = if (shutterReady) Brush.radialGradient(
                                    listOf(Color(0xFFFF9D85), MogCoral)
                                ) else SolidColor(EqLine)
                            )
                            .clickable(enabled = shutterReady) { takePhoto() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (capturing || grading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(30.dp),
                                strokeWidth = 3.dp
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (capturing || grading) "Capturing…" else "Tap to capture",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = EqInk
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Good lighting, plain background, no filters",
                        fontSize = 12.sp,
                        color = EqMuted
                    )
                }
            }
            Spacer(Modifier.width(1.dp))
        }

        // --- quality gate overlays ---
        if (grade != null && shot != null) {
            if (grade.pass) {
                // compact non-blocking chip, auto-continues
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 132.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(EqPillDark)
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Quality ${grade.score}/100 ✓",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "retake",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 13.sp,
                            modifier = Modifier.clickable { clearPending() }
                        )
                    }
                }
            } else {
                // v2.6-photogate begin: HARD GATE — full-screen retake explainer.
                // "Use anyway" is only unlocked after 3 consecutive failures.
                PhotoGateExplainer(
                    grade = grade,
                    allowBypass = failStreak >= 3,
                    retakeLabel = Strings.s("cam_retake", LanguageStore.isHinglish), // v2.6-hinglish
                    onRetake = { clearPending() },
                    onCancel = { clearPending() },
                    onUseAnyway = { acceptPending() }
                )
                // v2.6-photogate end
            }
        }

        // --- best-pic picker overlay ---
        if (showPicker) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(AppBg)
            ) {
                BestPicPicker(
                    onPick = { bmp ->
                        showPicker = false
                        // v2.6-photogate begin: gallery picks go through the same hard gate
                        gradePickedPhoto(bmp)
                        // v2.6-photogate end
                    },
                    onDismiss = { showPicker = false }
                )
            }
        }

        // v2.6-photogate begin: gallery hard-gate overlays
        if (galleryGrading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MogCoral)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Checking your pic…",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
        val gateGrade = galleryGrade
        val gateBmp = galleryBmp
        if (gateGrade != null && gateBmp != null && !galleryGrading) {
            PhotoGateExplainer(
                grade = gateGrade,
                allowBypass = failStreak >= 3,
                retakeLabel = "Pick another",
                onRetake = {
                    galleryGrade = null
                    galleryBmp = null
                    showPicker = true
                },
                onCancel = {
                    galleryGrade = null
                    galleryBmp = null
                },
                onUseAnyway = {
                    galleryGrade = null
                    galleryBmp = null
                    onAnalyze(gateBmp, gateBmp, gateBmp)
                }
            )
        }
        // v2.6-photogate end
    }
}

// v2.6-photogate begin
/**
 * Full-screen hard-gate explainer. Names exactly what failed and how to fix
 * each issue. Big "Retake" + "Cancel" (back to camera); "Use anyway" only
 * unlocks after 3 consecutive failures, with an honest warning that the
 * score may be off. Warm cream editorial look, Gen Z tone, no emojis.
 */
@Composable
private fun PhotoGateExplainer(
    grade: PhotoGrade,
    allowBypass: Boolean,
    retakeLabel: String,
    onRetake: () -> Unit,
    onCancel: () -> Unit,
    onUseAnyway: () -> Unit
) {
    val rows: List<Pair<String, String>> =
        if (grade.details.isNotEmpty()) grade.details.map { it.title to it.fix }
        else grade.issues.map { it to "retake in better light with your face in frame" }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            EqSectionLabel("photo check")
            Spacer(Modifier.height(12.dp))
            EqHeadline(
                text = "This pic won't score right",
                size = 30,
                color = EqInk,
                align = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Bad photo = bad score. Fix the stuff below and your score will actually mean something.",
                color = EqMuted,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            // score ring
            Box(
                modifier = Modifier.size(112.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { grade.score / 100f },
                    modifier = Modifier.fillMaxSize(),
                    color = MogCoralDark,
                    trackColor = EqLine,
                    strokeWidth = 9.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${grade.score}",
                        fontWeight = FontWeight.Bold,
                        color = EqInk,
                        fontSize = 28.sp
                    )
                    Text(
                        text = "/100",
                        color = EqMuted,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            // one fix line per failed check, inside a glass card
            EqGlassCard(modifier = Modifier.fillMaxWidth()) {
                rows.forEach { (title, fix) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MogCoral)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontWeight = FontWeight.SemiBold,
                                color = EqInk,
                                fontSize = 16.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = fix,
                                color = EqMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(16.dp))
            EqCoralPillButton(
                text = retakeLabel,
                onClick = onRetake,
                modifier = Modifier.fillMaxWidth()
            )
            if (allowBypass) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Use anyway is unlocked — heads up, the score might be way off.",
                    color = EqMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                TextButton(
                    onClick = onUseAnyway,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Use anyway",
                        color = MogCoralDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cancel",
                    color = EqInk,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
// v2.6-photogate end

/** Small pill button for the camera top bar — glass chip, dark when active. */
@Composable
private fun TopPill(
    text: String,
    onClick: () -> Unit,
    active: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) EqPillDark else Color.White.copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (active) Color.White else EqInk,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Score ring: coral for pass, deep coral for fail. */
@Composable
private fun ScoreRing(score: Int, pass: Boolean) {
    Box(
        modifier = Modifier.size(96.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { score / 100f },
            modifier = Modifier.fillMaxSize(),
            color = if (pass) MogCoral else MogCoralDark,
            trackColor = EqLine,
            strokeWidth = 8.dp
        )
        Text(
            text = "$score",
            fontWeight = FontWeight.Bold,
            color = EqInk,
            fontSize = 24.sp
        )
    }
}
