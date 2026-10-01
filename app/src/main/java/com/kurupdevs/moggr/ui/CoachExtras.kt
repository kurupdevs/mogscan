package com.kurupdevs.moggr.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqLavender
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.EqPeach
import com.kurupdevs.moggr.ui.theme.EqPillDark
import com.kurupdevs.moggr.ui.theme.EqSage
import com.kurupdevs.moggr.ui.theme.MogCoral
import com.kurupdevs.moggr.ui.theme.MogCoralDark
// v2.6-hinglish begin
import com.kurupdevs.moggr.util.LanguageStore
// v2.6-hinglish end

// ---------------------------------------------------------------------------
// Morning check-in card
// ---------------------------------------------------------------------------

private val SLEEP_OPTIONS = listOf("<6h", "6–8h", "8+h")
private val FOCUS_OPTIONS = listOf("skin", "hair", "posture", "debloat")

@Composable
fun MorningCheckinCard(
    onDone: (sleep: String, puff: Int, focus: String) -> Unit,
    onSkip: () -> Unit
) {
    // v2.6-hinglish: check-in labels follow the app language.
    val hi = LanguageStore.isHinglish
    // Display-translated focus options; the stored value stays the English key.
    val focusOpts = FOCUS_OPTIONS.mapIndexed { i, stored ->
        val key = "focus_opt_$i"
        val label = Strings.s(key, hi)
        stored to if (label == key) stored else label
    }
    var sleep by remember { mutableStateOf<String?>(null) }
    var puff by remember { mutableStateOf<Int?>(null) }
    var focus by remember { mutableStateOf<String?>(null) }

    EqGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column {
            EqHeadline(Strings.s("coach_checkin_title", hi), size = 20)
            EqBody(Strings.s("coach_checkin_sub", hi), size = 12)
            Spacer(Modifier.height(12.dp))

            EqSectionLabel(Strings.s("coach_checkin_sleep", hi))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SLEEP_OPTIONS.forEach { opt ->
                    CheckinPill(opt, selected = sleep == opt, onClick = { sleep = opt })
                }
            }
            Spacer(Modifier.height(12.dp))

            EqSectionLabel(Strings.s("coach_checkin_puff", hi))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                (1..5).forEach { n ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (puff == n) EqPillDark else MogCoral.copy(alpha = 0.15f))
                            .clickable { puff = n },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$n",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (puff == n) Color.White else EqInk
                        )
                    }
                }
                Text(Strings.s("coach_puff_scale", hi), fontSize = 11.sp, color = EqMuted)
            }
            Spacer(Modifier.height(12.dp))

            EqSectionLabel(Strings.s("coach_checkin_focus", hi))
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                focusOpts.forEach { (stored, label) ->
                    CheckinPill(label, selected = focus == stored, onClick = { focus = stored })
                }
            }
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSkip) {
                    Text(Strings.s("coach_checkin_skip", hi), color = EqMuted, fontSize = 14.sp)
                }
                Spacer(Modifier.width(4.dp))
                EqCoralPillButton(
                    text = Strings.s("coach_checkin_log", hi),
                    onClick = { onDone(sleep!!, puff!!, focus!!) },
                    enabled = sleep != null && puff != null && focus != null
                )
            }
        }
    }
}

@Composable
private fun CheckinPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) EqPillDark else Color.White.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else EqInk
        )
    }
}

// ---------------------------------------------------------------------------
// Vibe dial (segmented control for the coach header)
// ---------------------------------------------------------------------------

private val VIBES = listOf(
    com.kurupdevs.moggr.util.CoachMemory.VIBE_BLUNT to "vibe_short_blunt",
    com.kurupdevs.moggr.util.CoachMemory.VIBE_BIGBRO to "vibe_short_bigbro",
    com.kurupdevs.moggr.util.CoachMemory.VIBE_HYPE to "vibe_short_hype"
)

