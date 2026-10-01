package com.kurupdevs.moggr.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class Helpline(val name: String, val number: String, val note: String)

/** India helplines. Opens the phone dialer (ACTION_DIAL — no call permission needed). */
private val HELPLINES: List<Helpline> = listOf(
    Helpline(
        "Vandrevala Foundation",
        "1860-2662-345",
        "24x7 · free · English + Hindi"
    ),
    Helpline(
        "iCall",
        "9152987821",
        "Mon–Sat 10am–8pm · free counseling"
    )
)

/**
 * Warm, non-preachy card for the Method tab. Never diagnoses — just points
 * at a human when the head feels heavy.
 */
@Composable
fun CrisisCard() {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFBEFE8)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            CapsLabel("IF YOUR HEAD FEELS HEAVY")
            Spacer(Modifier.height(8.dp))
            Text(
                "A score is just a photo,",
                fontFamily = MogSerif,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PslText,
                lineHeight = 28.sp
            )
            Text(
                "not your worth.",
                fontFamily = MogSerif,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                fontSize = 22.sp,
                color = PslText,
                lineHeight = 28.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "If a scan ever messes with your head, talk to someone — a friend, " +
                    "family, or one of these free helplines. We're not professionals; " +
                    "these people are.",
                fontSize = 14.sp,
                color = PslGrey,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(12.dp))
            HELPLINES.forEach { line ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_DIAL,
                                    Uri.parse("tel:${line.number.replace("-", "")}")
                                )
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            line.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PslText
                        )
                        Text(line.note, fontSize = 12.sp, color = PslGrey)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        line.number,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PslBlue
                    )
                }
            }
        }
    }
}

/**
 * Compact one-line link used in the Coach screen's offline/distress fallback.
 * Tapping toggles the full CrisisCard inline.
 */
@Composable
fun CrisisLinkButton(expanded: Boolean, onToggle: () -> Unit) {
    Text(
        text = if (expanded) "hide" else "feeling low? talk to someone ↓",
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = PslBlue,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp)
    )
}
