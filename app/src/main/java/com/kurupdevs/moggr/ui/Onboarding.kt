package com.kurupdevs.moggr.ui

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kurupdevs.moggr.util.UserProfile
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqLine
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.EqPillDark
import com.kurupdevs.moggr.ui.theme.MogCoral
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.util.Calendar

// v2.7 equilibrium skin: legacy shared vals, names kept for every other file
// that references them. Values now point at the new greige palette.
val PslBlue = MogCoral
val PslBlack = Color(0xFFFAF7F1) // keep: used as both light text + light bg elsewhere
val PslCard = Color(0xFFF6F2EA) // EqCard: warm frosted card
val PslGrey = EqMuted
val PslText = EqInk
val PslDeep = EqPillDark
val MogSerif = EqSerif

/** Warm greige gradient backdrop, v2.7 equilibrium style. */
val MoggrBg = eqBackgroundBrush()

/** Small letter-spaced uppercase label — now the shared equilibrium label. */
@Composable
fun CapsLabel(text: String, modifier: Modifier = Modifier) {
    EqSectionLabel(text, modifier)
}

/** Rounded pill chip; selected state is filled ink like the reference UI. */
@Composable
fun MogChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) PslText else Color.Transparent)
            .border(
                1.dp,
                if (selected) PslText else EqLine,
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else PslText
        )
    }
}

// ---------- Intro: full-screen looping video, Get started only ----------

@Composable
fun IntroVideoScreen(onGetStarted: () -> Unit) {
    Box(Modifier.fillMaxSize().background(MoggrBg)) {
        AndroidView(
            factory = { ctx ->
                CropVideoView(ctx).apply {
                    setVideoURI(
                        Uri.parse("android.resource://${ctx.packageName}/raw/intro")
                    )
                    setOnPreparedListener { mp ->
                        setVideoSize(mp.videoWidth, mp.videoHeight)
                        mp.isLooping = true
                        start()
                    }
                }
            },
            modifier = Modifier.fillMaxSize().clipToBounds()
        )
        // Bottom scrim so the button stays readable over the video
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)),
                        startY = 900f
                    )
                )
        )
        EqCoralPillButton(
            text = "Get started",
            onClick = onGetStarted,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp)
        )
        Text(
            "Moggr",
            fontFamily = EqSerif,
            fontStyle = FontStyle.Italic,
            fontSize = 30.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 64.dp)
        )
    }
}

// ---------- Questionnaire (reference style) ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuestionFlow(
    onComplete: (UserProfile) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    // v2.6-science begin
    var ageBracket by remember { mutableStateOf("") }
    // v2.6-science end

    // DOB wheel state
    val months = remember {
        listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
    }
    val days = remember { (1..31).map { it.toString() } }
    val years = remember { (1960..2026).map { it.toString() }.reversed() }
    var monthIdx by remember { mutableIntStateOf(0) }
    var dayIdx by remember { mutableIntStateOf(0) }
    var yearIdx by remember { mutableIntStateOf(years.indexOf("2005").takeIf { it >= 0 } ?: 20) }

    // v2.6-science begin: added age-bracket step -> 6 steps total
    val totalSteps = 6
    // v2.6-science end

    fun next() { if (step < totalSteps - 1) step++ }
    fun back() { if (step > 0) step-- else onBack() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MoggrBg)
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::back, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = PslText)
            }
            Spacer(Modifier.size(8.dp))
            LinearProgressIndicator(
                progress = { (step + 1) / totalSteps.toFloat() },
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = MogCoral,
                trackColor = EqLine
            )
            Spacer(Modifier.size(40.dp))
        }
        Spacer(Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CapsLabel("Set up your profile (optional)")
        }
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "Step ${step + 1} of $totalSteps",
                fontSize = 12.sp,
                color = PslGrey
            )
        }
        Spacer(Modifier.height(16.dp))

        when (step) {
            0 -> {
                QuestionTitle(
                    title = "What should we call you?",
                    subtitle = "Your report and plan will be personalized with your name."
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Your name", color = PslGrey) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = textFieldColors()
                )
                Spacer(Modifier.height(24.dp))
                PslNextButton("Next", enabled = name.isNotBlank(), onClick = ::next)
                SkipForNowButton(onSkip)
            }

            1 -> {
                QuestionTitle(
                    title = "Which language do you prefer?",
                    subtitle = "We'll use this for your tips and plan."
                )
                OptionList(
                    options = listOf("English", "Hindi", "Hinglish"),
                    selected = language,
                    onSelect = { language = it }
                )
                Spacer(Modifier.height(24.dp))
                PslNextButton("Next", enabled = language.isNotBlank(), onClick = ::next)
                SkipForNowButton(onSkip)
            }

            2 -> {
                QuestionTitle(
                    title = "How tall are you?",
                    subtitle = "Height helps calibrate your facial proportions."
                )
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { input ->
                        val filtered = input.filter { c -> c.isDigit() || c == '.' }.take(5)
                        if (filtered.count { it == '.' } <= 1) heightText = filtered
                    },
                    placeholder = { Text("cm or ft — e.g. 175 or 5.8", color = PslGrey) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = textFieldColors()
                )
                Spacer(Modifier.height(24.dp))
                PslNextButton("Next", enabled = parseHeightCm(heightText) != null, onClick = ::next)
                SkipForNowButton(onSkip)
            }

            3 -> {
                QuestionTitle(
                    title = "When were you born?",
                    subtitle = "This helps calibrate your plan to your age group."
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WheelColumn(
                        items = months,
                        selected = monthIdx,
                        onSelected = { monthIdx = it },
                        modifier = Modifier.weight(1.4f)
                    )
                    WheelColumn(
                        items = days,
                        selected = dayIdx,
                        onSelected = { dayIdx = it },
                        modifier = Modifier.weight(0.8f)
                    )
                    WheelColumn(
                        items = years,
                        selected = yearIdx,
                        onSelected = { yearIdx = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(24.dp))
                PslNextButton("Next", enabled = true, onClick = ::next)
                SkipForNowButton(onSkip)
            }

            4 -> {
                // v2.6-science begin: optional age-bracket question
                QuestionTitle(
                    title = "Which age group are you in?",
                    subtitle = "Younger users get softer language and fewer scans per day."
                )
                OptionList(
                    options = com.kurupdevs.moggr.util.TeenMode.BRACKETS,
                    selected = ageBracket,
                    onSelect = { ageBracket = it }
                )
                Spacer(Modifier.height(24.dp))
                PslNextButton("Next", enabled = ageBracket.isNotBlank(), onClick = ::next)
                SkipForNowButton(onSkip)
                // v2.6-science end
            }

            5 -> {
                QuestionTitle(
                    title = "What's your main goal?",
                    subtitle = "We'll shape your ascension roadmap around it."
                )
                OptionList(
                    options = listOf("Ascend my looks", "Boost my confidence", "Just curious"),
                    selected = goal,
                    onSelect = { goal = it }
                )
                Spacer(Modifier.height(24.dp))
                PslNextButton(
                    "Start my scan",
                    enabled = goal.isNotBlank(),
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, years[yearIdx].toInt())
                            set(Calendar.MONTH, monthIdx)
                            set(Calendar.DAY_OF_MONTH, dayIdx + 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onComplete(
                            UserProfile(
                                name = name.trim(),
                                language = language,
                                heightCm = parseHeightCm(heightText) ?: 0,
                                dobMillis = cal.timeInMillis,
                                goal = goal,
                                // v2.6-science begin
                                ageBracket = ageBracket
                                // v2.6-science end
                            )
                        )
                    }
                )
                SkipForNowButton(onSkip)
            }
        }
    }
}

