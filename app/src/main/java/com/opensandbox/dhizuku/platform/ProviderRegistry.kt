package com.opensandbox.dhizuku.platform

import android.content.Context
import com.opensandbox.dhizuku.channels.DhizukuPolicyChannel
import com.opensandbox.dhizuku.channels.SheveryShellChannel
import com.opensandbox.dhizuku.channels.ShizukuShellChannel
import com.opensandbox.dhizuku.delegation.DelegationScopes
import com.opensandbox.dhizuku.delegation.DhizukuProtocol

/**
 * ★ 兼容层唯一的注册表 ★
 *
 * 要接入新的能力提供方（新的权限授予器、新的 shell 实现），只改这个文件：
 * 在 [providers] 里加一行，其余代码一行不动。
 * 顺序 = 优先级：越靠前越先被路由选中。
 */
object ProviderRegistry {

    private const val PACKAGE_SHE = "com.hamondev.shevery"

    fun providers(context: Context): List<CapabilityProvider> = listOf(
        DhizukuPolicyChannel(context, ProviderId.SHE, PACKAGE_SHE, DelegationScopes.ALL),
        DhizukuPolicyChannel(context, ProviderId.DHI, DhizukuProtocol.OFFICIAL_PACKAGE, DelegationScopes.ALL),
        SheveryShellChannel(context),
        ShizukuShellChannel(context)
    )
}
