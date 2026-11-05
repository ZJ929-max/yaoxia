package com.opensandbox.dhizuku.ui

import android.content.Context
import com.opensandbox.dhizuku.R

/**
 * 外观预设：Material 3 / MIUI / One UI。
 * 只切换主题资源，界面代码不需要知道有几种风格。
 */
object Appearance {

    private const val PREFERENCES = "appearance"
    private const val KEY_THEME = "theme_index"

    private val themes = intArrayOf(
        R.style.Theme_OpenSandbox,
        R.style.Theme_OpenSandbox_Miui,
        R.style.Theme_OpenSandbox_OneUi
    )

    private val labels = intArrayOf(
        R.string.appearance_material,
        R.string.appearance_miui,
        R.string.appearance_oneui
    )

    fun themeRes(context: Context): Int = themes[index(context)]

    fun labelRes(context: Context): Int = labels[index(context)]

    /** 切到下一套风格，返回新的名字资源。 */
    fun cycle(context: Context): Int {
        val next = (index(context) + 1) % themes.size
        preferences(context).edit().putInt(KEY_THEME, next).apply()
        return labels[next]
    }

    private fun index(context: Context): Int =
        preferences(context).getInt(KEY_THEME, 0) % themes.size

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
