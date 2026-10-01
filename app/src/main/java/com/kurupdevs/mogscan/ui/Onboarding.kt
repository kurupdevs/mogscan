package com.kurupdevs.mogscan.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kurupdevs.mogscan.util.ProfileStore
import com.kurupdevs.mogscan.util.UserProfile
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.util.Calendar

val PslBlue = Color(0xFF1E90FF)
val PslBlack = Color(0xFF000000)
val PslCard = Color(0xFF141414)
val PslGrey = Color(0xFF9E9E9E)

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
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Get started", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.size(8.dp))
            LinearProgressIndicator(
                progress = { (step + 1) / totalSteps.toFloat() },
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color.White,
                trackColor = Color(0xFF2A2A2A)
            )
            Spacer(Modifier.size(40.dp))
        }
        Spacer(Modifier.height(24.dp))

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
                    subtitle = "This gives us information about your growth stage & potential and will be used to calibrate your custom plan."
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
                        color = if (isSel) Color.White else Color.White.copy(alpha = 0.35f),
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
                .background(Color.White.copy(alpha = 0.10f))
        )
    }
}

@Composable
private fun QuestionTitle(title: String, subtitle: String) {
    Text(
        title,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center,
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

/** White option cards with a checkbox square, like the reference. */
@Composable
private fun OptionList(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { opt ->
            val isSel = opt == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .border(
                        width = if (isSel) 2.dp else 0.dp,
                        color = if (isSel) PslBlue else Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelect(opt) }
                    .padding(horizontal = 18.dp, vertical = 17.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .border(
                            2.dp,
                            if (isSel) PslBlue else Color(0xFF9E9E9E),
                            RoundedCornerShape(6.dp)
                        )
                        .background(
                            if (isSel) PslBlue else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSel) {
                        Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.size(14.dp))
                Text(
                    opt,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
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
            disabledContainerColor = Color(0xFF2A2A2A),
            contentColor = Color.White,
            disabledContentColor = Color(0xFF777777)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(14.dp)
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
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = PslBlue,
    unfocusedBorderColor = Color(0xFF3A3A3A),
    cursorColor = PslBlue,
    focusedContainerColor = PslCard,
    unfocusedContainerColor = PslCard
)
