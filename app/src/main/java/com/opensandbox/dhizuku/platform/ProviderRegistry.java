package com.opensandbox.dhizuku.platform;

import android.content.Context;

import com.opensandbox.dhizuku.channels.DhizukuPolicyChannel;
import com.opensandbox.dhizuku.channels.ShizukuShellChannel;
import com.opensandbox.dhizuku.delegation.DelegationScopes;

import java.util.Arrays;
import java.util.List;

/**
 * ★ 兼容层唯一的注册表 ★
 * <p>
 * 要接入新的能力提供方（新的权限授予器、新的 shell 实现），只改这个文件：
 * 在 {@link #providers(Context)} 里加一行，其余代码一行不动。
 * 顺序 = 优先级：越靠前越先被路由选中。
 */
public final class ProviderRegistry {

    private static final String PACKAGE_DHI = "com.rosan.dhizuku";
    private static final String PACKAGE_SHE = "com.hamondev.shevery";

    private ProviderRegistry() {
    }

    public static List<CapabilityProvider> providers(Context context) {
        return Arrays.asList(
                new DhizukuPolicyChannel(context, ProviderId.SHE, PACKAGE_SHE, DelegationScopes.ALL),
                new DhizukuPolicyChannel(context, ProviderId.DHI, PACKAGE_DHI, DelegationScopes.ALL),
                new ShizukuShellChannel(context)
        );
    }
}
