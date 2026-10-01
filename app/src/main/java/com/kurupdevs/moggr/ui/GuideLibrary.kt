package com.kurupdevs.moggr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.analysis.PslReport

// ---------- Small shared building blocks (guides library only) ----------

@Composable
private fun SectionCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PslCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PslText)
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 13.sp, color = PslGrey)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text("•  ", fontSize = 14.sp, color = PslBlue, fontWeight = FontWeight.Bold)
        Text(text, fontSize = 14.sp, color = PslText, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun QuizOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) PslBlue.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.6f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) PslBlue else PslGrey.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            fontSize = 14.sp,
            color = PslText,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Spacer(Modifier.size(6.dp))
            Text("✓", fontSize = 14.sp, color = PslBlue, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ChecklistRow(text: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = PslBlue)
        )
        Text(
            text,
            fontSize = 14.sp,
            color = if (checked) PslGrey else PslText,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun ExpandableDayCard(day: String, title: String, items: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    val checked = remember { mutableStateListOf<Boolean>().apply { repeat(items.size) { add(false) } } }
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
                    Text(day, fontSize = 12.sp, color = PslBlue, fontWeight = FontWeight.Bold)
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PslText)
                }
                Text(
                    "${checked.count { it }} / ${items.size}",
                    fontSize = 12.sp,
                    color = PslGrey,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = PslBlue
                )
            }
            if (expanded) {
                Spacer(Modifier.height(6.dp))
                items.forEachIndexed { i, item ->
                    ChecklistRow(item, checked[i]) { checked[i] = !checked[i] }
                }
            }
        }
    }
}

// ---------- Skin quiz → 3-step routine (#7) ----------

private data class SkinQuestion(
    val title: String,
    val options: List<Pair<String, String>> // label → value
)

private val SKIN_QUESTIONS = listOf(
    SkinQuestion(
        "By midday, your skin feels…",
        listOf("Shiny / greasy" to "oily", "Tight" to "dry", "Shiny only on the T-zone" to "combo", "Comfortable" to "normal")
    ),
    SkinQuestion(
        "Does your skin react easily?",
        listOf("Yes — goes red / itchy often" to "sensitive", "Sometimes" to "sometimes", "Nope, pretty chill" to "no")
    ),
    SkinQuestion(
        "How often do you break out?",
        listOf("Often" to "often", "Sometimes" to "sometimes", "Rarely" to "rarely")
    ),
    SkinQuestion(
        "Sun time on a normal day?",
        listOf("Mostly indoors" to "low", "1–2 hrs outside" to "medium", "Long hours outside" to "high")
    ),
    SkinQuestion(
        "Budget for the full routine?",
        listOf("Under ₹300" to "low", "₹300–800" to "mid", "₹800+" to "high")
    )
)

