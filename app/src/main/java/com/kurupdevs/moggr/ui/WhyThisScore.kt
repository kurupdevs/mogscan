package com.kurupdevs.moggr.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.kurupdevs.moggr.analysis.FeatureScore
import com.kurupdevs.moggr.analysis.PslReport
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.EqPeach
import com.kurupdevs.moggr.ui.theme.EqRose
import com.kurupdevs.moggr.ui.theme.EqSage
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

// v2.6-science begin: honest study citations per measurement. Only real,
// well-known citations; everything else is a community benchmark, not science.
private val CITATIONS: Map<String, String> = mapOf(
    "FWHR" to "Carré & McCormick (2008)",
    "Facial thirds" to "Farkas anthropometry",
    "Facial fifths" to "Farkas anthropometry",
    "Symmetry" to "Rhodes et al. (2006)",
    "Skin clarity" to "Fink et al. — skin evenness"
)

private fun citationFor(name: String): String =
    CITATIONS[name] ?: "community benchmark — not a clinical measure"
// v2.6-science end

/** Tap-a-feature dialog: score, yours-vs-ideal line, explainer, fix priority. */
@Composable
fun WhyThisScoreDialog(
    feature: FeatureScore,
    report: PslReport,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        // v2.7 reskin: frosted-glass dialog, serif score, pastel priority strip.
        EqGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) {
                EqSectionLabel(feature.name)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EqHeadline(
                        text = String.format(Locale.US, "%.1f", feature.score),
                        size = 40,
                        color = EqInk
                    )
                    Spacer(Modifier.width(10.dp))
                    EqBody("/ 8", size = 16)
                }
                Spacer(Modifier.height(10.dp))

                val detail = report.measureDetails[feature.name]
                if (!detail.isNullOrBlank()) {
                    Text("Yours vs ideal", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = EqInk)
                    Spacer(Modifier.height(4.dp))
                    EqBody(detail)
                    Spacer(Modifier.height(10.dp))
                }

                Text("What this means", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = EqInk)
                Spacer(Modifier.height(4.dp))
                EqBody(explainerFor(feature.name))
                // v2.6-science begin
                Spacer(Modifier.height(6.dp))
                Text(
                    "Source: ${citationFor(feature.name)}",
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    color = EqMuted
                )
                // v2.6-science end
                Spacer(Modifier.height(12.dp))

                val (priority, hint) = when {
                    feature.score < 4.0 -> "High" to "this is dragging your score down — work it first"
                    feature.score < 6.0 -> "Medium" to "decent, but there's real room to push it higher"
                    else -> "Maintain" to "this is carrying you — keep it"
                }
                // v2.7 reskin: pastel priority strip instead of raw status tints.
                val tint = when {
                    feature.score < 4.0 -> EqRose
                    feature.score < 6.0 -> EqPeach
                    else -> EqSage
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(tint)
                        .padding(12.dp)
                ) {
                    Text(
                        "Fix priority: $priority — $hint.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EqInk
                    )
                }
                Spacer(Modifier.height(14.dp))
                EqCoralPillButton(
                    text = "Got it",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
