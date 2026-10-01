package com.kurupdevs.moggr.util

import android.content.Context
import com.kurupdevs.moggr.analysis.PslReport
import java.time.LocalDate
import java.time.ZoneOffset

/** One 30-day block of the ascension plan. */
data class PlanPhase(
    val index: Int,
    val title: String,
    val days: IntRange,
    val goal: String,
    val weeklyFocus: List<String>,
    val tasks: List<RoutineTask>
)

/** Built from the report's weakest features — the plan adapts to your face. */
data class AscensionPlan(
    val phases: List<PlanPhase>,
    /** Weakest feature names, weakest first. */
    val weakestFeatures: List<String>
)

object PlanStore {
    private const val PREFS = "moggr_plan"
    private const val TOTAL_DAYS = 90

    /** Current plan day 1..90. First call stamps the plan start date. */
    fun planDay(context: Context): Int {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var start = p.getLong("plan_start_ms", 0L)
        val todayStart = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        if (start == 0L) {
            start = todayStart
            p.edit().putLong("plan_start_ms", start).apply()
        }
        val day = ((todayStart - start) / 86_400_000L + 1).toInt()
        return day.coerceIn(1, TOTAL_DAYS)
    }

    fun resetPlan(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove("plan_start_ms").apply()
    }

    fun phaseForDay(day: Int): Int = when (day) {
        in 1..30 -> 0
        in 31..60 -> 1
        else -> 2
    }

    private fun core(id: String): RoutineTask = ROUTINE_TASKS.first { it.id == id }

    private val depuff = RoutineTask(
        "morning_depuff", "Morning de-puff",
        "Cold spoon or cold water 2 min on puffy areas",
        TaskCategory.DEBLOAT
    )
    private val sleepWindow = RoutineTask(
        "sleep_window", "Sleep window",
        "In bed by 11 — same time every night",
        TaskCategory.SLEEP
    )
    private val neckCurls = RoutineTask(
        "neck_curls", "Neck curls",
        "Bodyweight neck curls build the frame around your jaw",
        TaskCategory.POSTURE, 3, "sets"
    )
    private val chinTucks = RoutineTask(
        "chin_tucks", "Chin tucks",
        "10 reps, 5s holds — fixes forward head posture",
        TaskCategory.POSTURE
    )
    private val wallAngels = RoutineTask(
        "wall_angels", "Wall angels",
        "2x10 — opens the chest, stacks the spine",
        TaskCategory.POSTURE
    )
    private val hairCare = RoutineTask(
        "hair_care", "Hair check",
        "Scalp cleanse today — is your cut working for your face?",
        TaskCategory.HAIR
    )
    private val groom = RoutineTask(
        "groom", "Grooming day",
        "Brows tidy, nails short, clean lineup",
        TaskCategory.STYLE
    )
    private val fitCheck = RoutineTask(
        "fit_check", "Fit check",
        "One outfit that actually fits your frame",
        TaskCategory.STYLE
    )
    private val photoPractice = RoutineTask(
        "photo_practice", "Photo practice",
        "Front photo, same light as your scan, neutral face",
        TaskCategory.STYLE
    )
    private val chewGum = RoutineTask(
        "chew_gum", "Gum session",
        "10 min, chew evenly both sides. Stop if your jaw clicks or hurts.",
        TaskCategory.CHEW, 10, "min"
    )

    private fun phase1() = PlanPhase(
        index = 0,
        title = "Foundation",
        days = 1..30,
        goal = "Skin clears up, sleep locks in, habits go automatic.",
        weeklyFocus = listOf(
            "Week 1 — AM/PM cleanse every single day",
            "Week 2 — SPF 30+ every morning, no skips",
            "Week 3 — 2L water + cut the junk food",
            "Week 4 — in bed by 11, same time nightly"
        ),
        tasks = listOf(
            core("cleanse"), core("sunscreen"), core("moisturize"),
            core("water"), core("sleep"), core("nojunk"),
            depuff, sleepWindow
        )
    )

