package com.opensandbox.dhizuku.ui;

/** 列表里的一行：应用 + 它当前的状态。 */
public final class AppRow {

    public final String packageName;
    public final String label;
    public final boolean frozen;
    public final boolean uninstallBlocked;

    public AppRow(String packageName, String label, boolean frozen, boolean uninstallBlocked) {
        this.packageName = packageName;
        this.label = label;
        this.frozen = frozen;
        this.uninstallBlocked = uninstallBlocked;
    }
}
