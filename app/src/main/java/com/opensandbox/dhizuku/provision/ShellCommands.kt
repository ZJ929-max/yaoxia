package com.opensandbox.dhizuku.provision

/**
 * shell 命令的唯一出处。
 * 命令模板散落在业务代码里是"隐性硬编码"——改一处忘一处。
 */
object ShellCommands {
    const val SHELL = "sh"
    const val EXECUTE = "-c"

    const val LIST_USERS = "pm list users"

    fun createManagedProfile(profileName: String): String = "pm create-user --profileOf 0 --managed $profileName"

    fun createPlainUser(profileName: String): String = "pm create-user $profileName"

    fun setProfileOwner(
        userId: Int,
        adminComponent: String,
    ): String = "dpm set-profile-owner --user $userId $adminComponent"

    fun installExisting(
        userId: Int,
        packageName: String,
    ): String = "pm install-existing --user $userId $packageName"

    fun uninstallForUser(
        userId: Int,
        packageName: String,
    ): String = "pm uninstall --user $userId $packageName"

    fun listPackages(userId: Int): String = "pm list packages --user $userId"

    fun resolveActivity(
        userId: Int,
        packageName: String,
    ): String = "cmd package resolve-activity --brief --user $userId $packageName"

    fun resolveActivityLegacy(
        userId: Int,
        packageName: String,
    ): String = "cmd package resolve-activity --user $userId --brief $packageName"

    fun launchByMonkey(
        userId: Int,
        packageName: String,
    ): String = "monkey -p $packageName -c android.intent.category.LAUNCHER --user $userId 1"

    fun launch(
        userId: Int,
        component: String,
    ): String = "am start --user $userId -n $component"

    fun switchUser(userId: Int): String = "am switch-user $userId"

    /**
     * 在指定用户里启动界面，并把前台真正切过去。
     * 只发 am start --user 不会切前台，用户会以为没启动。
     */
    fun launchAndSwitch(
        userId: Int,
        component: String,
    ): String = "am start-user $userId; am switch-user $userId; am start --user $userId -n $component"

    fun launchLauncherAndSwitch(
        userId: Int,
        packageName: String,
    ): String =
        "am start-user $userId; am switch-user $userId; " +
            "monkey -p $packageName -c android.intent.category.LAUNCHER --user $userId 1"
}
