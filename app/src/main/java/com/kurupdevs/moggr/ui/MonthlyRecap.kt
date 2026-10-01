package com.kurupdevs.moggr.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.analysis.PslReport
import com.kurupdevs.moggr.util.RoutineStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ScanSnapshot(val date: String, val psl: Double)

/**
 * Records one PSL snapshot per scan-day so the monthly recap can compare the
 * first and latest scan of the month. Snapshots are taken when this card sees
 * a report — no scan history is fabricated.
 */
object RecapStore {
    private const val PREFS = "moggr_recap"

    private fun monthKey(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    fun recordScan(context: Context, psl: Double) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit().putString("scan_${LocalDate.now()}", psl.toString()).apply()
    }

    fun monthSnapshots(context: Context): List<ScanSnapshot> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val prefix = "scan_${monthKey()}"
        return p.all.keys
            .filter { it.startsWith(prefix) }
            .mapNotNull { k ->
                val date = k.removePrefix("scan_")
                (p.all[k] as? String)?.toDoubleOrNull()?.let { ScanSnapshot(date, it) }
            }
            .sortedBy { it.date }
    }

    fun recordStreak(context: Context, streak: Int) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = "best_streak_${monthKey()}"
        if (streak > p.getInt(key, 0)) p.edit().putInt(key, streak).apply()
    }

    fun bestStreak(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt("best_streak_${monthKey()}", 0)
}

@Composable
fun MonthlyRecapCard(report: PslReport?) {
    val context = LocalContext.current
    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(report?.overallPsl) {
        if (report != null) {
            RecapStore.recordScan(context, report.overallPsl)
            RecapStore.recordStreak(context, RoutineStore.load(context).streak)
            tick++
        }
    }

    val snaps = remember(tick) { RecapStore.monthSnapshots(context) }
    val best = remember(tick) { RecapStore.bestStreak(context) }
    val tasksDone = remember(tick) { RoutineStore.tasksCompletedTotal(context) }
    val monthName = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM"))
    }

    EqGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column {
            EqSectionLabel("$monthName RECAP".uppercase(Locale.US))
            Spacer(Modifier.height(10.dp))
            if (snaps.isEmpty()) {
                EqHeadline("No scans logged yet.", size = 18)
                Spacer(Modifier.height(4.dp))
                EqBody(
                    "Your recap builds itself as you scan — same light, same angles, " +
                        "every few weeks, and the delta shows up here.",
                    size = 13
                )
            } else {
                val first = snaps.first()
                val latest = snaps.last()
                val delta = latest.psl - first.psl
                val deltaText = String.format(Locale.US, "%+.1f", delta)
                val deltaColor = when {
                    delta > 0.049 -> Color(0xFF12B76A)
                    delta < -0.049 -> Color(0xFFD92D20)
                    else -> PslGrey
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatCol("PSL change", deltaText, deltaColor)
                    StatCol("Best streak", if (best > 0) "${best}d" else "—", PslText)
                    StatCol("Tasks done", "$tasksDone", PslText)
                }
                Spacer(Modifier.height(10.dp))
                EqBody(
                    if (snaps.size == 1)
                        "One scan so far (${String.format(Locale.US, "%.1f", latest.psl)} PSL) — scan again in a few weeks to see movement."
                    else
                        "${String.format(Locale.US, "%.1f", first.psl)} → ${String.format(Locale.US, "%.1f", latest.psl)} PSL across ${snaps.size} scans. " +
                            "Same setup each time keeps it comparable.",
                    size = 13
                )
            }
        }
    }
}

@Composable
private fun StatCol(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontFamily = EqSerif,
            fontSize = 26.sp,
            color = color
        )
        Text(label, fontSize = 12.sp, color = PslGrey)
    }
}