/** iOS-style drum/wheel picker column with a highlight on the centered row. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    items: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = 46.dp
    val visibleCount = 5
    val padItems = visibleCount / 2
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }

    // Jump to the selected item (centered) on first composition
    LaunchedEffect(Unit) {
        listState.scrollToItem((selected - padItems).coerceAtLeast(0))
    }
    // Report the centered item whenever scrolling settles
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .map { scrolling -> scrolling to (listState.firstVisibleItemIndex + padItems) }
            .distinctUntilChanged()
            .filter { (scrolling, _) -> !scrolling }
            .collect { (_, centered) ->
                val clamped = centered.coerceIn(0, items.lastIndex)
                if (clamped != selected) onSelected(clamped)
                // snap the centered item exactly into place
                val target = (clamped - padItems).coerceAtLeast(0)
                if (listState.firstVisibleItemIndex != target ||
                    listState.firstVisibleItemScrollOffset != 0
                ) {
                    listState.scrollToItem(target)
                }
            }
    }

    Box(modifier = modifier.height(itemHeight * visibleCount)) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(padItems) { Spacer(Modifier.height(itemHeight)) }
            itemsIndexed(items) { index, label ->
                val isSel = index == selected
                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .clickable {
                            onSelected(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = if (isSel) 21.sp else 17.sp,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSel) PslText else PslText.copy(alpha = 0.35f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            items(padItems) { Spacer(Modifier.height(itemHeight)) }
        }
        // Center highlight behind the selected row
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(MogCoral.copy(alpha = 0.10f))
        )
    }
}

/** Outlined pill under each question step — profile setup is optional. */
@Composable
private fun SkipForNowButton(onSkip: () -> Unit) {
    Spacer(Modifier.height(12.dp))
    EqPillButton(
        text = "Skip for now",
        onClick = onSkip,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    )
}

@Composable
private fun QuestionTitle(title: String, subtitle: String) {
    EqHeadline(
        text = title,
        size = 30,
        align = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = subtitle,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = PslGrey,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    )
    Spacer(Modifier.height(28.dp))
}

/** Pill option rows — full-width rounded pills, selected fills ink. */
@Composable
private fun OptionList(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { opt ->
            val isSel = opt == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(if (isSel) PslText else Color.White.copy(alpha = 0.85f))
                    .border(
                        width = 1.dp,
                        color = if (isSel) PslText else EqLine,
                        shape = RoundedCornerShape(50)
                    )
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    opt,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSel) Color.White else PslText,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PslNextButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    EqCoralPillButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Accepts cm (100-250) or feet (3.0-8.9, e.g. 5.8) and returns cm. */
private fun parseHeightCm(input: String): Int? {
    val v = input.toDoubleOrNull() ?: return null
    return when {
        v in 100.0..250.0 -> v.toInt()
        v in 3.0..8.9 -> (v * 30.48).toInt()
        else -> null
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = PslText,
    unfocusedTextColor = PslText,
    focusedBorderColor = MogCoral,
    unfocusedBorderColor = EqLine,
    cursorColor = MogCoral,
    focusedContainerColor = Color.White.copy(alpha = 0.75f),
    unfocusedContainerColor = Color.White.copy(alpha = 0.55f)
)
