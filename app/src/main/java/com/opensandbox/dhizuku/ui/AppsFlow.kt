package com.opensandbox.dhizuku.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.opensandbox.dhizuku.R
import com.opensandbox.dhizuku.apps.AppCatalog
import com.opensandbox.dhizuku.apps.FrozenStore
import com.opensandbox.dhizuku.core.AsyncTaskRunner
import com.opensandbox.dhizuku.core.Outcome
import com.opensandbox.dhizuku.channels.PolicyChannel

/** 应用列表与冻结/禁卸载。从 AppState 拆出来的第二块。 */
class AppsFlow(
    private val context: Context,
    private val async: AsyncTaskRunner,
    private val catalog: AppCatalog,
    private val frozenStore: FrozenStore,
    private val controlProvider: () -> com.opensandbox.dhizuku.apps.AppControl?,
    private val policyProvider: () -> List<PolicyChannel>,
    private val report: (String) -> Unit,
) {
    var apps by mutableStateOf<List<AppUi>>(emptyList())
        private set
    var busyPackages by mutableStateOf<Set<String>>(emptySet())
        private set

    data class AppUi(
        val packageName: String,
        val label: String,
        val frozen: Boolean,
        val uninstallBlocked: Boolean,
    )

    fun refresh() {
        val policy = controlProvider() ?: return
        async.execute({
            val entries = catalog.load(context.packageManager, context.packageName)
            val remembered = frozenStore.load()

            val built = mutableListOf<AppUi>()
            val frozen = mutableSetOf<String>()
            val seen = mutableSetOf<String>()

            for (entry in entries) {
                seen += entry.packageName
                val isFrozen = entry.packageName in remembered || policy.isFrozen(entry.packageName)
                if (isFrozen) frozen += entry.packageName
                built += AppUi(
                    packageName = entry.packageName,
                    label = entry.label,
                    frozen = isFrozen,
                    uninstallBlocked = policy.isUninstallBlocked(entry.packageName),
                )
            }

            for (packageName in remembered) {
                if (packageName in seen || !policy.isFrozen(packageName)) continue
                frozen += packageName
                built += AppUi(
                    packageName = packageName,
                    label = packageName,
                    frozen = true,
                    uninstallBlocked = policy.isUninstallBlocked(packageName),
                )
            }

            built.sortBy { it.label.lowercase() }
            Outcome.ok(built to frozen)
        }, { outcome ->
            val snapshot = outcome.valueOrNull() ?: return@execute
            apps = snapshot.first
            frozenStore.save(snapshot.second)
        })
    }

    fun setFrozen(app: AppUi, target: Boolean) {
        if (controlProvider() == null || app.packageName in busyPackages) return

        busyPackages = busyPackages + app.packageName
        async.execute({ withFallback { it.setFrozen(app.packageName, target) } }, { outcome ->
            busyPackages = busyPackages - app.packageName
            if (!outcome.isSuccess) {
                report(context.getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty()))
                return@execute
            }
            apps = apps.map { if (it.packageName == app.packageName) it.copy(frozen = target) else it }
            report(
                context.getString(
                    if (target) R.string.toast_frozen else R.string.toast_unfrozen,
                    app.label,
                ),
            )
            refresh()
        })
    }

    fun toggleUninstallBlock(app: AppUi) {
        if (controlProvider() == null) return
        val target = !app.uninstallBlocked
        async.execute({ withFallback { it.setUninstallBlocked(app.packageName, target) } }, { outcome ->
            if (!outcome.isSuccess) {
                report(context.getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty()))
                return@execute
            }
            report(
                context.getString(
                    if (target) R.string.toast_uninstall_blocked else R.string.toast_uninstall_allowed,
                    app.label,
                ),
            )
            refresh()
        })
    }

    /** 一个通道失败就换下一个（she → dhi …）。 */
    private fun withFallback(action: (PolicyChannel) -> Outcome<Unit>): Outcome<Unit> {
        var last: Outcome<Unit> = Outcome.fail("没有可用的策略通道")
        for (channel in policyProvider()) {
            val result = action(channel)
            if (result.isSuccess) return result
            last = result
        }
        return last
    }
}
