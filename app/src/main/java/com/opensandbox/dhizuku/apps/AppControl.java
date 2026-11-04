package com.opensandbox.dhizuku.apps;

import com.opensandbox.dhizuku.core.Outcome;

/**
 * 对单个应用执行管控动作的抽象。
 * 界面依赖它，而不是直接依赖 DevicePolicyManager。
 */
public interface AppControl {

    boolean isFrozen(String packageName);

    Outcome<Void> setFrozen(String packageName, boolean frozen);

    boolean isUninstallBlocked(String packageName);

    Outcome<Void> setUninstallBlocked(String packageName, boolean blocked);
}
