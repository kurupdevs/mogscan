package com.kurupdevs.moggr.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import java.time.LocalDate

data class DebloatLog(val puffiness: Int, val water: Int, val lowSodium: Boolean)

object DebloatStore {
    private const val PREFS = "moggr_debloat"

    fun log(context: Context, puffiness: Int, water: Int, lowSodium: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("debloat_${LocalDate.now()}", "$puffiness|$water|$lowSodium")
            .apply()
    }

    fun today(context: Context): DebloatLog? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("debloat_${LocalDate.now()}", null) ?: return null
        return parse(raw)
    }

    fun last7(context: Context): List<Pair<String, DebloatLog>> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return (0..6).mapNotNull { back ->
            val d = LocalDate.now().minusDays(back.toLong()).toString()
            p.getString("debloat_$d", null)?.let { d to parse(it) }
        }
    }

    private fun parse(raw: String): DebloatLog {
        val parts = raw.split("|")
        return DebloatLog(
            puffiness = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(1, 5) ?: 3,
            water = parts.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(0) ?: 0,
            lowSodium = parts.getOrNull(2)?.toBoolean() ?: false
        )
    }
}

// ---------- Full check-in card (Routine tab) ----------

@Composable
fun DebloatCheckinCard() {
    val context = LocalContext.current
    var saved by remember { mutableStateOf(DebloatStore.today(context)) }
    var puffiness by remember(saved) { mutableIntStateOf(saved?.puffiness ?: 3) }
    var water by remember(saved) { mutableIntStateOf(saved?.water ?: 0) }
    var lowSodium by remember(saved) { mutableStateOf(saved?.lowSodium ?: false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            CapsLabel("DEBLOAT CHECK-IN")
            Spacer(Modifier.height(6.dp))
            Text(
                "Morning face check — 30 seconds, keeps you honest.",
                fontSize = 14.sp, color = PslGrey
            )
            Spacer(Modifier.height(14.dp))

            Text("Puffiness this morning", fontSize = 14.sp, color = PslText, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            PuffinessDots(selected = puffiness, onSelect = { puffiness = it })
            Spacer(Modifier.height(4.dp))
            Text("1 = sharp, 5 = marshmallow", fontSize = 12.sp, color = PslGrey)

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Water so far", fontSize = 14.sp, color = PslText, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CounterBtn("−") { water = (water - 1).coerceAtLeast(0) }
                    Text(
                        "$water",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PslText,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    CounterBtn("+") { water++ }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Low sodium yesterday", fontSize = 14.sp, color = PslText, fontWeight = FontWeight.SemiBold)
                    Text("Salty dinners show up on your face", fontSize = 12.sp, color = PslGrey)
                }
                Switch(
                    checked = lowSodium,
                    onCheckedChange = { lowSodium = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF12B76A))
                )
            }

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    DebloatStore.log(context, puffiness, water, lowSodium)
                    saved = DebloatStore.today(context)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PslDeep),
                shape = RoundedCornerShape(50),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (saved == null) "Log check-in" else "Update check-in", fontWeight = FontWeight.Bold)
            }
            if (saved != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Logged for today — see you tomorrow morning.",
                    fontSize = 12.sp, color = Color(0xFF067647),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun PuffinessDots(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        (1..5).forEach { n ->
            val active = n <= selected
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (active) PslBlue else Color(0xFFEDE7DB))
                    .border(
                        1.dp,
                        if (active) PslBlue else Color(0xFFD6CFC2),
                        CircleShape
                    )
                    .clickable { onSelect(n) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$n",
                    color = if (active) Color.White else PslGrey,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun CounterBtn(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFFEDE7DB))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PslText)
    }
}

// ---------- Compact morning card (Home tab) ----------

@Composable
fun DebloatMorningMini() {
    val context = LocalContext.current
    var saved by remember { mutableStateOf(DebloatStore.today(context)) }
    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            CapsLabel("MORNING CHECK-IN")
            Spacer(Modifier.height(8.dp))
            if (saved != null) {
                Text(
                    "Puffiness ${saved!!.puffiness}/5 — logged. Stay the course.",
                    fontSize = 14.sp, color = PslText, fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    "How puffy this morning? Tap to log.",
                    fontSize = 14.sp, color = PslText, fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                PuffinessDots(selected = 0, onSelect = { n ->
                    DebloatStore.log(context, n, 0, false)
                    saved = DebloatStore.today(context)
                })
            }
        }
    }
}
