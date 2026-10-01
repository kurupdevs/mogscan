package com.kurupdevs.moggr.util

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * v2.6 challenge programs — fully on-device.
 *
 * Two structured programs (7-day debloat, 30-day glow-up) with daily photo
 * check-ins stored in app-private files, streak tracking, freeze/revive
 * mechanics and a local-only buddy mode (no accounts, no servers).
 */
enum class ChallengeId(val key: String, val days: Int, val title: String, val tagline: String) {
    DEBLOAT7("debloat7", 7, "7-Day Debloat", "Drop the puff. Sharpen the jawline."),
    GLOWUP30("glowup30", 30, "30-Day Glow-Up", "Skin, sleep, hair, posture — the full arc.");

    companion object {
        fun fromKey(key: String?): ChallengeId? = entries.firstOrNull { it.key == key }
    }
}

data class ChallengeTask(val id: String, val title: String, val detail: String)

data class ChallengeState(
    val active: ChallengeId?,
    val start: LocalDate?,
    /** 1-based index of today within the challenge, or 0 when none active. */
    val dayIndex: Int,
    val streak: Int,
    /** True when a day was missed and the streak needs a revive. */
    val broken: Boolean,
    val freezesTotal: Int,
    val freezesAvailable: Int,
    val reviveAvailable: Boolean,
    val checkins: Map<LocalDate, File>,
    val frozen: Set<LocalDate>,
    val myCode: String,
    val buddyCode: String?,
    val buddyStreak: Int
)

object ChallengeStore {
    private const val PREFS = "moggr_challenges"
    private const val CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

    // ---------- task lists: safe softmaxx content only ----------

    private val WATER_TASK = ChallengeTask(
        "water", "8 glasses of water", "Spread through the day — consistency beats chugging"
    )

    private val DEBLOAT_BASE = listOf(
        ChallengeTask(
            "checkin_photo", "Morning check-in photo",
            "Same light, same angle every day — that's how you see change"
        ),
        WATER_TASK,
        ChallengeTask(
            "lowsodium", "Low-sodium meals",
            "Sauce on the side, skip the extra salt shaker"
        )
    )

    private val DEBLOAT_FOCUS = listOf(
        "No junk food today" to "One clean day beats one perfect week",
        "Potassium day" to "Banana or coconut water — balances the sodium",
        "20-min walk" to "Easy cardio helps flush water retention",
        "No sugar drinks" to "Soda and juice bloat more than you think",
        "Sleep by 11pm" to "Tired bodies hold water and crave salt",
        "Light dinner" to "Eat early, eat light — wake up sharper",
        "Final comparison" to "Line up day 1 vs today and look at the jaw"
    )

    private val GLOWUP_SKIN = listOf(
        "Morning cleanse" to "Lukewarm water + gentle cleanser",
        "Sunscreen SPF 30+" to "Face and neck, every morning",
        "Moisturize AM + PM" to "Seal it in while skin is damp",
        "Change pillowcase" to "Fresh fabric, fewer breakouts",
        "No touching your face" to "Hands off — all day"
    )
    private val GLOWUP_SLEEP = listOf(
        "No screens 30 min before bed" to "Your skin repairs while you sleep",
        "Fixed bedtime tonight" to "Same time as last night",
        "7–8 hours tonight" to "Non-negotiable recovery"
    )
    private val GLOWUP_HAIR = listOf(
        "5-min scalp massage" to "Fingertips, not nails — boosts blood flow",
        "Wash + condition day" to "Clean scalp, conditioned ends",
        "Haircut check" to "Trim flyaways or book the cut you've been delaying",
        "Hair off the forehead" to "Keep the hairline visible and clean"
    )
    private val GLOWUP_POSTURE = listOf(
        "Chin tucks — 10 min" to "Fixes the forward-head look",
        "Wall angels — 10 min" to "Opens the chest, straightens the stance",
        "Tongue on palate — 10 min" to "Whole tongue resting up, lips closed",
        "Tall posture all day" to "Shoulders back every time you remember"
    )
    private val GLOWUP_FOCUS_ORDER = listOf("skin", "sleep", "hair", "posture")

    fun tasksForDay(id: ChallengeId, day: Int): List<ChallengeTask> {
        val d = day.coerceAtLeast(1)
        return when (id) {
            ChallengeId.DEBLOAT7 -> {
                val focus = DEBLOAT_FOCUS[(d - 1) % DEBLOAT_FOCUS.size]
                DEBLOAT_BASE + ChallengeTask("focus_d$d", focus.first, focus.second)
            }
            ChallengeId.GLOWUP30 -> {
                val focus = GLOWUP_FOCUS_ORDER[(d - 1) % GLOWUP_FOCUS_ORDER.size]
                val pool = when (focus) {
                    "skin" -> GLOWUP_SKIN
                    "sleep" -> GLOWUP_SLEEP
                    "hair" -> GLOWUP_HAIR
                    else -> GLOWUP_POSTURE
                }
                val t1 = pool[(d - 1) % pool.size]
                val t2 = pool[d % pool.size]
                listOf(
                    WATER_TASK,
                    ChallengeTask("f1_d$d", t1.first, t1.second),
                    ChallengeTask("f2_d$d", t2.first, t2.second)
                )
            }
        }
    }

