package com.kurupdevs.moggr.analysis

/**
 * Frontal face-shape classifier from width/height ratios.
 * Rules of thumb from community rating conventions — a rough visual read,
 * not a bone measurement. Returns (shape, note).
 */
object FaceShape {

    fun classifyFaceShape(
        cheekW: Float,
        jawW: Float,
        foreheadW: Float,
        faceH: Float
    ): Pair<String, String> {
        if (cheekW <= 0f || faceH <= 0f) {
            return "Unknown" to "Couldn't measure face shape from this photo."
        }
        val hWRatio = faceH / cheekW
        val jawRatio = if (jawW > 0f) jawW / cheekW else 0.85f
        val hasForehead = foreheadW > 0f
        val cheekForeRatio = if (hasForehead) cheekW / foreheadW else 1f
        return when {
            hWRatio > 1.5f ->
                "Oblong" to "Oblong — longer than it is wide; width at the sides balances it."
            jawRatio > 0.95f && cheekForeRatio in 0.95f..1.05f ->
                if (hWRatio < 1.35f)
                    "Square" to "Square — forehead, cheekbones and jaw all read a similar width."
                else
                    "Round" to "Round — full through the cheeks with softer angles."
            hasForehead && foreheadW > cheekW * 1.05f && jawW < cheekW * 0.9f ->
                "Heart" to "Heart — wider forehead tapering to a narrower chin."
            hasForehead && cheekW > foreheadW * 1.08f && cheekW > jawW * 1.08f ->
                "Diamond" to "Diamond — cheekbones are the widest point."
            else ->
                "Oval" to "Oval — balanced thirds, jaw slightly narrower than cheekbones."
        }
    }
}
