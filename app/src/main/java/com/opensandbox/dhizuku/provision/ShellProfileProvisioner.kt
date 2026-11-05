package com.opensandbox.dhizuku.provision

import android.content.Context
import com.opensandbox.dhizuku.SandboxAdmin
import com.opensandbox.dhizuku.channels.ShellChannel
import com.opensandbox.dhizuku.core.Outcome

/**
 * 用 shell 通道把"工作资料"建起来。顺序是死的，错一步就得删用户重来：
 *
 * 1. pm list users            —— 已有同名用户就直接复用
 * 2. pm create-user --profileOf 0 --managed <name>
 * 3. dpm set-profile-owner --user <id> <pkg>/.SandboxAdmin   ← 必须在用户"完成设置"之前
 * 4. pm install-existing --user <id> <pkg>                   —— 把自己装进资料，才能在资料里当 PO
 */
class ShellProfileProvisioner(
    private val context: Context,
    private val shell: ShellChannel
) : ProfileProvisioner {

    private val packageName: String = context.packageName

    private val adminComponent: String =
        android.content.ComponentName(context, SandboxAdmin::class.java).flattenToShortString()

    override fun ensureManagedProfile(profileName: String): Outcome<ProfileProvisioner.ProfileInfo> =
        Outcome.catching {
            findUser(profileName)?.let { userId ->
                return@catching ProfileProvisioner.ProfileInfo(userId, profileName, createdNow = false)
            }

            val usersBefore = runCatching { run(ShellCommands.LIST_USERS) }.getOrDefault("")
            val created = try {
                run(ShellCommands.createManagedProfile(profileName))
            } catch (throwable: Throwable) {
                error("${throwable.message}\n当前用户列表：\n$usersBefore")
            }
            val userId = parseCreatedUserId(created)
                ?: error("无法从输出中解析新用户 id：$created")

            // 关键顺序：先设 Profile Owner，再装应用，最后才允许启动该用户
            run(ShellCommands.setProfileOwner(userId, adminComponent))
            run(ShellCommands.installExisting(userId, packageName))

            ProfileProvisioner.ProfileInfo(userId, profileName, createdNow = true)
        }

    /** 已存在同名用户时直接返回它的 id。 */
    private fun findUser(profileName: String): Int? {
        val output = run(ShellCommands.LIST_USERS)
        val pattern = Regex("""UserInfo\{(\d+):([^:}]+):""")
        return pattern.findAll(output)
            .firstOrNull { it.groupValues[2] == profileName }
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
    }

    private fun parseCreatedUserId(output: String): Int? =
        Regex("""created user id (\d+)""").find(output)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("""\bid (\d+)""").find(output)?.groupValues?.get(1)?.toIntOrNull()

    private fun run(command: String): String {
        val result = shell.execute(ShellCommands.SHELL, ShellCommands.EXECUTE, command)
        if (!result.isSuccess) {
            error("命令失败：$command → ${result.errorOrNull()}")
        }
        return result.valueOrNull().orEmpty()
    }

    companion object {
        const val DEFAULT_PROFILE_NAME = "OpenSandbox"
    }
}
