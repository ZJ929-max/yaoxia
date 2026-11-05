package com.opensandbox.dhizuku.channels

import com.opensandbox.dhizuku.core.Outcome
import rikka.shizuku.Shizuku
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 执行 shell 命令的公共实现。
 * shi 和 she 最终都拿到同一套 Shizuku 服务接口，所以命令执行只有这一份。
 */
internal object ShizukuProcessRunner {

    private const val TIMEOUT_MILLIS = 30_000L

    /** 发出去就返回：适合 am start 这类会一直等目标用户启动的命令。 */
    fun start(command: Array<out String>): Outcome<Unit> = Outcome.catching {
        Shizuku.newProcess(command, null, null)
        Unit
    }

    fun run(command: Array<out String>): Outcome<String> = Outcome.catching {
        val process = Shizuku.newProcess(command, null, null)

        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()

        val stderrReader = Thread {
            runCatching { process.errorStream.use { it.copyTo(stderr) } }
        }.apply { isDaemon = true; start() }

        // 看门狗 + 阻塞 waitFor：不要用 exitValue()，远程进程未退出时会抛异常
        val finished = AtomicBoolean(false)
        val watchdog = Thread {
            runCatching {
                Thread.sleep(TIMEOUT_MILLIS)
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
}
