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
import com.kurupdevs.moggr.ui.theme.EqCard
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.MogCoral
// v2.6-hinglish begin
import com.kurupdevs.moggr.util.LanguageStore
// v2.6-hinglish end

// ---------- Small shared building blocks (guides library only) ----------

@Composable
private fun SectionCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    EqGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EqInk)
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 13.sp, color = EqMuted)
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text("•  ", fontSize = 14.sp, color = MogCoral, fontWeight = FontWeight.Bold)
        Text(text, fontSize = 14.sp, color = EqInk, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun QuizOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MogCoral.copy(alpha = 0.14f) else EqCard.copy(alpha = 0.6f))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MogCoral else EqMuted.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            fontSize = 14.sp,
            color = EqInk,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Spacer(Modifier.size(6.dp))
            Text("✓", fontSize = 14.sp, color = MogCoral, fontWeight = FontWeight.Bold)
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
            colors = CheckboxDefaults.colors(checkedColor = MogCoral)
        )
        Text(
            text,
            fontSize = 14.sp,
            color = if (checked) EqMuted else EqInk,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun ExpandableDayCard(day: String, title: String, items: List<String>) {
    var expanded by remember { mutableStateOf(false) }
    val checked = remember { mutableStateListOf<Boolean>().apply { repeat(items.size) { add(false) } } }
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
                    Text(day, fontSize = 12.sp, color = MogCoral, fontWeight = FontWeight.Bold)
                    Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = EqInk)
                }
                Text(
                    "${checked.count { it }} / ${items.size}",
                    fontSize = 12.sp,
                    color = EqMuted,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MogCoral
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
    )
)

