package com.opensandbox.dhizuku.provision

import com.opensandbox.dhizuku.channels.ShellChannel
import com.opensandbox.dhizuku.core.Outcome

/** 对某个隔离用户里的应用做增删查启——全部走 shell（见 ShellCommands）。 */
class ProfileApps(private val shell: ShellChannel) {

    private val userPattern = Regex("UserInfo\\{(\\d+):([^:}]+):")

    /** 按名字找已存在的隔离用户（例如上次建好的 user 10）。 */
    fun findUser(name: String): Outcome<Int?> = Outcome.catching {
        val output = run(ShellCommands.LIST_USERS)
        userPattern.findAll(output)
            .firstOrNull { it.groupValues[2] == name }
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
    }

    fun installed(userId: Int): Outcome<Set<String>> = Outcome.catching {
        run(ShellCommands.listPackages(userId))
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:") }
            .toCollection(linkedSetOf())
    }

    fun install(userId: Int, packageName: String): Outcome<Unit> =
        Outcome.catching { run(ShellCommands.installExisting(userId, packageName)) }

    fun remove(userId: Int, packageName: String): Outcome<Unit> =
        Outcome.catching { run(ShellCommands.uninstallForUser(userId, packageName)) }

    fun launch(userId: Int, component: String): Outcome<Unit> =
        Outcome.catching { run(ShellCommands.launch(userId, component)) }

    private fun run(command: String): String {
        val result = shell.execute(ShellCommands.SHELL, ShellCommands.EXECUTE, command)
        if (!result.isSuccess) error("命令失败：$command → ${result.errorOrNull()}")
        return result.valueOrNull().orEmpty()
    }
}
