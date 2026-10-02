package com.kurupdevs.moggr.util

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Checks the latest Moggr release on GitHub and compares it to the installed
 * version. All network work happens off the main thread via [checkLatest].
 */
object UpdateCheck {

    private const val LATEST_URL = "https://api.github.com/repos/kurupdevs/Moggr/releases/latest"
    private const val CORAL = 0xFFED5564L

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    data class UpdateInfo(val latestTag: String, val isNewer: Boolean)

    /**
     * Returns [UpdateInfo] or null on any failure (network, parse, version-format).
     * [currentVersionName] is the BuildConfig-style "X.Y" string (e.g. "3.5");
     * the GitHub tag is expected in "vX.Y" form.
     */
    suspend fun checkLatest(currentVersionName: String): UpdateInfo? {
        return try {
            val body = withContext(Dispatchers.IO) {
                val request = Request.Builder().url(LATEST_URL).get().build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    response.body?.string()
                }
            } ?: return null

            val tag = JSONObject(body).optString("tag_name", "").ifBlank { return null }
            UpdateInfo(latestTag = tag, isNewer = isNewer(tag, currentVersionName))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Compares "v3.6" vs "3.5" numerically, component by component.
     * Missing components count as 0, so "v3.6" > "3.5.0".
     * Returns false if either side isn't parseable.
     */
    fun isNewer(tag: String, currentVersionName: String): Boolean {
        val latest = parse(tag) ?: return false
        val current = parse(currentVersionName) ?: return false
        val maxLen = maxOf(latest.size, current.size)
        for (i in 0 until maxLen) {
            val a = latest.getOrNull(i) ?: 0
            val b = current.getOrNull(i) ?: 0
            if (a != b) return a > b
        }
        return false
    }

    private fun parse(version: String): List<Int>? {
        val stripped = version.trim().removePrefix("v").removePrefix("V")
        if (stripped.isEmpty()) return null
        return try {
            stripped.split(".").map { it.toInt() }
        } catch (e: NumberFormatException) {
            null
        }
    }

    /** Non-blocking banner shown when a newer release is available. */
    @Composable
    fun UpdateBanner(info: UpdateInfo, onDismiss: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(CORAL).copy(alpha = 0.14f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${info.latestTag} available — get it from kurupdevs.github.io/moggr",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = Color(CORAL))
                }
            }
        }
    }
}
