package com.kurupdevs.moggr.util

import android.content.Context

/**
 * Teen age-mode: softer framing for 13-17 year olds, plus stricter scan limits.
 * A user counts as teen if they picked the "13–17" bracket in onboarding, or if
 * their saved date of birth puts them under 18 and no bracket was chosen.
 */
object TeenMode {
    const val BRACKET_TEEN = "13–17"
    const val BRACKET_ADULT = "18+"

    /** Age-bracket options shown in onboarding. */
    val BRACKETS: List<String> = listOf(BRACKET_TEEN, BRACKET_ADULT)

    fun isTeen(context: Context): Boolean = isTeen(ProfileStore.load(context))

    fun isTeen(profile: UserProfile?): Boolean {
        if (profile == null) return false
        return when (profile.ageBracket) {
            BRACKET_TEEN -> true
            BRACKET_ADULT -> false
            else -> profile.dobMillis > 0 && ProfileStore.ageYears(profile.dobMillis) < 18
        }
    }
}

/**
 * Softer framing strings for teen mode: competitive "mog"/"mogging" language
 * becomes "glow-up" language. Checked in key result + coach strings.
 */
object TeenStrings {
    /** "mog" -> "glow-up" for teens. */
    fun mog(teen: Boolean): String = if (teen) "glow-up" else "mog"

    /** "mogging" -> "glowing up" for teens. */
    fun mogging(teen: Boolean): String = if (teen) "glowing up" else "mogging"

    /** Short roadmaps line, e.g. "3 moves to close it". */
    fun movesLine(teen: Boolean): String =
        if (teen) "3 glow-up moves to close it" else "3 moves to close it"
}
