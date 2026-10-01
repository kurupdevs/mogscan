package com.kurupdevs.moggr.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------- Softmaxx guides library ----------
// Every method Moggr recommends, rewritten in our own words.
// Only safe, non-medical methods live here: no drugs, no hormones,
// no procedures, no copied text from anywhere.

data class Guide(val title: String, val category: String, val body: String)

val GUIDES: List<Guide> = listOf(
    Guide(
        "Beard density routine",
        "Face",
        "Massage along the jawline and cheeks with your fingertips for 3-5 minutes every day " +
            "to boost blood flow to the follicles. Eat protein-rich food, sleep 7-9 hours, and keep " +
            "the skin under your facial hair clean so follicles stay unblocked. Expect first visible " +
            "gains in 4-6 weeks and real coverage in 3-6 months. No product does the work for you — " +
            "consistency does."
    ),
    Guide(
        "Fragrance basics",
        "Presence",
        "One or two sprays on pulse points — the sides of the neck and the wrists. Less is more: " +
            "people should only notice it up close. Fresh or citrus scents fit daytime, woody or " +
            "spicy scents fit evenings. Don't layer five different scents; pick one and wear it well."
    ),
    Guide(
        "Under-chin tightening drill",
        "Jawline",
        "Press your whole tongue flat against the roof of your mouth, then open your jaw slowly " +
            "and as wide as comfortable, and close it. Do 3 sets of 10, twice a day. Over weeks this " +
            "tightens the muscles under the chin for a cleaner throat-to-jaw angle. Stop immediately " +
            "if your jaw clicks, aches, or locks — never force it."
    ),
    Guide(
        "Sleep for your face",
        "Skin & eyes",
        "Get 7-9 hours on a consistent schedule — puffy eyes and dull skin are usually a sleep " +
            "problem first. Get sunlight within 30 minutes of waking to set your body clock, dim the " +
            "lights at night, keep the room dark and cool, and stop eating 3-4 hours before bed."
    ),
    Guide(
        "Neck training",
        "Frame",
        "A thicker neck frames the jaw and widens your silhouette. Neck curls: lie face-up, curl " +
            "your chin toward your chest, 3 sets of 15, twice a week. Add dumbbell shrugs and farmer " +
            "carries for the traps. Warm up with slow head circles first, keep the motion controlled, " +
            "never train through pain, and cap it at 2-3 sessions per week."
    ),
    Guide(
        "Tongue posture & chewing",
        "Structure",
        "Rest your whole tongue on the roof of your mouth, seal your lips, and breathe through " +
            "your nose — all day, every day. Chew evenly on both sides instead of favoring one; " +
            "tougher foods or gum for 10-15 minutes a day builds jaw muscle tone. Posture habits " +
            "compound over months, not days."
    ),
    Guide(
        "Fix forward head posture",
        "Posture",
        "When your head juts forward, your jawline disappears. Stand with your back against a " +
            "wall, tuck your chin straight back like making a double chin, hold 5 seconds, repeat " +
            "10-15 times daily. Keep screens at eye level instead of looking down, and sleep on " +
            "your back when you can."
    ),
    Guide(
        "Clean bulking",
        "Body",
        "To build size without bloating your face: eat roughly 200-300 kcal above maintenance, " +
            "get about 2g of protein per kg of bodyweight, and train big compound lifts with " +
            "progressive overload 2-4 times a week. A dirty bulk shows in the face first and takes " +
            "months to reverse — build the frame, not the fat."
    ),
    Guide(
        "Scalp care",
        "Hair",
        "Wash 2-3 times a week with a gentle shampoo — over-washing strips the scalp and causes " +
            "more shedding. Massage your scalp with your fingertips for 5 minutes daily to boost " +
            "blood flow. Keep tools clean, don't scratch or over-scrub, and track progress with " +
            "monthly photos in the same lighting."
    ),
    Guide(
        "Voice drills",
        "Presence",
        "Breathe from the belly, not the chest: keep your shoulders still and let your stomach " +
            "rise as you inhale. Stay hydrated through the day and keep an upright posture so the " +
            "throat stays open. Read aloud for 5 minutes daily at a steady, calm pace and record " +
            "yourself — you'll hear the difference within weeks."
    )
)

@Composable
fun GuidesSection() {
    Column(Modifier.fillMaxWidth()) {
        CapsLabel("SOFTMAXX GUIDES")
        Spacer(Modifier.height(8.dp))
        Text(
            "Every method Moggr recommends, in plain words. Tap a guide to read it.",
            fontSize = 13.sp,
            color = PslGrey,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        GUIDES.forEach { guide -> GuideCard(guide) }
    }
}

@Composable
private fun GuideCard(guide: Guide) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { expanded = !expanded }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        guide.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PslText
                    )
                    Text(guide.category, fontSize = 12.sp, color = PslGrey)
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = PslBlue
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(guide.body, fontSize = 14.sp, color = PslText)
            }
        }
    }
}
