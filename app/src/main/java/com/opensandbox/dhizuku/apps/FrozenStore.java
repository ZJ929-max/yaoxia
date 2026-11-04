package com.opensandbox.dhizuku.apps;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/**
 * 记住"哪些包是被我们冻结的"。
 * 用途：应用被冻结后会从启动器消失，没有这份记录就无法把它解冻回来。
 */
public final class FrozenStore {

    private static final String PREFERENCES_NAME = "open_sandbox_state";
    private static final String KEY_FROZEN = "frozen_packages";

    private final SharedPreferences preferences;

    public FrozenStore(Context context) {
        this.preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public Set<String> load() {
        return new HashSet<>(preferences.getStringSet(KEY_FROZEN, new HashSet<>()));
    }

    public void save(Set<String> packageNames) {
        preferences.edit().putStringSet(KEY_FROZEN, new HashSet<>(packageNames)).apply();
    }
}
