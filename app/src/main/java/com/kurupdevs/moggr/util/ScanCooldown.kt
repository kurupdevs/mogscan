package com.kurupdevs.moggr.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Daily scan cooldowns, enforced on-device with zero permissions.
 * Adults get 10 scans/day; teen mode (13–17) gets 5/day, always on.
 * Scans are consumed at the scan trigger point, before analysis runs.
 */
object ScanCooldown {
    private const val PREFS = "mogscan_scan_cooldown"
    private const val KEY_DATE = "scan_date"
    private const val KEY_COUNT = "scan_count"

    const val LIMIT_ADULT = 10
    const val LIMIT_TEEN = 5

    sealed interface Result {
        data class Allowed(val remaining: Int) : Result
        data class Blocked(val message: String) : Result
    }

    /**
     * Consumes one scan for today. Returns Allowed(remaining) or Blocked(message).
     * The day boundary resets the counter automatically.
     */
    fun tryScan(context: Context): Result {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val lastDay = prefs.getString(KEY_DATE, null)
        val used = if (lastDay == today) prefs.getInt(KEY_COUNT, 0) else 0
        val limit = if (TeenMode.isTeen(context)) LIMIT_TEEN else LIMIT_ADULT
        if (used >= limit) {
            return Result.Blocked(
                "bhai, itne scans se face nahi badlega — kal phir try kar"
            )
        }
        prefs.edit()
            .putString(KEY_DATE, today)
            .putInt(KEY_COUNT, used + 1)
            .apply()
        return Result.Allowed(limit - used - 1)
    }

    /** Remaining scans today without consuming one. */
    fun remaining(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val used = if (prefs.getString(KEY_DATE, null) == today) prefs.getInt(KEY_COUNT, 0) else 0
        val limit = if (TeenMode.isTeen(context)) LIMIT_TEEN else LIMIT_ADULT
        return (limit - used).coerceAtLeast(0)
    }
}
