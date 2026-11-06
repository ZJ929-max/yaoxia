package com.opensandbox.dhizuku.apps

import android.content.Intent
import android.content.pm.PackageManager
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * 应用清单加载：
 * - 标签并行解析（4 线程），避免 200+ 次串行 PackageManager 查询；
 * - 标签按包名缓存，二次加载只补新增/变更项；
 * - force=true 时清缓存重扫。
 */
class AppCatalog {

    private val pool = Executors.newFixedThreadPool(4) { r ->
        Thread(r, "catalog").apply { isDaemon = true }
    }
    private val labelCache = HashMap<String, String>()

    @Synchronized
    fun load(packageManager: PackageManager, excludedPackage: String, force: Boolean = false): List<AppEntry> {
        if (force) labelCache.clear()

        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val infos = packageManager.queryIntentActivities(intent, 0)
            .mapNotNull { it.activityInfo }
            .filter { it.packageName != excludedPackage }
            .distinctBy { it.packageName }

        val missing = infos.filter { it.packageName !in labelCache }
        if (missing.isNotEmpty()) {
            val tasks = missing.map { info ->
                Callable {
                    val label = runCatching { info.loadLabel(packageManager).toString() }
                        .getOrDefault(info.packageName)
                    info.packageName to label
                }
            }
            pool.invokeAll(tasks).forEach { future ->
                runCatching { future.get() }.getOrNull()?.let { (pkg, label) -> labelCache[pkg] = label }
            }
        }

        return infos.map { AppEntry(it.packageName, labelCache[it.packageName] ?: it.packageName) }
            .sortedBy { it.label.lowercase() }
    }
}
