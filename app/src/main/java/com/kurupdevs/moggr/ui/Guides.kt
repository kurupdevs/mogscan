package com.kurupdevs.moggr.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kurupdevs.moggr.R
import com.kurupdevs.moggr.analysis.PslReport
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.MogCoral
// v2.6-hinglish begin
import com.kurupdevs.moggr.util.LanguageStore
// v2.6-hinglish end

// ---------- Glow-up cards: swipeable visual guides ----------

data class GlowUpCard(val imageRes: Int, val title: String)

val GLOWUP_CARDS: List<GlowUpCard> = listOf(
    GlowUpCard(R.drawable.glowup_cover, "Glow-Up Tips"),
    GlowUpCard(R.drawable.glowup_facewash, "Face Wash Technique"),
    GlowUpCard(R.drawable.glowup_breath, "Fresh Breath Routine"),
    GlowUpCard(R.drawable.glowup_posture, "Backpack Posture Reset"),
    GlowUpCard(R.drawable.glowup_outfit, "Outfit Fit Guide"),
    GlowUpCard(R.drawable.glowup_shoes, "Shoe Cleanup Guide")
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlowUpCarousel() {
    val pagerState = rememberPagerState(pageCount = { GLOWUP_CARDS.size })
    // v2.6-hinglish: translated card titles.
    val hi = LanguageStore.isHinglish
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            EqSectionLabel(Strings.s("caps_glowup", hi))
            Text(
                "${pagerState.currentPage + 1} / ${GLOWUP_CARDS.size}",
                fontSize = 12.sp,
                color = EqMuted
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val card = GLOWUP_CARDS[page]
            val title = Strings.s("glowup_$page", hi).let {
                if (it == "glowup_$page") card.title else it
            }
            EqGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            ) {
                Column {
                    Image(
                        painter = painterResource(card.imageRes),
                        contentDescription = title,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.72f)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EqInk
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(GLOWUP_CARDS.size) { i ->
                val active = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (active) 9.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (active) MogCoral else EqMuted.copy(alpha = 0.35f))
                )
            }
        }
        Text(
            Strings.s("swipe_next", hi),
            fontSize = 12.sp,
            color = EqMuted,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 6.dp)
        )
    }
}

// ---------- Winter Arc: tap-to-open slide deck ----------

