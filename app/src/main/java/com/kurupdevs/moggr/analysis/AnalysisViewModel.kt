package com.kurupdevs.moggr.analysis

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kurupdevs.moggr.util.ReportStore
import kotlinx.coroutines.Dispatchers
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

    private val _recomputing = MutableStateFlow(false)
    /** True while a verify-points recalculation is running. */
    val recomputing: StateFlow<Boolean> = _recomputing

    // v2.6-landmark: full-resolution geometry behind the latest scan, so the
    // verify-points UI can offer draggable markers + recalculation.
    private var lastSnapshot: FaceAnalyzer.FaceGeometrySnapshot? = null
    private var lastFront: Bitmap? = null

    /** Editable geometry for the current report, null for pre-v2.6 saved reports. */
    val verifySnapshot: FaceAnalyzer.FaceGeometrySnapshot?
        get() = lastSnapshot

    fun reset() {
        _uiState.value = AnalysisUiState.Idle
    }

    fun analyze(front: Bitmap, leftProfile: Bitmap, rightProfile: Bitmap) {
        if (_uiState.value is AnalysisUiState.Loading) return
        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Loading
            try {
                val res = FaceAnalyzer.analyze(listOf(front, leftProfile, rightProfile))
                lastSnapshot = res.geometry
                lastFront = front
                _uiState.value = AnalysisUiState.Success(res.report)
            } catch (e: Exception) {
                _uiState.value = AnalysisUiState.Error(
                    e.message ?: "Analysis failed. Try again with better lighting."
                )
            }
        }
    }

    /**
     * v2.6-landmark: re-runs the measurement pipeline on the user's corrected
     * landmark positions and publishes the updated report (also re-saved, so
     * the corrected read is the one that persists).
     */
    fun recomputeWith(corrected: FaceAnalyzer.FaceGeometrySnapshot) {
        val current = _uiState.value as? AnalysisUiState.Success ?: return
        val bmp = lastFront ?: return
        if (_recomputing.value) return
        viewModelScope.launch(Dispatchers.Default) {
            _recomputing.value = true
            try {
                val updated = FaceAnalyzer.recompute(current.report, corrected, bmp)
                lastSnapshot = corrected
                _uiState.value = AnalysisUiState.Success(updated)
                ReportStore.save(getApplication(), updated, lastFront)
            } finally {
                _recomputing.value = false
            }
        }
    }
}
