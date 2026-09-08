package com.varedog.reminder

import android.content.Context

object Prefs {

    private const val FILE = "overlay_prefs"
    private const val KEY_BG = "bg_color"
    private const val KEY_TEXT = "text_color"
    private const val KEY_TRANS = "bg_transparent"
    private const val KEY_X = "pos_x"
    private const val KEY_Y = "pos_y"
    private const val KEY_SEC = "close_seconds"
    private const val KEY_SIZE = "text_size"

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

    val PRESET_TEXT_COLORS = intArrayOf(
        0xFFF5F5F5.toInt(),
        0xFF111111.toInt(),
        0xFFFBBF24.toInt(),
        0xFFEF4444.toInt(),
        0xFF22C55E.toInt(),
        0xFF3B82F6.toInt(),
        0xFFEC4899.toInt(),
        0xFF9CA3AF.toInt()
    )

    private fun sp(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun bgColor(context: Context): Int = sp(context).getInt(KEY_BG, 0xE6181818.toInt())
    fun textColor(context: Context): Int = sp(context).getInt(KEY_TEXT, 0xFFF5F5F5.toInt())
    fun bgTransparent(context: Context): Boolean = sp(context).getBoolean(KEY_TRANS, false)
    fun posX(context: Context): Int = sp(context).getInt(KEY_X, 24)
    fun posY(context: Context): Int = sp(context).getInt(KEY_Y, 200)
    fun closeSeconds(context: Context): Int = sp(context).getInt(KEY_SEC, 5)
    fun textSize(context: Context): Int = sp(context).getInt(KEY_SIZE, 15)

    fun save(
        context: Context,
        bg: Int,
        text: Int,
        transparent: Boolean,
        x: Int,
        y: Int,
        seconds: Int,
        size: Int
    ) {
        sp(context).edit()
            .putInt(KEY_BG, bg)
            .putInt(KEY_TEXT, text)
            .putBoolean(KEY_TRANS, transparent)
            .putInt(KEY_X, x)
            .putInt(KEY_Y, y)
            .putInt(KEY_SEC, seconds)
            .putInt(KEY_SIZE, size)
            .apply()
    }
}