    // ---------- buddy code ----------

    fun myCode(context: Context): String {
        val p = prefs(context)
        var code = p.getString("buddy_code", null)
        if (code.isNullOrBlank()) {
            code = (1..6).map { CODE_CHARS.random() }.joinToString("")
            p.edit().putString("buddy_code", code).apply()
        }
        return code
    }

    fun regenerateCode(context: Context): String {
        val code = (1..6).map { CODE_CHARS.random() }.joinToString("")
        prefs(context).edit().putString("buddy_code", code).apply()
        return code
    }

    /** Pairs locally with the entered code. No accounts, no servers. */
    fun pairBuddy(context: Context, raw: String): Boolean {
        val code = raw.trim().uppercase().filter { it.isLetterOrDigit() }
        if (code.length != 6 || code == myCode(context)) return false
        prefs(context).edit().putString("buddy_pair_code", code).apply()
        return true
    }

    fun unpairBuddy(context: Context) {
        prefs(context).edit()
            .remove("buddy_pair_code")
            .remove("buddy_streak")
            .apply()
    }

    /** Buddy types their own streak number — honor system, no verification claims. */
    fun setBuddyStreak(context: Context, streak: Int) {
        prefs(context).edit().putInt("buddy_streak", streak.coerceAtLeast(0)).apply()
    }

    // ---------- challenge lifecycle ----------

    fun start(context: Context, id: ChallengeId) {
        val p = prefs(context)
        val ed = p.edit()
            .putString("active", id.key)
            .putString("start_${id.key}", LocalDate.now().toString())
            .putBoolean("revive_used_${id.key}", false)
            .putInt("freezes_used_${id.key}", 0)
            .remove("completed_${id.key}")
            .remove("frozen_${id.key}")
        // Fresh run: clear per-day task check-offs and old check-in photos.
        p.all.keys.filter { it.startsWith("tasks_${id.key}_") }.forEach { ed.remove(it) }
        ed.apply()
        challengeDir(context, id).listFiles()?.forEach { it.delete() }
    }

    fun quit(context: Context) {
        prefs(context).edit().remove("active").apply()
    }

    /** 1 freeze per 7 challenge days (debloat = 1, glow-up = 4). */
    fun freezesTotal(id: ChallengeId): Int = id.days / 7

    fun state(context: Context): ChallengeState {
        val p = prefs(context)
        val today = LocalDate.now()
        val myCode = myCode(context)
        val buddyCode = p.getString("buddy_pair_code", null)
        val buddyStreak = p.getInt("buddy_streak", 0)
        val active = ChallengeId.fromKey(p.getString("active", null))
        if (active == null) {
            return ChallengeState(
                active = null, start = null, dayIndex = 0, streak = 0, broken = false,
                freezesTotal = 0, freezesAvailable = 0, reviveAvailable = false,
                checkins = emptyMap(), frozen = emptySet(),
                myCode = myCode, buddyCode = buddyCode, buddyStreak = buddyStreak
            )
        }
        val start = p.getString("start_${active.key}", null)?.let {
            try { LocalDate.parse(it) } catch (_: Exception) { null }
        } ?: today
        val completed = completedDates(p, active)
        val frozen = frozenDates(p, active)
        val (streak, broken) = computeStreak(completed, frozen, today)
        val total = freezesTotal(active)
        val used = p.getInt("freezes_used_${active.key}", 0).coerceIn(0, total)
        val reviveUsed = p.getBoolean("revive_used_${active.key}", false)
        return ChallengeState(
            active = active,
            start = start,
            dayIndex = ChronoUnit.DAYS.between(start, today).toInt().coerceAtLeast(0) + 1,
            streak = streak,
            broken = broken,
            freezesTotal = total,
            freezesAvailable = (total - used).coerceAtLeast(0),
            reviveAvailable = broken && !reviveUsed,
            checkins = completed.mapNotNull { d ->
                val f = checkinFile(context, active, d)
                if (f.exists()) d to f else null
            }.toMap(),
            frozen = frozen,
            myCode = myCode,
            buddyCode = buddyCode,
            buddyStreak = buddyStreak
        )
    }

    /** Consecutive covered days ending today-or-yesterday. Frozen days bridge gaps. */
    private fun computeStreak(
        completed: Set<LocalDate>,
        frozen: Set<LocalDate>,
        today: LocalDate
    ): Pair<Int, Boolean> {
        fun covered(d: LocalDate) = d in completed || d in frozen
        var cursor = today
        if (!covered(cursor)) cursor = today.minusDays(1)
        var streak = 0
        while (covered(cursor)) {
            streak++
            cursor = cursor.minusDays(1)
        }
        val broken = completed.isNotEmpty() && !covered(today) && !covered(today.minusDays(1))
        return streak to broken
    }

