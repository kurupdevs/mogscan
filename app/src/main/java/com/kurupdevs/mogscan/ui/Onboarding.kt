package com.kurupdevs.mogscan.ui

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.kurupdevs.mogscan.util.ProfileStore
import com.kurupdevs.mogscan.util.UserProfile

val PslBlue = Color(0xFF1E90FF)
val PslBlack = Color(0xFF000000)
val PslCard = Color(0xFF141414)
val PslGrey = Color(0xFF9E9E9E)

// ---------- Intro: full-screen looping video ----------

@Composable
fun IntroVideoScreen(onGetStarted: () -> Unit) {
    val context = LocalContext.current
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
        // Bottom scrim + CTA
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f), Color.Black),
                        startY = 600f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "MogScan",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Free PSL face rating — 3 angles, real scores,\nno paywall, photos never leave your phone.",
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onGetStarted,
                colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Get started", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------- Questionnaire ----------

@OptIn(ExperimentalMaterial3Api::class)
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
    val dateState = androidx.compose.material3.rememberDatePickerState()

    val totalSteps = 5

    fun next() { if (step < totalSteps - 1) step++ }
    fun back() { if (step > 0) step-- else onBackToIntro() }

    Column(
        Modifier
            .fillMaxSize()
            .background(PslBlack)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = ::back, modifier = Modifier.size(40.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Spacer(Modifier.padding(4.dp))
            LinearProgressIndicator(
                progress = { (step + 1) / totalSteps.toFloat() },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = PslBlue,
                trackColor = Color(0xFF2A2A2A)
            )
        }
        Spacer(Modifier.height(28.dp))

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
                    onValueChange = { heightText = it.filter { c -> c.isDigit() }.take(3) },
                    placeholder = { Text("Height in cm", color = PslGrey) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = textFieldColors()
                )
                Spacer(Modifier.height(24.dp))
                val h = heightText.toIntOrNull() ?: 0
                PslNextButton("Next", enabled = h in 100..250, onClick = ::next)
            }

            3 -> {
                QuestionTitle(
                    title = "When were you born?",
                    subtitle = "This tells us about your growth stage and helps calibrate your custom plan."
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = PslCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DatePicker(
                        state = dateState,
                        showModeToggle = false,
                        colors = androidx.compose.material3.DatePickerDefaults.colors(
                            containerColor = PslCard,
                            titleContentColor = Color.White,
                            headlineContentColor = Color.White,
                            weekdayContentColor = PslGrey,
                            subheadContentColor = Color.White,
                            yearContentColor = Color.White,
                            currentYearContentColor = PslBlue,
                            selectedYearContentColor = Color.White,
                            selectedYearContainerColor = PslBlue,
                            dayContentColor = Color.White,
                            selectedDayContentColor = Color.White,
                            selectedDayContainerColor = PslBlue,
                            todayContentColor = PslBlue,
                            todayDateBorderColor = PslBlue
                        )
                    )
                }
                Spacer(Modifier.height(16.dp))
                PslNextButton("Next", enabled = dateState.selectedDateMillis != null, onClick = ::next)
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
                        onComplete(
                            UserProfile(
                                name = name.trim(),
                                language = language,
                                heightCm = heightText.toIntOrNull() ?: 0,
                                dobMillis = dateState.selectedDateMillis ?: 0L,
                                goal = goal
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun QuestionTitle(title: String, subtitle: String) {
    Text(
        title,
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    Text(
        subtitle,
        fontSize = 14.sp,
        color = PslGrey,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
    Spacer(Modifier.height(28.dp))
}

@Composable
private fun OptionList(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        options.forEach { opt ->
            val isSel = opt == selected
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSel) Color.White else PslCard
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = if (isSel) 2.dp else 0.dp,
                        color = if (isSel) PslBlue else Color.Transparent,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelect(opt) }
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        opt,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSel) Color.Black else Color.White
                    )
                }
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
            disabledContainerColor = Color(0xFF2A2A2A)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
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