    private fun phase2() = PlanPhase(
        index = 1,
        title = "Frame",
        days = 31..60,
        goal = "Neck thicker, posture taller, hair dialed in.",
        weeklyFocus = listOf(
            "Week 5–6 — neck curls 3x a week",
            "Week 7 — chin tucks daily, fix the forward head",
            "Week 8 — haircut that fits your face shape"
        ),
        tasks = listOf(
            neckCurls, chinTucks, wallAngels, hairCare,
            core("tongue"), core("posture"), core("water"), core("sleep")
        )
    )

    private fun phase3() = PlanPhase(
        index = 2,
        title = "Finish",
        days = 61..90,
        goal = "Style locked, photo-ready, posture on autopilot.",
        weeklyFocus = listOf(
            "Week 9 — basics check: fits that actually fit",
            "Week 10 — grooming day: brows, nails, lineup",
            "Week 11–12 — photo practice under the same light"
        ),
        tasks = listOf(
            groom, fitCheck, photoPractice, chewGum,
            core("posture"), core("tongue"), core("sleep"), core("water")
        )
    )

    /** Look up any plan/core task by id (for cards that render shared tasks). */
    fun taskById(id: String): RoutineTask? =
        (listOf(phase1(), phase2(), phase3()).flatMap { it.tasks } + ROUTINE_TASKS)
            .associateBy { it.id }[id]

    fun buildPlan(report: PslReport?): AscensionPlan {        val weakest = (report?.features ?: emptyList())
            .sortedBy { it.score }
            .take(3)
            .map { it.name }
        return AscensionPlan(
            phases = listOf(phase1(), phase2(), phase3()),
            weakestFeatures = weakest
        )
    }

    /**
     * Smart daily list: current phase tasks + tasks targeting your 3 weakest
     * measured features + the core streak habits. Undone tasks sort first.
     */
    fun todayTasks(
        report: PslReport?,
        planDay: Int,
        doneIds: Set<String>
    ): List<RoutineTask> {
        val plan = buildPlan(report)
        val phase = plan.phases[phaseForDay(planDay).coerceIn(0, 2)]
        val out = LinkedHashMap<String, RoutineTask>()
        fun add(t: RoutineTask) { out.putIfAbsent(t.id, t) }
        phase.tasks.forEach(::add)
        weakFeatureTasks(plan.weakestFeatures).forEach(::add)
        ROUTINE_TASKS.forEach(::add)
        return out.values.sortedBy { it.id in doneIds }
    }

    /** One targeted task per weak feature, stable ids across renders. */
    fun weakFeatureTasks(weakest: List<String>): List<RoutineTask> {
        return weakest.mapNotNull { name ->
            val id = "weak_" + name.lowercase().replace(Regex("[^a-z]"), "")
            val n = name.lowercase()
            when {
                "jaw" in n || "chin" in n || "cheek" in n -> RoutineTask(
                    id, "Jaw focus: $name",
                    "10-min gum session, even both sides. Stop if your jaw clicks or hurts.",
                    TaskCategory.CHEW, 10, "min"
                )
                "eye" in n || "brow" in n -> RoutineTask(
                    id, "Eye-area reset: $name",
                    "Cold spoon 2 min + no late-night scrolling tonight",
                    TaskCategory.SKIN
                )
                "nose" in n || "lip" in n -> RoutineTask(
                    id, "Framing: $name",
                    "Photo-angle practice — hair and light do the heavy lifting",
                    TaskCategory.STYLE
                )
                "profile" in n -> RoutineTask(
                    id, "Profile work: $name",
                    "Chin tucks + stand tall — posture changes your side read",
                    TaskCategory.POSTURE
                )
                else -> RoutineTask(
                    id, "Harmony fix: $name",
                    "5 chin tucks now — forward head posture flattens your read",
                    TaskCategory.POSTURE
                )
            }
        }
    }
}
