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
import com.opensandbox.dhizuku.provision.DhizukuProfileProvisioner
import com.opensandbox.dhizuku.provision.ProfileApps
import com.opensandbox.dhizuku.provision.ShellCommands
import com.opensandbox.dhizuku.provision.ShellProfileProvisioner

/** 外观预设，只影响配色，不影响布局。 */
enum class Skin { MATERIAL, MIUI }

/**
 * 界面状态与操作。Compose 只读这里的状态，所有耗时动作都在后台线程跑。
 * 这样四个页面共享同一份状态，不需要层层传参。
 */
class AppState(
    private val context: Context,
) {
    private val async = AsyncTaskRunner("open-sandbox-io")
    private val router = CapabilityRouter(ProviderRegistry.providers(context))
    private val catalog = AppCatalog()
    private val frozenStore = FrozenStore(context)

    private val prefs = context.getSharedPreferences("profile_state", Context.MODE_PRIVATE)

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
    var profileUserId by mutableStateOf<Int?>(prefs.getInt("user_id", -1).takeIf { it >= 0 })
    var profileApps by mutableStateOf<List<String>>(emptyList())
    var skin by mutableStateOf(Skin.MATERIAL)
    var glassEnabled by mutableStateOf(true)
    var shellLabel by mutableStateOf("")
    var pendingLaunch by mutableStateOf<AppUi?>(null)
    var pendingSwitchBack by mutableStateOf(false)

    var busyPackages by mutableStateOf<Set<String>>(emptySet())

    data class ChannelUi(
        val name: String,
        val available: Boolean,
    )

    data class AppUi(
        val packageName: String,
        val label: String,
        val frozen: Boolean,
        val uninstallBlocked: Boolean,
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
                    authorized.valueOrNull() == true,
                ),
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
        session =
            context.getString(
                R.string.session_summary,
                connection.session.ownerPackage,
                connection.session.serverVersion,
                connection.session.providerAuthority,
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
                status =
                    context.getString(
                        R.string.status_no_policy_channel,
                        outcome.errorOrNull().orEmpty(),
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
            val list =
                router.providers().map { provider: CapabilityProvider ->
                    val availability = provider.isAvailable(context)
                    ChannelUi(
                        provider.describe(),
                        availability.isSuccess && availability.valueOrNull() == true,
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
                status =
                    if (scopes.isEmpty()) {
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
            status =
                if (outcome.isSuccess) {
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
                built +=
                    AppUi(
                        entry.packageName,
                        entry.label,
                        isFrozen,
                        policy.isUninstallBlocked(entry.packageName),
                    )
            }
            for (packageName in remembered) {
                if (packageName in seen || !policy.isFrozen(packageName)) continue
                frozen += packageName
                built +=
                    AppUi(
                        packageName,
                        packageName,
                        true,
                        policy.isUninstallBlocked(packageName),
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
    fun setFrozen(
        app: AppUi,
        target: Boolean,
    ) {
        if (control == null) return
        if (app.packageName in busyPackages) return

        busyPackages = busyPackages + app.packageName
        async.execute({ withPolicyFallback { it.setFrozen(app.packageName, target) } }, { outcome ->
            busyPackages = busyPackages - app.packageName
            if (outcome.isSuccess) {
                // 先本地更新，界面立刻反映；随后再从系统读一遍校准
                apps =
                    apps.map {
                        if (it.packageName == app.packageName) it.copy(frozen = target) else it
                    }
                status =
                    context.getString(
                        if (target) R.string.toast_frozen else R.string.toast_unfrozen,
                        app.label,
                    )
                refreshApps()
            } else {
                status =
                    context.getString(
                        R.string.toast_action_failed,
                        outcome.errorOrNull().orEmpty(),
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
            status =
                if (outcome.isSuccess) {
                    context.getString(
                        if (target) R.string.toast_uninstall_blocked else R.string.toast_uninstall_allowed,
                        app.label,
                    )
                } else {
                    context.getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty())
                }
            refreshApps()
        })
    }

    // ------------------------------------------------------------------ 隔离空间

    /**
     * 建隔离空间：先试 DO 的 binder（能建出真·工作资料），失败自动回落 shell（建完整次级用户）。
     * 两条路都失败才报错，并且把两边的原因都写出来——否则用户只看到一句"失败"，无从下手。
     */
    fun createProfile() {
        status = context.getString(R.string.status_profile_creating)
        async.execute({
            val name = ShellProfileProvisioner.DEFAULT_PROFILE_NAME

            val viaDhi =
                if (delegation != null) {
                    DhizukuProfileProvisioner(context).ensureManagedProfile(name)
                } else {
                    null
                }
            if (viaDhi?.isSuccess == true) {
                return@execute viaDhi
            }

            val channel =
                shell
                    ?: return@execute (viaDhi ?: Outcome.fail(context.getString(R.string.status_shell_unavailable, "没有可用通道")))

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
            status =
                if (outcome.isSuccess) {
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

    /** 诊断报告：通道、会话、状态、版本，一次性复制。 */
    private val managers =
        listOf(
            "com.hamondev.shevery" to "she",
            "moe.shizuku.privileged.api" to "shi",
            "com.rosan.dhizuku" to "dhi",
            "frb.axeron.manager" to "ax(经 shi 兼容层可用)",
        )

    fun installedManagers(): List<String> =
        managers.mapNotNull { (pkg, label) ->
            if (runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess) label else null
        }

    fun diagnosticsReport(): String =
        buildString {
            appendLine("version=${com.opensandbox.dhizuku.BuildConfig.VERSION_NAME}")
            appendLine("installed=${installedManagers().joinToString(",")}")
            appendLine(session)
            channels.forEach { appendLine("${it.name} available=${it.available}") }
            appendLine("shell in use=$shellLabel")
            appendLine(status)
            appendLine("log=${logFile()?.absolutePath ?: "无"}")
        }

    fun logFile(): java.io.File? =
        runCatching {
            java.io.File(context.filesDir, "crash-last.txt").takeIf { it.exists() }
        }.getOrNull()

    // ------------------------------------------------------------------ 隔离空间内的应用

    fun refreshProfileApps() {
        val channel =
            shell ?: run {
                status = context.getString(R.string.status_shell_unavailable, "")
                return
            }
        val profile = ProfileApps(channel)

        async.execute({
            val userId =
                profile
                    .findUser(ShellProfileProvisioner.DEFAULT_PROFILE_NAME)
                    .valueOrNull() ?: error("还没有隔离空间，先点上面的按钮建一个")
            val installed = profile.installed(userId)
            if (!installed.isSuccess) error(installed.errorOrNull().orEmpty())
            Outcome.ok(ProfileSnapshot(userId, installed.valueOrNull().orEmpty().sorted()))
        }, { outcome ->
            val snapshot = outcome.valueOrNull()
            if (snapshot == null) {
                status = context.getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty())
                return@execute
            }
            profileUserId = snapshot.userId
            profileApps = snapshot.packages
            prefs.edit().putInt("user_id", snapshot.userId).apply()
            status = context.getString(R.string.profile_apps_count, snapshot.packages.size)
        })
    }

    fun installIntoProfile(packageName: String) {
        val channel = shell ?: return
        val userId = profileUserId ?: return
        val profile = ProfileApps(channel)
        async.execute({
            profile.install(userId, packageName)
        }, { outcome ->
            status =
                if (outcome.isSuccess) {
                    context.getString(R.string.profile_installed, packageName, userId)
                } else {
                    context.getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty())
                }
            refreshProfileApps()
        })
    }

    /** 点「打开」先征求同意：切用户会把本应用一起切走。 */
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

    /** 从隔离空间切回主空间（0 号用户），同样"发出去不等"。 */
    fun switchToOwner() {
        val channel = shell ?: return
        async.execute({
            channel.executeDetached(
                ShellCommands.SHELL,
                ShellCommands.EXECUTE,
                ShellCommands.switchUser(0),
            )
        }, { outcome ->
            status =
                if (outcome.isSuccess) {
                    context.getString(R.string.profile_switch_back_ok)
                } else {
                    context.getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty())
                }
        })
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

    fun launchInProfile(packageName: String) {
        val userId = profileUserId ?: return
        val channel = shell ?: return
        val profile = ProfileApps(channel)

        // am start --user 会等目标用户启动，必须"发出去就返回"
        async.execute({
            val component = profile.resolveComponent(userId, packageName)
            if (component != null) {
                profile.launch(userId, component)
            } else {
                profile.launchByMonkey(userId, packageName)
            }
        }, { outcome ->
            status =
                if (outcome.isSuccess) {
                    context.getString(R.string.profile_launch_requested, packageName)
                } else {
                    context.getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty())
                }
        })
    }

    fun removeFromProfile(packageName: String) {
        val channel = shell ?: return
        val userId = profileUserId ?: return
        val profile = ProfileApps(channel)
        async.execute({
            profile.remove(userId, packageName)
        }, { outcome ->
            status =
                if (outcome.isSuccess) {
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
        val authorized: Boolean,
    )

    private data class Snapshot(
        val apps: List<AppUi>,
        val frozen: Set<String>,
    )

    private data class ProfileSnapshot(
        val userId: Int,
        val packages: List<String>,
    )
}
