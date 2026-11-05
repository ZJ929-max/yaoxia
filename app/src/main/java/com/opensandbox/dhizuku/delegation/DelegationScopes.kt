package com.opensandbox.dhizuku.delegation

import android.app.admin.DevicePolicyManager

/**
 * 委派范围的单一事实来源。
 * 以后要加/减范围，只改这里，界面和业务代码都不用动。
 */
object DelegationScopes {
    val ALL: Set<String> =
        linkedSetOf(
            DevicePolicyManager.DELEGATION_PACKAGE_ACCESS,
            DevicePolicyManager.DELEGATION_BLOCK_UNINSTALL,
            DevicePolicyManager.DELEGATION_PERMISSION_GRANT,
            DevicePolicyManager.DELEGATION_APP_RESTRICTIONS,
            DevicePolicyManager.DELEGATION_ENABLE_SYSTEM_APP,
        )
}
