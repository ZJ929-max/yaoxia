package com.opensandbox.dhizuku.provision

import com.opensandbox.dhizuku.channels.ShellChannel
import com.opensandbox.dhizuku.core.Outcome

/** 对某个隔离用户里的应用做增删查启——全部走 shell（见 ShellCommands）。 */
class ProfileApps(
    private val shell: ShellChannel,
) {
    private val SYSTEM_PREFIXES =
        listOf(
            "android",
            "com.android",
            "com.google",
            "com.miui",
            "com.xiaomi",
            "com.qualcomm",
            "com.mediatek",
            "com.hamondev",
            "com.rosan",
            "moe.shizuku",
        )

    private val userPattern = Regex("UserInfo\\{(\\d+):([^:}]+):")

    /** 按名字找已存在的隔离用户（例如上次建好的 user 10）。 */
    fun findUser(name: String): Outcome<Int?> =
        Outcome.catching {
            val output = run(ShellCommands.LIST_USERS)
            userPattern
                .findAll(output)
                .firstOrNull { it.groupValues[2] == name }
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
        }

    fun installed(userId: Int): Outcome<Set<String>> =
        Outcome.catching {
            run(ShellCommands.listPackages(userId))
                .lineSequence()
                .map { it.trim() }
                .filter { it.startsWith("package:") }
                .map { it.removePrefix("package:") }
                .filterNot { isSystemPackage(it) }
                .toCollection(linkedSetOf())
        }

    fun install(
        userId: Int,
        packageName: String,
    ): Outcome<Unit> = Outcome.catching { run(ShellCommands.installExisting(userId, packageName)) }

    fun remove(
        userId: Int,
        packageName: String,
    ): Outcome<Unit> = Outcome.catching { run(ShellCommands.uninstallForUser(userId, packageName)) }

    /** 解析启动组件；两种 --user 位置都试，都拿不到返回 null（交给 monkey 兜底）。 */
    fun resolveComponent(
        userId: Int,
        packageName: String,
    ): String? {
        for (command in listOf(
            ShellCommands.resolveActivity(userId, packageName),
            ShellCommands.resolveActivityLegacy(userId, packageName),
        )) {
            val output =
                shell
                    .execute(ShellCommands.SHELL, ShellCommands.EXECUTE, command)
                    .valueOrNull()
                    .orEmpty()
            output
                .lineSequence()
                .map { it.trim() }
                .lastOrNull { it.contains("/") && !it.startsWith("priority=") }
                ?.let { return it }
        }
        return null
    }

    fun launchByMonkey(
        userId: Int,
        packageName: String,
    ): Outcome<Unit> =
        Outcome.catching {
            shell.executeDetached(
                ShellCommands.SHELL,
                ShellCommands.EXECUTE,
                ShellCommands.launchByMonkey(userId, packageName),
            )
        }

    fun launch(
        userId: Int,
        component: String,
    ): Outcome<Unit> = Outcome.catching { run(ShellCommands.launch(userId, component)) }

    /** 粗略过滤系统应用：这些前缀基本覆盖 AOSP / Google / 小米 / 我们自己的管理器。 */
    private fun isSystemPackage(packageName: String): Boolean = SYSTEM_PREFIXES.any { packageName == it || packageName.startsWith("$it.") }

    private fun run(command: String): String {
        val result = shell.execute(ShellCommands.SHELL, ShellCommands.EXECUTE, command)
        if (!result.isSuccess) error("命令失败：$command → ${result.errorOrNull()}")
        return result.valueOrNull().orEmpty()
    }
}
