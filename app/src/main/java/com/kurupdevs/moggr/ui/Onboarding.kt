package com.kurupdevs.moggr.ui

import android.net.Uri
import android.widget.VideoView
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kurupdevs.moggr.util.ProfileStore
import com.kurupdevs.moggr.util.UserProfile
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.util.Calendar

val PslBlue = Color(0xFFE07856)
val PslBlack = Color(0xFFFAF7F1)
val PslCard = Color(0xFFFFFFFF)
val PslGrey = Color(0xFF78716C)
val PslText = Color(0xFF1C1917)
val PslDeep = Color(0xFF1C1917)
val MogSerif = FontFamily.Serif

/** Small letter-spaced uppercase label, e.g. "TODAY'S THOUGHT". */
@Composable
fun CapsLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = PslGrey,
        letterSpacing = 2.sp,
        modifier = modifier
    )
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
                if (selected) PslText else Color(0xFFE2DCD2),
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
            color = if (selected) PslBlack else PslText
        )
    }
}

// ---------- Intro: full-screen looping video, Get started only ----------

@Composable
fun IntroVideoScreen(onGetStarted: () -> Unit) {
    Box(Modifier.fillMaxSize().background(PslBlack)) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    setVideoURI(
                        Uri.parse("android.resource://${ctx.packageName}/raw/intro")
                    )
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        start()
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
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
        Button(
            onClick = onGetStarted,
            colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp)
                .height(56.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text("Get started", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "Moggr",
            fontFamily = MogSerif,
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
    onBackToIntro: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    var language by remember { mutableStateOf("") }
    var heightText by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }

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

    val totalSteps = 5

    fun next() { if (step < totalSteps - 1) step++ }
    fun back() { if (step > 0) step-- else onBackToIntro() }

    Column(
        Modifier
            .fillMaxSize()
            .background(PslBlack)
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
                color = PslBlue,
                trackColor = Color(0xFFEDE7DB)
            )
            Spacer(Modifier.size(40.dp))
        }
        Spacer(Modifier.height(24.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CapsLabel("STEP ${step + 1} OF $totalSteps")
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
            }

            4 -> {
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
                                goal = goal
                            )
                        )
                    }
                )
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
                .background(PslBlue.copy(alpha = 0.08f))
        )
    }
}

@Composable
private fun QuestionTitle(title: String, subtitle: String) {
    Text(
        title,
        fontFamily = MogSerif,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold,
        color = PslText,
        textAlign = TextAlign.Center,
        lineHeight = 36.sp,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(10.dp))
    Text(
        subtitle,
        fontSize = 14.sp,
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
                    .background(if (isSel) PslText else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (isSel) PslText else Color(0xFFEDE7DB),
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
                    color = if (isSel) PslBlack else PslText,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PslNextButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = PslBlue,
            disabledContainerColor = Color(0xFFEDE7DB),
            contentColor = Color.White,
            disabledContentColor = Color(0xFF98A2B3)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(50)
    ) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
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
    focusedBorderColor = PslBlue,
    unfocusedBorderColor = Color(0xFFD8D0C2),
    cursorColor = PslBlue,
    focusedContainerColor = PslCard,
    unfocusedContainerColor = PslCard
)
