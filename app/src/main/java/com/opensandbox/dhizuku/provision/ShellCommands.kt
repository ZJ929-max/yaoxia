package com.opensandbox.dhizuku.provision

/**
 * shell 命令的唯一出处。
 * 命令模板散落在业务代码里是"隐性硬编码"——改一处忘一处。
 */
object ShellCommands {

    const val SHELL = "sh"
    const val EXECUTE = "-c"

    const val LIST_USERS = "pm list users"

    fun createManagedProfile(profileName: String): String =
        "pm create-user --profileOf 0 --managed $profileName"

    fun createPlainUser(profileName: String): String =
        "pm create-user $profileName"

    fun setProfileOwner(userId: Int, adminComponent: String): String =
        "dpm set-profile-owner --user $userId $adminComponent"

    fun installExisting(userId: Int, packageName: String): String =
        "pm install-existing --user $userId $packageName"
}
