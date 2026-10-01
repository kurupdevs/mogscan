package com.kurupdevs.moggr.util

import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SleepNight(val date: String, val bedMs: Long, val wakeMs: Long)

/**
 * Sleep log + 90-minute cycle math. No health claims — just scheduling around
 * sleep cycles and caffeine timing.
 */
object SleepStore {
    private const val PREFS = "moggr_sleep"
    private const val CYCLE_MS = 90L * 60_000L
    private const val CAFFEINE_HALF_LIFE_H = 8L
    const val TARGET_HOURS = 8f

    private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")
    private val dayFmt = DateTimeFormatter.ofPattern("EEE")

    fun fmtTime(ms: Long): String =
        LocalTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault()).format(timeFmt)

    fun dayLabel(ms: Long): String =
        LocalDate.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault()).format(dayFmt)

    /** Default target wake 6:30 AM, persisted once the user sets it. */
    fun targetWakeMs(context: Context): Long {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val saved = p.getLong("target_wake_ms", 0L)
        if (saved != 0L) return saved
        return todayAt(context, 6, 30)
    }

    fun setTargetWake(context: Context, hour: Int, minute: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong("target_wake_ms", todayAt(context, hour, minute)).apply()
    }

    private fun todayAt(context: Context, hour: Int, minute: Int): Long {
        val zone = ZoneId.systemDefault()
        return LocalDate.now(zone).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
    }

    /** Target wake rolled forward to its next occurrence (tomorrow if passed). */
    fun nextWakeMs(context: Context): Long {
        val w = targetWakeMs(context)
        return if (w <= System.currentTimeMillis()) w + 24 * 3_600_000L else w
    }

    /** Suggested bedtimes for a full 5 or 6 sleep cycles before [wakeMs]. */
    fun suggestedBedtimes(wakeMs: Long): Pair<Long, Long> =
        Pair(wakeMs - 5 * CYCLE_MS, wakeMs - 6 * CYCLE_MS)

    /** "wake 6:30 AM → fall asleep 11:00 PM or 9:30 PM" */
    fun cycleLine(wakeMs: Long): String {
        val (five, six) = suggestedBedtimes(wakeMs)
        return "wake ${fmtTime(wakeMs)} → fall asleep ${fmtTime(five)} or ${fmtTime(six)}"
    }

    fun logNight(context: Context, bedMs: Long, wakeMs: Long) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val date = LocalDate.ofInstant(Instant.ofEpochMilli(wakeMs), ZoneId.systemDefault()).toString()
        p.edit()
            .putLong("bed_$date", bedMs)
            .putLong("wake_$date", wakeMs)
            .apply()
    }

    fun lastNight(context: Context): SleepNight? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.all.keys
            .filter { it.startsWith("wake_") }
            .map { it.removePrefix("wake_") }
            .sortedDescending()
            .firstOrNull()
            ?.let { date ->
                SleepNight(date, p.getLong("bed_$date", 0L), p.getLong("wake_$date", 0L))
            }
    }

    /** Up to the last 7 logged nights, newest first. */
    fun history7d(context: Context): List<SleepNight> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.all.keys
            .filter { it.startsWith("wake_") }
            .map { it.removePrefix("wake_") }
            .sortedDescending()
            .take(7)
            .map { date -> SleepNight(date, p.getLong("bed_$date", 0L), p.getLong("wake_$date", 0L)) }
            .filter { it.bedMs > 0 && it.wakeMs > it.bedMs }
    }

    fun hoursOf(night: SleepNight): Float =
        (night.wakeMs - night.bedMs) / 3_600_000f

    /** Total sleep debt vs the 8h target across logged nights (hours, >= 0). */
    fun sleepDebtHours(context: Context): Float =
        history7d(context).sumOf { (TARGET_HOURS - hoursOf(it)).coerceAtLeast(0f).toDouble() }.toFloat()

    fun logCaffeine(context: Context, ms: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong("last_caffeine_ms", ms).apply()
    }

    fun lastCaffeineMs(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("last_caffeine_ms", 0L)

    /** Caffeine curfew = bedtime minus ~8h (rough half-life buffer). */
    fun curfewMs(bedMs: Long): Long = bedMs - CAFFEINE_HALF_LIFE_H * 3_600_000L

    /** "caffeine curfew in 3h 20m" / "curfew active — skip the coffee" / null if unknown. */
    fun curfewLine(context: Context, bedMs: Long, nowMs: Long = System.currentTimeMillis()): String? {
        if (bedMs <= 0L) return null
        val left = curfewMs(bedMs) - nowMs
        return if (left <= 0) {
            "curfew active — skip the coffee"
        } else {
            val h = left / 3_600_000L
            val m = (left % 3_600_000L) / 60_000L
            "caffeine curfew in ${h}h ${m}m"
        }
    }
}
