package com.aether.android.data

import android.content.Context

/**
 * Stores the user's own Groq API key locally, in this app's private
 * SharedPreferences (sandboxed per-app by Android, not readable by other
 * apps without root). Not encrypted-at-rest yet — that's a reasonable
 * hardening step later (androidx.security EncryptedSharedPreferences),
 * skipped for now to avoid pulling in a dependency that can't be verified
 * in this environment.
 */
class ApiKeyStore(context: Context) {

    private val prefs = context.getSharedPreferences("aether_secrets", Context.MODE_PRIVATE)

    fun getGroqKey(): String? = prefs.getString(KEY_GROQ, null)?.takeIf { it.isNotBlank() }

    fun setGroqKey(key: String?) {
        prefs.edit().apply {
            if (key.isNullOrBlank()) remove(KEY_GROQ) else putString(KEY_GROQ, key.trim())
        }.apply()
    }

    private companion object {
        const val KEY_GROQ = "groq_api_key"
    }
}
