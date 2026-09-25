package com.hadi.dynamicisland

import android.content.Context

object IslandPrefs {
    private const val PREFS_NAME = "island_prefs"
    private const val KEY_SERVICE_ENABLED = "service_enabled"
    private const val KEY_SUPPRESS_DUPLICATES = "suppress_duplicates"
    private const val KEY_KEEP_IN_SHADE = "keep_in_shade"
    private const val KEY_KEEP_PRIORITY = "keep_priority"
    private const val KEY_SHOW_MEDIA = "show_media"
    private const val KEY_SHOW_CHARGING = "show_charging"
    private const val KEY_SHOW_TIMER = "show_timer"
    private const val KEY_AUTO_POSITION = "auto_position"
    private const val KEY_CENTER_OFFSET = "center_offset_dp"
    private const val KEY_TOP_OFFSET = "top_offset_dp"
    private const val KEY_PILL_WIDTH = "pill_width_dp"
    private const val KEY_PILL_HEIGHT = "pill_height_dp"
    private const val KEY_AUTO_CENTER = "auto_center_dp"
    private const val KEY_AUTO_WIDTH = "auto_width_dp"
    private const val KEY_AUTO_HEIGHT = "auto_height_dp"
    private const val KEY_AUTO_TOP = "auto_top_dp"

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

    fun keepInShade(context: Context): Boolean =
        prefs(context).getBoolean(KEY_KEEP_IN_SHADE, true)

    fun setKeepInShade(context: Context, keep: Boolean) {
        prefs(context).edit().putBoolean(KEY_KEEP_IN_SHADE, keep).apply()
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

    fun autoPosition(context: Context): Boolean =
        prefs(context).getBoolean(KEY_AUTO_POSITION, true)

    fun setAutoPosition(context: Context, auto: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_POSITION, auto).apply()
    }

    fun centerOffsetDp(context: Context): Float =
        prefs(context).getFloat(KEY_CENTER_OFFSET, 0f)

    fun setCenterOffsetDp(context: Context, offsetDp: Float) {
        prefs(context).edit().putFloat(KEY_CENTER_OFFSET, offsetDp).apply()
    }

    fun topOffsetDp(context: Context): Float =
        prefs(context).getFloat(KEY_TOP_OFFSET, 8f)

    fun setTopOffsetDp(context: Context, offsetDp: Float) {
        prefs(context).edit().putFloat(KEY_TOP_OFFSET, offsetDp).apply()
    }

    fun pillWidthDp(context: Context): Float =
        prefs(context).getFloat(KEY_PILL_WIDTH, 44f)

    fun setPillWidthDp(context: Context, widthDp: Float) {
        prefs(context).edit().putFloat(KEY_PILL_WIDTH, widthDp).apply()
    }

    fun pillHeightDp(context: Context): Float =
        prefs(context).getFloat(KEY_PILL_HEIGHT, 32f)

    fun setPillHeightDp(context: Context, heightDp: Float) {
        prefs(context).edit().putFloat(KEY_PILL_HEIGHT, heightDp).apply()
    }

    fun setAutoGeometry(
        context: Context,
        centerDp: Float,
        widthDp: Float,
        heightDp: Float,
        topDp: Float
    ) {
        prefs(context).edit()
            .putFloat(KEY_AUTO_CENTER, centerDp)
            .putFloat(KEY_AUTO_WIDTH, widthDp)
            .putFloat(KEY_AUTO_HEIGHT, heightDp)
            .putFloat(KEY_AUTO_TOP, topDp)
            .apply()
    }

    fun autoGeometry(context: Context): FloatArray {
        val values = prefs(context)
        return floatArrayOf(
            values.getFloat(KEY_AUTO_CENTER, 0f),
            values.getFloat(KEY_AUTO_WIDTH, 0f),
            values.getFloat(KEY_AUTO_HEIGHT, 0f),
            values.getFloat(KEY_AUTO_TOP, 0f)
        )
    }
}
