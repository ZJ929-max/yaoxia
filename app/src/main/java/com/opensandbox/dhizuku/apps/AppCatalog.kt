package com.opensandbox.dhizuku.apps

import android.content.Intent
import android.content.pm.PackageManager

/** 只负责"有哪些应用"，不关心它们的管控状态。 */
class AppCatalog {

    fun load(packageManager: PackageManager, excludedPackage: String): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        return packageManager.queryIntentActivities(intent, 0)
            .asSequence()
            .mapNotNull { info ->
                val activity = info.activityInfo ?: return@mapNotNull null
                val packageName = activity.packageName
                if (packageName == excludedPackage) return@mapNotNull null
                AppEntry(packageName, info.loadLabel(packageManager).toString())
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
