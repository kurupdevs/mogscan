package com.kurupdevs.moggr.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.ui.theme.EqInk
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.ui.theme.MogCoral
import com.kurupdevs.moggr.ui.theme.MogCoralDark
import com.kurupdevs.moggr.util.PhotoGrade
import com.kurupdevs.moggr.util.PhotoQuality
import com.kurupdevs.moggr.util.PhotoRanker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "Rank my pics": pick photos from the gallery (system picker, no storage
 * permission needed), grade each on-device with [PhotoQuality], order them
 * with [PhotoRanker] (best first), and hand the tapped photo to the normal
 * analysis entry point. Nothing ever leaves the device.
 */
private data class RankedEntry(val bitmap: Bitmap, val grade: PhotoGrade)

@Composable
fun BestPicPicker(
    onPick: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var entries by remember { mutableStateOf<List<RankedEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        loading = true
        scope.launch(Dispatchers.Default) {
            val items = uris.take(12).mapNotNull { uri ->
                val bmp = decodeDownscaled(context, uri, 1024) ?: return@mapNotNull null
                val grade = try {
                    PhotoQuality.grade(bmp)
                } catch (_: Exception) {
                    null
                } ?: return@mapNotNull null
                RankedEntry(bmp, grade)
            }
            val order = try {
                PhotoRanker.rank(items.map { it.bitmap })
            } catch (_: Exception) {
                // fail-open: quality score order if the ranker hiccups
                items.indices.sortedByDescending { items[it].grade.score }
            }
            val ranked = order.mapNotNull { items.getOrNull(it) }
            withContext(Dispatchers.Main) {
                entries = ranked
                loading = false
            }
        }
    }

    // v2.7 reskin: greige background, serif headline, glass rows, coral CTA.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(eqBackgroundBrush())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = EqInk)
            }
            Column(Modifier.weight(1f)) {
                EqHeadline("Rank my pics", size = 22, color = EqInk)
                EqSectionLabel("best first · all on-device")
            }
        }
        Spacer(Modifier.height(12.dp))

        when {
            loading -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MogCoral)
                        Spacer(Modifier.height(12.dp))
                        EqSectionLabel("grading your pics")
                    }
                }
            }

            entries.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Pick a few selfies from your gallery",
                            color = EqMuted,
                            fontSize = 15.sp
                        )
                        Text(
                            "and we'll rank them by photo quality.",
                            color = EqMuted,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        EqCoralPillButton(
                            text = "Pick photos",
                            onClick = { launcher.launch("image/*") }
                        )
                        Spacer(Modifier.height(12.dp))
                        EqSectionLabel("no uploads · stays on your phone")
                    }
                }
            }

            else -> {
                EqSectionLabel("tap any photo to analyze it")
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(entries.withIndex().toList(), key = { it.index }) { (rank, entry) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.55f))
                                .border(1.dp, Color.White.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                                .clickable { onPick(entry.bitmap) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (rank == 0) MogCoral
                                        else EqInk.copy(alpha = 0.08f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "#${rank + 1}",
                                    fontWeight = FontWeight.Bold,
                                    color = if (rank == 0) Color.White else EqInk,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Image(
                                bitmap = entry.bitmap.asImageBitmap(),
                                contentDescription = "Ranked photo #${rank + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${entry.grade.score}/100",
                                    fontWeight = FontWeight.Bold,
                                    color = MogCoralDark,
                                    fontSize = 17.sp
                                )
                                Text(
                                    entry.grade.issues.firstOrNull()
                                        ?: "Clean shot — good to go",
                                    color = EqMuted,
                                    fontSize = 13.sp,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pick more photos",
                    color = MogCoralDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { launcher.launch("image/*") }
                        .padding(8.dp)
                )
            }
        }
    }
}

/** Decodes a content URI downscaled so the long edge is <= [maxDim] px. */
private fun decodeDownscaled(context: Context, uri: Uri, maxDim: Int): Bitmap? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxDim) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    } catch (_: Exception) {
        null
    }
}
