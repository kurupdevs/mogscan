package com.kurupdevs.moggr.analysis

data class FeatureScore(
    val name: String,
    val score: Double,
    val note: String
)

data class PillarScore(
    val name: String,
    val score: Double,
    val note: String
)

data class Improvement(
    val area: String,
    val method: String,
    val effort: String
)

/**
 * One measured point of the face mesh, normalized to 0..1 in the source
 * bitmap's own coordinate space (x = px / bitmapWidth).
 * kind: 0 = face oval, 1 = eye, 2 = brow, 3 = nose, 4 = mouth, 5 = ear/cheek.
 */
data class LandmarkPt(
    val x: Float,
    val y: Float,
    val kind: Int
)

data class PslReport(
    val overallPsl: Double,
    val overall100: Int,
    val features: List<FeatureScore>,
    val strengths: List<String>,
    val improvements: List<Improvement>,
    val summary: String,
    val anglesRead: Int,
    val decile: Double = 0.0,
    val percentile: Int = 0,
    val pillars: List<PillarScore> = emptyList(),
    val photoNotes: List<String> = emptyList(),
    val failoCount: Int = 0,
    val haloCount: Int = 0,
    /** Real measured face mesh for the scan overlay (empty for pre-1.7 reports). */
    val landmarkMesh: List<LandmarkPt> = emptyList(),
    /** Face bounding box, normalized 0..1 (left, top, right, bottom). */
    val faceBox: List<Float> = emptyList(),
    /** Normalized brow/nose-base y positions for thirds guides (top, brow, noseBase, chin). */
    val thirdsY: List<Float> = emptyList(),
    /** 0..1 confidence in the read, derived from pose/lighting/distance/angles. */
    val confidence: Double = 0.0,
    /** ± points of score uncertainty shown next to the PSL. */
    val uncertainty: Double = 0.5,
    /** Softmaxx ceiling recomputed from measured gaps (0.0 = pre-1.7 report). */
    val potentialPsl: Double = 0.0,
    /** Face shape classification, e.g. "Oval" ("" = not measured). */
    val faceShape: String = "",
    val faceShapeNote: String = "",
    /** Key = FeatureScore.name, value = "yours vs ideal" string (filled by the measure chunk). */
    val measureDetails: Map<String, String> = emptyMap(),
    /** "Warm" / "Cool" / "Neutral" / "" (unknown). */
    val skinUndertone: String = "",
    val canthalTiltDeg: Double = 0.0,
    /** Epoch millis of the scan; 0 = legacy report. */
    val timestamp: Long = 0L
) {
    /** Top 3 scored features, best first. */
    val topTraits: List<FeatureScore>
        get() = features.sortedByDescending { it.score }.take(3)
}

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data object Loading : AnalysisUiState
    data class Success(val report: PslReport) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}
