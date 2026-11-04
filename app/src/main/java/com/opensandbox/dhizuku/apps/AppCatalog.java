package com.opensandbox.dhizuku.apps;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 只负责"有哪些应用"，不关心它们的管控状态。 */
public final class AppCatalog {

    public List<AppEntry> load(PackageManager packageManager, String excludedPackage) {
        Map<String, String> launchable = new LinkedHashMap<>();

        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> resolved = packageManager.queryIntentActivities(intent, 0);
        if (resolved != null) {
            for (ResolveInfo info : resolved) {
                if (info.activityInfo == null) {
                    continue;
                }
                String packageName = info.activityInfo.packageName;
                if (packageName.equals(excludedPackage)) {
                    continue;
                }
                launchable.put(packageName, String.valueOf(info.loadLabel(packageManager)));
            }
        }

        List<AppEntry> entries = new ArrayList<>();
        for (Map.Entry<String, String> item : launchable.entrySet()) {
            entries.add(new AppEntry(item.getKey(), item.getValue()));
        }
        entries.sort((left, right) -> String.CASE_INSENSITIVE_ORDER.compare(left.label, right.label));
        return entries;
    }
}
