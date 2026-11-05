package com.opensandbox.dhizuku.channels

import com.opensandbox.dhizuku.core.Outcome
import com.opensandbox.dhizuku.platform.CapabilityProvider

/** 应用管控通道：冻结、禁止卸载。 */
interface PolicyChannel : CapabilityProvider {

    fun isFrozen(packageName: String): Boolean

    fun setFrozen(packageName: String, frozen: Boolean): Outcome<Unit>

    fun isUninstallBlocked(packageName: String): Boolean

    fun setUninstallBlocked(packageName: String, blocked: Boolean): Outcome<Unit>
}
