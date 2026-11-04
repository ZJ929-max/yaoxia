package com.opensandbox.dhizuku.apps;

/** 一个可被管控的应用：只描述"是谁"，不描述"状态"。 */
public final class AppEntry {

    public final String packageName;
    public final String label;

    public AppEntry(String packageName, String label) {
        this.packageName = packageName;
        this.label = label;
    }
}
