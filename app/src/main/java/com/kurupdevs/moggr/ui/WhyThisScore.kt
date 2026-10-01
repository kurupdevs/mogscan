package com.kurupdevs.moggr.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kurupdevs.moggr.analysis.FeatureScore
import com.kurupdevs.moggr.analysis.PslReport
import java.util.Locale

/** Plain-words explainer for every feature the measure chunk can score. */
private val EXPLAINERS: Map<String, String> = mapOf(
    "FWHR" to "Facial width-to-height ratio — how wide your face is versus how tall. Wider midfaces read more masculine and dominant in community ratings.",
    "Brows" to "Eyebrow shape, thickness and placement. Strong, straight, low-set brows frame the eyes and add dimorphism to the upper third.",
    "Brow ridge" to "Eyebrow shape, thickness and placement. Strong, straight, low-set brows frame the eyes and add dimorphism to the upper third.",
    "Cheekbones" to "How high and defined your cheekbones sit. Visible zygos give the midface structure and catch light — a big harmony driver.",
    "Chin" to "Chin projection and width — how far forward the chin sits and how it balances against the jaw. A defined chin anchors the whole lower third.",
    "Chin projection" to "Chin projection and width — how far forward the chin sits and how it balances against the jaw. A defined chin anchors the whole lower third.",
    "Eye spacing" to "Distance between your eyes relative to face width. The ideal sits in a narrow band — too close or too wide both cost points.",
    "Eyes" to "The overall eye area — shape, tilt and openness. Almond eyes with a slight upward tilt score highest in community ratings.",
    "Eye area" to "The overall eye area — shape, tilt and openness. Almond eyes with a slight upward tilt score highest in community ratings.",
    "Canthal tilt" to "The angle of the eye corners — a slight upward (positive) tilt reads alert and attractive, a downward tilt reads tired.",
    "Facial fifths" to "Your face width split into five equal vertical slices. Ideally the eyes sit one eye-width apart with balanced fifths on each side.",
    "Facial thirds" to "Your face height split into forehead, midface and lower third. Balanced thirds read as harmonious — no one section dominates.",
    "Forehead" to "Forehead height and slope relative to the rest of the face. It should sit in proportion to the midface and lower third.",
    "Jaw angle" to "The angle of the jaw corner. Squarer, more defined angles read masculine and sharp; soft angles read younger.",
    "Jawline" to "Jaw width and definition — a wide, clean jawline frames the lower face and is one of the heaviest-weighted traits in ratings.",
    "Jaw width" to "Jaw width and definition — a wide, clean jawline frames the lower face and is one of the heaviest-weighted traits in ratings.",
    "Lips" to "Lip fullness and proportion to the face. Balanced, hydrated lips score best — extremes in either direction cost points.",
    "Lower third" to "Everything from the nose base down — chin, jaw and lips. It carries a huge share of the overall read.",
    "Midface" to "Length of the midface (eyes to mouth) versus the rest of the face. Shorter midfaces are generally preferred in community ratings.",
    "Midface ratio" to "Length of the midface (eyes to mouth) versus the rest of the face. Shorter midfaces are generally preferred in community ratings.",
    "Nose" to "Nose width and projection versus the rest of the face. A nose balanced with mouth width scores best.",
    "Side profile" to "How your face reads from the side — forehead slope, nose projection, chin position. Front photos can only estimate this.",
    "Skin clarity" to "How clear and even your skin reads — blemishes, redness and texture. One of the fastest wins because it responds to routine.",
    "Symmetry" to "How closely the left and right halves of your face mirror each other. Near-symmetry reads as healthy and attractive."
)

private fun explainerFor(name: String): String =
    EXPLAINERS[name]
        ?: "How this feature measures against the community ideal. Higher means closer to the ratios experienced raters reward."

/** Tap-a-feature dialog: score, yours-vs-ideal line, explainer, fix priority. */
@Composable
fun WhyThisScoreDialog(
    feature: FeatureScore,
    report: PslReport,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PslCard),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(22.dp)) {
                Text(
                    feature.name.uppercase(),
                    color = PslBlue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        String.format(Locale.US, "%.1f", feature.score),
                        fontFamily = MogSerif,
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = PslText
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("/ 8", fontSize = 16.sp, color = PslGrey)
                }
                Spacer(Modifier.height(10.dp))

                val detail = report.measureDetails[feature.name]
                if (!detail.isNullOrBlank()) {
                    Text("Yours vs ideal", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PslText)
                    Spacer(Modifier.height(4.dp))
                    Text(detail, fontSize = 14.sp, color = PslGrey)
                    Spacer(Modifier.height(10.dp))
                }

                Text("What this means", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PslText)
                Spacer(Modifier.height(4.dp))
                Text(explainerFor(feature.name), fontSize = 14.sp, color = PslGrey)
                Spacer(Modifier.height(12.dp))

                val (priority, hint) = when {
                    feature.score < 4.0 -> "High" to "this is dragging your score down — work it first"
                    feature.score < 6.0 -> "Medium" to "decent, but there's real room to push it higher"
                    else -> "Maintain" to "this is carrying you — keep it"
                }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            feature.score < 4.0 -> Color(0xFFFEF3F2)
                            feature.score < 6.0 -> Color(0xFFFEF6E7)
                            else -> Color(0xFFECFDF3)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Fix priority: $priority — $hint.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PslText,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
