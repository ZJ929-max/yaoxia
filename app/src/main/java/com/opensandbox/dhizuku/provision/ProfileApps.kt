package com.opensandbox.dhizuku.provision

import com.opensandbox.dhizuku.channels.ShellChannel
import com.opensandbox.dhizuku.core.Outcome

/**
 * 隔离用户里的应用增删查启 —— 全部走 shell（见 ShellCommands）。
 *
 * 优化（参考 Island 的 AppListManager 思路）：
 * - `pm list users` / `pm list packages` 结果做短期缓存，避免同一屏内重复调用；
 * - 任何写操作（装/卸/删用户）都会让缓存失效，保证读到的不是脏数据；
 * - 缓存 TTL 很短（秒级），代价可忽略，但能省掉大量 binder 往返。
 */
class ProfileApps(
    private val shell: ShellChannel,
) {
    private data class Entry<T>(val value: T, val at: Long)

    private var usersCache: Entry<String>? = null
    private val packagesCache = HashMap<Int, Entry<Set<String>>>()

    fun invalidate(userId: Int? = null) {
        if (userId == null) packagesCache.clear() else packagesCache.remove(userId)
    }

    private fun usersOutput(): String {
        val now = System.currentTimeMillis()
        usersCache?.let { if (now - it.at < USERS_TTL) return it.value }
        val fresh = run(ShellCommands.LIST_USERS)
        usersCache = Entry(fresh, now)
        return fresh
    }

    /** 按名字找已存在的隔离用户（例如上次建好的 user 11）。 */
    fun findUser(name: String): Outcome<Int?> = Outcome.catching { UserListParser.findId(usersOutput(), name) }

    fun installed(userId: Int): Outcome<Set<String>> = Outcome.catching {
        val now = System.currentTimeMillis()
        packagesCache[userId]?.let { if (now - it.at < PACKAGES_TTL) return@catching it.value }

        val fresh = run(ShellCommands.listPackages(userId))
            .lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:") }
            .filterNot { isSystemPackage(it) }
            .toCollection(linkedSetOf())

        packagesCache[userId] = Entry(fresh, now)
        fresh
    }

    fun install(userId: Int, packageName: String): Outcome<Unit> = Outcome.catching {
        run(ShellCommands.installExisting(userId, packageName))
        invalidate(userId)
    }

    fun remove(userId: Int, packageName: String): Outcome<Unit> = Outcome.catching {
        run(ShellCommands.uninstallForUser(userId, packageName))
        invalidate(userId)
    }

    /** 解析启动组件；两种 --user 位置都试，都拿不到返回 null（交给 monkey 兜底）。 */
    fun resolveComponent(userId: Int, packageName: String): String? {
        for (command in listOf(
            ShellCommands.resolveActivity(userId, packageName),
            ShellCommands.resolveActivityLegacy(userId, packageName),
        )) {
            val output = shell.execute(ShellCommands.SHELL, ShellCommands.EXECUTE, command)
                .valueOrNull().orEmpty()
            output.lineSequence()
                .map { it.trim() }
                .lastOrNull { it.contains("/") && !it.startsWith("priority=") }
                ?.let { return it }
        }
        return null
    }

    /** 删除整个隔离用户：先停再删。 */
    fun removeUser(userId: Int): Outcome<Unit> = Outcome.catching {
        runCatching { run(ShellCommands.stopUser(userId)) }
        run(ShellCommands.removeUser(userId))
        usersCache = null
        invalidate(userId)
    }

    fun launchByMonkey(userId: Int, packageName: String): Outcome<Unit> = Outcome.catching {
        shell.executeDetached(
            ShellCommands.SHELL,
            ShellCommands.EXECUTE,
            ShellCommands.launchByMonkey(userId, packageName),
        )
    }

    fun launch(userId: Int, component: String): Outcome<Unit> =
        Outcome.catching { run(ShellCommands.launch(userId, component)) }

    /** 粗略过滤系统应用：覆盖 AOSP / Google / 小米 / 我们自己的管理器。 */
    private fun isSystemPackage(packageName: String): Boolean =
        SYSTEM_PREFIXES.any { packageName == it || packageName.startsWith("$it.") }

    private fun run(command: String): String {
        val result = shell.execute(ShellCommands.SHELL, ShellCommands.EXECUTE, command)
        if (!result.isSuccess) error("命令失败：$command → ${result.errorOrNull()}")
        return result.valueOrNull().orEmpty()
    }

    private companion object {
        const val USERS_TTL = 3_000L
        const val PACKAGES_TTL = 2_000L

        val SYSTEM_PREFIXES = listOf(
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
    }
}
