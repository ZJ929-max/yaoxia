// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.channels

import android.content.Context
import android.content.pm.PackageManager
import com.opensandbox.dhizuku.core.Outcome
import com.opensandbox.dhizuku.platform.Capability
import com.opensandbox.dhizuku.platform.CapabilityProvider
import com.opensandbox.dhizuku.platform.ProviderId
import rikka.shizuku.Shizuku
import java.util.EnumSet

class ShizukuShellChannel(context: Context) : CapabilityProvider, ShellChannel {

    private val appContext: Context = context.applicationContext

    override val id: ProviderId = ProviderId.SHI
    override val capabilities: Set<Capability> = EnumSet.of(Capability.SHELL)

    override fun describe(): String = "shi · $SHIZUKU_PACKAGE（${status()}）"

    /** 给自检/诊断用：未安装 / 服务未运行 / 未授权 / 可用。 */
    fun status(): String = when {
        !installed() -> "未安装"
        !binderAlive() -> "服务未运行"
        !permitted() -> "未授权"
        else -> "可用"
    }

    override fun isAvailable(context: Context): Outcome<Boolean> =
        Outcome.ok(installed() && binderAlive() && permitted())

    /** 装了但没授权时主动拉起授权弹窗；已授权则什么都不做。 */
    override fun requestPermission(): Outcome<Unit> = Outcome.catching {
        check(installed()) { "未安装 Shizuku" }
        check(binderAlive()) { "Shizuku 服务未运行，请先启动 Shizuku" }
        if (!permitted()) Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
        Unit
    }

    override fun execute(vararg command: String): Outcome<String> =
        if (ready()) ShizukuProcessRunner.run(command)
        else Outcome.fail("shi 不可用：${status()}")

    override fun executeDetached(vararg command: String): Outcome<Unit> =
        if (ready()) ShizukuProcessRunner.start(command)
        else Outcome.fail("shi 不可用：${status()}")

    private fun ready() = installed() && binderAlive() && permitted()

    private fun installed(): Boolean =
        runCatching { appContext.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0) }.isSuccess

    private fun binderAlive(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    private fun permitted(): Boolean =
        runCatching { Shizuku.checkSelfPermission() }.getOrDefault(-1) == PackageManager.PERMISSION_GRANTED

    private companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val PERMISSION_REQUEST_CODE = 2001
    }
}
