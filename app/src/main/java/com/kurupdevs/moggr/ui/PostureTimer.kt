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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedButton
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

    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            CapsLabel("POSTURE TRACK")
            Spacer(Modifier.height(6.dp))
            Text(
                "Chin tucks fix forward head posture — the silent jawline killer.",
                fontSize = 14.sp, color = PslGrey
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(170.dp)) {
                        val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                        drawArc(
                            color = Color(0xFFEDE7DB),
                            startAngle = -90f, sweepAngle = 360f,
                            useCenter = false, style = stroke
                        )
                        drawArc(
                            color = PslBlue,
                            startAngle = -90f, sweepAngle = 360f * progress,
                            useCenter = false, style = stroke
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (finished) {
                            Text("done", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = PslText)
                            Text("10/10", fontSize = 14.sp, color = PslGrey)
                        } else {
                            Text(
                                "${holdLeft}s",
                                fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = PslText
                            )
                            Text(
                                "rep $rep / $TOTAL_REPS",
                                fontSize = 14.sp, color = PslGrey, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (finished) "Stacked. Your neck will thank you."
                else "Tuck your chin straight back, hold, release. Gentle — stop if anything hurts.",
                fontSize = 13.sp, color = PslGrey,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (finished) {
                    OutlinedButton(
                        onClick = { rep = 1; holdLeft = HOLD_SECS; finished = false; running = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50)
                    ) { Text("Run it back", color = PslBlue) }
                } else {
                    Button(
                        onClick = { running = !running },
                        colors = ButtonDefaults.buttonColors(containerColor = PslDeep),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50)
                    ) { Text(if (running) "Pause" else if (rep > 1 || holdLeft < HOLD_SECS) "Resume" else "Start", fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = { running = false; rep = 1; holdLeft = HOLD_SECS },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50)
                    ) { Text("Reset", color = PslBlue) }
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
                            uncheckedColor = PslGrey
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(task.title, color = PslText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(task.detail, color = PslGrey, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
