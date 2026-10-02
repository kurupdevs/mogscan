package com.kurupdevs.moggr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.R
import kotlin.math.roundToInt

/**
 * v3.0: Ayush's developer profile — matches his pink mockup:
 * hero photo, avatar, "Ayush", Online, @kurupdevs, true femboy coder pill,
 * 4 tappable social boxes, Tech Stack chips, tappable project cards.
 */
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val uri = LocalUriHandler.current
    val context = LocalContext.current
    // Real engagement score: % of last 7 days with any routine/scan/voice activity.
    val insightScore = remember {
        val routine = lastWeekActivity(context)
        val scans = lastWeekScans(context)
        val voice = lastWeekVoice(context)
        val active = (0..6).count { i ->
            routine[i].state == DayState.DONE ||
                scans[i].state == DayState.DONE ||
                voice[i].state == DayState.DONE
        }
        ((active / 7f) * 100).roundToInt()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ---- Hero photo + overlapping avatar ----
            Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
                Image(
                    painter = painterResource(R.drawable.dev_hero),
                    contentDescription = "Ayush",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(255.dp),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f))
                        .align(Alignment.TopStart)
                ) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Back", tint = PkInk)
                }
                Image(
                    painter = painterResource(R.drawable.dev_avatar),
                    contentDescription = "Ayush",
                    modifier = Modifier
                        .size(128.dp)
                        .align(Alignment.BottomCenter)
                        .clip(CircleShape)
                        .border(5.dp, Color.White, CircleShape)
                        .shadow(10.dp, CircleShape, spotColor = PkCoral.copy(alpha = 0.4f)),
                    contentScale = ContentScale.Crop
                )
            }

            // ---- Name block ----
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ayush",
                    fontFamily = LtSerif,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = PkInk
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34C77B))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Online",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF34C77B)
                    )
                }
                Text(
                    text = "@kurupdevs",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = PkMuted
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(PkPill)
                        .padding(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "true femboy coder",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PkInk
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            // ---- 3 social boxes (mockup) ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PkSocialBox(
                    logo = R.drawable.logo_github,
                    label = "@kurupdevs",
                    onClick = { uri.openUri("https://github.com/kurupdevs") },
                    modifier = Modifier.weight(1f)
                )
                PkSocialBox(
                    logo = R.drawable.logo_instagram,
                    label = "@frkurup",
                    onClick = { uri.openUri("https://instagram.com/frkurup") },
                    modifier = Modifier.weight(1f)
                )
                PkSocialBox(
                    logo = R.drawable.logo_tiktok,
                    label = "@ayushhfr",
                    onClick = { uri.openUri("https://tiktok.com/@ayushhfr") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(20.dp))

            // ---- Profile Insights (mockup) — real score from last 7 days of activity ----
            PkCard {
                Column(Modifier.padding(vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Profile Insights",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PkInk
                        )
                        Text(
                            text = "$insightScore / 100",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = PkMuted
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .clip(RoundedCornerShape(50))
                            .background(PkPill)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(insightScore / 100f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFF3C6B8), PkCoral)
                                    )
                                )
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Keep engaging to unlock +500 glow points",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = PkInk
                    )
                }
            }
            Spacer(Modifier.height(22.dp))

            // ---- Projects ----
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Projects",
                    fontFamily = LtSerif,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = PkInk
                )
                Text(
                    text = "What I've been building",
                    fontSize = 15.sp,
                    color = PkMuted
                )
            }
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PkProjectCard(
                    icon = R.drawable.ic_moggr_project,
                    title = "Moggr",
                    desc = "On-device face rating & glow-up coach. 100% private.",
                    onClick = { uri.openUri("https://github.com/kurupdevs/mogscan") }
                )
                PkProjectCard(
                    icon = R.drawable.logo_kurubeats,
                    title = "KuruBeats",
                    desc = "Ad-free open-source music player for Android.",
                    onClick = { uri.openUri("https://github.com/kurupdevs/KuruBeats") }
                )
                PkProjectCard(
                    icon = R.drawable.ic_userbot,
                    title = "KURUPUSERBOT",
                    desc = "Telegram userbot with 26 modules.",
                    onClick = { uri.openUri("https://github.com/kurupdevs/KURUPUSERBOT") }
                )
                // More on GitHub
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1E1E1E))
                        .clickable { uri.openUri("https://github.com/kurupdevs") }
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "More on GitHub",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "@kurupdevs",
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = "→",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = PkCoral
                        )
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PkCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = Color(0x1A5C2E2E))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .padding(horizontal = 18.dp),
        content = content
    )
}

@Composable
private fun PkSocialBox(logo: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .shadow(5.dp, RoundedCornerShape(20.dp), spotColor = Color(0x1A5C2E2E))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(logo),
            contentDescription = label,
            modifier = Modifier.size(50.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = PkMuted,
            maxLines = 1
        )
    }
}

@Composable
private fun PkProjectCard(icon: Int? = null, monogram: String? = null, title: String, desc: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = Color(0x1A5C2E2E))
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = title,
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else if (monogram != null) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = monogram,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PkInk
                )
                Text(
                    text = desc,
                    fontSize = 13.sp,
                    color = PkMuted
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "View →",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = PkCoral
        )
    }
}
