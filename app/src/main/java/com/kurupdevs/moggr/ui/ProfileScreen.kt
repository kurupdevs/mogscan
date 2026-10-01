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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.People
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
 * v3.0: Ayush's developer profile — matches his pink mockup:
 * hero photo, avatar, "Ayush", Online, @kurupdevs, true femboy coder pill,
 * 4 tappable social boxes, Tech Stack chips, tappable project cards.
 */
@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val uri = LocalUriHandler.current
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

            // ---- 4 social boxes ----
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
                PkSocialBox(
                    logo = R.drawable.logo_kurubeats,
                    label = "KuruBeats",
                    onClick = { uri.openUri("https://github.com/kurupdevs/KuruBeats") },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(20.dp))

            // ---- Tech Stack ----
            PkCard {
                Text(
                    text = "Tech Stack",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PkInk,
                    modifier = Modifier.padding(top = 14.dp, bottom = 12.dp)
                )
                PkLangChips(
                    items = listOf(
                        "Kotlin" to listOf(Color(0xFF7F52FF)),
                        "Python" to listOf(Color(0xFF3776AB)),
                        "JavaScript" to listOf(Color(0xFFE8C547)),
                        "Java" to listOf(Color(0xFFB07219)),
                        "HTML/CSS" to listOf(Color(0xFFE34C26), Color(0xFF7F52FF)),
                        "GDScript" to listOf(Color(0xFF478CBF))
                    )
                )
                Spacer(Modifier.height(14.dp))
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
                    icon = R.drawable.ic_launcher_foreground,
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
                    monogram = "K",
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
            Spacer(Modifier.height(22.dp))

            // ---- Games ----
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Games",
                    fontFamily = LtSerif,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = PkInk
                )
                Text(
                    text = "What I'm playing",
                    fontSize = 15.sp,
                    color = PkMuted
                )
            }
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PkGameCard("Black Myth: Wukong", "Action RPG")
                PkGameCard("Marvel Rivals", "Hero shooter")
                PkGameCard("Tekken 8", "Fighting")
                PkGameCard("Batman: Arkham Knight", "Action adventure")
            }
            Spacer(Modifier.height(22.dp))

            // ---- Contact ----
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "Contact",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PkInk
                )
            }
            Spacer(Modifier.height(10.dp))
            PkCard {
                PkContactRow(
                    icon = Icons.Filled.Place,
                    text = "Indore, India"
                )
                PkDivider()
                PkContactRow(
                    icon = Icons.Filled.ChatBubbleOutline,
                    text = "DMs open on Instagram @frkurup",
                    onClick = { uri.openUri("https://instagram.com/frkurup") }
                )
                PkDivider()
                PkContactRow(
                    icon = Icons.Filled.People,
                    text = "Open to collaborate"
                )
                Spacer(Modifier.height(8.dp))
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
            .padding(vertical = 16.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(logo),
            contentDescription = label,
            modifier = Modifier.size(46.dp),
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
private fun PkLangChips(items: List<Pair<String, List<Color>>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var row = mutableListOf<Pair<String, List<Color>>>()
        for (item in items) {
            row.add(item)
            if (row.size == 2 || item == items.last()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((name, dots) in row) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(PkPill)
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (dot in dots) {
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .background(dot)
                                )
                                Spacer(Modifier.width(3.dp))
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = PkInk
                            )
                        }
                    }
                }
                row = mutableListOf()
            }
        }
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

@Composable
private fun PkGameCard(title: String, genre: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(18.dp), spotColor = Color(0x1A5C2E2E))
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PkPill),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.first().toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = PkCoralDeep
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = PkInk)
            Text(text = genre, fontSize = 12.sp, color = PkMuted)
        }
    }
}

@Composable
private fun PkContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = PkMuted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = PkInk)
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
