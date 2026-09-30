package com.kurupdevs.mogscan.analysis

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

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
    val summary: String
)

sealed interface AnalysisUiState {
    data object Idle : AnalysisUiState
    data object Loading : AnalysisUiState
    data class Success(val report: PslReport) : AnalysisUiState
    data class Error(val message: String) : AnalysisUiState
}

/**
 * Sends the three captured face photos (front, left profile, right profile)
 * to Gemini and parses the returned PSL rating report.
 */
class AnalysisViewModel(app: Application) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState

    fun reset() {
        _uiState.value = AnalysisUiState.Idle
    }

    fun analyze(apiKey: String, front: Bitmap, leftProfile: Bitmap, rightProfile: Bitmap) {
        if (_uiState.value is AnalysisUiState.Loading) return
        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Loading
            try {
                val downsized = withContext(Dispatchers.Default) {
                    listOf(front, leftProfile, rightProfile).map { downscale(it) }
                }
                val model = GenerativeModel(
                    modelName = "gemini-2.0-flash",
                    apiKey = apiKey,
                    generationConfig = generationConfig {
                        temperature = 0.4f
                        responseMimeType = "application/json"
                    }
                )
                val input = content {
                    downsized.forEach { image(it) }
                    text(buildPrompt())
                }
                val response = withContext(Dispatchers.IO) { model.generateContent(input) }
                val raw = response.text ?: throw IllegalStateException("Empty response from model")
                _uiState.value = AnalysisUiState.Success(parseReport(raw))
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("API key", ignoreCase = true) == true ->
                        "Invalid API key. Check the key and try again."
                    e.message?.contains("quota", ignoreCase = true) == true ->
                        "Free quota exceeded. Try again later."
                    else -> e.message ?: "Analysis failed. Check your connection and try again."
                }
                _uiState.value = AnalysisUiState.Error(msg)
            }
        }
    }

    private fun downscale(src: Bitmap, maxDim: Int = 1024): Bitmap {
        val longest = maxOf(src.width, src.height)
        if (longest <= maxDim) return src
        val scale = maxDim / longest.toFloat()
        return Bitmap.createScaledBitmap(
            src,
            (src.width * scale).toInt(),
            (src.height * scale).toInt(),
            true
        )
    }

    private fun buildPrompt(): String = """
        You are an expert facial aesthetics analyst. You rate faces on the PSL scale from 1.0 to 9.0,
        where 3.0-3.9 is below average, 4.0-4.5 is average (normie), 5.0-5.9 is attractive,
        6.0-6.9 is model tier, and 7.0+ is extremely rare.

        You are given 3 photos of the same person, in order: FRONT view, LEFT PROFILE view, RIGHT PROFILE view.
        Use all three angles. Judge bone structure and soft features: jawline definition and width,
        cheekbone prominence, chin projection and shape, eye area (canthal tilt, spacing, under-eye support),
        eyebrows, nose (width, projection, symmetry), lips, facial thirds proportions, overall symmetry,
        skin quality, hairline, forward facial growth, neck and head posture.

        Return STRICT JSON only. No markdown fences, no commentary. Exactly this shape:
        {
          "overall_psl": <number 1.0-9.0, one decimal>,
          "overall_100": <integer 0-100>,
          "features": [
            {"name": "Jawline", "score": <1.0-9.0>, "note": "<one short sentence>"},
            ... 8 to 12 features total
          ],
          "strengths": ["<short phrase>", ... up to 4],
          "improvements": [
            {"area": "...", "method": "<concrete softmaxxing method>", "effort": "easy|medium|hard"},
            ... 4 to 7 items
          ],
          "summary": "<2-3 sentence honest summary>"
        }

        Rules:
        - Be honest but never cruel or mocking.
        - Feature scores must be consistent with overall_psl (within about 1.5 points).
        - Improvements must be NON-SURGICAL only: skincare, hairstyle, facial hair, tongue posture (mewing),
          chewing, head/neck posture, sleep, body-fat reduction through diet and training, style and grooming,
          photo lighting and angles. Never recommend surgery, fillers, or any medical procedure.
    """.trimIndent()

    private fun parseReport(raw: String): PslReport {
        val cleaned = raw.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```").trim()
        val o = JSONObject(cleaned)

        val features = mutableListOf<FeatureScore>()
        val fArr = o.optJSONArray("features")
        if (fArr != null) {
            for (i in 0 until fArr.length()) {
                val f = fArr.getJSONObject(i)
                features.add(
                    FeatureScore(
                        name = f.optString("name", "Feature"),
                        score = f.optDouble("score", 4.0).coerceIn(1.0, 9.0),
                        note = f.optString("note", "")
                    )
                )
            }
        }

        val strengths = mutableListOf<String>()
        val sArr = o.optJSONArray("strengths")
        if (sArr != null) {
            for (i in 0 until sArr.length()) strengths.add(sArr.optString(i))
        }

        val improvements = mutableListOf<Improvement>()
        val iArr = o.optJSONArray("improvements")
        if (iArr != null) {
            for (i in 0 until iArr.length()) {
                val im = iArr.getJSONObject(i)
                improvements.add(
                    Improvement(
                        area = im.optString("area", "Looks"),
                        method = im.optString("method", ""),
                        effort = im.optString("effort", "medium")
                    )
                )
            }
        }

        return PslReport(
            overallPsl = o.optDouble("overall_psl", 4.0).coerceIn(1.0, 9.0),
            overall100 = o.optInt("overall_100", 50).coerceIn(0, 100),
            features = features,
            strengths = strengths,
            improvements = improvements,
            summary = o.optString("summary", "")
        )
    }
}
