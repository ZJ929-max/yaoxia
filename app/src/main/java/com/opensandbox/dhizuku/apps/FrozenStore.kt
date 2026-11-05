package com.opensandbox.dhizuku.apps

import android.content.Context

/**
 * 记住"哪些包是被我们冻结的"。
 * 用途：应用被冻结后会从启动器消失，没有这份记录就无法把它解冻回来。
 */
class FrozenStore(context: Context) {

    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): Set<String> =
        preferences.getStringSet(KEY_FROZEN, emptySet())?.toSet() ?: emptySet()

    fun save(packageNames: Set<String>) {
        preferences.edit().putStringSet(KEY_FROZEN, packageNames.toSet()).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "open_sandbox_state"
        const val KEY_FROZEN = "frozen_packages"
    }
}
