package com.opensandbox.dhizuku.apps

import com.opensandbox.dhizuku.channels.PolicyChannel
import com.opensandbox.dhizuku.core.Outcome

/** 把"管控动作"转交给路由选中的策略通道，自身不含任何系统调用。 */
class PolicyAppControl(private val channel: PolicyChannel) : AppControl {

    override fun isFrozen(packageName: String): Boolean = channel.isFrozen(packageName)

    override fun setFrozen(packageName: String, frozen: Boolean): Outcome<Unit> =
        channel.setFrozen(packageName, frozen)

    override fun isUninstallBlocked(packageName: String): Boolean =
        channel.isUninstallBlocked(packageName)

    override fun setUninstallBlocked(packageName: String, blocked: Boolean): Outcome<Unit> =
        channel.setUninstallBlocked(packageName, blocked)
}
