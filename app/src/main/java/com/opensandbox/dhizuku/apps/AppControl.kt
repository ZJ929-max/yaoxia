package com.opensandbox.dhizuku.apps

import com.opensandbox.dhizuku.core.Outcome

/**
 * 对单个应用执行管控动作的抽象。
 * 界面依赖它，而不是直接依赖 DevicePolicyManager。
 */
interface AppControl {

    fun isFrozen(packageName: String): Boolean

    fun setFrozen(packageName: String, frozen: Boolean): Outcome<Unit>

    fun isUninstallBlocked(packageName: String): Boolean

    fun setUninstallBlocked(packageName: String, blocked: Boolean): Outcome<Unit>
}