@Composable
fun SkinQuizCard() {
    // v2.6-hinglish: quiz questions/options translated by index key.
    val hi = LanguageStore.isHinglish
    val questions = remember(hi) {
        SKIN_QUESTIONS.mapIndexed { qi, q ->
            val tKey = "sq$qi"
            SkinQuestion(
                Strings.s(tKey, hi).let { if (it == tKey) q.title else it },
                q.options.mapIndexed { oi, (label, value) ->
                    val oKey = "sq${qi}_o$oi"
                    val l = Strings.s(oKey, hi)
                    (if (l == oKey) label else l) to value
                }
            )
        }
    }
    val answers = remember { mutableStateListOf<String?>().apply { repeat(SKIN_QUESTIONS.size) { add(null) } } }
    val done = answers.all { it != null }

    Column(Modifier.fillMaxWidth()) {
        EqSectionLabel(Strings.s("caps_skin", hi))
        Spacer(Modifier.height(8.dp))
        if (!done) {
            SectionCard(
                title = Strings.s("skinq_title", hi),
                subtitle = Strings.s("skinq_sub", hi)
            ) {
                questions.forEachIndexed { qi, q ->
                    Text(q.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = EqInk)
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
    onRetake: () -> Unit
) {
    // v2.6-hinglish: result guidance in the current language.
    val hi = LanguageStore.isHinglish
    val cleanser = when (skinType) {
        "oily" -> Strings.s("skin_cleanser_oily", hi)
        "dry" -> Strings.s("skin_cleanser_dry", hi)
        "combo" -> Strings.s("skin_cleanser_combo", hi)
        else -> Strings.s("skin_cleanser_normal", hi)
    }
    val moisturizer = when (skinType) {
        "oily" -> Strings.s("skin_moist_oily", hi)
        "dry" -> Strings.s("skin_moist_dry", hi)
        "combo" -> Strings.s("skin_moist_combo", hi)
        else -> Strings.s("skin_moist_normal", hi)
    }
    val spf = when {
        sun == "high" -> Strings.s("skin_spf_high", hi)
        sensitivity == "sensitive" -> Strings.s("skin_spf_sensitive", hi)
        else -> Strings.s("skin_spf_default", hi)
    }
    val extras = buildList {
        if (sensitivity == "sensitive") add(Strings.s("skin_extra_sensitive", hi))
        if (acne == "often") add(Strings.s("skin_extra_acne", hi))
    }

    SectionCard(
        title = Strings.s("skin_result_title", hi),
        subtitle = Strings.s("skin_result_sub", hi)
    ) {
        RoutineStep("1", Strings.s("skin_step_cleanser", hi), cleanser)
        RoutineStep("2", Strings.s("skin_step_moist", hi), moisturizer)
        RoutineStep("3", Strings.s("skin_step_spf", hi), spf)
        if (extras.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            extras.forEach { Bullet(it) }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            Strings.s("retake_quiz", hi),
            fontSize = 13.sp,
            color = MogCoral,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable { onRetake() }
        )
    }
    Spacer(Modifier.height(10.dp))
    SectionCard(
        title = Strings.s("spf_title", hi),
        subtitle = Strings.s("spf_sub", hi)
    ) {
        for (i in 0 until 4) {
            Bullet(Strings.s("spf_b$i", hi))
        }
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
                .background(MogCoral),
            contentAlignment = Alignment.Center
        ) {
            Text(number, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
        }
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = EqInk)
            Text(guidance, fontSize = 13.sp, color = EqMuted)
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
    // v2.6-hinglish: 7-day plan + sodium guide translated by index key.
    val hi = LanguageStore.isHinglish
    Column(Modifier.fillMaxWidth()) {
        EqSectionLabel(Strings.s("caps_debloat", hi))
        Spacer(Modifier.height(8.dp))
        Text(
            Strings.s("debloat_sub", hi),
            fontSize = 13.sp,
            color = EqMuted,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        DEBLOAT_DAYS.forEachIndexed { di, (day, titleToItems) ->
            val (_, items) = titleToItems
            val tKey = "debloat_d${di}t"
            val title = Strings.s(tKey, hi).let { if (it == tKey) titleToItems.first else it }
            val hiItems = items.mapIndexed { ii, item ->
                val iKey = "debloat_d${di}i$ii"
                Strings.s(iKey, hi).let { if (it == iKey) item else it }
            }
            ExpandableDayCard(day, title, hiItems)
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(
            title = Strings.s("sod_title", hi),
            subtitle = Strings.s("sod_sub", hi)
        ) {
            Text(Strings.s("sod_high", hi), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MogCoral)
            for (i in 0 until 5) Bullet(Strings.s("sod_h$i", hi))
            Spacer(Modifier.height(8.dp))
            Text(Strings.s("sod_low", hi), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MogCoral)
            for (i in 0 until 5) Bullet(Strings.s("sod_l$i", hi))
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
    // v2.6-hinglish: hair advice translated via hair_shape_<shape> keys.
    val hi = LanguageStore.isHinglish
    fun adviceFor(name: String, fallback: String): String {
        val key = "hair_shape_${name.lowercase()}"
        return Strings.s(key, hi).let { if (it == key) fallback else it }
    }
    val shape = report?.faceShape.orEmpty().trim()
    val matched = HAIR_BY_SHAPE.firstOrNull { it.first.equals(shape, ignoreCase = true) }

    Column(Modifier.fillMaxWidth()) {
        EqSectionLabel(Strings.s("caps_hair", hi))
        Spacer(Modifier.height(8.dp))
        if (matched != null) {
            SectionCard(
                title = Strings.fmt("hair_your_cut", hi, "s" to matched.first),
                subtitle = Strings.s("hair_matched_sub", hi)
            ) {
                Text(adviceFor(matched.first, matched.second), fontSize = 14.sp, color = EqInk)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                Strings.s("hair_other", hi),
                fontSize = 13.sp,
                color = EqMuted,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        } else {
            Text(
                Strings.s("hair_no_scan", hi),
                fontSize = 13.sp,
                color = EqMuted,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        HAIR_BY_SHAPE.filter { it != matched }.forEach { (name, advice) ->
            var expanded by remember { mutableStateOf(false) }
            EqGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { expanded = !expanded }
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EqInk,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = MogCoral
                        )
                    }
                    if (expanded) {
                        Spacer(Modifier.height(6.dp))
                        Text(adviceFor(name, advice), fontSize = 14.sp, color = EqInk)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(
            title = Strings.s("barber_title", hi),
            subtitle = Strings.s("barber_sub", hi)
        ) {
            Text(
                Strings.s("barber_script", hi),
                fontSize = 14.sp,
                color = EqInk,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(6.dp))
            Bullet(Strings.s("barber_b1", hi))
            Bullet(Strings.s("barber_b2", hi))
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(
            title = Strings.s("curly_title", hi),
            subtitle = Strings.s("curly_sub", hi)
        ) {
            for (i in 0 until 5) Bullet(Strings.s("curly_b$i", hi))
        }
        Spacer(Modifier.height(10.dp))
        SectionCard(
            title = Strings.s("style_title", hi),
            subtitle = Strings.s("style_sub", hi)
        ) {
            for (i in 0 until 4) {
                RoutineStep(
                    "${i + 1}",
                    Strings.s("style_s$i", hi),
                    Strings.s("style_s${i}d", hi)
                )
            }
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
    // v2.6-hinglish: posture weeks translated by index key.
    val hi = LanguageStore.isHinglish
    Column(Modifier.fillMaxWidth()) {
        EqSectionLabel(Strings.s("caps_posture", hi))
        Spacer(Modifier.height(8.dp))
        Text(
            Strings.s("posture_sub", hi),
            fontSize = 13.sp,
            color = EqMuted,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        POSTURE_WEEKS.forEachIndexed { wi, (week, items) ->
            var done by remember { mutableStateOf(false) }
            var expanded by remember { mutableStateOf(false) }
            val wKey = "posture_w$wi"
            val weekTitle = Strings.s(wKey, hi).let { if (it == wKey) week else it }
            val hiItems = items.mapIndexed { ii, item ->
                val iKey = "posture_w${wi}i$ii"
                Strings.s(iKey, hi).let { if (it == iKey) item else it }
            }
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
                        Checkbox(
                            checked = done,
                            onCheckedChange = { done = it },
                            colors = CheckboxDefaults.colors(checkedColor = MogCoral)
                        )
                        Text(
                            weekTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (done) EqMuted else EqInk,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 6.dp)
                        )
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = MogCoral
                        )
                    }
                    if (expanded) {
                        Spacer(Modifier.height(6.dp))
                        hiItems.forEach { Bullet(it) }
                    }
                }
            }
        }
        Text(
            Strings.s("posture_foot", hi),
            fontSize = 12.sp,
            color = EqMuted,
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
    // v2.6-hinglish: season quiz translated by index key.
    val hi = LanguageStore.isHinglish
    val questions = remember(hi) {
        SEASON_QUESTIONS.mapIndexed { qi, q ->
            val tKey = "season_q$qi"
            val wKey = "season_q${qi}w"
            val cKey = "season_q${qi}c"
            SeasonQuestion(
                Strings.s(tKey, hi).let { if (it == tKey) q.title else it },
                Strings.s(wKey, hi).let { if (it == wKey) q.warmOption else it },
                Strings.s(cKey, hi).let { if (it == cKey) q.coolOption else it }
            )
        }
    }
    val answers = remember { mutableStateListOf<Boolean?>().apply { repeat(SEASON_QUESTIONS.size) { add(null) } } }
    val done = answers.all { it != null }

    Column(Modifier.fillMaxWidth()) {
        EqSectionLabel(Strings.s("caps_season", hi))
        Spacer(Modifier.height(8.dp))
        if (!done) {
            SectionCard(
                title = Strings.s("season_title", hi),
                subtitle = Strings.s("season_sub", hi)
            ) {
                questions.forEachIndexed { qi, q ->
                    Text(q.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = EqInk)
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
                Strings.s("retake_quiz", hi),
                fontSize = 13.sp,
                color = MogCoral,
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
