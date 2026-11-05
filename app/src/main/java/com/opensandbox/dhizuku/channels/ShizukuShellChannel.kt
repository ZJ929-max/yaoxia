package com.opensandbox.dhizuku.channels

import android.content.Context
import android.content.pm.PackageManager
import com.opensandbox.dhizuku.core.Outcome
import com.opensandbox.dhizuku.platform.Capability
import com.opensandbox.dhizuku.platform.CapabilityProvider
import com.opensandbox.dhizuku.platform.ProviderId
import rikka.shizuku.Shizuku
import java.io.ByteArrayOutputStream
import java.util.EnumSet
import java.util.concurrent.atomic.AtomicBoolean

/**
 * shi（Shizuku）通道：以 shell 身份执行命令。
 * 工作资料的创建、把应用装进资料、指定 Profile Owner 只能走这里。
 */
class ShizukuShellChannel(context: Context) : CapabilityProvider, ShellChannel {

    private val appContext: Context = context.applicationContext

    override val id: ProviderId = ProviderId.SHI

    override val capabilities: Set<Capability> = EnumSet.of(Capability.SHELL)

    override fun describe(): String = "shi · $SHIZUKU_PACKAGE"

    override fun isAvailable(context: Context): Outcome<Boolean> = Outcome.catching {
        installed() && Shizuku.pingBinder() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }

    override fun requestPermission(): Outcome<Unit> = Outcome.catching {
        Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
    }

    override fun execute(vararg command: String): Outcome<String> = Outcome.catching {
        val process = Shizuku.newProcess(command, null, null)

        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()

        val stderrReader = Thread {
            runCatching { process.errorStream.use { it.copyTo(stderr) } }
        }.apply { isDaemon = true; start() }

        // 用"看门狗 + 阻塞 waitFor"：不要用 exitValue()，
        // Shizuku 的远程进程在未真正退出时会抛 "process hasn't exited"。
        val finished = AtomicBoolean(false)
        val watchdog = Thread {
            runCatching {
                Thread.sleep(COMMAND_TIMEOUT_MILLIS)
                if (!finished.get()) process.destroyForcibly()
            }
        }.apply { isDaemon = true; start() }

        process.inputStream.use { it.copyTo(stdout) }
        val exitCode = process.waitFor()

        finished.set(true)
        watchdog.interrupt()
        stderrReader.join(1000)

        val out = stdout.toString(Charsets.UTF_8.name()).trim()
        val err = stderr.toString(Charsets.UTF_8.name()).trim()

        check(exitCode == 0) {
            val detail = err.ifEmpty { out }
            "exit $exitCode" + if (detail.isEmpty()) "" else ": $detail"
        }
        out
    }

    private fun installed(): Boolean = runCatching {
        appContext.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
    }.isSuccess

    private companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val PERMISSION_REQUEST_CODE = 2001
        const val COMMAND_TIMEOUT_MILLIS = 30_000L
    }
}
