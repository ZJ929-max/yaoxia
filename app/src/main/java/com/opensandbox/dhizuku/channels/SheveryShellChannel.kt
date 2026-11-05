package com.opensandbox.dhizuku.channels

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import com.opensandbox.dhizuku.core.Outcome
import com.opensandbox.dhizuku.platform.Capability
import com.opensandbox.dhizuku.platform.CapabilityProvider
import com.opensandbox.dhizuku.platform.ProviderId
import rikka.shizuku.Shizuku
import java.util.EnumSet

/**
 * she 自己的 API：通过 ContentProvider 的 requestBinder 取得 shell 服务的 binder，
 * 再注入给 Shizuku SDK 使用。
 *
 * 关键点：不同版本 she 的常量名/授权名可能变，所以这里**不写死常量**，
 * 而是枚举候选授权名 + 遍历返回 Bundle 里的所有 Binder，谁活下来就用谁。
 */
class SheveryShellChannel(
    context: Context,
) : CapabilityProvider,
    ShellChannel {
    private val appContext: Context = context.applicationContext

    override val id: ProviderId = ProviderId.SHE

    override val capabilities: Set<Capability> = EnumSet.of(Capability.SHELL)

    override fun describe(): String = "she · $MANAGER_PACKAGE (shell)"

    override fun isAvailable(context: Context): Outcome<Boolean> =
        Outcome.catching {
            installed() && ensureBinderInjected()
        }

    override fun requestPermission(): Outcome<Unit> =
        Outcome.catching {
            check(installed()) { "she 未安装" }
            // 权限是普通权限，装包即授予；这里只触发一次活体检测
            check(ensureBinderInjected()) { "未能从 she 取得 shell binder" }
        }

    override fun executeDetached(vararg command: String): Outcome<Unit> {
        if (!ensureBinderInjected()) return Outcome.fail("she 的 shell binder 不可用")
        return ShizukuProcessRunner.start(command)
    }

    override fun execute(vararg command: String): Outcome<String> {
        if (!ensureBinderInjected()) {
            return Outcome.fail("she 的 shell binder 不可用")
        }
        return ShizukuProcessRunner.run(command)
    }

    // ------------------------------------------------------------------ 内部

    private fun installed(): Boolean = runCatching { appContext.packageManager.getPackageInfo(MANAGER_PACKAGE, 0) }.isSuccess

    private var injected = false

    private fun ensureBinderInjected(): Boolean {
        if (injected && Shizuku.pingBinder()) return true

        val binder = obtainBinder() ?: return false
        runCatching { Shizuku.onBinderReceived(binder, MANAGER_PACKAGE) }
        injected = runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        return injected
    }

    private fun obtainBinder(): IBinder? {
        for (authority in candidateAuthorities()) {
            val bundle =
                runCatching {
                    appContext.contentResolver.call(
                        Uri.parse("content://$authority"),
                        METHOD_REQUEST_BINDER,
                        null,
                        Bundle(),
                    )
                }.getOrNull() ?: continue

            runCatching { bundle.classLoader = javaClass.classLoader }

            for (key in bundle.keySet()) {
                val value = runCatching { bundle.get(key) }.getOrNull() ?: continue
                val binder = extractBinder(value) ?: continue
                if (runCatching { binder.pingBinder() }.getOrDefault(false)) return binder
            }
        }
        return null
    }

    /** BinderContainer 这类包装对象：取 getBinder() 或字段 binder。 */
    private fun extractBinder(value: Any): IBinder? {
        if (value is IBinder) return value

        runCatching {
            value.javaClass.getMethod("getBinder").invoke(value) as? IBinder
        }.getOrNull()?.let { return it }

        return runCatching {
            value.javaClass
                .getDeclaredField("binder")
                .apply { isAccessible = true }
                .get(value) as? IBinder
        }.getOrNull()
    }

    private fun candidateAuthorities(): List<String> =
        listOf(
            "$MANAGER_PACKAGE.shevery",
            "$MANAGER_PACKAGE.shizuku",
            "$MANAGER_PACKAGE.connector",
            "$MANAGER_PACKAGE.server",
        )

    private companion object {
        const val MANAGER_PACKAGE = "com.hamondev.shevery"
        const val METHOD_REQUEST_BINDER = "requestBinder"
    }
}
