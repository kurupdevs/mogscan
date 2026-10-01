package com.kurupdevs.moggr.util

import android.content.Context

data class UserProfile(
    val name: String,
    val language: String,
    val heightCm: Int,
    val dobMillis: Long,
    val goal: String,
    // v2.6-science begin
    val ageBracket: String = ""
    // v2.6-science end
)

object ProfileStore {
    private const val PREFS = "mogscan_profile"

    fun save(context: Context, profile: UserProfile) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("name", profile.name)
            .putString("language", profile.language)
            .putInt("heightCm", profile.heightCm)
            .putLong("dobMillis", profile.dobMillis)
            .putString("goal", profile.goal)
            // v2.6-science begin
            .putString("ageBracket", profile.ageBracket)
            // v2.6-science end
            .apply()
    }

    fun load(context: Context): UserProfile? {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = p.getString("name", null) ?: return null
        val dob = p.getLong("dobMillis", 0L).takeIf { it > 0 } ?: return null
        return UserProfile(
            name = name,
            language = p.getString("language", "English") ?: "English",
            heightCm = p.getInt("heightCm", 0),
            dobMillis = dob,
            goal = p.getString("goal", "") ?: "",
            // v2.6-science begin
            ageBracket = p.getString("ageBracket", "") ?: ""
            // v2.6-science end
        )
    }

    fun ageYears(dobMillis: Long): Int {
        val now = java.util.Calendar.getInstance()
        val dob = java.util.Calendar.getInstance().apply { timeInMillis = dobMillis }
        var age = now.get(java.util.Calendar.YEAR) - dob.get(java.util.Calendar.YEAR)
        if (now.get(java.util.Calendar.DAY_OF_YEAR) < dob.get(java.util.Calendar.DAY_OF_YEAR)) age--
        return age.coerceAtLeast(0)
    }
}
