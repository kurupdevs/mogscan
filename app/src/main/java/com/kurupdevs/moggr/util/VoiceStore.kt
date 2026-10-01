package com.kurupdevs.moggr.util

import android.content.Context
import java.time.LocalDate

/** Last drill date + streak for the voice drills. Completing any drill counts for the day. */
data class VoiceStreak(val streak: Int, val doneToday: Boolean)

object VoiceStore {
    private const val PREFS = "moggr_voice"

    fun load(context: Context): VoiceStreak {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now()
        val last = p.getString("last_drill", null)
        val streak = p.getInt("streak", 0)
        val alive = last == today.toString() || last == today.minusDays(1).toString()
        return VoiceStreak(streak = if (alive) streak else 0, doneToday = last == today.toString())
    }

    /** Marks today's drill done; streak grows only on consecutive days. */
    fun markDrillDone(context: Context): VoiceStreak {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val today = LocalDate.now()
        val todayS = today.toString()
        val last = p.getString("last_drill", null)
        val prev = p.getInt("streak", 0)
        val newStreak = when (last) {
            todayS -> prev
            today.minusDays(1).toString() -> prev + 1
            else -> 1
        }
        p.edit().putString("last_drill", todayS).putInt("streak", newStreak).apply()
        return VoiceStreak(newStreak, true)
    }
}
