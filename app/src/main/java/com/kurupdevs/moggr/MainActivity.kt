package com.kurupdevs.moggr

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
// v2.6-science begin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
// v2.6-science end
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kurupdevs.moggr.BuildConfig
import com.kurupdevs.moggr.analysis.AnalysisUiState
import com.kurupdevs.moggr.analysis.AnalysisViewModel
import com.kurupdevs.moggr.camera.CameraCapture
import com.kurupdevs.moggr.ui.AnalysisErrorState
import com.kurupdevs.moggr.ui.AnalyzingScreen
import com.kurupdevs.moggr.ui.IntroVideoScreen
import com.kurupdevs.moggr.ui.MainTabs
import com.kurupdevs.moggr.ui.MoggrBg
import com.kurupdevs.moggr.ui.PslBlue
import com.kurupdevs.moggr.ui.PslGrey
import com.kurupdevs.moggr.ui.PslText
import com.kurupdevs.moggr.ui.ProfileScreen
import com.kurupdevs.moggr.ui.QuestionFlow
import com.kurupdevs.moggr.ui.ResultScreen
import com.kurupdevs.moggr.ui.theme.MoggrTheme
import com.kurupdevs.moggr.util.ProfileStore
import com.kurupdevs.moggr.util.ReportStore
import com.kurupdevs.moggr.util.AppLock
import com.kurupdevs.moggr.util.SignatureCheck
import com.kurupdevs.moggr.util.UpdateCheck
// v2.6-science begin
import com.kurupdevs.moggr.util.ScanCooldown
// v2.6-science end
import com.kurupdevs.moggr.util.UserProfile
import kotlinx.coroutines.delay

private enum class Screen { INTRO, INFO, QUESTIONS, CAMERA, ANALYZING, RESULT, MAIN }

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // v2.8-sec: fail closed on tampered installs. Inert until the release
        // cert hash is filled into SignatureCheck.EXPECTED_SIG_SHA256.
        if (SignatureCheck.EXPECTED_SIG_SHA256 != "REPLACE_ME" &&
            !SignatureCheck.isIntact(this)
        ) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Security check failed")
                .setMessage("This copy of Moggr looks tampered. Please reinstall from the official release.")
                .setCancelable(false)
                .setPositiveButton("Close") { _, _ -> finish() }
                .show()
            return
        }
        setContent {
            MoggrTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MoggrApp()
                }
            }
        }
    }
}

