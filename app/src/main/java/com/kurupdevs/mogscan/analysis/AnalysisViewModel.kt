package com.kurupdevs.mogscan.analysis

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Runs the three captured face photos through the on-device [FaceAnalyzer]
 * (ML Kit, bundled model — no network, no API key) and exposes the PSL report.
 */
class AnalysisViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState

    fun reset() {
        _uiState.value = AnalysisUiState.Idle
    }

    fun analyze(front: Bitmap, leftProfile: Bitmap, rightProfile: Bitmap) {
        if (_uiState.value is AnalysisUiState.Loading) return
        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Loading
            try {
                val report = FaceAnalyzer.analyze(listOf(front, leftProfile, rightProfile))
                _uiState.value = AnalysisUiState.Success(report)
            } catch (e: Exception) {
                _uiState.value = AnalysisUiState.Error(
                    e.message ?: "Analysis failed. Try again with better lighting."
                )
            }
        }
    }
}
