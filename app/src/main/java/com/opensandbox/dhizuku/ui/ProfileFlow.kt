package com.opensandbox.dhizuku.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.opensandbox.dhizuku.R
import com.opensandbox.dhizuku.channels.ShellChannel
import com.opensandbox.dhizuku.core.AsyncTaskRunner
import com.opensandbox.dhizuku.core.Outcome
import com.opensandbox.dhizuku.provision.ProfileApps
import com.opensandbox.dhizuku.provision.ShellCommands
import com.opensandbox.dhizuku.provision.ShellProfileProvisioner

/**
 * 隔离空间的全部状态与动作。
 * 从 AppState 里拆出来，避免"上帝类"；AppState 只做转发。
 */
class ProfileFlow(
    private val context: Context,
    private val async: AsyncTaskRunner,
    private val shellProvider: () -> ShellChannel?,
    private val report: (String) -> Unit,
) {
    var userId by mutableStateOf<Int?>(null)
        private set

    var packages by mutableStateOf<List<String>>(emptyList())
        private set

    var busy by mutableStateOf(false)
        private set

    private fun profileApps(): ProfileApps? = shellProvider()?.let { ProfileApps(it) }

    fun refresh() {
        val profile = profileApps()
        if (profile == null) {
            report(context.getString(R.string.status_shell_unavailable, "没有可用 shell 通道"))
            return
        }

        busy = true
        async.execute({
            val id = profile.findUser(ShellProfileProvisioner.DEFAULT_PROFILE_NAME).valueOrNull()
                ?: error("还没有隔离空间，先点上面的按钮建一个")
            val installed = profile.installed(id)
            if (!installed.isSuccess) error(installed.errorOrNull().orEmpty())
            Outcome.ok(id to installed.valueOrNull().orEmpty().sorted())
        }, { outcome ->
            busy = false
            val snapshot = outcome.valueOrNull()
            if (snapshot == null) {
                report(context.getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty()))
                return@execute
            }
            userId = snapshot.first
            packages = snapshot.second
            report(context.getString(R.string.profile_apps_count, snapshot.second.size))
        })
    }

    fun install(packageName: String) {
        val profile = profileApps() ?: return
        val id = userId ?: return
        busy = true
        async.execute({ profile.install(id, packageName) }, { outcome ->
            busy = false
            report(
                if (outcome.isSuccess) {
                    context.getString(R.string.profile_installed, packageName, id)
                } else {
                    context.getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty())
                }
            )
            refresh()
        })
    }

    fun remove(packageName: String) {
        val profile = profileApps() ?: return
        val id = userId ?: return
        busy = true
        async.execute({ profile.remove(id, packageName) }, { outcome ->
            busy = false
            report(
                if (outcome.isSuccess) {
                    context.getString(R.string.profile_removed, packageName, id)
                } else {
                    context.getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty())
                }
            )
            refresh()
        })
    }

    /** am start --user 会等目标用户启动，所以"发出去就返回"。 */
    fun launch(packageName: String) {
        val profile = profileApps() ?: return
        val id = userId ?: return
        async.execute({
            val component = profile.resolveComponent(id, packageName)
            if (component != null) {
                profile.launch(id, component)
            } else {
                profile.launchByMonkey(id, packageName)
            }
        }, { outcome ->
            report(
                if (outcome.isSuccess) {
                    context.getString(R.string.profile_launch_requested, packageName)
                } else {
                    context.getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty())
                }
            )
        })
    }

    fun switchBack() {
        val channel = shellProvider() ?: return
        async.execute({
            channel.executeDetached(
                ShellCommands.SHELL,
                ShellCommands.EXECUTE,
                ShellCommands.switchUser(0),
            )
        }, { outcome ->
            report(
                if (outcome.isSuccess) {
                    context.getString(R.string.profile_switch_back_ok)
                } else {
                    context.getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty())
                }
            )
        })
    }
}
