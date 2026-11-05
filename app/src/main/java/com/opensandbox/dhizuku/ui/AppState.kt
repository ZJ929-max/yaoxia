package com.opensandbox.dhizuku.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.opensandbox.dhizuku.R
import com.opensandbox.dhizuku.apps.AppCatalog
import com.opensandbox.dhizuku.apps.AppControl
import com.opensandbox.dhizuku.apps.FrozenStore
import com.opensandbox.dhizuku.apps.PolicyAppControl
import com.opensandbox.dhizuku.channels.PolicyChannel
import com.opensandbox.dhizuku.channels.ShellChannel
import com.opensandbox.dhizuku.core.AsyncTaskRunner
import com.opensandbox.dhizuku.core.Outcome
import com.opensandbox.dhizuku.delegation.DelegationService
import com.opensandbox.dhizuku.platform.CapabilityProvider
import com.opensandbox.dhizuku.platform.CapabilityRouter
import com.opensandbox.dhizuku.platform.ProviderRegistry
import com.opensandbox.dhizuku.provision.ProfileApps
import com.opensandbox.dhizuku.provision.ShellProfileProvisioner
import com.opensandbox.dhizuku.provision.SystemProfileProvisioner

/** 外观预设，只影响配色，不影响布局。 */
enum class Skin { MATERIAL, MIUI }

/**
 * 界面状态与操作。Compose 只读这里的状态，所有耗时动作都在后台线程跑。
 * 这样四个页面共享同一份状态，不需要层层传参。
 */
class AppState(private val context: Context) {

    private val async = AsyncTaskRunner("open-sandbox-io")
    private val router = CapabilityRouter(ProviderRegistry.providers(context))
    private val catalog = AppCatalog()
    private val frozenStore = FrozenStore(context)

    private var delegation: DelegationService? = null
    private var control: AppControl? = null
    private var policyChannels: List<PolicyChannel> = emptyList()
    private var shell: ShellChannel? = null

    var status by mutableStateOf(context.getString(R.string.status_connecting))
    var session by mutableStateOf("")
    var channels by mutableStateOf<List<ChannelUi>>(emptyList())
    var apps by mutableStateOf<List<AppUi>>(emptyList())
    var scopes by mutableStateOf<Set<String>>(emptySet())
    var busy by mutableStateOf(false)
    var profileUserId by mutableStateOf<Int?>(null)
    var profileApps by mutableStateOf<List<String>>(emptyList())
    var skin by mutableStateOf(Skin.MATERIAL)
    var glassEnabled by mutableStateOf(true)
    var shellLabel by mutableStateOf("")

    var busyPackages by mutableStateOf<Set<String>>(emptySet())

    data class ChannelUi(val name: String, val available: Boolean)
    data class AppUi(
        val packageName: String,
        val label: String,
        val frozen: Boolean,
        val uninstallBlocked: Boolean
    )

    // ------------------------------------------------------------------ 连接

    fun connect() {
        status = context.getString(R.string.status_connecting)
        refreshChannels()
        busy = true
        async.execute({
            val resolved = router.resolve(context, DelegationService::class.java)
            if (!resolved.isSuccess) {
                return@execute Outcome.fail<Connection>(resolved.errorOrNull().orEmpty())
            }
            val service = resolved.valueOrNull()!!

            val connected = service.connect(context)
            if (!connected.isSuccess) {
                return@execute Outcome.fail<Connection>(connected.errorOrNull().orEmpty())
            }
            val authorized = service.isAuthorized()
            if (!authorized.isSuccess) {
                return@execute Outcome.fail<Connection>(authorized.errorOrNull().orEmpty())
            }
            Outcome.ok(
                Connection(
                    service,
                    connected.valueOrNull()!!,
                    authorized.valueOrNull() == true
                )
            )
        }, ::onConnected)
    }

