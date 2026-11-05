package com.opensandbox.dhizuku.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.opensandbox.dhizuku.BuildConfig
import com.opensandbox.dhizuku.R
import com.opensandbox.dhizuku.apps.AppCatalog
import com.opensandbox.dhizuku.apps.AppControl
import com.opensandbox.dhizuku.apps.FrozenStore
import com.opensandbox.dhizuku.apps.PolicyAppControl
import com.opensandbox.dhizuku.channels.PolicyChannel
import com.opensandbox.dhizuku.channels.ShellChannel
import com.opensandbox.dhizuku.core.AsyncTaskRunner
import com.opensandbox.dhizuku.core.FailureReporter
import com.opensandbox.dhizuku.core.Outcome
import com.opensandbox.dhizuku.delegation.DelegationService
import com.opensandbox.dhizuku.platform.CapabilityRouter
import com.opensandbox.dhizuku.platform.ProviderRegistry
import com.opensandbox.dhizuku.provision.ShellProfileProvisioner

/**
 * 组装层：装配路由与通道，渲染状态，转发用户操作。
 * 这里不出现任何 dhi / she / shi 的具体类名——那些都在 ProviderRegistry 里。
 */
class MainActivity : AppCompatActivity() {

    private val async = AsyncTaskRunner("open-sandbox-io")
    private val catalog = AppCatalog()
    private val rows = mutableListOf<AppRow>()

    private lateinit var router: CapabilityRouter
    private lateinit var frozenStore: FrozenStore
    private lateinit var adapter: AppListAdapter

    private var delegation: DelegationService? = null
    private var appControl: AppControl? = null
    private var shellChannel: ShellChannel? = null

    private lateinit var statusView: TextView
    private lateinit var channelsView: TextView
    private var sessionText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(Appearance.themeRes(this))
        super.onCreate(savedInstanceState)

        router = CapabilityRouter(ProviderRegistry.providers(this))
        frozenStore = FrozenStore(this)
        adapter = AppListAdapter(this)

