package com.kurupdevs.moggr.analysis

data class FeatureScore(
    val name: String,
    val score: Double,
    val note: String
)

data class Improvement(
    val area: String,
    val method: String,
    val effort: String
)

data class PslReport(
    val overallPsl: Double,
    val overall100: Int,
    val features: List<FeatureScore>,
    val strengths: List<String>,
    val improvements: List<Improvement>,
    val summary: String,
    val anglesRead: Int
)

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data object Loading : AnalysisUiState
    data class Success(val report: PslReport) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}