@Composable
fun SkinQuizCard() {
    val answers = remember { mutableStateListOf<String?>().apply { repeat(SKIN_QUESTIONS.size) { add(null) } } }
    val done = answers.all { it != null }

    Column(Modifier.fillMaxWidth()) {
        CapsLabel("SKIN QUIZ")
        Spacer(Modifier.height(8.dp))
        if (!done) {
            SectionCard(
                title = "Find your 3-step routine",
                subtitle = "Tap one option per question — takes 20 seconds."
            ) {
                SKIN_QUESTIONS.forEachIndexed { qi, q ->
                    Text(q.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PslText)
                    Spacer(Modifier.height(4.dp))
                    q.options.forEach { (label, value) ->
                        QuizOption(label, answers[qi] == value) { answers[qi] = value }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        } else {
            SkinRoutineResult(
                skinType = answers[0]!!,
                sensitivity = answers[1]!!,
                acne = answers[2]!!,
                sun = answers[3]!!,
                budget = answers[4]!!,
                onRetake = { repeat(answers.size) { answers[it] = null } }
            )
        }
    }
}

@Composable
private fun SkinRoutineResult(
    skinType: String,
    sensitivity: String,
    acne: String,
    sun: String,
    budget: String,
    onRetake: () -> Unit
) {
    val cleanser = when (skinType) {
        "oily" -> "Gel or light foaming cleanser — skip the heavy cream washes."
        "dry" -> "Gentle cream or milky cleanser — avoid foaming 'deep clean' ones."
        "combo" -> "Gentle gel cleanser — light for the T-zone, kind to the cheeks."
        else -> "Any gentle cleanser — keep it simple."
    }
    val moisturizer = when (skinType) {
        "oily" -> "Lightweight gel moisturizer — skip the heavy creams."
        "dry" -> "Richer cream moisturizer — your skin will drink it up."
        "combo" -> "Light lotion or gel-cream texture."
        else -> "A basic light moisturizer."
    }
    val spf = when {
        sun == "high" -> "SPF 30+ every morning, reapplied every 3 hrs outdoors — carry it with you."
        sensitivity == "sensitive" -> "SPF 30+ every morning — go fragrance-free if your skin reacts."
        else -> "SPF 30+ every morning — the single highest-ROI skincare step."
    }
    val extras = buildList {
        if (sensitivity == "sensitive") add("Fragrance-free everything — your skin votes no on fragrance.")
        if (acne == "often") add("Keep it boring: cleanser, moisturizer, SPF. More products do not mean faster clear skin — persistent acne is a dermatologist visit, not more serums.")
        if (budget == "low") add("Budget route: one gentle cleanser + one moisturizer + one sunscreen is the whole routine. Skip serums until the basics are daily.")
    }

    SectionCard(
        title = "YOUR 3-STEP",
        subtitle = "Morning + night. Same three steps, every day."
    ) {
        RoutineStep("1", "Cleanser", cleanser)
        RoutineStep("2", "Moisturizer", moisturizer)
        RoutineStep("3", "SPF 30+", spf)
        if (extras.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            extras.forEach { Bullet(it) }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Retake quiz",
            fontSize = 13.sp,
            color = PslBlue,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onRetake() }
        )
    }
    Spacer(Modifier.height(10.dp))
    SectionCard(
        title = "Desi no-white-cast sunscreen guide",
        subtitle = "Why sunscreen looks ashy on brown skin — and how to dodge it."
    ) {
        Bullet("Chemical vs mineral: chemical filters absorb UV and usually blend invisibly; mineral filters (zinc, titanium) sit on top and reflect light — that white layer is the cast.")
        Bullet("On brown skin, chemical or hybrid gel sunscreens disappear. Mineral-only ones almost always leave a grey/white cast.")
        Bullet("Reapply every 3 hrs when you're outdoors — one morning layer doesn't survive the day.")
        Bullet("Indian-market tips: look for gel sunscreens labelled 'no white cast'; skip thick white creams unless the label says otherwise; a 50g tube covers about a month of daily face use.")
    }
}

@Composable
private fun RoutineStep(number: String, name: String, guidance: String) {
    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(end = 10.dp, top = 2.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(PslBlue),
            contentAlignment = Alignment.Center
        ) {
            Text(number, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
        }
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PslText)
            Text(guidance, fontSize = 13.sp, color = PslGrey)
        }
    }
}

// ---------- Debloat protocol (#8) ----------

private val DEBLOAT_DAYS = listOf(
    "Day 1" to ("Water reset" to listOf(
        "Drink 500ml water right after waking up",
        "No pickles / achaar today",
        "Finish dinner 3 hrs before bed"
    )),
    "Day 2" to ("Sodium audit" to listOf(
        "Skip papad and instant noodles today",
        "Read one namkeen packet's sodium label — you'll be shocked",
        "Drink water with every meal"
    )),
    "Day 3" to ("Potassium day" to listOf(
        "Eat a banana and drink coconut water",
        "Add palak to one meal",
        "No restaurant food today"
    )),
    "Day 4" to ("Sleep push" to listOf(
        "Get 8 hrs of sleep tonight",
        "No chai / coffee after 4pm",
        "Last meal 3 hrs before bed"
    )),
    "Day 5" to ("Lymph day" to listOf(
        "5-min morning lymphatic massage: gentle strokes from jaw → down the neck → to the collarbones",
        "Cold water face rinse after",
        "Keep sodium low today"
    )),
    "Day 6" to ("Restaurant rules" to listOf(
        "If eating out: skip the heavy gravies, ask for less salt",
        "Dahi + cucumber with dinner",
        "Walk 15 mins after dinner"
    )),
    "Day 7" to ("Photo check-in" to listOf(
        "Take a photo in the same lighting and angle as Day 1",
        "Compare — keep what worked as daily habits",
        "This is education, not a crash plan — debloat is a habit, not a week"
    ))
)

