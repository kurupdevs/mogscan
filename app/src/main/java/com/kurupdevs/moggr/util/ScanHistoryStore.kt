package com.kurupdevs.moggr.util

import android.content.Context
import com.kurupdevs.moggr.analysis.PslReport
import com.kurupdevs.moggr.ui.pslTierShort
import org.json.JSONArray
import org.json.JSONObject

/**
 * Scan history for the progress timeline: a SharedPreferences JSON list,
 * newest first, capped at 60 entries. Photos stay on-device — only the
 * file path is stored, never uploaded anywhere.
 */
object ScanHistoryStore {
    private const val PREFS = "scan_history_prefs"
    private const val KEY = "entries"
    const val MAX_ENTRIES = 60

    data class ScanEntry(
        val timestamp: Long,
        val psl: Double,
        val decile: Double,
        val tier: String,
        val features: Map<String, Double>,
        val photoPath: String?
    )

    /** Append the report; dedupes by timestamp so re-renders never double-count. */
    fun append(context: Context, report: PslReport, photoPath: String?) {
        try {
            val list = load(context).toMutableList()
            val ts = if (report.timestamp != 0L) report.timestamp else System.currentTimeMillis()
            if (list.any { it.timestamp == ts }) return
            list.add(
                0,
                ScanEntry(
                    timestamp = ts,
                    psl = report.overallPsl,
                    decile = report.decile,
                    tier = pslTierShort(report.overallPsl),
                    features = report.features.associate { it.name to it.score },
                    photoPath = photoPath
                )
            )
            val trimmed = list.take(MAX_ENTRIES)
            val arr = JSONArray()
            trimmed.forEach { e ->
                val o = JSONObject()
                o.put("timestamp", e.timestamp)
                o.put("psl", e.psl)
                o.put("decile", e.decile)
                o.put("tier", e.tier)
                if (e.photoPath != null) o.put("photoPath", e.photoPath)
                val feats = JSONObject()
                e.features.forEach { (k, v) -> feats.put(k, v) }
                o.put("features", feats)
                arr.put(o)
            }
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY, arr.toString())
                .apply()
        } catch (_: Exception) {
        }
    }

    /** Newest first. Empty list when nothing has been scanned yet. */
    fun load(context: Context): List<ScanEntry> {
        return try {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, null) ?: return emptyList()
            val arr = JSONArray(raw)
            val out = mutableListOf<ScanEntry>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val feats = mutableMapOf<String, Double>()
                val fa = o.optJSONObject("features")
                if (fa != null) {
                    val keys = fa.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        feats[k] = fa.optDouble(k)
                    }
                }
                out.add(
                    ScanEntry(
                        timestamp = o.optLong("timestamp"),
                        psl = o.optDouble("psl"),
                        decile = o.optDouble("decile"),
                        tier = o.optString("tier"),
                        features = feats,
                        photoPath = if (o.has("photoPath")) o.optString("photoPath") else null
                    )
                )
            }
            out.sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun latest(context: Context): ScanEntry? = load(context).firstOrNull()

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY)
            .apply()
    }
}
