package com.kurupdevs.moggr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.ui.theme.EqGreigeDeep
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqInkSoft
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.MogCoral
import com.kurupdevs.moggr.util.SleepStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

// ---------- Full sleep card (Routine tab) ----------

@Composable
fun SleepTrackerCard() {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }

    val zone = remember { ZoneId.systemDefault() }
    fun atToday(h: Int, m: Int): Long =
        LocalDate.now(zone).atTime(h, m).atZone(zone).toInstant().toEpochMilli()

    var wakeH by remember { mutableIntStateOf(6) }
    var wakeM by remember { mutableIntStateOf(30) }
    var bedH by remember { mutableIntStateOf(23) }
    var bedM by remember { mutableIntStateOf(0) }

    // seed the wake steppers from the saved target once
    val savedWake = remember(refresh) { SleepStore.targetWakeMs(context) }
    androidx.compose.runtime.LaunchedEffect(savedWake) {
        val t = java.time.LocalTime.ofInstant(Instant.ofEpochMilli(savedWake), zone)
        wakeH = t.hour
        wakeM = t.minute
    }

    val wakeMs = atToday(wakeH, wakeM)
    val bedMs = atToday(bedH, bedM)
    val nextWake = remember(wakeMs) {
        val w = wakeMs
        if (w <= System.currentTimeMillis()) w + 24 * 3_600_000L else w
    }
    val lastNight = remember(refresh) { SleepStore.lastNight(context) }
    val history = remember(refresh) { SleepStore.history7d(context).reversed() }
    val debt = remember(refresh) { SleepStore.sleepDebtHours(context) }
    val suggestedBed = remember(nextWake) { SleepStore.suggestedBedtimes(nextWake).first }
    val curfew = remember(suggestedBed) { SleepStore.curfewLine(context, suggestedBed) }

    EqGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column {
            EqSectionLabel("SLEEP")
            Spacer(Modifier.height(6.dp))
            EqBody("Wake up on a cycle, not mid-dream.")
            Spacer(Modifier.height(14.dp))

            Text("I wake up at", fontSize = 13.sp, color = EqMuted, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            TimeStepper(hour = wakeH, minute = wakeM, onChange = { h, m ->
                wakeH = h; wakeM = m
                SleepStore.setTargetWake(context, h, m)
            })
            Spacer(Modifier.height(10.dp))
            // NOTE: cycle line comes from SleepStore (util) and stays English.
            Text(
                SleepStore.cycleLine(nextWake),
                fontSize = 14.sp, color = EqInk, fontWeight = FontWeight.SemiBold
            )
            EqBody("5 or 6 full 90-min cycles — pick the earlier one when you can.", size = 12)

            Spacer(Modifier.height(16.dp))
            Text("Log last night", fontSize = 13.sp, color = EqMuted, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Bed", fontSize = 12.sp, color = EqMuted)
                    TimeStepper(hour = bedH, minute = bedM, onChange = { h, m -> bedH = h; bedM = m }, compact = true)
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Wake", fontSize = 12.sp, color = EqMuted)
                    TimeStepper(hour = wakeH, minute = wakeM, onChange = { h, m -> wakeH = h; wakeM = m }, compact = true)
                }
            }
            Spacer(Modifier.height(8.dp))
            EqDarkPillButton(
                text = "Log night",
                onClick = {
                    var b = bedMs
                    var w = wakeMs
                    if (w <= b) w += 24 * 3_600_000L // bedtime was yesterday evening
                    SleepStore.logNight(context, b, w)
                    refresh++
                },
                modifier = Modifier.fillMaxWidth()
            )
            lastNight?.let {
                Spacer(Modifier.height(8.dp))
                val hrs = SleepStore.hoursOf(it)
                Text(
                    "Last night: ${SleepStore.fmtTime(it.bedMs)} → ${SleepStore.fmtTime(it.wakeMs)} " +
                        "(${String.format(Locale.US, "%.1f", hrs)}h)",
                    fontSize = 13.sp, color = EqMuted
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Last coffee", fontSize = 14.sp, color = EqInk, fontWeight = FontWeight.SemiBold)
                Text(
                    "log it",
                    fontSize = 13.sp, color = MogCoral, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        SleepStore.logCaffeine(context, System.currentTimeMillis())
                        refresh++
                    }
                )
            }
            curfew?.let {
                Spacer(Modifier.height(4.dp))
                // NOTE: curfew line comes from SleepStore (util) and stays English.
                Text(it, fontSize = 13.sp, color = EqInkSoft)
                EqBody("Caffeine hangs around ~8h — late cups steal deep sleep.", size = 12)
            }

            Spacer(Modifier.height(16.dp))
            Text("7-day trend", fontSize = 13.sp, color = EqMuted, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            if (history.isEmpty()) {
                EqBody("Log a night to start your trend.", size = 13)
            } else {
                history.forEach { night ->
                    SleepBarRow(night)
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (debt <= 0.05f) "No sleep debt — clean week."
                    else "Sleep debt: ${String.format(Locale.US, "%.1f", debt)}h vs 8h target",
                    fontSize = 13.sp, color = EqInk, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SleepBarRow(night: com.kurupdevs.moggr.util.SleepNight) {
    val hrs = SleepStore.hoursOf(night)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            SleepStore.dayLabel(night.wakeMs),
            fontSize = 12.sp, color = EqMuted,
            modifier = Modifier.width(36.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(EqGreigeDeep)
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val fillW = size.width * (hrs / 10f).coerceIn(0f, 1f)
                drawRect(
                    color = if (hrs >= 8f) Color(0xFF12B76A) else MogCoral,
                    size = androidx.compose.ui.geometry.Size(fillW, size.height)
                )
                val tickX = size.width * 0.8f // 8h of a 10h scale
                drawRect(
                    color = EqInk,
                    topLeft = androidx.compose.ui.geometry.Offset(tickX - 1f, 0f),
                    size = androidx.compose.ui.geometry.Size(2f, size.height)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            String.format(Locale.US, "%.1fh", hrs),
            fontSize = 12.sp, color = EqInk, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(44.dp)
        )
    }
}

@Composable
private fun TimeStepper(
    hour: Int,
    minute: Int,
    onChange: (Int, Int) -> Unit,
    compact: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepperButton("−") { onChange((hour + 23) % 24, minute) }
        Text(
            String.format(Locale.US, "%02d:%02d", hour, minute),
            fontSize = if (compact) 15.sp else 17.sp,
            fontWeight = FontWeight.Bold,
            color = EqInk,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        StepperButton("+") { onChange((hour + 1) % 24, minute) }
        Spacer(Modifier.width(6.dp))
        StepperButton("−15") { onChange(hour, (minute + 45) % 60) }
        StepperButton("+15") { onChange(hour, (minute + 15) % 60) }
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(EqGreigeDeep)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EqInk)
    }
}

// ---------- Compact card (Home tab) ----------

@Composable
fun SleepMiniCard() {
    val context = LocalContext.current
    val wakeMs = remember { SleepStore.nextWakeMs(context) }
    val (five, six) = remember(wakeMs) { SleepStore.suggestedBedtimes(wakeMs) }
    EqGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                EqSectionLabel("TONIGHT")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Bed by ${SleepStore.fmtTime(five)} or ${SleepStore.fmtTime(six)}",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EqInk
                )
                EqBody(
                    "for a clean ${SleepStore.fmtTime(wakeMs)} wake-up",
                    size = 13
                )
            }
        }
    }
}