@Composable
fun DebloatSection() {
    Column(Modifier.fillMaxWidth()) {
        CapsLabel("DEBLOAT PROTOCOL")
        Spacer(Modifier.height(8.dp))
        Text(
            "A 7-day morning plan to drop face puffiness. Tap a day to open its checklist.",
            fontSize = 13.sp,
            color = PslGrey,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        DEBLOAT_DAYS.forEach { (day, titleToItems) ->
            val (title, items) = titleToItems
            ExpandableDayCard(day, title, items)
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(
            title = "Desi Sodium Guide",
            subtitle = "Salt hides in desi staples — here's where."
        ) {
            Text("HIGH SODIUM — go easy", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PslBlue)
            Bullet("Pickles / achaar — one spoonful can carry half a day's salt")
            Bullet("Papad — roasted or fried, both are salt bombs")
            Bullet("Instant noodles — the masala sachet is the culprit")
            Bullet("Chips & namkeen — portion it out, never eat from the bag")
            Bullet("Restaurant gravies — loaded with butter and salt")
            Spacer(Modifier.height(8.dp))
            Text("DEBLOAT FRIENDLY — eat more", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PslBlue)
            Bullet("Banana — potassium helps flush excess sodium")
            Bullet("Coconut water — hydration + potassium")
            Bullet("Palak — potassium-rich, low sodium")
            Bullet("Dahi — light, keeps digestion calm")
            Bullet("Cucumber — water-rich and cooling")
        }
    }
}

// ---------- Haircut + styling (#11) ----------

private val HAIR_BY_SHAPE = listOf(
    "Oval" to "Most cuts work — your proportions are already balanced. Textured crop or a clean side part, don't fight what you have.",
    "Round" to "Height on top, faded short sides. Avoid bowl cuts and anything that adds width at the cheeks.",
    "Square" to "Soften the angles with texture on top and a messy fringe. Very short buzz cuts make the jaw look boxier.",
    "Oblong" to "Avoid extra height on top. A side part with fuller sides keeps the face from looking longer.",
    "Heart" to "Volume around the jaw balances a wider forehead. Textured fringe, medium length.",
    "Diamond" to "Fringe plus width at the forehead and jawline. Avoid cuts that collapse flat on the sides."
)

@Composable
fun HairSection(report: PslReport?) {
    val shape = report?.faceShape.orEmpty().trim()
    val matched = HAIR_BY_SHAPE.firstOrNull { it.first.equals(shape, ignoreCase = true) }

    Column(Modifier.fillMaxWidth()) {
        CapsLabel("HAIR & STYLING")
        Spacer(Modifier.height(8.dp))
        if (matched != null) {
            SectionCard(
                title = "Your cut: ${matched.first} face",
                subtitle = "Matched from your scan."
            ) {
                Text(matched.second, fontSize = 14.sp, color = PslText)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Other face shapes",
                fontSize = 13.sp,
                color = PslGrey,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        } else {
            Text(
                "Take a scan to get your shape-matched cut. General guide below:",
                fontSize = 13.sp,
                color = PslGrey,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        HAIR_BY_SHAPE.filter { it != matched }.forEach { (name, advice) ->
            var expanded by remember { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = PslCard),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { expanded = !expanded }
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PslText,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = PslBlue
                        )
                    }
                    if (expanded) {
                        Spacer(Modifier.height(6.dp))
                        Text(advice, fontSize = 14.sp, color = PslText)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(
            title = "What to tell your barber",
            subtitle = "Copy-paste script for the chair."
        ) {
            Text(
                "\"3 on the sides, scissors on top, keep some length to style, taper the neckline, clean up the edges.\"",
                fontSize = 14.sp,
                color = PslText,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            Bullet("Bring a photo of the cut you want — words mean different things to different barbers.")
            Bullet("If it feels off halfway through, speak up then — not after.")
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(
            title = "Curly hair playbook",
            subtitle = "Curls need a different routine, not more products."
        ) {
            Bullet("Wash 2x a week with sulfate-free shampoo — daily washing dries curls out.")
            Bullet("Condition every wash, then leave-in conditioner on damp hair.")
            Bullet("Never brush dry curls — detangle wet with fingers or a wide-tooth comb.")
            Bullet("Sleep on a satin pillowcase to cut overnight frizz.")
            Bullet("Less heat, more patience — curls reward consistency.")
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(
            title = "5-minute styling routine",
            subtitle = "The daily minimum that actually works."
        ) {
            RoutineStep("1", "Damp hair", "Start with towel-dried, slightly damp hair — never soaking, never dry.")
            RoutineStep("2", "Pea-size product", "Matte clay for texture, cream for a neat look. Less than you think.")
            RoutineStep("3", "Blow direction", "Blow-dry in the direction you want it to sit — heat sets the shape.")
            RoutineStep("4", "Finish", "Style with fingers, not a comb, for a natural finish. Done.")
        }
    }
}

// ---------- Posture 30-day challenge (#9 content) ----------

private val POSTURE_WEEKS = listOf(
    "Week 1 — Foundation" to listOf(
        "Chin tucks: 10 reps × 5-second hold, every day",
        "Raise your screen to eye level — stop craning down at your phone",
        "Catch yourself jutting your head forward, pull it back"
    ),
    "Week 2 — Open up" to listOf(
        "Keep the chin tucks going daily",
        "Wall angels: 3 × 10 — back flat against the wall",
        "Un-hunch your shoulders once every hour"
    ),
    "Week 3 — Chest reset" to listOf(
        "Doorway chest stretch: 2 × 30s per side",
        "Keep screens at eye level — the habit from Week 1",
        "Sleep on your back when you can"
    ),
    "Week 4 — Lock it in" to listOf(
        "Full combo daily: chin tucks + wall angels + doorway stretch",
        "Posture is maintenance now — the hard part was starting"
    )
)

@Composable
fun PostureSection() {
    Column(Modifier.fillMaxWidth()) {
        CapsLabel("POSTURE CHALLENGE")
        Spacer(Modifier.height(8.dp))
        Text(
            "30 days to fix forward head posture — the silent jawline killer. Tick weeks as you finish them.",
            fontSize = 13.sp,
            color = PslGrey,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        POSTURE_WEEKS.forEach { (week, items) ->
            var done by remember { mutableStateOf(false) }
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
                        Checkbox(
                            checked = done,
                            onCheckedChange = { done = it },
                            colors = CheckboxDefaults.colors(checkedColor = PslBlue)
                        )
                        Text(
                            week,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (done) PslGrey else PslText,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 6.dp)
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = PslBlue
                        )
                    }
                    if (expanded) {
                        Spacer(Modifier.height(6.dp))
                        items.forEach { Bullet(it) }
                    }
                }
            }
        }
        Text(
            "Full habit tracking lives in the Routine tab — this is the challenge plan.",
            fontSize = 12.sp,
            color = PslGrey,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

// ---------- Season color quiz (#15) ----------

private data class SeasonQuestion(
    val title: String,
    val warmOption: String,
    val coolOption: String
)

private val SEASON_QUESTIONS = listOf(
    SeasonQuestion(
        "Check your wrist veins in daylight",
        "Look green",
        "Look blue / purple"
    ),
    SeasonQuestion(
        "Which jewelry looks better on you?",
        "Gold",
        "Silver"
    ),
    SeasonQuestion(
        "What happens in the sun?",
        "Tan easily",
        "Burn first"
    )
)

@Composable
fun SeasonQuizCard(report: PslReport?) {
    val answers = remember { mutableStateListOf<Boolean?>().apply { repeat(SEASON_QUESTIONS.size) { add(null) } } }
    val done = answers.all { it != null }

    Column(Modifier.fillMaxWidth()) {
        CapsLabel("COLOR SEASON")
        Spacer(Modifier.height(8.dp))
        if (!done) {
            SectionCard(
                title = "Find your season",
                subtitle = "3 questions — wear the colors that make your skin look alive."
            ) {
                SEASON_QUESTIONS.forEachIndexed { qi, q ->
                    Text(q.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = PslText)
                    Spacer(Modifier.height(4.dp))
                    QuizOption(q.warmOption, answers[qi] == true) { answers[qi] = true }
                    QuizOption(q.coolOption, answers[qi] == false) { answers[qi] = false }
                    Spacer(Modifier.height(10.dp))
                }
            }
        } else {
            val warmVotes = answers.count { it == true }
            val coolVotes = answers.count { it == false }
            val season = seasonFromVotes(warmVotes, coolVotes, report?.skinUndertone.orEmpty())
            SeasonResultContent(season)
            Spacer(Modifier.height(6.dp))
            Text(
                "Retake quiz",
                fontSize = 13.sp,
                color = PslBlue,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { repeat(answers.size) { answers[it] = null } }
                    .padding(vertical = 4.dp)
            )
            Spacer(Modifier.height(6.dp))
            SwatchTestCard()
        }
    }
}
