package com.opensandbox.dhizuku.delegation;

import android.app.admin.DevicePolicyManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 委派范围的单一事实来源。
 * 以后要加/减范围，只改这里，界面和业务代码都不用动。
 */
public final class DelegationScopes {

    public static final Set<String> PACKAGE_ACCESS = single(DevicePolicyManager.DELEGATION_PACKAGE_ACCESS);

    public static final Set<String> UNINSTALL_BLOCK = single(DevicePolicyManager.DELEGATION_BLOCK_UNINSTALL);

    public static final Set<String> PERMISSION_GRANT = single(DevicePolicyManager.DELEGATION_PERMISSION_GRANT);

    public static final Set<String> APP_RESTRICTIONS = single(DevicePolicyManager.DELEGATION_APP_RESTRICTIONS);

    public static final Set<String> ENABLE_SYSTEM_APP = single(DevicePolicyManager.DELEGATION_ENABLE_SYSTEM_APP);

    /** 全部范围：需要的都从这里取，不在业务代码里拼数组。 */
    public static final Set<String> ALL = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            DevicePolicyManager.DELEGATION_PACKAGE_ACCESS,
            DevicePolicyManager.DELEGATION_BLOCK_UNINSTALL,
            DevicePolicyManager.DELEGATION_PERMISSION_GRANT,
            DevicePolicyManager.DELEGATION_APP_RESTRICTIONS,
            DevicePolicyManager.DELEGATION_ENABLE_SYSTEM_APP
    )));

    private DelegationScopes() {
    }

    private static Set<String> single(String scope) {
        return Collections.singleton(scope);
    }
}