    private fun onConnected(outcome: Outcome<Connection>) {
        busy = false
        if (!outcome.isSuccess) {
            status = context.getString(R.string.status_connect_failed, outcome.errorOrNull().orEmpty())
            return
        }
        val connection = outcome.valueOrNull() ?: return
        delegation = connection.service
        session = context.getString(
            R.string.session_summary,
            connection.session.ownerPackage,
            connection.session.serverVersion,
            connection.session.providerAuthority
        )

        if (connection.authorized) {
            status = context.getString(R.string.status_authorized, session)
            resolvePolicy()
        } else {
            status = context.getString(R.string.status_waiting_authorization, session)
            requestAuthorization()
        }
    }

    private fun requestAuthorization() {
        delegation?.requestAuthorization(context as android.app.Activity) { result ->
            if (result.isSuccess && result.valueOrNull() == true) {
                resolvePolicy()
            } else {
                status = context.getString(R.string.status_authorization_denied)
            }
        }
    }

    private fun resolvePolicy() {
        async.execute({ router.resolve(context, PolicyChannel::class.java) }, { outcome ->
            if (!outcome.isSuccess) {
                status = context.getString(
                    R.string.status_no_policy_channel, outcome.errorOrNull().orEmpty()
                )
                return@execute
            }
            val primary = outcome.valueOrNull()!!
            policyChannels = router.available(context, PolicyChannel::class.java)
            control = PolicyAppControl(primary)
            resolveShell()
            loadScopes()
        })
    }

    private fun resolveShell() {
        async.execute({ router.resolve(context, ShellChannel::class.java) }, { outcome ->
            shell = outcome.valueOrNull()
            shellLabel = shell?.describe().orEmpty()
        })
    }

    fun refreshChannels() {
        async.execute({
            val list = router.providers().map { provider: CapabilityProvider ->
                val availability = provider.isAvailable(context)
                ChannelUi(
                    provider.describe(),
                    availability.isSuccess && availability.valueOrNull() == true
                )
            }
            Outcome.ok(list)
        }, { outcome -> channels = outcome.valueOrNull().orEmpty() })
    }

    // ------------------------------------------------------------------ 委派

    private fun loadScopes() {
        val service = delegation ?: return
        async.execute({ service.grantedScopes() }, { outcome ->
            if (outcome.isSuccess) {
                scopes = outcome.valueOrNull().orEmpty()
                status = if (scopes.isEmpty()) {
                    context.getString(R.string.status_no_scopes, session)
                } else {
                    context.getString(R.string.status_scopes, session, scopes.toString())
                }
                refreshApps()
            } else {
                status = context.getString(R.string.status_scopes_failed, outcome.errorOrNull().orEmpty())
            }
        })
    }

    fun requestScopes() {
        val service = delegation ?: return
        status = context.getString(R.string.status_requesting_scopes)
        async.execute({ service.requestScopes(service.supportedScopes()) }, { outcome ->
            status = if (outcome.isSuccess) {
                context.getString(R.string.status_scopes_updated, outcome.valueOrNull().toString())
            } else {
                context.getString(R.string.status_scope_request_failed, outcome.errorOrNull().orEmpty())
            }
            loadScopes()
        })
    }

    // ------------------------------------------------------------------ 应用

