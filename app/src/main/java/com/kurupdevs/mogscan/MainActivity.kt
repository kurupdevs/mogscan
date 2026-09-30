package com.kurupdevs.mogscan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kurupdevs.mogscan.analysis.AnalysisUiState
import com.kurupdevs.mogscan.analysis.AnalysisViewModel
import com.kurupdevs.mogscan.camera.CameraCapture
import com.kurupdevs.mogscan.ui.AnalyzingOverlay
import com.kurupdevs.mogscan.ui.AnalysisErrorState
import com.kurupdevs.mogscan.ui.ResultScreen
import com.kurupdevs.mogscan.ui.SetupScreen
import com.kurupdevs.mogscan.ui.theme.MogScanTheme
import com.kurupdevs.mogscan.util.ApiKeyStore

private enum class Screen { SETUP, CAMERA, RESULT }

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

    var apiKey by remember { mutableStateOf(ApiKeyStore.get(context)) }
    var screen by remember { mutableStateOf(if (apiKey == null) Screen.SETUP else Screen.CAMERA) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionDenied = !granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission && !permissionDenied) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val analysisState by vm.uiState.collectAsState()
    LaunchedEffect(analysisState) {
        if (analysisState is AnalysisUiState.Success) screen = Screen.RESULT
    }

    when (screen) {
        Screen.SETUP -> SetupScreen(onKeySaved = { key ->
            ApiKeyStore.save(context, key)
            apiKey = key
            screen = Screen.CAMERA
        })

        Screen.CAMERA -> {
            if (!hasCameraPermission) {
                CameraPermissionRationale(
                    denied = permissionDenied,
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            } else {
                Box(Modifier.fillMaxSize()) {
                    CameraCapture(
                        onAnalyze = { front: Bitmap, left: Bitmap, right: Bitmap ->
                            val key = apiKey
                            if (key != null) vm.analyze(key, front, left, right)
                        }
                    )
                    when (val s = analysisState) {
                        is AnalysisUiState.Loading -> AnalyzingOverlay(onCancel = { vm.reset() })
                        is AnalysisUiState.Error -> AnalysisErrorState(s, onDismiss = { vm.reset() })
                        else -> Unit
                    }
                }
            }
        }

        Screen.RESULT -> {
            val s = analysisState
            if (s is AnalysisUiState.Success) {
                ResultScreen(
                    report = s.report,
                    onRescan = {
                        vm.reset()
                        screen = Screen.CAMERA
                    },
                    onChangeKey = {
                        ApiKeyStore.clear(context)
                        apiKey = null
                        vm.reset()
                        screen = Screen.SETUP
                    }
                )
            } else {
                // Report lost (e.g. process restart) — go back to camera
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
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(64.dp))
        Text(
            text = "Camera access needed",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (denied)
                "You denied camera access. MogScan needs it to capture your three scan angles. " +
                    "Please allow it in Settings → Apps → MogScan → Permissions."
            else
                "MogScan captures your face from three angles to rate it. " +
                    "Photos are only sent to Google's Gemini API for analysis.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        if (!denied) {
            Button(onClick = onRequest) { Text("Allow camera") }
        }
    }
}