@Composable
private fun MoggrApp() {
    val context = LocalContext.current
    val vm: AnalysisViewModel = viewModel()

    // v2.6-hinglish begin: load saved app language (English / Hinglish) once.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.kurupdevs.moggr.util.LanguageStore.load(context)
    }
    // v2.6-hinglish end

    var screen by remember { mutableStateOf(Screen.INTRO) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var pendingPhotos by remember { mutableStateOf<Triple<Bitmap, Bitmap, Bitmap>?>(null) }
    var analyzingMinDone by remember { mutableStateOf(false) }
    // Where CAMERA's back press goes: INTRO for first-run scans, MAIN for rescans.
    var scanReturnTo by remember { mutableStateOf(Screen.INTRO) }
    // v2.6-science begin: daily scan cooldown block message
    var cooldownMsg by remember { mutableStateOf<String?>(null) }
    // v2.6-science end

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionDenied = !granted
    }

    val analysisState by vm.uiState.collectAsState()
    // v2.6-landmark begin
    val recomputing by vm.recomputing.collectAsState()
    // v2.6-landmark end

    // v2.8-sec begin: FLAG_SECURE on camera/scan/result screens — no
    // screenshots, no screen recording, no recents thumbnails of faces.
    val activity = context as? androidx.activity.ComponentActivity
    DisposableEffect(screen) {
        val secure = screen == Screen.CAMERA || screen == Screen.ANALYZING ||
            screen == Screen.RESULT
        if (secure) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    // v2.8-sec begin: biometric app lock. Fresh process starts unlocked;
    // returning from background requires auth.
    var showLock by remember { mutableStateOf(AppLock.isLocked()) }
    DisposableEffect(activity) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                showLock = AppLock.isLocked()
            }
        }
        activity?.lifecycle?.addObserver(obs)
        onDispose { activity?.lifecycle?.removeObserver(obs) }
    }

    // v2.8-sec begin: non-blocking update check (never forced).
    var updateInfo by remember { mutableStateOf<UpdateCheck.UpdateInfo?>(null) }
    var updateDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        updateInfo = try {
            UpdateCheck.checkLatest(BuildConfig.VERSION_NAME)
        } catch (_: Exception) {
            null
        }
    }
    // v2.8-sec end

    if (showLock && activity != null) {
        LockGate(
            onUnlocked = { showLock = false },
            onFailed = { activity.finish() }
        )
        return
    }

    // Advance from ANALYZING once the animation has played AND the result is ready.
    // The report + front photo are saved permanently so the user never re-scans.
    LaunchedEffect(analysisState, analyzingMinDone, screen) {
        if (screen != Screen.ANALYZING) return@LaunchedEffect
        if (!analyzingMinDone) return@LaunchedEffect
        val s = analysisState
        when (s) {
            is AnalysisUiState.Success -> {
                ReportStore.save(context, s.report, pendingPhotos?.first)
                screen = Screen.RESULT
            }
            is AnalysisUiState.Error -> {
                pendingPhotos = null
                vm.reset()
                screen = Screen.CAMERA
            }
            else -> Unit
        }
    }

    when (screen) {
        Screen.INTRO -> {
            IntroVideoScreen(
                onGetStarted = {
                    profile = ProfileStore.load(context)
                    if (profile != null && ReportStore.hasSaved(context)) {
                        screen = Screen.MAIN
                    } else {
                        // v2.5 scan-first: straight to CAMERA, no info slides or
                        // questions up front. Works with zero profile.
                        scanReturnTo = Screen.INTRO
                        screen = Screen.CAMERA
                    }
                }
            )
        }

        // Screen.INFO (info slides) retired from the critical path in v2.5 —
        // scan-first flow goes INTRO → CAMERA. The InfoSlidesScreen composable
        // stays in ui/InfoSlides.kt untouched. The enum value is kept unused
        // so other references don't break.

        Screen.INFO -> { screen = Screen.CAMERA }
        Screen.QUESTIONS -> {
            QuestionFlow(
                onComplete = { p ->
                    ProfileStore.save(context, p)
                    profile = p
                    screen = Screen.MAIN
                },
                onSkip = { screen = Screen.MAIN },
                onBack = { screen = Screen.RESULT }
            )
        }

        Screen.CAMERA -> {
            // First-run: back goes to INTRO, not an app exit. Rescans return to MAIN.
            BackHandler { screen = scanReturnTo }
            if (!hasCameraPermission) {
                if (!permissionDenied) {
                    LaunchedEffect(Unit) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
                CameraPermissionRationale(
                    denied = permissionDenied,
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            } else {
                Box(Modifier.fillMaxSize()) {
                    CameraCapture(
                        onAnalyze = { front: Bitmap, left: Bitmap, right: Bitmap ->
                            // v2.6-science begin: scan cooldown enforced at the trigger point
                            when (val scan = ScanCooldown.tryScan(context)) {
                                is ScanCooldown.Result.Blocked -> cooldownMsg = scan.message
                                is ScanCooldown.Result.Allowed -> {
                                    pendingPhotos = Triple(front, left, right)
                                    analyzingMinDone = false
                                    vm.reset()
                                    screen = Screen.ANALYZING
                                }
                            }
                            // v2.6-science end
                        }
                    )
                    val s = analysisState
                    if (s is AnalysisUiState.Error) {
                        AnalysisErrorState(s, onDismiss = { vm.reset() })
                    }
                    // v2.6-science begin: friendly cooldown-block dialog
                    cooldownMsg?.let { msg ->
                        AlertDialog(
                            onDismissRequest = { cooldownMsg = null },
                            title = { Text("Scan limit reached") },
                            text = { Text(msg) },
                            confirmButton = {
                                TextButton(onClick = { cooldownMsg = null }) {
                                    Text("Got it", color = PslBlue)
                                }
                            }
                        )
                    }
                    // v2.6-science end
                }
            }
        }

        Screen.ANALYZING -> {
            // v2.6-landmark begin: photo feeds the staged scanline card.
            AnalyzingScreen(photo = pendingPhotos?.first)
            // v2.6-landmark end
            LaunchedEffect(Unit) {
                val photos = pendingPhotos
                if (photos != null) {
                    vm.analyze(photos.first, photos.second, photos.third)
                }
                delay(4600)
                analyzingMinDone = true
            }
        }

        Screen.RESULT -> {
            val s = analysisState
            if (s is AnalysisUiState.Success) {
                ResultScreen(
                    report = s.report,
                    profile = profile ?: ProfileStore.load(context),
                    photo = pendingPhotos?.first,
                    onRescan = {
                        vm.reset()
                        pendingPhotos = null
                        screen = Screen.CAMERA
                    },
                    // v2.5: optional profile setup after the result, skippable.
                    // Users who already have a profile skip straight to MAIN.
                    onNext = {
                        profile = profile ?: ProfileStore.load(context)
                        screen = if (profile == null) Screen.QUESTIONS else Screen.MAIN
                    },
                    // v2.6-landmark begin: verify-points recalculation wiring.
                    verifySnapshot = vm.verifySnapshot,
                    recomputing = recomputing,
                    onRecalculate = { vm.recomputeWith(it) }
                    // v2.6-landmark end
                )
            } else {
                LaunchedEffect(Unit) { screen = Screen.CAMERA }
            }
        }
        Screen.MAIN -> {
            // v3.0: developer profile overlay (tappable socials).
            var showProfile by remember { mutableStateOf(false) }
            if (showProfile) {
                BackHandler { showProfile = false }
                ProfileScreen(onBack = { showProfile = false })
            } else {
            // v2.8-sec: non-blocking update banner (never forced).
            if (!updateDismissed) {
                updateInfo?.let { info ->
                    if (info.isNewer) {
                        UpdateCheck.UpdateBanner(
                            info = info,
                            onDismiss = { updateDismissed = true }
                        )
                    }
                }
            }
            val s = analysisState
            MainTabs(
                report = (s as? AnalysisUiState.Success)?.report
                    ?: ReportStore.loadReport(context),
                profile = profile ?: ProfileStore.load(context),
                frontPhoto = pendingPhotos?.first ?: ReportStore.loadPhoto(context),
                userName = (profile ?: ProfileStore.load(context))?.name.orEmpty(),
                onRescan = {
                    vm.reset()
                    pendingPhotos = null
                    scanReturnTo = Screen.MAIN
                    screen = Screen.CAMERA
                },
                onOpenProfile = { showProfile = true }
            )
            }
        }
    }
}

// v2.8-sec: biometric gate shown when the app returns from background locked.
@Composable
private fun LockGate(onUnlocked: () -> Unit, onFailed: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    fun tryUnlock() {
        if (activity != null) {
            AppLock.unlock(activity) { ok ->
                if (ok) onUnlocked() else onFailed()
            }
        } else {
            onFailed()
        }
    }
    // Prompt immediately; the button is a fallback if it gets dismissed.
    LaunchedEffect(Unit) { tryUnlock() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoggrBg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Moggr is locked", fontSize = 20.sp, color = PslText)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Verify it's you to continue.",
            fontSize = 13.sp,
            color = PslGrey
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { tryUnlock() },
            colors = ButtonDefaults.buttonColors(containerColor = PslBlue)
        ) { Text("Unlock") }
    }
}

@Composable
private fun CameraPermissionRationale(denied: Boolean, onRequest: () -> Unit) {    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MoggrBg)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))
        Text(
            text = "Moggr Face Scan",
            fontSize = 22.sp,
            color = PslText
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your privacy is our priority",
            fontSize = 13.sp,
            color = PslGrey
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (denied)
                "Camera access was denied. Moggr needs it to capture your three scan angles. " +
                    "Please allow it in Settings → Apps → Moggr → Permissions."
            else
                "Moggr captures your face from three angles and rates it on your phone. " +
                    "No photo ever leaves your device.",
            textAlign = TextAlign.Center,
            color = PslGrey
        )
        Spacer(Modifier.height(24.dp))
        if (!denied) {
            Button(
                onClick = onRequest,
                colors = ButtonDefaults.buttonColors(containerColor = PslBlue)
            ) { Text("Allow camera") }
        }
    }
}
