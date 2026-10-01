package com.kurupdevs.mogscan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kurupdevs.mogscan.analysis.AnalysisUiState
import com.kurupdevs.mogscan.analysis.AnalysisViewModel
import com.kurupdevs.mogscan.camera.CameraCapture
import com.kurupdevs.mogscan.ui.AnalysisErrorState
import com.kurupdevs.mogscan.ui.AnalyzingScreen
import com.kurupdevs.mogscan.ui.InfoSlidesScreen
import com.kurupdevs.mogscan.ui.IntroVideoScreen
import com.kurupdevs.mogscan.ui.PslBlack
import com.kurupdevs.mogscan.ui.PslBlue
import com.kurupdevs.mogscan.ui.PslGrey
import com.kurupdevs.mogscan.ui.QuestionFlow
import com.kurupdevs.mogscan.ui.ResultScreen
import com.kurupdevs.mogscan.ui.theme.MogScanTheme
import com.kurupdevs.mogscan.util.ProfileStore
import com.kurupdevs.mogscan.util.UserProfile
import kotlinx.coroutines.delay

private enum class Screen { INTRO, INFO, QUESTIONS, CAMERA, ANALYZING, RESULT }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MogScanTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MogScanApp()
                }
            }
        }
    }
}

@Composable
private fun MogScanApp() {
    val context = LocalContext.current
    val vm: AnalysisViewModel = viewModel()

    var screen by remember { mutableStateOf(Screen.INTRO) }
    var profile by remember { mutableStateOf<UserProfile?>(null) }
    var pendingPhotos by remember { mutableStateOf<Triple<Bitmap, Bitmap, Bitmap>?>(null) }
    var analyzingMinDone by remember { mutableStateOf(false) }

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

    // Advance from ANALYZING once the animation has played AND the result is ready.
    LaunchedEffect(analysisState, analyzingMinDone, screen) {
        if (screen != Screen.ANALYZING) return@LaunchedEffect
        if (!analyzingMinDone) return@LaunchedEffect
        when (analysisState) {
            is AnalysisUiState.Success -> screen = Screen.RESULT
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
                    screen = if (profile != null) Screen.CAMERA else Screen.INFO
                }
            )
        }

        Screen.INFO -> {
            InfoSlidesScreen(onDone = { screen = Screen.QUESTIONS })
        }

        Screen.QUESTIONS -> {
            QuestionFlow(
                onComplete = { p ->
                    ProfileStore.save(context, p)
                    profile = p
                    screen = Screen.CAMERA
                },
                onBackToIntro = { screen = Screen.INTRO }
            )
        }

        Screen.CAMERA -> {
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
                            pendingPhotos = Triple(front, left, right)
                            analyzingMinDone = false
                            vm.reset()
                            screen = Screen.ANALYZING
                        }
                    )
                    val s = analysisState
                    if (s is AnalysisUiState.Error) {
                        AnalysisErrorState(s, onDismiss = { vm.reset() })
                    }
                }
            }
        }

        Screen.ANALYZING -> {
            AnalyzingScreen()
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
                    onRescan = {
                        vm.reset()
                        pendingPhotos = null
                        screen = Screen.CAMERA
                    }
                )
            } else {
                LaunchedEffect(Unit) { screen = Screen.CAMERA }
            }
        }
    }
}

@Composable
private fun CameraPermissionRationale(denied: Boolean, onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PslBlack)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))
        Text(
            text = "PSL AI Face Scan",
            fontSize = 22.sp,
            color = Color.White
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
                "Camera access was denied. MogScan needs it to capture your three scan angles. " +
                    "Please allow it in Settings → Apps → MogScan → Permissions."
            else
                "MogScan captures your face from three angles and rates it on your phone. " +
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