    fun refreshApps() {
        val policy = control ?: return
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
                    entry.packageName,
                    entry.label,
                    isFrozen,
                    policy.isUninstallBlocked(entry.packageName)
                )
            }
            for (packageName in remembered) {
                if (packageName in seen || !policy.isFrozen(packageName)) continue
                frozen += packageName
                built += AppUi(
                    packageName,
                    packageName,
                    true,
                    policy.isUninstallBlocked(packageName)
                )
            }
            built.sortBy { it.label.lowercase() }
            Outcome.ok(Snapshot(built, frozen))
        }, { outcome ->
            val snapshot = outcome.valueOrNull() ?: return@execute
            apps = snapshot.apps
            frozenStore.save(snapshot.frozen)
        })
    }

    /** 目标值由开关回调给出，不再用「取反当前值」——避免连点/刷新延迟导致的反向操作。 */
    fun setFrozen(app: AppUi, target: Boolean) {
        if (control == null) return
        if (app.packageName in busyPackages) return

        busyPackages = busyPackages + app.packageName
        async.execute({ withPolicyFallback { it.setFrozen(app.packageName, target) } }, { outcome ->
            busyPackages = busyPackages - app.packageName
            if (outcome.isSuccess) {
                // 先本地更新，界面立刻反映；随后再从系统读一遍校准
                apps = apps.map {
                    if (it.packageName == app.packageName) it.copy(frozen = target) else it
                }
                status = context.getString(
                    if (target) R.string.toast_frozen else R.string.toast_unfrozen,
                    app.label
                )
                refreshApps()
            } else {
                status = context.getString(
                    R.string.toast_action_failed, outcome.errorOrNull().orEmpty()
                )
            }
        })
    }

    /** 一个通道失败就换下一个（she → dhi → …），这是"智能调用"的落地。 */
    private fun withPolicyFallback(action: (PolicyChannel) -> Outcome<Unit>): Outcome<Unit> {
        var last: Outcome<Unit> = Outcome.fail("没有可用的策略通道")
        for (channel in policyChannels) {
            val result = action(channel)
            if (result.isSuccess) return result
            last = result
        }
        return last
    }

    fun toggleUninstallBlock(app: AppUi) {
        if (control == null) return
        val target = !app.uninstallBlocked
        async.execute({ withPolicyFallback { it.setUninstallBlocked(app.packageName, target) } }, { outcome ->
            status = if (outcome.isSuccess) {
                context.getString(
                    if (target) R.string.toast_uninstall_blocked else R.string.toast_uninstall_allowed,
                    app.label
                )
            } else {
                context.getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty())
            }
            refreshApps()
        })
    }

    // ------------------------------------------------------------------ 隔离空间

    /** 首选：系统自带的工作资料配置流程（不需要 shell）。 */
    fun provisionManagedProfile() {
        val activity = context as? android.app.Activity ?: return
        val provisioner = SystemProfileProvisioner(
            activity,
            android.content.ComponentName(context, com.opensandbox.dhizuku.SandboxAdmin::class.java)
        )
        val launched = provisioner.launch()
        status = if (launched.isSuccess) {
            context.getString(R.string.status_profile_system_flow)
        } else {
            context.getString(R.string.status_profile_failed, launched.errorOrNull().orEmpty())
        }
    }

    fun onProvisionResult(resultCode: Int) {
        val activity = context as? android.app.Activity ?: return
        val provisioner = SystemProfileProvisioner(
            activity,
            android.content.ComponentName(context, com.opensandbox.dhizuku.SandboxAdmin::class.java)
        )
        val result = provisioner.onResult(resultCode)
        status = if (result.isSuccess) {
            context.getString(R.string.status_profile_system_ok)
        } else {
            context.getString(R.string.status_profile_failed, result.errorOrNull().orEmpty())
        }
    }

    /** 兜底：用 shi 的 shell 建（部分 ROM 上系统流程被禁时可用）。 */
    fun createProfile() {
        val channel = shell ?: run {
            status = context.getString(R.string.status_shell_unavailable, "")
            return
        }
        val provisioner = ShellProfileProvisioner(context, channel)

        status = context.getString(R.string.status_profile_creating)
        async.execute({
            provisioner.ensureManagedProfile(ShellProfileProvisioner.DEFAULT_PROFILE_NAME)
        }, { outcome ->
            status = if (outcome.isSuccess) {
                val info = outcome.valueOrNull()!!
                context.getString(
                    if (info.createdNow) R.string.status_profile_created else R.string.status_profile_exists,
                    info.userId, info.name
                )
            } else {
                context.getString(R.string.status_profile_failed, outcome.errorOrNull().orEmpty())
            }
        })
    }

    /** 诊断报告：通道、会话、状态、版本，一次性复制。 */
    private val managers = listOf(
        "com.hamondev.shevery" to "she",
        "moe.shizuku.privileged.api" to "shi",
        "com.rosan.dhizuku" to "dhi",
        "frb.axeron.manager" to "ax(经 shi 兼容层可用)"
    )

    fun installedManagers(): List<String> = managers.mapNotNull { (pkg, label) ->
        if (runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess) label else null
    }

    fun diagnosticsReport(): String = buildString {
        appendLine("version=${com.opensandbox.dhizuku.BuildConfig.VERSION_NAME}")
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

    // ------------------------------------------------------------------ 隔离空间内的应用

    fun refreshProfileApps() {
        val channel = shell ?: run {
            status = context.getString(R.string.status_shell_unavailable, "")
            return
        }
        async.execute({
            val users = channel.execute("sh", "-c", "pm list users").valueOrNull().orEmpty()
            val id = Regex("""UserInfo\{(\d+):${ShellProfileProvisioner.DEFAULT_PROFILE_NAME}:""")
                .find(users)?.groupValues?.get(1)?.toIntOrNull()
                ?: error("还没有隔离空间，先在上面创建")

            val listing = channel.execute("sh", "-c", "pm list packages --user $id")
                .valueOrNull().orEmpty()
            val packages = listing.lineSequence()
                .map { it.removePrefix("package:").trim() }
                .filter { it.isNotEmpty() }
                .sorted()
                .toList()
            Outcome.ok(ProfileSnapshot(id, packages))
        }, { outcome ->
            val snapshot = outcome.valueOrNull()
            if (snapshot == null) {
                status = context.getString(
                    R.string.toast_action_failed, outcome.errorOrNull().orEmpty()
                )
                return@execute
            }
            profileUserId = snapshot.userId
            profileApps = snapshot.packages
            status = context.getString(R.string.profile_apps_count, snapshot.packages.size)
        })
    }

    fun installIntoProfile(packageName: String) {
        val channel = shell ?: return
        val userId = profileUserId ?: return
        async.execute({
            channel.execute("sh", "-c", "pm install-existing --user $userId $packageName")
        }, { outcome ->
            status = if (outcome.isSuccess) {
                context.getString(R.string.profile_installed, packageName, userId)
            } else {
                context.getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty())
            }
            refreshProfileApps()
        })
    }

    fun launchInProfile(packageName: String) {
        val userId = profileUserId ?: return
        val channel = shell ?: return

        // 关键：am start --user 会一直等目标用户启动，必须"发出去就返回"，
        // 否则唯一的工作线程会被占死，界面看起来像系统卡死。
        async.execute({
            val resolved = channel.execute(
                "sh", "-c", "cmd package resolve-activity --user $userId --brief $packageName"
            ).valueOrNull().orEmpty()
            val component = resolved.lineSequence()
                .map { it.trim() }
                .lastOrNull { it.contains("/") }
                ?: error("找不到启动入口：$packageName")
            channel.executeDetached("sh", "-c", "am start --user $userId -n $component")
        }, { outcome ->
            status = if (outcome.isSuccess) {
                context.getString(R.string.profile_launched, packageName)
            } else {
                context.getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty())
            }
        })
    }

    fun removeFromProfile(packageName: String) {
        val channel = shell ?: return
        val userId = profileUserId ?: return
        async.execute({
            channel.execute("sh", "-c", "pm uninstall --user $userId $packageName")
        }, { outcome ->
            status = if (outcome.isSuccess) {
                context.getString(R.string.profile_removed, packageName, userId)
            } else {
                context.getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty())
            }
            refreshProfileApps()
        })
    }

    fun shutdown() = async.shutdown()

    // ------------------------------------------------------------------ 内部

    private data class Connection(
        val service: DelegationService,
        val session: DelegationService.Session,
        val authorized: Boolean
    )

    private data class Snapshot(val apps: List<AppUi>, val frozen: Set<String>)

    private data class ProfileSnapshot(val userId: Int, val packages: List<String>)
}
