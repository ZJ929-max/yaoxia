package com.opensandbox.dhizuku.apps;

import com.opensandbox.dhizuku.channels.PolicyChannel;
import com.opensandbox.dhizuku.core.Outcome;

/** 把"管控动作"转交给路由选中的策略通道，自身不含任何系统调用。 */
public final class PolicyAppControl implements AppControl {

    private final PolicyChannel channel;

    public PolicyAppControl(PolicyChannel channel) {
        this.channel = channel;
    }

    @Override
    public boolean isFrozen(String packageName) {
        return channel.isFrozen(packageName);
    }

    @Override
    public Outcome<Void> setFrozen(String packageName, boolean frozen) {
        return channel.setFrozen(packageName, frozen);
    }

    @Override
    public boolean isUninstallBlocked(String packageName) {
        return channel.isUninstallBlocked(packageName);
    }

    @Override
    public Outcome<Void> setUninstallBlocked(String packageName, boolean blocked) {
        return channel.setUninstallBlocked(packageName, blocked);
    }
}