val WINTER_ARC_CARDS: List<GlowUpCard> = listOf(
    GlowUpCard(R.drawable.winterarc_cover, "Best Winter Arc Ascension Guide"),
    GlowUpCard(R.drawable.winterarc_hunter, "Hunter Eyes"),
    GlowUpCard(R.drawable.winterarc_lymph, "Depuff: Lymphatic Drainage"),
    GlowUpCard(R.drawable.winterarc_mewing, "Jawline Through Mewing")
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WinterArcSection() {
    var open by remember { mutableStateOf(false) }
    // v2.6-hinglish: translated labels.
    val hi = LanguageStore.isHinglish
    Column(Modifier.fillMaxWidth()) {
        EqSectionLabel(Strings.s("caps_winter", hi))
        Spacer(Modifier.height(8.dp))
        EqGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(R.drawable.winterarc_cover),
                    contentDescription = "Winter Arc",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        Strings.s("winter_title", hi),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EqInk
                    )
                    Text(
                        Strings.s("winter_sub", hi),
                        fontSize = 12.sp,
                        color = EqMuted
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Open",
                    tint = MogCoral
                )
            }
        }
    }
    if (open) {
        Dialog(
            onDismissRequest = { open = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val pagerState = rememberPagerState(pageCount = { WINTER_ARC_CARDS.size })
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val card = WINTER_ARC_CARDS[page]
                    val slideTitle = Strings.s("winter_slide_$page", hi).let {
                        if (it == "winter_slide_$page") card.title else it
                    }
                    Image(
                        painter = painterResource(card.imageRes),
                        contentDescription = slideTitle,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.66f)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${pagerState.currentPage + 1} / ${WINTER_ARC_CARDS.size}",
                        fontSize = 13.sp,
                        color = Color.White
                    )
                    IconButton(onClick = { open = false }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(WINTER_ARC_CARDS.size) { i ->
                        val active = i == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (active) 9.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) Color.White
                                    else Color.White.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
            }
        }
    }
}

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
    ),
    Guide(
        "Eyebrow grooming basics",
        "Face",
        "Brush your brows straight up with a clean spoolie or an old toothbrush. Anything that " +
            "sticks way past your natural brow line, trim with tiny scissors — comb up, snip only " +
            "the tips. Tweeze just the obvious strays below the arch, one hair at a time, stepping " +
            "back between pulls. Your natural arch is the blueprint — don't carve a new shape. " +
            "Over-plucked brows take months to grow back, so take less than you think."
    ),
    Guide(
        "Smile & teeth basics",
        "Face",
        "Brush twice a day for two full minutes, floss before bed, and brush your tongue — most " +
            "bad breath lives there. See a dentist twice a year even if nothing hurts. Rinse with " +
            "water after coffee or chai so stains don't set. Skip chemical whitening strips and " +
            "DIY bleach hacks — they wreck enamel. Clean teeth beat white teeth."
    ),
    Guide(
        "30-day jawline program",
        "Jawline",
        "The plan: sugar-free gum 10-15 minutes a day, 4 days a week; the under-chin drill daily; " +
            "neck curls twice a week. TMJ SAFETY — read this first: if your jaw clicks, aches, or " +
            "locks, stop immediately and rest it for a few days. Never force your jaw open or grind " +
            "through pain. Jaw pain that doesn't settle in a week is a dentist visit, not a harder " +
            "workout. This builds muscle tone over months — it does not change bone, and anyone " +
            "claiming 30-day bone change is lying."
    ),
    Guide(
        "Chewing for tone",
        "Jawline",
        "Chew evenly on both sides — favoring one side builds one side more. Keep your tongue on " +
            "the roof of your mouth between meals and breathe through your nose. Tougher foods or " +
            "gum a few times a week keeps the masseters active. Honest framing: this builds muscle " +
            "tone and a sharper look over months. It does not reshape your jawbone. Posture and low " +
            "face bloat matter more than any chew count."
    )
)

@Composable
fun GuidesSection(report: PslReport? = null) {
    // v2.6-hinglish: softmaxx headers translated.
    val hi = LanguageStore.isHinglish
    Column(Modifier.fillMaxWidth()) {
        GlowUpCarousel()
        Spacer(Modifier.height(20.dp))
        WinterArcSection()
        Spacer(Modifier.height(20.dp))
        HairSection(report)
        Spacer(Modifier.height(20.dp))
        SkinQuizCard()
        Spacer(Modifier.height(20.dp))
        DebloatSection()
        Spacer(Modifier.height(20.dp))
        PostureSection()
        Spacer(Modifier.height(20.dp))
        SeasonQuizCard(report)
        Spacer(Modifier.height(20.dp))
        EqSectionLabel(Strings.s("caps_softmaxx", hi))
        Spacer(Modifier.height(8.dp))
        EqBody(
            Strings.s("softmaxx_sub", hi),
            size = 13,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        GUIDES.forEach { guide -> GuideCard(guide) }
    }
}

/** v2.6-hinglish: guide categories in Hinglish. */
private fun guideCategoryHi(cat: String, hi: Boolean): String {
    if (!hi) return cat
    return when (cat) {
        "Skin & eyes" -> "Skin aur eyes"
        "Hair" -> "Baal"
        else -> cat
    }
}

@Composable
private fun GuideCard(guide: Guide) {
    var expanded by remember { mutableStateOf(false) }
    // v2.6-hinglish: translated title + body.
    val hi = LanguageStore.isHinglish
    EqGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { expanded = !expanded }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        Strings.guideTitle(guide.title, hi),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EqInk
                    )
                    Text(guideCategoryHi(guide.category, hi), fontSize = 12.sp, color = EqMuted)
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MogCoral
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    Strings.guideBody(guide.body, guide.title, hi),
                    fontSize = 14.sp,
                    color = EqInk
                )
            }
        }
    }
}
