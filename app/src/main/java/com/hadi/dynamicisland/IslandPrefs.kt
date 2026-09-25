package com.hadi.dynamicisland

import android.content.Context

object IslandPrefs {
    private const val PREFS_NAME = "island_prefs"
    private const val KEY_SERVICE_ENABLED = "service_enabled"
    private const val KEY_SUPPRESS_DUPLICATES = "suppress_duplicates"
    private const val KEY_KEEP_PRIORITY = "keep_priority"
    private const val KEY_SHOW_MEDIA = "show_media"
    private const val KEY_SHOW_CHARGING = "show_charging"
    private const val KEY_SHOW_TIMER = "show_timer"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isServiceEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SERVICE_ENABLED, false)

    fun setServiceEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
    }

    fun suppressDuplicates(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SUPPRESS_DUPLICATES, true)

    fun setSuppressDuplicates(context: Context, suppress: Boolean) {
        prefs(context).edit().putBoolean(KEY_SUPPRESS_DUPLICATES, suppress).apply()
    }

    fun keepPriority(context: Context): Boolean =
        prefs(context).getBoolean(KEY_KEEP_PRIORITY, true)

    fun setKeepPriority(context: Context, keep: Boolean) {
        prefs(context).edit().putBoolean(KEY_KEEP_PRIORITY, keep).apply()
    }

    fun showMedia(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_MEDIA, true)

    fun setShowMedia(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_MEDIA, show).apply()
    }

    fun showCharging(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_CHARGING, true)

    fun setShowCharging(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_CHARGING, show).apply()
    }

    fun showTimer(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SHOW_TIMER, true)

    fun setShowTimer(context: Context, show: Boolean) {
        prefs(context).edit().putBoolean(KEY_SHOW_TIMER, show).apply()
    }
}
