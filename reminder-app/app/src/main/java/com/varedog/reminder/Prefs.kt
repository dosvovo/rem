package com.varedog.reminder

import android.content.Context

object Prefs {

    private const val FILE = "overlay_prefs"
    private const val KEY_BG = "bg_color"
    private const val KEY_TRANS = "bg_transparent"
    private const val KEY_X = "pos_x"
    private const val KEY_Y = "pos_y"
    private const val KEY_SEC = "close_seconds"

    val PRESET_COLORS = intArrayOf(
        0xFF181818.toInt(),
        0xFF1976D2.toInt(),
        0xFF388E3C.toInt(),
        0xFFD81B60.toInt(),
        0xFFF59E0B.toInt(),
        0xFF6A1B9A.toInt(),
        0xFF00838F.toInt(),
        0xFF37474F.toInt()
    )

    private fun sp(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun bgColor(context: Context): Int = sp(context).getInt(KEY_BG, 0xE6181818.toInt())
    fun bgTransparent(context: Context): Boolean = sp(context).getBoolean(KEY_TRANS, false)
    fun posX(context: Context): Int = sp(context).getInt(KEY_X, 24)
    fun posY(context: Context): Int = sp(context).getInt(KEY_Y, 200)
    fun closeSeconds(context: Context): Int = sp(context).getInt(KEY_SEC, 5)

    fun save(context: Context, bg: Int, transparent: Boolean, x: Int, y: Int, seconds: Int) {
        sp(context).edit()
            .putInt(KEY_BG, bg)
            .putBoolean(KEY_TRANS, transparent)
            .putInt(KEY_X, x)
            .putInt(KEY_Y, y)
            .putInt(KEY_SEC, seconds)
            .apply()
    }
}
