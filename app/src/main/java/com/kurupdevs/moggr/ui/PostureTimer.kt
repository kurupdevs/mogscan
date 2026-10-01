package com.kurupdevs.moggr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.ui.theme.EqGreigeDeep
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.MogCoral
import com.kurupdevs.moggr.util.PlanStore
import kotlinx.coroutines.delay

private const val HOLD_SECS = 5
private const val TOTAL_REPS = 10

/**
 * Guided chin-tuck timer (5s hold x 10) with a progress ring, plus the other
 * posture tasks as checkable rows. TMJ-safe: gentle holds only, stop on pain.
 */
@Composable
fun PostureTrackCard(
    done: Set<String>,
    onToggle: (String) -> Unit
) {
    var running by remember { mutableStateOf(false) }
    var rep by remember { mutableIntStateOf(1) }
    var holdLeft by remember { mutableIntStateOf(HOLD_SECS) }
    var finished by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running) {
            delay(1000L)
            if (holdLeft > 1) {
                holdLeft--
            } else if (rep < TOTAL_REPS) {
                rep++
                holdLeft = HOLD_SECS
            } else {
                running = false
                finished = true
            }
        }
    }
    LaunchedEffect(finished) {
        if (finished && "chin_tucks" !in done) onToggle("chin_tucks")
    }

    val progress = (HOLD_SECS - holdLeft) / HOLD_SECS.toFloat()

    EqGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column {
            EqSectionLabel("POSTURE TRACK")
            Spacer(Modifier.height(6.dp))
            EqBody("Chin tucks fix forward head posture — the silent jawline killer.")
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(170.dp)) {
                        val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                        drawArc(
                            color = EqGreigeDeep,
                            startAngle = -90f, sweepAngle = 360f,
                            useCenter = false, style = stroke
                        )
                        drawArc(
                            color = MogCoral,
                            startAngle = -90f, sweepAngle = 360f * progress,
                            useCenter = false, style = stroke
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (finished) {
                            Text("done", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = EqInk)
                            Text("10/10", fontSize = 14.sp, color = EqMuted)
                        } else {
                            Text(
                                "${holdLeft}s",
                                fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = EqInk
                            )
                            Text(
                                "rep $rep / $TOTAL_REPS",
                                fontSize = 14.sp, color = EqMuted, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            EqBody(
                if (finished) "Stacked. Your neck will thank you."
                else "Tuck your chin straight back, hold, release. Gentle — stop if anything hurts.",
                size = 13,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (finished) {
                    EqPillButton(
                        text = "Run it back",
                        onClick = { rep = 1; holdLeft = HOLD_SECS; finished = false; running = true },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    EqDarkPillButton(
                        text = if (running) "Pause" else if (rep > 1 || holdLeft < HOLD_SECS) "Resume" else "Start",
                        onClick = { running = !running },
                        modifier = Modifier.weight(1f)
                    )
                    EqPillButton(
                        text = "Reset",
                        onClick = { running = false; rep = 1; holdLeft = HOLD_SECS },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            listOf("neck_curls", "wall_angels").forEach { id ->
                val task = PlanStore.taskById(id) ?: return@forEach
                val checked = id in done
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { onToggle(id) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF12B76A),
                            uncheckedColor = EqMuted
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        // NOTE: task titles/details come from PlanStore (util) and stay English.
                        Text(task.title, color = EqInk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(task.detail, color = EqMuted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
