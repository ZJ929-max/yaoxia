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
    private var shell: ShellChannel? = null

    var status by mutableStateOf(context.getString(R.string.status_connecting))
    var session by mutableStateOf("")
    var channels by mutableStateOf<List<ChannelUi>>(emptyList())
    var apps by mutableStateOf<List<AppUi>>(emptyList())
    var scopes by mutableStateOf<Set<String>>(emptySet())
    var busy by mutableStateOf(false)
    var skin by mutableStateOf(Skin.MATERIAL)
    var glassEnabled by mutableStateOf(true)

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
            control = PolicyAppControl(outcome.valueOrNull()!!)
            resolveShell()
            loadScopes()
        })
    }

    private fun resolveShell() {
        async.execute({ router.resolve(context, ShellChannel::class.java) }, { outcome ->
            shell = outcome.valueOrNull()
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

    fun toggleFrozen(app: AppUi) {
        val policy = control ?: return
        val target = !app.frozen
        async.execute({ policy.setFrozen(app.packageName, target) }, { outcome ->
            status = if (outcome.isSuccess) {
                context.getString(
                    if (target) R.string.toast_frozen else R.string.toast_unfrozen,
                    app.label
                )
            } else {
                context.getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty())
            }
            refreshApps()
        })
    }

    fun toggleUninstallBlock(app: AppUi) {
        val policy = control ?: return
        val target = !app.uninstallBlocked
        async.execute({ policy.setUninstallBlocked(app.packageName, target) }, { outcome ->
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

    fun shutdown() = async.shutdown()

    // ------------------------------------------------------------------ 内部

    private data class Connection(
        val service: DelegationService,
        val session: DelegationService.Session,
        val authorized: Boolean
    )

    private data class Snapshot(val apps: List<AppUi>, val frozen: Set<String>)
}
