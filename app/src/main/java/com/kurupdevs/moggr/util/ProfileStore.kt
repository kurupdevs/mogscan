package com.kurupdevs.moggr.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

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
    // v2.8-sec begin: encrypted store uses its own file so the legacy
    // plaintext file can be deleted after a successful migration.
    private const val PREFS_ENC = "mogscan_profile_enc"
    // v2.8-sec end

    // v2.8-sec begin: PII (name + DOB) now lives in EncryptedSharedPreferences.
    // If crypto init ever fails, fail-open to plain prefs so onboarding and
    // launch never break.
    private var usePlainFallback = false
    private var migrated = false

    private fun masterKey(context: Context): MasterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private fun plainPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun prefs(context: Context): SharedPreferences {
        if (usePlainFallback) return plainPrefs(context)
        return try {
            val encrypted = EncryptedSharedPreferences.create(
                context,
                PREFS_ENC,
                masterKey(context),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            migrateIfNeeded(context, encrypted)
            encrypted
        } catch (_: Exception) {
            usePlainFallback = true
            plainPrefs(context)
        }
    }

    /** One-time move of the legacy plaintext profile into the encrypted store. */
    private fun migrateIfNeeded(context: Context, encrypted: SharedPreferences) {
        if (migrated) return
        migrated = true
        try {
            if (encrypted.contains("name")) return // already on encrypted storage
            val legacy = plainPrefs(context)
            if (!legacy.contains("name")) return // nothing to migrate
            val editor = encrypted.edit()
            editor.putString("name", legacy.getString("name", null))
            editor.putString("language", legacy.getString("language", "English"))
            editor.putInt("heightCm", legacy.getInt("heightCm", 0))
            editor.putLong("dobMillis", legacy.getLong("dobMillis", 0L))
            editor.putString("goal", legacy.getString("goal", ""))
            // v2.6-science begin
            editor.putString("ageBracket", legacy.getString("ageBracket", ""))
            // v2.6-science end
            // Only wipe the plaintext file once the encrypted copy is durable.
            if (editor.commit()) {
                context.deleteSharedPreferences(PREFS)
            }
        } catch (_: Exception) {
            // best-effort migration; the profile stays readable from its current store
        }
    }
    // v2.8-sec end

    fun save(context: Context, profile: UserProfile) {
        prefs(context).edit()
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
        val p = prefs(context)
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
