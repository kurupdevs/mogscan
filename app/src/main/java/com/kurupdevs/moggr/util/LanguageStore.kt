package com.kurupdevs.moggr.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * v2.6: full-app language setting — English ("en") or Hinglish ("hi").
 * Persisted in SharedPreferences; exposed as Compose snapshot state so the
 * whole UI recomposes when the user flips the EN/HI toggle.
 */
object LanguageStore {
    const val EN = "en"
    const val HI = "hi"

    private const val PREFS = "moggr_language"
    private const val KEY = "lang"

    var isHinglish: Boolean by mutableStateOf(false)
        private set

    /** Load the saved language. Call once at app start (before any screen). */
    fun load(context: Context) {
        val code = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, EN) ?: EN
        isHinglish = code == HI
    }

    /** Save + publish a new language. `hi = true` means Hinglish. */
    fun set(context: Context, hi: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, if (hi) HI else EN)
            .apply()
        isHinglish = hi
    }
}
