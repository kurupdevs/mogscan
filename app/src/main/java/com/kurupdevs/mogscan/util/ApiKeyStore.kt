package com.kurupdevs.mogscan.util

import android.content.Context

/** Stores the user's own Gemini API key on-device. The key never leaves the
 *  phone except in direct HTTPS calls to Google's Generative AI API. */
object ApiKeyStore {
    private const val PREFS = "mogscan_prefs"
    private const val KEY = "gemini_api_key"

    fun get(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null)
            ?.takeIf { it.isNotBlank() }

    fun save(context: Context, key: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, key.trim())
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY)
            .apply()
    }
}
