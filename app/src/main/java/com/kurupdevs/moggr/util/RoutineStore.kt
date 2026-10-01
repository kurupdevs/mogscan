package com.kurupdevs.moggr.util

import android.content.Context
import java.time.LocalDate

data class RoutineTask(val id: String, val title: String, val detail: String)

/** Daily softmaxxing checklist. Generic habits only — no products, no medical claims. */
val ROUTINE_TASKS = listOf(
    RoutineTask("cleanse", "Morning cleanse", "Lukewarm water + gentle cleanser"),
    RoutineTask("sunscreen", "Sunscreen", "SPF 30+ on face and neck"),
    RoutineTask("moisturize", "Moisturize", "Morning and night, every day"),
    RoutineTask("posture", "Posture drills", "10 min — chin tucks + wall angels"),
    RoutineTask("tongue", "Tongue posture", "10 min — whole tongue resting on palate"),
    RoutineTask("water", "Hydrate", "2+ litres of water through the day"),
    RoutineTask("sleep", "Sleep 7–8 hrs", "No screens 30 min before bed"),
    RoutineTask("nojunk", "Clean eating", "No junk food or excess sugar today")
)

data class RoutineDayState(val done: Set<String>, val streak: Int)

object RoutineStore {
    private const val PREFS = "moggr_routine"

    private fun today(): LocalDate = LocalDate.now()
    private fun key(d: LocalDate) = d.toString()

    fun load(context: Context): RoutineDayState {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val t = today()
        val done = p.getStringSet("done_${key(t)}", emptySet())?.toSet() ?: emptySet()
        var streak = p.getInt("streak", 0)
        val lastFull = p.getString("last_full_day", null)?.let {
            try { LocalDate.parse(it) } catch (_: Exception) { null }
        }
        // Streak breaks if the last fully-completed day is older than yesterday.
        if (lastFull != null && lastFull.isBefore(t.minusDays(1))) {
            streak = 0
            p.edit().putInt("streak", 0).apply()
        }
        return RoutineDayState(done, streak)
    }

    fun toggle(context: Context, taskId: String): RoutineDayState {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val t = today()
        val doneKey = "done_${key(t)}"
        val done = (p.getStringSet(doneKey, emptySet()) ?: emptySet()).toMutableSet()
        if (taskId in done) done.remove(taskId) else done.add(taskId)
        p.edit().putStringSet(doneKey, done).apply()

        var streak = p.getInt("streak", 0)
        if (done.size >= ROUTINE_TASKS.size) {
            val lastFull = p.getString("last_full_day", null)
            if (lastFull != key(t)) {
                val yesterday = key(t.minusDays(1))
                streak = if (lastFull == yesterday) streak + 1 else 1
                p.edit().putInt("streak", streak).putString("last_full_day", key(t)).apply()
            }
        }
        return RoutineDayState(done.toSet(), streak)
    }
}
