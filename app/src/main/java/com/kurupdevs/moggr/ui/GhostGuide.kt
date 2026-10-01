package com.kurupdevs.moggr.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.moggr.ui.theme.EqMuted
import com.kurupdevs.moggr.util.ReportStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ghost overlay for rescans: renders the user's last scan photo faintly
 * behind the viewfinder so they can line the new scan up with the old one.
 * Everything stays on-device; the photo is downscaled (max 512px) to stay
 * OOM-safe.
 */

/** Loads the last saved scan photo downscaled to [maxDim] px on its long edge. Null if none. */
suspend fun loadGhostBitmap(context: Context, maxDim: Int = 512): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            val full = ReportStore.loadPhoto(context) ?: return@withContext null
            val scale = (maxOf(full.width, full.height) / maxDim.toFloat()).coerceAtLeast(1f)
            val w = (full.width / scale).toInt().coerceAtLeast(1)
            val h = (full.height / scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(full, w, h, true)
        } catch (_: Exception) {
            null
        }
    }

@Composable
fun GhostOverlay(
    bitmap: Bitmap?,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!visible || bitmap == null) return
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Your last scan, shown faintly as a guide",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize(0.72f)
                .alpha(0.22f)
        )
        // v2.7 reskin: glass caption chip instead of the raw caps label.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.55f))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Line up with your last scan".uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = EqMuted
            )
        }
    }
}
