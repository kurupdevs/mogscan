package com.kurupdevs.moggr.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.R

/**
 * v3.0: Ayush's developer profile — pink aesthetic, tappable socials.
 * Opened from the pink Developer card on the Home tab.
 */
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val uri = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PkBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Back", tint = PkInk)
            }
            Text(
                text = "DEVELOPER",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
                color = PkMuted
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            // Hero
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.dev_avatar),
                    contentDescription = "Ayush",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .shadow(12.dp, CircleShape, spotColor = PkCoral.copy(alpha = 0.35f)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Ayush",
                    fontFamily = LtSerif,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = PkInk
                )
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
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "true femboy coder",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PkInk
                    )
                }
            }
            Spacer(Modifier.height(22.dp))

            // Socials — tappable
            PkCard {
                PkSocialRow(
                    label = "GitHub",
                    value = "@kurupdevs",
                    onClick = { uri.openUri("https://github.com/kurupdevs") }
                )
                PkDivider()
                PkSocialRow(
                    label = "Instagram",
                    value = "@frkurup",
                    onClick = { uri.openUri("https://instagram.com/frkurup") }
                )
                PkDivider()
                PkSocialRow(
                    label = "TikTok",
                    value = "@ayushhfr",
                    onClick = { uri.openUri("https://tiktok.com/@ayushhfr") }
                )
            }
            Spacer(Modifier.height(16.dp))

            // Tech stack
            Text("TECH STACK", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, color = PkMuted)
            Spacer(Modifier.height(8.dp))
            PkChipFlow(
                items = listOf("Kotlin", "Python", "JavaScript", "Java", "HTML/CSS", "GDScript")
            )
            Spacer(Modifier.height(16.dp))

            // Projects
            Text("PROJECTS", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, color = PkMuted)
            Spacer(Modifier.height(8.dp))
            PkCard {
                PkRow(label = "Moggr", sub = "Free on-device face rating")
                PkDivider()
                PkRow(label = "KuruBeats", sub = "Open music player")
                PkDivider()
                PkRow(label = "KURUPUSERBOT", sub = "Telegram userbot")
            }
            Spacer(Modifier.height(16.dp))

            // Contact
            Text("CONTACT", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, color = PkMuted)
            Spacer(Modifier.height(8.dp))
            PkCard {
                PkRow(label = "Indore, India", sub = "Class 12th")
                PkDivider()
                PkRow(label = "DMs open", sub = "Instagram @frkurup")
                PkDivider()
                PkRow(label = "Open to collaborate", sub = "Say hi anytime")
            }
            Spacer(Modifier.height(16.dp))

            // Games
            Text("CURRENTLY PLAYING", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp, color = PkMuted)
            Spacer(Modifier.height(8.dp))
            PkChipFlow(
                items = listOf("Black Myth: Wukong", "Marvel Rivals", "Tekken 8", "Batman: Arkham Knight")
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PkCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(18.dp), spotColor = Color(0x1A5C2E2E))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        content = content
    )
}

@Composable
private fun PkSocialRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PkInk)
            Text(text = value, fontSize = 13.sp, color = PkMuted)
        }
        Icon(Icons.Filled.OpenInNew, contentDescription = "Open", tint = PkCoralDeep, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PkRow(label: String, sub: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 11.dp)) {
        Text(text = label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PkInk)
        Text(text = sub, fontSize = 13.sp, color = PkMuted)
    }
}

@Composable
private fun PkDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(PkLine)
    )
}

@Composable
private fun PkChipFlow(items: List<String>) {
    // simple wrapping flow using nested rows
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var row = mutableListOf<String>()
        for (item in items) {
            row.add(item)
            if (row.size == 2 || item == items.last()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (chip in row) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(text = chip, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PkInk)
                        }
                    }
                }
                row = mutableListOf()
            }
        }
    }
}
