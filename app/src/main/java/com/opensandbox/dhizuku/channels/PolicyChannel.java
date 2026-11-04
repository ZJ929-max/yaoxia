package com.opensandbox.dhizuku.channels;

import com.opensandbox.dhizuku.core.Outcome;
import com.opensandbox.dhizuku.platform.CapabilityProvider;

/** 应用管控通道：冻结、禁止卸载。 */
public interface PolicyChannel extends CapabilityProvider {

    boolean isFrozen(String packageName);

    Outcome<Void> setFrozen(String packageName, boolean frozen);

    boolean isUninstallBlocked(String packageName);

    Outcome<Void> setUninstallBlocked(String packageName, boolean blocked);
}
