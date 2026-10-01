package com.kurupdevs.moggr.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.analysis.FaceAnalyzer
import com.kurupdevs.moggr.analysis.PslReport
import com.kurupdevs.moggr.coach.CoachClient
import com.kurupdevs.moggr.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private data class ChatMsg(
    val isUser: Boolean,
    val text: String,
    val isPhotoScan: Boolean = false
)

private const val COACH_FALLBACK =
    "Coach is resting right now — the free API didn't answer. Try again in a bit."

@Composable
fun CoachScreen(
    report: PslReport?,
    userName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val metricsCtx = remember(report) {
        report?.let {
            CoachClient.reportContext(
                it.overallPsl,
                it.features.map { f -> f.name to f.score },
                it.anglesRead,
                decile = it.decile,
                tier = pslLabel(it.overallPsl),
                failos = it.failoCount,
                halos = it.haloCount,
                pillars = it.pillars.map { p -> p.name to p.score }
            )
        } ?: "No scan yet — user hasn't completed a face scan."
    }
    val baseSystem = remember(userName, metricsCtx) {
        CoachClient.systemPrompt(userName.ifBlank { "there" }, metricsCtx)
    }
    val greeting = remember(report) {
        if (report != null) {
            "Yo${if (userName.isNotBlank()) " $userName" else ""} — I'm Moggr's Looksmaxing AI. " +
                "I see your scan: ${String.format(Locale.US, "%.1f", report.overallPsl)} PSL. " +
                "Ask me anything — what's dragging your score, what to fix first, hair, skin, " +
                "photos. Or send a fresh photo and I'll break it down."
        } else {
            "Yo — I'm Moggr's Looksmaxing AI. No scan on file yet, but ask me anything about " +
                "looksmaxxing: jawline, skin, hair, posture, photos. Or send a photo and " +
                "I'll scan it on your phone and break it down."
        }
    }

    var messages by remember { mutableStateOf(listOf(ChatMsg(false, greeting))) }
    var input by remember { mutableStateOf("") }
    var waiting by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    fun history(): List<Pair<Boolean, String>> = messages.map { it.isUser to it.text }

    fun streamReply(sys: String, h: List<Pair<Boolean, String>>, prompt: String) {
        val sb = StringBuilder()
        messages = messages + ChatMsg(false, "")
        CoachClient.askStream(sys, h, prompt,
            onToken = { tok ->
                sb.append(tok)
                val cur = messages
                if (cur.isNotEmpty()) {
                    messages = cur.dropLast(1) + cur.last().copy(text = sb.toString())
                }
            },
            onDone = { reply ->
                waiting = false
                val cur = messages
                val finalText = reply ?: if (sb.isNotEmpty()) sb.toString() else COACH_FALLBACK
                messages = if (cur.isNotEmpty()) {
                    cur.dropLast(1) + cur.last().copy(text = finalText)
                } else {
                    cur + ChatMsg(false, finalText)
                }
            }
        )
    }

    fun sendText(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || waiting) return
        val h = history()
        messages = messages + ChatMsg(true, clean)
        input = ""
        waiting = true
        streamReply(baseSystem, h, clean)
    }

    // Instant quick replies — answered locally from the scan, zero network wait.
    fun sendQuick(kind: String, label: String) {
        if (waiting) return
        messages = messages + ChatMsg(true, label)
        messages = messages + ChatMsg(false, instantAnswer(kind, report))
    }

    fun sendPhoto(uri: Uri) {
        if (waiting) return
        val h = history()
        messages = messages + ChatMsg(true, "Break down this photo.", isPhotoScan = true)
        waiting = true
        scope.launch(Dispatchers.Default) {
            try {
                val bmp = decodeDownscaled(context, uri)
                val rep = FaceAnalyzer.analyze(listOf(bmp))
                val photoCtx = "FRESH PHOTO SCAN done on-device just now (single photo): " +
                    CoachClient.reportContext(
                        rep.overallPsl,
                        rep.features.map { f -> f.name to f.score },
                        1,
                        decile = rep.decile,
                        tier = pslLabel(rep.overallPsl),
                        failos = rep.failoCount,
                        halos = rep.haloCount,
                        pillars = rep.pillars.map { p -> p.name to p.score }
                    )
                val sys = baseSystem + "\n\nThe user just sent an additional photo, " +
                    "scanned on-device: $photoCtx Analyze THIS photo scan now — full " +
                    "breakdown: verdict, why, top 3 fixes, what not to worry about. " +
                    "Note: this is a single-photo read, so treat it as a limited estimate — " +
                    "say so briefly."
                withContext(Dispatchers.Main) {
                    streamReply(
                        sys, h,
                        "I just sent a photo — give me your full analysis of it."
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    waiting = false
                    messages = messages + ChatMsg(
                        false,
                        "Couldn't read that photo — try another one."
                    )
                }
            }
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) sendPhoto(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PslBlack)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("‹ Back", color = PslBlue, fontSize = 16.sp)
            }
            Image(
                painter = painterResource(id = R.drawable.moggr_coach),
                contentDescription = "Moggr Coach",
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Moggr Coach",
                    fontFamily = MogSerif,
                    color = PslText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Looksmaxing AI · softmaxxing guidance · blunt & honest",
                    color = PslGrey,
                    fontSize = 12.sp
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFEDE7DB))
        )

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(msg)
            }
            if (waiting) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PslCard),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = PslBlue,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Coach is typing…", color = PslGrey, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // Quick replies — instant answers, no network wait
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickChip("Top 3 fixes") { sendQuick("fixes", "Top 3 fixes") }
            QuickChip("Weakest feature") { sendQuick("weakest", "Weakest feature") }
            QuickChip("Skin routine") { sendQuick("skin", "Skin routine") }
            QuickChip("Hair advice") { sendQuick("hair", "Hair advice") }
        }

        // Input row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { photoPicker.launch("image/*") },
                enabled = !waiting,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF4EFE7))
            ) {
                Text("◉", color = PslBlue, fontSize = 20.sp)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about jawline, skin, hair…", color = PslGrey) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PslText,
                    unfocusedTextColor = PslText,
                    focusedBorderColor = PslBlue,
                    unfocusedBorderColor = Color(0xFFD8D0C2),
                    cursorColor = PslBlue
                ),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendText(input) }),
                singleLine = false,
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { sendText(input) },
                enabled = !waiting && input.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PslBlue),
                shape = RoundedCornerShape(50),
                modifier = Modifier.height(52.dp)
            ) {
                Text("Send", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Quick replies answer instantly. Longer questions stream live from the free AI — " +
            "your photos never leave your phone; only your questions and measurement numbers may be sent to the AI service.",
            fontSize = 11.sp,
            color = PslGrey,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun ChatBubble(msg: ChatMsg) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (msg.isUser) PslBlue else PslCard
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(Modifier.padding(12.dp)) {
                if (msg.isPhotoScan) {
                    Text(
                        "PHOTO SCAN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (msg.isUser) Color.White.copy(alpha = 0.8f) else PslBlue
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    msg.text,
                    color = if (msg.isUser) Color.White else PslText,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun QuickChip(label: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4EFE7)),
        shape = RoundedCornerShape(50)
    ) {
        Text(
            label,
            color = PslBlue,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

private val FEATURE_TIPS = mapOf(
    "Brows" to "Clean up strays but keep them natural — thicker, straighter brows read stronger. Don't over-pluck.",
    "Cheekbones" to "Nothing pops cheekbones like lower body fat. Side lighting in photos helps too.",
    "Chin" to "Mostly genetic — but no forward-head posture and staying lean keep the jaw-to-chin line clean.",
    "Eye spacing" to "Genetic, don't chase millimeters here — brows, sleep and grooming matter more.",
    "Eyes" to "Fix what's fixable: 7-9h sleep for under-eyes, groomed brows, clean eye area.",
    "FWHR" to "Bone structure — leanness sharpens it. Chase low body fat, not the number.",
    "Facial thirds" to "Balance beats any single third — the right haircut and beard can visually rebalance thirds.",
    "Facial fifths" to "Mostly genetic. Beard and hair framing can tweak perceived width.",
    "Jaw angle" to "Genetic gonial angle — leanness, posture and neck training give the sharpest look your structure allows.",
    "Jawline" to "Debloat (less salt, more water, good sleep) + low body fat = sharper jawline. Chewing builds masseter tone over months.",
    "Lips" to "Keep them healthy — balm, hydration, no picking. Fullness is genetic.",
    "Midface ratio" to "Mostly genetic. Avoid long hair dragging the face down; groomed brows help balance.",
    "Nose" to "Genetic. Glasses and haircut balance it in photos — that's the play.",
    "Side profile" to "Chin-to-neck angle: fix forward head posture, stay lean. Profile = posture + leanness + genetics.",
    "Symmetry" to "Nobody is symmetric. Sleep on your back, chew evenly both sides — small habits, small gains."
)

private fun instantAnswer(kind: String, report: PslReport?): String {
    val feats = report?.features?.sortedBy { it.score } ?: emptyList()
    val fmt = { d: Double -> String.format(Locale.US, "%.1f", d) }
    return when (kind) {
        "fixes" -> if (feats.isEmpty()) {
            "No scan on file yet — scan your face first and I'll rank your exact fixes."
        } else buildString {
            append("Verdict: fix your failos first, not your halos.\n\n")
            feats.take(3).forEachIndexed { i, f ->
                append("${i + 1}. ${f.name} (${fmt(f.score)}) — ${FEATURE_TIPS[f.name] ?: "Softmaxxing basics: leanness, skin, sleep, posture."}\n")
            }
            append("\nWhat NOT to worry about: your strongest features are already carrying you.")
        }
        "weakest" -> if (feats.isEmpty()) {
            "No scan on file yet — scan your face first."
        } else {
            val f = feats.first()
            "Verdict: your weakest measured feature is ${f.name} (${fmt(f.score)}).\n\n" +
                "Why it matters: it's your biggest failo, so fixing it moves your whole read.\n\n" +
                "What to do: ${FEATURE_TIPS[f.name] ?: "Softmaxxing basics — leanness, skin, sleep, posture."}\n\n" +
                "What NOT to worry about: one weak feature doesn't define the face — harmony does."
        }
        "skin" -> "Verdict: skin is the highest-ROI fix in softmaxxing.\n\n" +
            "1. Wash twice daily — gentle cleanser, not soap.\n" +
            "2. Moisturize + sunscreen every morning.\n" +
            "3. Sleep 7-9h — under-eyes and dullness start here.\n" +
            "4. Stop touching your face; change pillowcases often.\n\n" +
            "What NOT to worry about: chasing 10 products. Consistency beats a shelf full of serums."
        else -> "Verdict: haircut is framing for your face.\n\n" +
            "1. Match the cut to your face shape — volume on top lengthens, shorter sides widen.\n" +
            "2. Clean neckline and edges beat an expensive cut gone shaggy.\n" +
            "3. Beard optional: stubble sharpens jawlines; a patchy beard hurts more than it helps.\n\n" +
            "What NOT to worry about: trends. Fit for YOUR face beats what's viral."
    }
}

private fun decodeDownscaled(context: Context, uri: Uri): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, bounds)
    }
    var sample = 1
    while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
    return context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts)
    } ?: throw IllegalStateException("Couldn't read that photo.")
}
