package com.kurupdevs.moggr.util

import android.content.Context
import java.time.LocalDate

enum class TaskCategory(val key: String, val label: String) {
    SKIN("skin", "Skin"),
    HAIR("hair", "Hair"),
    POSTURE("posture", "Posture"),
    SLEEP("sleep", "Sleep"),
    DEBLOAT("debloat", "Debloat"),
    CHEW("chew", "Jaw"),
    STYLE("style", "Style");

    companion object {
        fun fromKey(key: String): TaskCategory =
            entries.firstOrNull { it.key == key } ?: SKIN
    }
}

data class RoutineTask(
    val id: String,
    val title: String,
    val detail: String,
    val category: TaskCategory = TaskCategory.SKIN,
    /** 0 = plain checkbox task; >0 shows a counter stepper. */
    val counterTarget: Int = 0,
    val counterUnit: String = ""
)

/** Daily softmaxxing checklist. Generic habits only — no products, no medical claims. */
val ROUTINE_TASKS = listOf(
    RoutineTask("cleanse", "Morning cleanse", "Lukewarm water + gentle cleanser", TaskCategory.SKIN),
    RoutineTask("sunscreen", "Sunscreen", "SPF 30+ on face and neck", TaskCategory.SKIN),
    RoutineTask("moisturize", "Moisturize", "Morning and night, every day", TaskCategory.SKIN),
    RoutineTask("posture", "Posture drills", "10 min — chin tucks + wall angels", TaskCategory.POSTURE),
    RoutineTask("tongue", "Tongue posture", "10 min — whole tongue resting on palate", TaskCategory.POSTURE),
    RoutineTask("water", "Hydrate", "2+ litres of water through the day", TaskCategory.DEBLOAT, 8, "glasses"),
    RoutineTask("sleep", "Sleep 7–8 hrs", "No screens 30 min before bed", TaskCategory.SLEEP),
    RoutineTask("nojunk", "Clean eating", "No junk food or excess sugar today", TaskCategory.DEBLOAT)
)

private val CORE_IDS: Set<String> = ROUTINE_TASKS.map { it.id }.toSet()

data class RoutineDayState(
    val done: Set<String>,
    val streak: Int,
    val counters: Map<String, Int> = emptyMap()
)

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
        return RoutineDayState(done, streak, loadCounters(p, t))
    }

    fun toggle(context: Context, taskId: String): RoutineDayState {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val t = today()
        val doneKey = "done_${key(t)}"
        val done = (p.getStringSet(doneKey, emptySet()) ?: emptySet()).toMutableSet()
        val added = taskId !in done
        if (added) done.add(taskId) else done.remove(taskId)
        val ed = p.edit().putStringSet(doneKey, done)
        if (added) {
            ed.putLong("tasks_completed_total", p.getLong("tasks_completed_total", 0L) + 1)
        }

        var streak = p.getInt("streak", 0)
        // A "full day" means every core habit done; plan/weak tasks don't count toward it.
        if (done.intersect(CORE_IDS).size >= CORE_IDS.size) {
            val lastFull = p.getString("last_full_day", null)
            if (lastFull != key(t)) {
                val yesterday = key(t.minusDays(1))
                streak = if (lastFull == yesterday) streak + 1 else 1
                ed.putInt("streak", streak).putString("last_full_day", key(t))
            }
        }
        ed.apply()
        return RoutineDayState(done.toSet(), streak, loadCounters(p, t))
    }

    /** Step a counter task up/down (clamped 0..target). Returns fresh state. */
    fun bumpCounter(context: Context, taskId: String, delta: Int): RoutineDayState {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val t = today()
        val task = ROUTINE_TASKS.firstOrNull { it.id == taskId }
        val target = task?.counterTarget ?: 99
        val key = "count_${key(t)}_$taskId"
        val next = (p.getInt(key, 0) + delta).coerceIn(0, target.coerceAtLeast(1))
        p.edit().putInt(key, next).apply()
        return load(context)
    }

    fun setCounter(context: Context, taskId: String, value: Int): RoutineDayState {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val t = today()
        val key = "count_${key(t)}_$taskId"
        p.edit().putInt(key, value.coerceAtLeast(0)).apply()
        return load(context)
    }

    /** All-time count of task check-offs, for the monthly recap. */
    fun tasksCompletedTotal(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong("tasks_completed_total", 0L)

    private fun loadCounters(
        p: android.content.SharedPreferences,
        t: LocalDate
    ): Map<String, Int> {
        val prefix = "count_${key(t)}_"
        return p.all
            .filter { (k, v) -> k.startsWith(prefix) && v is Int }
            .mapKeys { (k, _) -> k.removePrefix(prefix) }
            .mapValues { (_, v) -> v as Int }
    }
}