@Composable
fun VibeSegmentedControl(
    vibe: String,
    onVibe: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // v2.6-hinglish: vibe pill labels translated.
        val hiVibe = LanguageStore.isHinglish
        VIBES.forEach { (key, labelKey) ->
            val label = Strings.s(labelKey, hiVibe).let { if (it == labelKey) labelKey else it }
            val selected = vibe == key
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) EqPillDark else Color.Transparent)
                    .clickable { onVibe(key) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) Color.White else EqInk
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Celebration banner
// ---------------------------------------------------------------------------

@Composable
fun CelebrationBanner(text: String, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(EqPeach)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    Strings.s("coach_milestone", LanguageStore.isHinglish),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = EqMuted
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text,
                    fontFamily = EqSerif,
                    fontSize = 16.sp,
                    color = EqInk
                )
            }
            TextButton(onClick = onDismiss) {
                Text("✕", color = EqMuted, fontSize = 15.sp)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Guided diagnosis flows
// ---------------------------------------------------------------------------

data class FlowStep(val question: String, val options: List<String>)

data class GuidedFlow(
    val id: String,
    val title: String,
    val subtitle: String,
    val steps: List<FlowStep>,
    val message: (List<String>) -> String
)

val GUIDED_FLOWS = listOf(
    GuidedFlow(
        id = "diagnose",
        title = "Diagnose my weakest trait",
        subtitle = "Find what's dragging your score",
        steps = listOf(
            FlowStep(
                "Which area bugs you most?",
                listOf("Eyes", "Jawline", "Skin", "Nose", "Hair", "Chin")
            ),
            FlowStep(
                "How does it look in photos vs mirror?",
                listOf("Worse in selfies", "Worse in mirror", "Same everywhere")
            ),
            FlowStep(
                "What have you already tried for it?",
                listOf("Nothing yet", "Skincare", "Gym / fat loss", "Haircut change", "Mewing / chewing")
            ),
            FlowStep(
                "What do you want out of this?",
                listOf("Fix it in 30 days", "Long-term game plan", "Just understand it")
            )
        ),
        message = { a ->
            "I ran your weakest-trait check: the area that bugs me most is ${a[0]}, " +
                "it looks ${a[1].lowercase()} compared to the mirror, I've already tried " +
                "${a[2].lowercase()}, and I want to ${a[3].lowercase()}. " +
                "Diagnose it against my scan — honest verdict, why it's reading that way, " +
                "and the top priority fixes."
        }
    ),
    GuidedFlow(
        id = "plan",
        title = "Build my 7-day plan",
        subtitle = "One week, day by day",
        steps = listOf(
            FlowStep(
                "Your #1 priority this week?",
                listOf("Debloat", "Skin", "Hair", "Posture", "Sleep")
            ),
            FlowStep(
                "How much time can you give daily?",
                listOf("15 min", "30 min", "1 hour")
            ),
            FlowStep(
                "Where do you usually slip?",
                listOf("Sleep", "Diet", "Consistency", "Don't know where to start")
            ),
            FlowStep(
                "Commitment level?",
                listOf("Locked in", "Mostly there", "Half in")
            )
        ),
        message = { a ->
            "Build me a 7-day softmaxxing plan: my #1 priority is ${a[0].lowercase()}, " +
                "I can give ${a[1]} daily, I usually slip on ${a[2].lowercase()}, " +
                "and my commitment level is ${a[3].lowercase()}. " +
                "Keep it concrete and day-by-day — no fluff."
        }
    ),
    GuidedFlow(
        id = "photos",
        title = "Fix my photos",
        subtitle = "Angles, light, presence",
        steps = listOf(
            FlowStep(
                "Where do your photos fall flat?",
                listOf("Lighting", "Angles", "Selfies", "Group photos")
            ),
            FlowStep(
                "What do you usually shoot on?",
                listOf("Front camera", "Back camera", "Whatever's closest")
            ),
            FlowStep(
                "Your current look in photos?",
                listOf("Bedhead", "Basic", "Trying", "Styled")
            ),
            FlowStep(
                "What are the photos for?",
                listOf("Better selfies", "Dating profile", "Instagram grid")
            )
        ),
        message = { a ->
            "My photos need fixing: they fall flat on ${a[0].lowercase()}, I usually shoot on " +
                "${a[1].lowercase()}, my current look is ${a[2].lowercase()}, and I want them for " +
                "${a[3].lowercase()}. Tell me exactly what to change — lighting, angles, " +
                "grooming, the lot."
        }
    )
)

@Composable
fun GuidedFlowCards(onFlow: (GuidedFlow) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // v2.6-hinglish: flow card titles follow the app language.
        val hiFlow = LanguageStore.isHinglish
        // v2.7: pastel recommendation tiles, rotating accents.
        val tileColors = listOf(EqLavender, EqSage, EqPeach)
        GUIDED_FLOWS.forEachIndexed { idx, flow ->
            val titleKey = when (flow.id) {
                "diagnose" -> "coach_flow_title"
                "plan" -> "coach_plan_title"
                else -> "coach_photo_title"
            }
            val subKey = when (flow.id) {
                "diagnose" -> "coach_flow_sub"
                "plan" -> "coach_plan_sub"
                else -> "coach_photo_sub"
            }
            val flowTitle = Strings.s(titleKey, hiFlow).let { if (it == titleKey) flow.title else it }
            val flowSub = Strings.s(subKey, hiFlow).let { if (it == subKey) flow.subtitle else it }
            Box(
                modifier = Modifier
                    .width(168.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(tileColors[idx % tileColors.size])
                    .clickable { onFlow(flow) }
                    .padding(13.dp)
            ) {
                Column {
                    Text(
                        flowTitle,
                        fontFamily = EqSerif,
                        fontSize = 16.sp,
                        color = EqInk,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(flowSub, fontSize = 11.sp, color = EqMuted)
                    Spacer(Modifier.height(8.dp))
                    Text(Strings.s("coach_start", hiFlow), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MogCoralDark)
                }
            }
        }
    }
}

@Composable
fun GuidedFlowDialog(
    flow: GuidedFlow,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    val answers = remember(flow.id) { mutableStateListOf<String>() }
    var stepIdx by remember(flow.id) { mutableStateOf(0) }
    var review by remember(flow.id) { mutableStateOf(false) }
    val steps = flow.steps
    // v2.6-hinglish: dialog chrome follows the app language.
    val hiDialog = LanguageStore.isHinglish

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(eqBackgroundBrush())
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(Strings.s("flow_close", hiDialog), color = EqMuted, fontSize = 14.sp)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (review) Strings.s("flow_review", hiDialog) else Strings.fmt("flow_step", hiDialog, "i" to "${stepIdx + 1}", "n" to "${steps.size}"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EqMuted
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                EqHeadline(flow.title, size = 26)
                Spacer(Modifier.height(16.dp))
                if (!review) {
                    val step = steps[stepIdx]
                    Text(
                        step.question,
                        fontFamily = EqSerif,
                        fontSize = 19.sp,
                        color = EqInk,
                        lineHeight = 24.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    step.options.forEach { opt ->
                        val selected = answers.getOrNull(stepIdx) == opt
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (selected) EqPillDark
                                    else Color.White.copy(alpha = 0.7f)
                                )
                                .clickable {
                                    if (answers.size > stepIdx) answers[stepIdx] = opt
                                    else answers.add(opt)
                                    if (stepIdx < steps.lastIndex) stepIdx++ else review = true
                                }
                        ) {
                            Text(
                                opt,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) Color.White else EqInk,
                                modifier = Modifier.padding(15.dp)
                            )
                        }
                    }
                } else {
                    steps.forEachIndexed { i, step ->
                        Text(
                            step.question,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EqMuted
                        )
                        Text(
                            answers.getOrNull(i) ?: "—",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EqInk
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
            // Bottom bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!review && stepIdx > 0) {
                    TextButton(onClick = { stepIdx-- }) {
                        Text(Strings.s("flow_back", hiDialog), color = EqMuted, fontSize = 14.sp)
                    }
                } else if (review) {
                    TextButton(onClick = { review = false; stepIdx = steps.lastIndex }) {
                        Text(Strings.s("flow_edit", hiDialog), color = EqMuted, fontSize = 14.sp)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                if (review) {
                    EqCoralPillButton(
                        text = "Send to Coach →",
                        onClick = { onSend(flow.message(answers.toList())) }
                    )
                }
            }
        }
    }
}