        FailureReporter.install(this)
        setContentView(buildContentView())
        guard { connect() }
        guard { resolveShellChannel() }
    }

    // ------------------------------------------------------------------ 界面

    private fun buildContentView(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dimension(R.dimen.spacing_screen_horizontal),
                dimension(R.dimen.spacing_screen_top),
                dimension(R.dimen.spacing_screen_horizontal),
                0
            )
        }

        root.addView(TextView(this).apply {
            setText(R.string.app_name)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_title))
            setTextColor(getColor(R.color.text_primary))
        })

        root.addView(TextView(this).apply {
            text = getString(R.string.version_format, BuildConfig.VERSION_NAME)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_subtitle))
            setTextColor(getColor(R.color.text_secondary))
        })

        channelsView = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_subtitle))
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, dimension(R.dimen.spacing_small), 0, dimension(R.dimen.spacing_small))
        }
        root.addView(channelsView)

        statusView = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.text_status))
            setTextColor(getColor(R.color.text_secondary))
            setPadding(0, dimension(R.dimen.spacing_small), 0, dimension(R.dimen.spacing_small))
            setText(R.string.status_connecting)
        }
        root.addView(statusView)

        root.addView(button(R.string.button_connect) { connect() })
        root.addView(appearanceButton())
        root.addView(button(R.string.button_scopes) { requestScopes() })
        root.addView(button(R.string.button_create_profile) { createProfile() })
        root.addView(button(R.string.button_refresh) { refreshApps() })
        root.addView(button(R.string.button_copy_diagnostics) { copyDiagnostics() })

        val rowAdapter = adapter
        val list = ListView(this).apply {
            adapter = rowAdapter
            dividerHeight = 0
            setOnItemClickListener { _, _, position, _ ->
                guard { toggleFrozen(rowAdapter.getItem(position)) }
            }
            setOnItemLongClickListener { _, _, position, _ ->
                guard { toggleUninstallBlock(rowAdapter.getItem(position)) }
                true
            }
        }
        root.addView(
            list,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        return root
    }

    private fun appearanceButton(): MaterialButton = MaterialButton(this).apply {
        text = getString(
            R.string.button_appearance,
            getString(Appearance.labelRes(this@MainActivity))
        )
        setOnClickListener { guard { Appearance.cycle(this@MainActivity); recreate() } }
    }

    private fun button(textRes: Int, onClick: () -> Unit): Button =
        MaterialButton(this).apply {
            setText(textRes)
            setOnClickListener { guard(onClick) }
        }

    private fun guard(block: () -> Unit) = FailureReporter.guard(this, block)

    private fun dimension(resId: Int): Int = resources.getDimensionPixelSize(resId)

    private fun setStatus(text: String) {
        statusView.text = text
    }

    // ------------------------------------------------------------------ 连接与通道

    private fun connect() {
        setStatus(getString(R.string.status_connecting))
        renderChannels()

        async.execute(
            work = {
                val resolved = router.resolve(this, DelegationService::class.java)
                if (!resolved.isSuccess) {
                    return@execute Outcome.fail<Connection>(resolved.errorOrNull().orEmpty())
                }
                val service = resolved.valueOrNull()!!

                val connected = service.connect(this)
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
            },
            onResult = { renderConnection(it) }
        )
    }

    private fun renderConnection(outcome: Outcome<Connection>) {
        if (!outcome.isSuccess) {
            setStatus(getString(R.string.status_connect_failed, outcome.errorOrNull().orEmpty()))
            return
        }

        val connection = outcome.valueOrNull() ?: return
        delegation = connection.service
        sessionText = getString(
            R.string.session_summary,
            connection.session.ownerPackage,
            connection.session.serverVersion,
            connection.session.providerAuthority
        )

        if (connection.authorized) {
            setStatus(getString(R.string.status_authorized, sessionText))
            resolvePolicyChannel()
        } else {
            setStatus(getString(R.string.status_waiting_authorization, sessionText))
            requestAuthorization()
        }
    }

    private fun renderChannels() {
        async.execute(
            work = {
                val report = router.providers().joinToString("\n") { provider ->
                    val availability = provider.isAvailable(this)
                    val available = availability.isSuccess && availability.valueOrNull() == true
                    provider.describe() + "  " + getString(
                        if (available) R.string.channel_available else R.string.channel_unavailable
                    )
                }
                Outcome.ok(report)
            },
            onResult = { channelsView.text = it.valueOrNull().orEmpty() }
        )
    }

    private fun requestAuthorization() {
        delegation?.requestAuthorization(this) { result ->
            if (result.isSuccess && result.valueOrNull() == true) {
                resolvePolicyChannel()
            } else {
                setStatus(getString(R.string.status_authorization_denied))
            }
        }
    }

    private fun resolveShellChannel() {
        async.execute(
            work = { router.resolve(this, ShellChannel::class.java) },
            onResult = { outcome ->
                shellChannel = outcome.valueOrNull()
                if (!outcome.isSuccess) {
                    setStatus(getString(R.string.status_shell_unavailable, outcome.errorOrNull().orEmpty()))
                }
            }
        )
    }

    private fun createProfile() {
        val shell = shellChannel ?: run {
            setStatus(getString(R.string.status_shell_unavailable, ""))
            return
        }
        val provisioner = ShellProfileProvisioner(this, shell)

        setStatus(getString(R.string.status_profile_creating))
        async.execute(
            work = { provisioner.ensureManagedProfile(ShellProfileProvisioner.DEFAULT_PROFILE_NAME) },
            onResult = { outcome ->
                if (!outcome.isSuccess) {
                    setStatus(getString(R.string.status_profile_failed, outcome.errorOrNull().orEmpty()))
                    return@execute
                }
                val info = outcome.valueOrNull() ?: return@execute
                setStatus(
                    getString(
                        if (info.createdNow) R.string.status_profile_created
                        else R.string.status_profile_exists,
                        info.userId, info.name
                    )
                )
            }
        )
    }

    private fun resolvePolicyChannel() {
        async.execute(
            work = { router.resolve(this, PolicyChannel::class.java) },
            onResult = { outcome ->
                if (!outcome.isSuccess) {
                    setStatus(
                        getString(R.string.status_no_policy_channel, outcome.errorOrNull().orEmpty())
                    )
                    return@execute
                }
                appControl = PolicyAppControl(outcome.valueOrNull()!!)
                loadScopes()
            }
        )
    }

    // ------------------------------------------------------------------ 委派范围

    private fun loadScopes() {
        val service = delegation ?: return

        async.execute(
            work = { service.grantedScopes() },
            onResult = { outcome ->
                if (!outcome.isSuccess) {
                    setStatus(getString(R.string.status_scopes_failed, outcome.errorOrNull().orEmpty()))
                    return@execute
                }
                val scopes = outcome.valueOrNull().orEmpty()
                setStatus(
                    if (scopes.isEmpty()) getString(R.string.status_no_scopes, sessionText)
                    else getString(R.string.status_scopes, sessionText, scopes.toString())
                )
                refreshApps()
            }
        )
    }

    private fun requestScopes() {
        val service = delegation ?: run {
            setStatus(getString(R.string.status_connecting))
            return
        }

        setStatus(getString(R.string.status_requesting_scopes))
        async.execute(
            work = { service.requestScopes(service.supportedScopes()) },
            onResult = { outcome ->
                if (!outcome.isSuccess) {
                    setStatus(
                        getString(R.string.status_scope_request_failed, outcome.errorOrNull().orEmpty())
                    )
                    return@execute
                }
                setStatus(getString(R.string.status_scopes_updated, outcome.valueOrNull().toString()))
                refreshApps()
            }
        )
    }

    // ------------------------------------------------------------------ 应用列表

    private fun refreshApps() {
        val control = appControl ?: run {
            setStatus(getString(R.string.status_no_policy_channel, ""))
            return
        }

        setStatus(getString(R.string.status_loading_apps))
        async.execute(
            work = {
                val entries = catalog.load(packageManager, packageName)
                val remembered = frozenStore.load()

                val built = mutableListOf<AppRow>()
                val frozenNow = mutableSetOf<String>()
                val seen = mutableSetOf<String>()

                for (entry in entries) {
                    seen += entry.packageName
                    val frozen = entry.packageName in remembered || control.isFrozen(entry.packageName)
                    if (frozen) frozenNow += entry.packageName
                    built += AppRow(
                        packageName = entry.packageName,
                        label = entry.label,
                        frozen = frozen,
                        uninstallBlocked = control.isUninstallBlocked(entry.packageName)
                    )
                }

                // 冻结后应用会从启动器消失，靠本地记录把它补回列表，否则无法解冻
                for (packageName in remembered) {
                    if (packageName in seen || !control.isFrozen(packageName)) continue
                    frozenNow += packageName
                    built += AppRow(
                        packageName = packageName,
                        label = packageName,
                        frozen = true,
                        uninstallBlocked = control.isUninstallBlocked(packageName)
                    )
                }

                built.sortBy { it.label.lowercase() }
                Outcome.ok(Snapshot(built, frozenNow))
            },
            onResult = { outcome ->
                if (!outcome.isSuccess) {
                    setStatus(getString(R.string.status_connect_failed, outcome.errorOrNull().orEmpty()))
                    return@execute
                }
                val snapshot = outcome.valueOrNull() ?: return@execute
                rows.clear()
                rows += snapshot.rows
                adapter.setRows(rows)
                frozenStore.save(snapshot.frozen)
                setStatus(getString(R.string.status_app_count, rows.size))
            }
        )
    }

    private fun toggleFrozen(row: AppRow) {
        val control = appControl ?: return
        val target = !row.frozen

        async.execute(
            work = { control.setFrozen(row.packageName, target) },
            onResult = { outcome ->
                if (!outcome.isSuccess) {
                    toast(getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty()))
                    return@execute
                }
                toast(
                    getString(
                        if (target) R.string.toast_frozen else R.string.toast_unfrozen,
                        row.label
                    )
                )
                refreshApps()
            }
        )
    }

    private fun toggleUninstallBlock(row: AppRow) {
        val control = appControl ?: return
        val target = !row.uninstallBlocked

        async.execute(
            work = { control.setUninstallBlocked(row.packageName, target) },
            onResult = { outcome ->
                if (!outcome.isSuccess) {
                    toast(getString(R.string.toast_action_failed, outcome.errorOrNull().orEmpty()))
                    return@execute
                }
                toast(
                    getString(
                        if (target) R.string.toast_uninstall_blocked
                        else R.string.toast_uninstall_allowed,
                        row.label
                    )
                )
                refreshApps()
            }
        )
    }

    private fun copyDiagnostics() {
        val report = channelsView.text.toString() + "\n" + sessionText + "\n" + statusView.text
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), report))
        toast(getString(R.string.toast_diagnostics_copied))
    }

    private fun toast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        async.shutdown()
    }

    // ------------------------------------------------------------------ 内部数据

    private data class Connection(
        val service: DelegationService,
        val session: DelegationService.Session,
        val authorized: Boolean
    )

    private data class Snapshot(
        val rows: List<AppRow>,
        val frozen: Set<String>
    )
}
