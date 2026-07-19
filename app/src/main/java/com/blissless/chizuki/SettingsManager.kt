package com.blissless.chizuki

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists user-facing app settings (no AniList — Chizuki is local-only).
 *
 * Keys:
 *   - check_updates_on_start  (Boolean, default true)  — silent GitHub release check on app launch
 *   - selected_extension_authority (String?, default null) — ContentProvider authority of the
 *                                                             currently-active streaming extension
 *   - streaming_method (String, default "exoplayer") — "exoplayer" or "iframe"
 */
class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "Chizuki_settings"
        private const val KEY_CHECK_UPDATES = "check_updates_on_start"
        private const val KEY_SELECTED_EXTENSION = "selected_extension_authority"
        private const val KEY_STREAMING_METHOD = "streaming_method"

        private const val DEFAULT_CHECK_UPDATES = true
        const val STREAMING_METHOD_EXOPLAYER = "exoplayer"
        const val STREAMING_METHOD_IFRAME = "iframe"
    }

    fun getCheckUpdatesOnStart(): Boolean =
        prefs.getBoolean(KEY_CHECK_UPDATES, DEFAULT_CHECK_UPDATES)

    fun setCheckUpdatesOnStart(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_CHECK_UPDATES, enabled).apply()
    }

    fun getSelectedExtensionAuthority(): String? =
        prefs.getString(KEY_SELECTED_EXTENSION, null)

    fun setSelectedExtensionAuthority(authority: String?) {
        prefs.edit().putString(KEY_SELECTED_EXTENSION, authority).apply()
    }

    fun getStreamingMethod(): String =
        prefs.getString(KEY_STREAMING_METHOD, STREAMING_METHOD_EXOPLAYER)
            ?: STREAMING_METHOD_EXOPLAYER

    fun setStreamingMethod(method: String) {
        prefs.edit().putString(KEY_STREAMING_METHOD, method).apply()
    }
}