    // ---------- daily tasks ----------

    fun doneTasks(context: Context, id: ChallengeId, date: LocalDate): Set<String> {
        return prefs(context)
            .getStringSet("tasks_${id.key}_${date}", emptySet())?.toSet() ?: emptySet()
    }

    fun toggleTask(context: Context, id: ChallengeId, date: LocalDate, taskId: String): Set<String> {
        val p = prefs(context)
        val key = "tasks_${id.key}_${date}"
        val done = (p.getStringSet(key, emptySet()) ?: emptySet()).toMutableSet()
        if (taskId in done) done.remove(taskId) else done.add(taskId)
        p.edit().putStringSet(key, done).apply()
        return done.toSet()
    }

    // ---------- photo check-ins (app-private files) ----------

    fun challengeDir(context: Context, id: ChallengeId): File =
        File(File(context.filesDir, "challenges"), id.key).apply { mkdirs() }

    private fun checkinFile(context: Context, id: ChallengeId, date: LocalDate): File =
        File(challengeDir(context, id), "checkin_$date.jpg")

    /**
     * Saves a downscaled copy of the check-in photo into app-private storage.
     * Returns the saved file.
     */
    fun saveCheckinPhoto(context: Context, id: ChallengeId, date: LocalDate, bmp: Bitmap): File {
        val maxDim = maxOf(bmp.width, bmp.height)
        val scaled = if (maxDim > 1024) {
            val scale = 1024f / maxDim
            Bitmap.createScaledBitmap(
                bmp,
                (bmp.width * scale).toInt(),
                (bmp.height * scale).toInt(),
                true
            )
        } else bmp
        val file = checkinFile(context, id, date)
        FileOutputStream(file).use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        if (scaled !== bmp) scaled.recycle()
        return file
    }

    /** Marks the date as checked in (photo saved separately via saveCheckinPhoto). */
    fun recordCheckin(context: Context, id: ChallengeId, date: LocalDate) {
        val p = prefs(context)
        val key = "completed_${id.key}"
        val done = (p.getStringSet(key, emptySet()) ?: emptySet()).toMutableSet()
        done.add(date.toString())
        p.edit().putStringSet(key, done).apply()
    }

    /**
     * Skip a day without breaking the streak. Covers yesterday if it was missed
     * (and today isn't done yet), otherwise covers today proactively.
     */
    fun useFreeze(context: Context): Boolean {
        val st = state(context)
        val active = st.active ?: return false
        if (st.freezesAvailable <= 0) return false
        val today = LocalDate.now()
        fun covered(d: LocalDate) = d in st.checkins.keys || d in st.frozen
        val target = when {
            !covered(today.minusDays(1)) && !covered(today) -> today.minusDays(1)
            !covered(today) -> today
            else -> return false
        }
        val p = prefs(context)
        val key = "frozen_${active.key}"
        val frozen = (p.getStringSet(key, emptySet()) ?: emptySet()).toMutableSet()
        frozen.add(target.toString())
        p.edit()
            .putStringSet(key, frozen)
            .putInt("freezes_used_${active.key}", p.getInt("freezes_used_${active.key}", 0) + 1)
            .apply()
        return true
    }

    /**
     * Restore a broken streak once per challenge. Bridges every missed day and
     * resets available freezes to zero (costs nothing otherwise).
     */
    fun revive(context: Context): Boolean {
        val st = state(context)
        val active = st.active ?: return false
        if (!st.reviveAvailable) return false
        val p = prefs(context)
        val today = LocalDate.now()
        val completed = completedDates(p, active)
        val frozen = (p.getStringSet("frozen_${active.key}", emptySet()) ?: emptySet()).toMutableSet()
        val doneStrs = completed.map { it.toString() }.toSet()
        var d = completed.minOrNull() ?: today
        while (!d.isAfter(today)) {
            val s = d.toString()
            if (s !in doneStrs && s !in frozen) frozen.add(s)
            d = d.plusDays(1)
        }
        p.edit()
            .putStringSet("frozen_${active.key}", frozen)
            .putBoolean("revive_used_${active.key}", true)
            .putInt("freezes_used_${active.key}", freezesTotal(active))
            .apply()
        return true
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun completedDates(p: SharedPreferences, id: ChallengeId): Set<LocalDate> =
        (p.getStringSet("completed_${id.key}", emptySet()) ?: emptySet()).mapNotNull {
            try { LocalDate.parse(it) } catch (_: Exception) { null }
        }.toSet()

    private fun frozenDates(p: SharedPreferences, id: ChallengeId): Set<LocalDate> =
        (p.getStringSet("frozen_${id.key}", emptySet()) ?: emptySet()).mapNotNull {
            try { LocalDate.parse(it) } catch (_: Exception) { null }
        }.toSet()
}
