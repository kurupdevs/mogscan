package com.kurupdevs.moggr.coach

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Keyless chat client for Moggr's Looksmaxing AI (Moggr Coach).
 * Uses the free Pollinations text API (no API key, no account).
 * The API is best-effort: [ask] calls back with null on any failure and the UI
 * must show a graceful offline fallback. `private=true` keeps every
 * conversation off the public feed.
 */
object CoachClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val main = Handler(Looper.getMainLooper())

    @Volatile
    private var lastCallMs = 0L

    private fun enc(s: String): String =
        URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    /**
     * @param history list of (isUser, text) pairs, oldest first.
     * @param cb invoked on the main thread with the coach reply, or null on failure.
     */
    fun ask(
        system: String,
        history: List<Pair<Boolean, String>>,
        newMessage: String,
        cb: (String?) -> Unit
    ) {
        Thread {
            // Anonymous tier ≈ 1 req / 5 s: pace client-side.
            val wait = 6000L - (System.currentTimeMillis() - lastCallMs)
            if (wait > 0) {
                try {
                    Thread.sleep(wait)
                } catch (_: InterruptedException) {
                    main.post { cb(null) }
                    return@Thread
                }
            }
            lastCallMs = System.currentTimeMillis()

            val transcript = buildString {
                append("Conversation so far:\n")
                for ((isUser, text) in history.takeLast(10)) {
                    append(if (isUser) "User: " else "Coach: ")
                    append(text.trim().take(500)).append('\n')
                }
                append("User: ").append(newMessage.trim().take(800))
                append("\nCoach:")
            }
            val url = "https://text.pollinations.ai/${enc(transcript)}" +
                "?model=openai-fast" +
                "&system=${enc(system)}" +
                "&private=true" +
                "&referrer=moggr"
            val reply: String? = try {
                val req = Request.Builder().url(url).get().build()
                http.newCall(req).execute().use { resp ->
                    val body = resp.body?.string()?.trim()
                    if (!resp.isSuccessful || body.isNullOrBlank()) {
                        null
                    } else if (body.contains("\"error\"")) {
                        // API error payload, not a chat reply.
                        null
                    } else {
                        body
                    }
                }
            } catch (_: Exception) {
                null
            }
            main.post { cb(reply) }
        }.start()
    }

    /**
     * @param memoryContext 2–4 lines of what the coach remembers about the user
     * (from [com.kurupdevs.moggr.util.CoachMemory.getMemoryContext]); blank means
     * nothing is remembered yet. Kept optional so older call sites keep working.
     */
    // v2.6-science: teen mode softens the framing for 13–17 users
    // v2.6-hinglish: hinglish param switches the coach to Hinglish bade-bhai register
    fun systemPrompt(
        userName: String,
        metricsContext: String,
        memoryContext: String = "",
        teen: Boolean = false,
        hinglish: Boolean = false
    ): String {
        // v2.6-hinglish: when hi mode is on, the coach replies in Hinglish,
        // bade-bhai register — same rules, same structure, different language.
        val langBlock = if (hinglish) """
        LANGUAGE (hard rule — follow it for every reply):
        Reply ONLY in Hinglish: Hindi written in Roman/English script, the way
        Indian Gen-Z actually talks ("bhai sun", "scene ye hai", "sahi kar raha hai").
        Warm elder-brother (bade bhai) register: direct and caring, zero lecture
        tone, zero corporate speak. Keep the same reply structure (verdict →
        why → top 3 fixes → what NOT to worry about) and the under-150-words
        limit. Never slip back into full English mid-reply.
        """.trimIndent() else ""
        val base = """
        You are Moggr's Looksmaxing AI — the in-app looksmaxxing coach (Moggr Coach).
        Blunt older-brother energy, Gen-Z register, zero corporate speak. Honest first,
        kind second. Never cruel about things the user can't change.

        YOUR POSTURE (grey pill): looks matter and genetics set the starting point —
        but effort moves plenty. You NEVER say "it's over", never joke about rope/LDAR,
        never validate defeatism. If a user talks blackpill fatalism, acknowledge the
        kernel of truth (bone structure is genetic; one photo is one moment in time),
        then redirect to what they control. If anyone expresses hopelessness or
        self-harm, drop the blunt tone entirely and take it seriously — tell them to
        talk to someone they trust or a professional. That overrides everything.

        RULES (never break these):
        - Every answer follows this structure: verdict (one line) → why (which measured
          features explain it) → top 3 fixes in priority order → what NOT to worry about.
        - Softmaxxing ONLY: fat loss/debloat, skincare, haircut for face shape,
          beard/grooming, brows, teeth, posture, sleep, gym/neck, fitted style,
          better photos/lighting.
        - NEVER recommend surgery or any medical procedure. For structural issues say
          "that's a consult-a-professional thing" and focus on what's in their control.
        - Never present mewing, hard chewing or bonesmashing as proven science.
          No medical advice, no diagnoses, ever. No pharma recommendations.
        - Fixing a failo beats adding a halo — prioritize the weakest measured feature.
        - Tip priority: leanness/debloat → skin → hair → brows/eye area → teeth →
          posture → neck → coloring.
        - Never promise point gains ("do X for +1 PSL") — that's community folklore,
          not a guarantee.
        - Speak PSL-native: LTN / MTN / HTN / Chadlite / Chad, halos and failos,
          "ascend", softmaxxing roadmap. Parse pill talk right: bluepill (looks don't
          matter cope), redpill (looks matter, level up), blackpill (genetics
          fatalism) — mirror the language, never the fatalism.
        - Use anatomical precision like experienced raters: ramus height vs tilt,
          gonial angle, chin projection, maxilla projection, zygo arc, alar base
          width, infraorbitals / negative vector, midface compactness, canthal tilt.
          Name the structure, not just the vibe.
        - Frame every rating as a photo-dependent estimate: lighting, lens, pose and
          angle change the read. Never destiny.
        - The 1-8 PSL numbers are looksmaxxing-community conventions, not validated
          science. Say so when asked; never present them as medical truth.
        - Keep replies under 150 words unless the user asks for detail.

        The user's face was measured ON-DEVICE by Moggr (PSL scale 1.0–8.0, real
        community tiers — no sugarcoating):
        $metricsContext
        User's name: $userName. Use it occasionally, not every message.
        ${if (teen) """
        TEEN MODE: this user is 13–17. Soften your framing: say "glow-up", not "mog"/
        "mogging" or dominance talk. No blackpill fatalism, no competitive ranking
        language — extra care with self-image. Be warm, encouraging, and fun; push
        healthy habits (sleep, skincare, posture, fitness, style), never fatalism.
        """ else ""}
        $langBlock
    """.trimIndent()
        return if (memoryContext.isBlank()) {
            base
        } else {
            base + "\n\nWhat you remember about the user (from earlier in-app " +
                "activity — don't restate it all, just use it to be more specific):\n" +
                memoryContext.trim()
        }
    }

    fun reportContext(
        overallPsl: Double,
        features: List<Pair<String, Double>>,
        anglesRead: Int,
        decile: Double = 0.0,
        tier: String = "",
        failos: Int = 0,
        halos: Int = 0,
        pillars: List<Pair<String, Double>> = emptyList()
    ): String {
        val feats = features.joinToString(", ") { (name, score) ->
            "$name ${String.format(Locale.US, "%.1f", score)}"
        }
        val pillarStr = if (pillars.isNotEmpty()) {
            " Pillars: " + pillars.joinToString(", ") { (name, score) ->
                "$name ${String.format(Locale.US, "%.1f", score)}"
            } + "."
        } else ""
        val tierStr = if (tier.isNotBlank()) ", $tier" else ""
        val decileStr = if (decile > 0) " (≈${String.format(Locale.US, "%.1f", decile)}/10 decile)" else ""
        return "Overall ${String.format(Locale.US, "%.1f", overallPsl)} PSL$decileStr$tierStr, " +
            "read from $anglesRead/3 angles. $failos failos, $halos halos.$pillarStr " +
            "Feature scores: $feats."
    }
}
