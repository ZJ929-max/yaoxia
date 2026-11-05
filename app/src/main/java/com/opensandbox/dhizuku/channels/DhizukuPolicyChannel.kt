package com.opensandbox.dhizuku.channels

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import com.opensandbox.dhizuku.SandboxAdmin
import com.opensandbox.dhizuku.core.Outcome
import com.opensandbox.dhizuku.delegation.DelegationService
import com.opensandbox.dhizuku.delegation.DhizukuProtocol
import com.opensandbox.dhizuku.platform.Capability
import com.opensandbox.dhizuku.platform.CapabilityProvider
import com.opensandbox.dhizuku.platform.ProviderId
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import java.util.EnumSet

/**
 * 一套实现同时服务 dhi 与 she —— 它们说同一个协议，
 * 差别只在"谁是 Device Owner、包名是什么"。
 * 新增同类实现，只要在 ProviderRegistry 里加一行。
 */
class DhizukuPolicyChannel(
    context: Context,
    override val id: ProviderId,
    private val ownerPackage: String,
    private val wantedScopes: Set<String>,
) : CapabilityProvider,
    PolicyChannel,
    DelegationService {
    private val appContext: Context = context.applicationContext
    private val admin = ComponentName(context, SandboxAdmin::class.java)

    private val policyManager: DevicePolicyManager?
        get() = appContext.getSystemService(DevicePolicyManager::class.java)

    override val capabilities: Set<Capability> =
        EnumSet.of(Capability.POLICY, Capability.DELEGATION)

    override fun describe(): String = "${id.label} · $ownerPackage"

    override fun isAvailable(context: Context): Outcome<Boolean> =
        Outcome.catching {
            val manager = policyManager ?: return@catching false
            val admins = manager.activeAdmins ?: return@catching false
            admins.any { it.packageName == ownerPackage && manager.isDeviceOwnerApp(ownerPackage) }
        }

    // ------------------------------------------------------------- Delegation

    override fun connect(context: Context): Outcome<DelegationService.Session> =
        Outcome.catching {
            check(Dhizuku.init(appContext)) { "初始化失败" }

            val version = runCatching { Dhizuku.getVersionName() }.getOrDefault("")
            DelegationService.Session(
                ownerPackage = ownerPackage,
                serverVersion = version,
                providerAuthority = DhizukuProtocol.providerAuthority(ownerPackage),
            )
        }

    override fun isAuthorized(): Outcome<Boolean> =
        Outcome.catching {
            Dhizuku.isPermissionGranted()
        }

    override fun requestAuthorization(
        activity: Activity,
        callback: (Outcome<Boolean>) -> Unit,
    ) {
        // AIDL 回调发生在 binder 线程，绝不能直接改 UI
        val main = Handler(Looper.getMainLooper())
        try {
            Dhizuku.requestPermission(
                object : DhizukuRequestPermissionListener() {
                    override fun onRequestPermission(grantResult: Int) {
                        main.post { callback(Outcome.ok(grantResult == PackageManager.PERMISSION_GRANTED)) }
                    }
                },
            )
        } catch (throwable: Throwable) {
            main.post { callback(Outcome.fail(throwable)) }
        }
    }

    override fun grantedScopes(): Outcome<Set<String>> =
        Outcome.catching {
            (Dhizuku.getDelegatedScopes() ?: emptyArray()).toCollection(linkedSetOf())
        }

    override fun requestScopes(scopes: Collection<String>): Outcome<Set<String>> {
        // setDelegatedScopes 是"整体替换"：先并上已有范围，别把用户手工授的抹掉
        val merged = (grantedScopes().valueOrNull().orEmpty() + scopes).toTypedArray()

        val applied = Outcome.catching { Dhizuku.setDelegatedScopes(merged) }
        if (!applied.isSuccess) {
            return Outcome.fail(applied.errorOrNull().orEmpty())
        }
        return grantedScopes()
    }

    override fun supportedScopes(): Set<String> = wantedScopes

    // ------------------------------------------------------------- Policy

    override fun isFrozen(packageName: String): Boolean =
        query { component ->
            policyManager?.isPackageSuspended(component, packageName) ?: false
        }

    override fun setFrozen(
        packageName: String,
        frozen: Boolean,
    ): Outcome<Unit> {
        val result =
            perform { component ->
                val failed = policyManager?.setPackagesSuspended(component, arrayOf(packageName), frozen)
                check(failed == null || failed.isEmpty()) { "系统未处理：${failed?.joinToString()}" }
            }
        return result
    }

    override fun isUninstallBlocked(packageName: String): Boolean =
        query { component ->
            policyManager?.isUninstallBlocked(component, packageName) ?: false
        }

    override fun setUninstallBlocked(
        packageName: String,
        blocked: Boolean,
    ): Outcome<Unit> =
        perform { component ->
            policyManager?.setUninstallBlocked(component, packageName, blocked)
        }

    // ------------------------------------------------------------- 内部

    /**
     * 委派身份下 admin 参数的要求因实现方而异：
     * 有的要求传调用方自己的 admin，有的只接受 null。先带 admin，再退到 null。
     */
    private fun ownerOrOwnAdmin(): ComponentName? = runCatching { Dhizuku.getOwnerComponent(appContext) }.getOrNull() ?: admin

    private fun perform(action: (ComponentName?) -> Unit): Outcome<Unit> {
        if (policyManager == null) return Outcome.fail("DevicePolicyManager 不可用")

        val first = runCatching { action(ownerOrOwnAdmin()) }
        if (first.isSuccess) return Outcome.ok(Unit)

        val second = runCatching { action(null) }
        if (second.isSuccess) return Outcome.ok(Unit)

        val firstError = first.exceptionOrNull()
        val secondError = second.exceptionOrNull()
        return Outcome.fail(
            "${firstError?.javaClass?.simpleName} / ${secondError?.javaClass?.simpleName}: " +
                (secondError?.message ?: ""),
        )
    }

    private fun query(action: (ComponentName?) -> Boolean): Boolean {
        runCatching { action(ownerOrOwnAdmin()) }.getOrNull()?.let { return it }
        return runCatching { action(null) }.getOrDefault(false)
    }
}
