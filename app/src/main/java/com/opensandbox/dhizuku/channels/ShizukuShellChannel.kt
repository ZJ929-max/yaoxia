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
import java.util.concurrent.TimeUnit

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

        // stderr 必须并发读，否则缓冲区满了会互相卡死；很多 pm 报错只写 stderr
        val stderrReader = Thread {
            runCatching { process.errorStream.use { it.copyTo(stderr) } }
        }.apply { isDaemon = true; start() }

        process.inputStream.use { it.copyTo(stdout) }

        // 没有超时的 shell 调用会把 UI 线程外的唯一工作线程永远占住
        if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            error("超时 ${COMMAND_TIMEOUT_SECONDS}s：${command.joinToString(" ")}")
        }
        stderrReader.join(1000)

        val out = stdout.toString(Charsets.UTF_8.name()).trim()
        val err = stderr.toString(Charsets.UTF_8.name()).trim()
        val exitCode = process.exitValue()

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
        const val COMMAND_TIMEOUT_SECONDS = 30L
    }
}
