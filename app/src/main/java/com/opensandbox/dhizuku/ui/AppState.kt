package com.opensandbox.dhizuku.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.opensandbox.dhizuku.BuildConfig
import com.opensandbox.dhizuku.R
import com.opensandbox.dhizuku.apps.AppCatalog
import com.opensandbox.dhizuku.apps.FrozenStore
import com.opensandbox.dhizuku.core.AsyncTaskRunner
import com.opensandbox.dhizuku.core.Outcome
import com.opensandbox.dhizuku.platform.CapabilityRouter
import com.opensandbox.dhizuku.platform.ProviderRegistry
import com.opensandbox.dhizuku.provision.DhizukuProfileProvisioner
import com.opensandbox.dhizuku.provision.ShellProfileProvisioner

typealias AppUi = AppsFlow.AppUi

/** 外观预设，只影响配色，不影响布局。 */
enum class Skin { SYSTEM, MATERIAL, MIUI }

/**
 * 组装层：把三块流程（连接 / 应用 / 隔离空间）接起来，只做转发与全局状态。
 * 具体逻辑分别在 ConnectionFlow、AppsFlow、ProfileFlow 里。
 */
class AppState(private val context: Context) {

    private val async = AsyncTaskRunner("open-sandbox-io")
    private val router = CapabilityRouter(ProviderRegistry.providers(context))
    private val catalog = AppCatalog()
    private val frozenStore = FrozenStore(context)

    var status by mutableStateOf(context.getString(R.string.status_connecting))
    var skin by mutableStateOf(Skin.MATERIAL)
    var glassEnabled by mutableStateOf(true)

    var pendingLaunch by mutableStateOf<AppUi?>(null)
    var pendingSwitchBack by mutableStateOf(false)

    private val connectionFlow: ConnectionFlow by lazy {
        ConnectionFlow(context, async, router, { status = it }, { appsFlow.refresh() })
    }

    private val appsFlow: AppsFlow by lazy {
        AppsFlow(
            context,
            async,
            catalog,
            frozenStore,
            { connectionFlow.control },
            { connectionFlow.policyChannels },
            { status = it },
        )
    }

    private val profileFlow: ProfileFlow by lazy {
        ProfileFlow(context, async, { connectionFlow.shell }) { status = it }
    }

    // ------------------------------------------------------------------ 只读转发

    val session: String get() = connectionFlow.session
    val channels: List<ConnectionFlow.ChannelUi> get() = connectionFlow.channels
    val scopes: Set<String> get() = connectionFlow.scopes
    val busy: Boolean get() = connectionFlow.busy
    val shellLabel: String get() = connectionFlow.shellLabel

    val apps: List<AppUi> get() = appsFlow.apps
    val busyPackages: Set<String> get() = appsFlow.busyPackages

    val profileUserId: Int? get() = profileFlow.userId
    val profileApps: List<String> get() = profileFlow.packages
    val profileBusy: Boolean get() = profileFlow.busy

    // ------------------------------------------------------------------ 动作转发

    fun connect() = connectionFlow.connect()

    fun refreshChannels() = connectionFlow.refreshChannels()

    fun requestScopes() = connectionFlow.requestScopes()

    fun refreshApps() = appsFlow.refresh()

    fun freezeBatch(packages: Set<String>, target: Boolean) = appsFlow.setFrozenBatch(packages, target)

    fun installBatch(packages: Set<String>) {
        packages.forEach { profileFlow.install(it) }
    }

    fun setFrozen(app: AppUi, target: Boolean) = appsFlow.setFrozen(app, target)

    fun toggleUninstallBlock(app: AppUi) = appsFlow.toggleUninstallBlock(app)

    fun refreshProfileApps() = profileFlow.refresh()

    fun installIntoProfile(packageName: String) = profileFlow.install(packageName)

    fun removeFromProfile(packageName: String) = profileFlow.remove(packageName)

    fun launchInProfile(packageName: String) = profileFlow.launch(packageName)

    fun switchToOwner() = profileFlow.switchBack()

    // ------------------------------------------------------------------ 打开前的确认

    fun requestLaunchInProfile(app: AppUi) {
        pendingLaunch = app
    }

    fun confirmPendingLaunch() {
        val app = pendingLaunch ?: return
        pendingLaunch = null
        launchInProfile(app.packageName)
    }

    fun cancelPendingLaunch() {
        pendingLaunch = null
    }

    fun requestSwitchToOwner() {
        pendingSwitchBack = true
    }

    fun confirmSwitchToOwner() {
        pendingSwitchBack = false
        switchToOwner()
    }

    fun cancelSwitchToOwner() {
        pendingSwitchBack = false
    }

    // ------------------------------------------------------------------ 建隔离空间

    /**
     * 先试 DO 的 binder（能建出真·工作资料），失败自动回落 shell（建完整次级用户）。
     * 两条都失败才报错，并把两边原因都写出来。
     */
    fun createProfile() {
        status = context.getString(R.string.status_profile_creating)
        async.execute({
            val name = ShellProfileProvisioner.DEFAULT_PROFILE_NAME

            val viaDhi = if (connectionFlow.delegation != null) {
                DhizukuProfileProvisioner(context).ensureManagedProfile(name)
            } else {
                null
            }
            if (viaDhi?.isSuccess == true) return@execute viaDhi

            val channel = connectionFlow.shell
                ?: return@execute (
                    viaDhi ?: Outcome.fail(
                        context.getString(R.string.status_shell_unavailable, "没有可用通道"),
                    )
                    )

            val viaShell = ShellProfileProvisioner(context, channel).ensureManagedProfile(name)
            if (viaShell.isSuccess) {
                viaShell
            } else {
                Outcome.fail(
                    listOfNotNull(viaDhi?.errorOrNull(), viaShell.errorOrNull())
                        .joinToString("  /  "),
                )
            }
        }, { outcome ->
            status = if (outcome.isSuccess) {
                val info = outcome.valueOrNull()!!
                context.getString(
                    if (info.createdNow) R.string.status_profile_created else R.string.status_profile_exists,
                    info.userId,
                    info.name,
                )
            } else {
                context.getString(R.string.status_profile_failed, outcome.errorOrNull().orEmpty())
            }
        })
    }

    // ------------------------------------------------------------------ 诊断

    private val managers = listOf(
        "com.hamondev.shevery" to "she",
        "moe.shizuku.privileged.api" to "shi",
        "com.rosan.dhizuku" to "dhi",
        "frb.axeron.manager" to "ax(经 shi 兼容层可用)",
    )

    fun installedManagers(): List<String> = managers.mapNotNull { (pkg, label) ->
        if (runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess) label else null
    }

    fun diagnosticsReport(): String = buildString {
        appendLine("version=${BuildConfig.VERSION_NAME}")
        appendLine("installed=${installedManagers().joinToString(",")}")
        appendLine(session)
        channels.forEach { appendLine("${it.name} available=${it.available}") }
        appendLine("shell in use=$shellLabel")
        appendLine(status)
        appendLine("log=${logFile()?.absolutePath ?: "无"}")
    }

    fun logFile(): java.io.File? = runCatching {
        java.io.File(context.filesDir, "crash-last.txt").takeIf { it.exists() }
    }.getOrNull()

    fun shutdown() = async.shutdown()
}
