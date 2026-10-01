package com.kurupdevs.moggr.util

import android.content.Context
import java.time.LocalDate

/**
 * Persistent memory for Moggr Coach: what the coach "remembers" about the user
 * across sessions. All local (SharedPreferences) — only the small context string
 * produced by [getMemoryContext] is ever attached to the system prompt.
 * No photos, ever — just numbers and the user's own words.
 */
object CoachMemory {

    const val VIBE_BLUNT = "blunt"
    const val VIBE_BIGBRO = "bigbro"
    const val VIBE_HYPE = "hype"

    private const val PREFS = "moggr_coach_memory"

    private fun todayStr(): String = LocalDate.now().toString()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------- Scan summaries ----------

    data class ReportSummary(
        val psl: Double,
        val tier: String,
        val weakest: List<String>,
        val date: String
    )

    /**
     * Saves the scan summary. Keeps the best-ever PSL for the all-time-best
     * celebration; the summary always reflects the latest scan.
     * @return true if this is a new all-time best AND a previous scan existed
     *         (i.e. worth celebrating), false otherwise.
     */
    fun saveReportSummary(context: Context, summary: ReportSummary): Boolean {
        val p = prefs(context)
        val hadPrior = p.contains("best_psl")
        val best = p.getFloat("best_psl", 0f)
        val isBest = summary.psl > best || !hadPrior
        p.edit()
            .putString("summary_psl", summary.psl.toString())
            .putString("summary_tier", summary.tier)
            .putString("summary_weakest", summary.weakest.joinToString("|"))
            .putString("summary_date", summary.date)
            .putFloat("best_psl", if (isBest) summary.psl.toFloat() else best)
            .apply()
        return hadPrior && isBest
    }

    fun loadSummary(context: Context): ReportSummary? {
        val p = prefs(context)
        val psl = p.getString("summary_psl", null)?.toDoubleOrNull() ?: return null
        return ReportSummary(
            psl = psl,
            tier = p.getString("summary_tier", "") ?: "",
            weakest = (p.getString("summary_weakest", "") ?: "").split("|").filter { it.isNotBlank() },
            date = p.getString("summary_date", "") ?: ""
        )
    }

    // ---------- Vibe ----------

    fun saveVibe(context: Context, vibe: String) {
        prefs(context).edit().putString("vibe", vibe).apply()
    }

    fun getVibe(context: Context): String =
        prefs(context).getString("vibe", VIBE_BLUNT) ?: VIBE_BLUNT

    // ---------- Morning check-ins ----------

    data class Checkin(val sleep: String, val puff: Int, val focus: String)

    fun logCheckin(context: Context, checkin: Checkin) {
        prefs(context).edit()
            .putString("checkin_${todayStr()}", "${checkin.sleep}¦${checkin.puff}¦${checkin.focus}")
            .apply()
    }

    fun todayCheckin(context: Context): Checkin? {
        val raw = prefs(context).getString("checkin_${todayStr()}", null) ?: return null
        val parts = raw.split("¦")
        if (parts.size != 3) return null
        return Checkin(parts[0], parts[1].toIntOrNull() ?: 3, parts[2])
    }

    /** Has the morning check-in card been shown today (completed or dismissed)? */
    fun checkinCardShownToday(context: Context): Boolean =
        prefs(context).getString("checkin_card_shown", null) == todayStr()

    fun markCheckinCardShown(context: Context) {
        prefs(context).edit().putString("checkin_card_shown", todayStr()).apply()
    }

    // ---------- Celebrations ----------

    fun hasSeenCelebration(context: Context, key: String): Boolean =
        (prefs(context).getStringSet("celebrations", emptySet()) ?: emptySet()).contains(key)

    fun markCelebration(context: Context, key: String) {
        val p = prefs(context)
        val seen = (p.getStringSet("celebrations", emptySet()) ?: emptySet()).toMutableSet()
        seen.add(key)
        p.edit().putStringSet("celebrations", seen).apply()
    }

    // ---------- Prompt context ----------

    /**
     * 2–4 short lines describing what the coach remembers, for the system prompt.
     * Returns "" when there's nothing worth remembering yet.
     */
    fun getMemoryContext(context: Context): String {
        val lines = mutableListOf<String>()
        val summary = loadSummary(context)
        if (summary != null) {
            val weak = summary.weakest.take(3).joinToString(", ")
            lines.add(
                "Last scan: ${fmtPsl(summary.psl)} PSL${summary.tier.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""}" +
                    (if (weak.isNotBlank()) ", weakest traits: $weak" else "") +
                    (if (summary.date.isNotBlank()) ", scanned ${summary.date}" else "") + "."
            )
        }
        val goal = ProfileStore.load(context)?.goal?.takeIf { it.isNotBlank() }
        if (goal != null) lines.add("Their stated looksmaxxing goal: $goal.")
        todayCheckin(context)?.let { c ->
            lines.add(
                "Morning check-in today: slept ${c.sleep}, face puffiness ${c.puff}/5, today's #1 focus: ${c.focus}."
            )
        }
        if (summary != null || goal != null) {
            lines.add(
                when (getVibe(context)) {
                    VIBE_BIGBRO -> "Talk to them like a warm big-brother: encouraging but honest, never sugarcoating."
                    VIBE_HYPE -> "Talk to them like a high-energy hype man: loud support, still honest about flaws."
                    else -> "Talk to them like a blunt older-brother: no fluff, straight facts, call out cope."
                }
            )
        }
        return lines.take(4).joinToString("\n")
    }

    private fun fmtPsl(psl: Double): String =
        String.format(java.util.Locale.US, "%.1f", psl)
}
