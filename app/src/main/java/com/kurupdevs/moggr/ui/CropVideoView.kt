package com.kurupdevs.moggr.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.VideoView

/**
 * VideoView that center-crops the video to completely fill its bounds,
 * so there are no letterbox gaps above/below (or on the sides).
 */
class CropVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : VideoView(context, attrs, defStyleAttr) {

    private var videoW = 0
    private var videoH = 0

    fun setVideoSize(w: Int, h: Int) {
        if (w > 0 && h > 0 && (w != videoW || h != videoH)) {
            videoW = w
            videoH = h
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = android.view.View.MeasureSpec.getSize(widthMeasureSpec)
        val h = android.view.View.MeasureSpec.getSize(heightMeasureSpec)
        if (videoW > 0 && videoH > 0 && w > 0 && h > 0) {
            // Scale so the video covers the whole view (may crop edges, never gaps).
            val scale = maxOf(w.toFloat() / videoW, h.toFloat() / videoH)
            setMeasuredDimension((videoW * scale).toInt(), (videoH * scale).toInt())
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}
