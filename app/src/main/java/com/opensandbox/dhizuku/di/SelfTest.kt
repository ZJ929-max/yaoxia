// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.di

import android.content.Context
import android.util.Log
import com.opensandbox.dhizuku.channels.ShellChannel
import com.opensandbox.dhizuku.platform.CapabilityRouter
import com.opensandbox.dhizuku.provision.ProfileApps
import com.opensandbox.dhizuku.provision.ShellCommands
import com.opensandbox.dhizuku.provision.ShellProfileProvisioner
import com.opensandbox.dhizuku.provision.UserListParser

/**
 * 无 UI 全链路自检：只做只读操作 + 可选的一步写操作。
 * 输出到 logcat，tag = YaoxiaTest，便于 adb 抓取。
 */
class SelfTest(
    private val context: Context,
    private val router: CapabilityRouter,
) {
    data class Result(val step: String, val ok: Boolean, val detail: String)

    fun run(): List<Result> {
        val results = mutableListOf<Result>()
        fun step(name: String, block: () -> String) {
            val r = try {
                Result(name, true, block())
            } catch (t: Throwable) {
                Result(name, false, "${t.javaClass.simpleName}: ${t.message}")
            }
            results += r
            Log.i(TAG, "${if (r.ok) "PASS" else "FAIL"} ${r.step} → ${r.detail}")
        }

        step("provider 列表") {
            router.providers().joinToString(",") { "${it.id.label}:${it.describe()}" }
        }
        step("delegation 连接") {
            val d = router.resolve(context, com.opensandbox.dhizuku.delegation.DelegationService::class.java)
            if (!d.isSuccess) error(d.errorOrNull().orEmpty())
            val s = d.valueOrNull()!!.connect(context)
            if (!s.isSuccess) error(s.errorOrNull().orEmpty())
            val session = s.valueOrNull()!!
            "owner=${session.ownerPackage} version=${session.serverVersion}"
        }
        step("委派范围") {
            val d = router.resolve(context, com.opensandbox.dhizuku.delegation.DelegationService::class.java)
            d.valueOrNull()?.grantedScopes()?.valueOrNull().orEmpty().joinToString(",")
        }

        var shell: ShellChannel? = null
        step("shell 通道") {
            val r = router.resolve(context, ShellChannel::class.java)
            if (!r.isSuccess) error(r.errorOrNull().orEmpty())
            shell = r.valueOrNull()
            shell!!.describe()
        }

        step("pm list users 解析") {
            val sh = shell ?: error("shell 不可用")
            val out = sh.execute(ShellCommands.SHELL, ShellCommands.EXECUTE, ShellCommands.LIST_USERS)
                .valueOrNull().orEmpty()
            val ids = UserListParser.allIds(out)
            check(ids.isNotEmpty()) { "解析不到用户，原始输出：${out.take(80)}" }
            "ids=$ids"
        }

        step("隔离空间定位") {
            val sh = shell ?: error("shell 不可用")
            val out = sh.execute(ShellCommands.SHELL, ShellCommands.EXECUTE, ShellCommands.LIST_USERS)
                .valueOrNull().orEmpty()
            val id = UserListParser.findId(out, ShellProfileProvisioner.DEFAULT_PROFILE_NAME)
            "user=${id ?: "不存在"}"
        }

        step("隔离空间内应用数") {
            val sh = shell ?: error("shell 不可用")
            val apps = ProfileApps(sh)
            val out = sh.execute(ShellCommands.SHELL, ShellCommands.EXECUTE, ShellCommands.LIST_USERS)
                .valueOrNull().orEmpty()
            val id = UserListParser.findId(out, ShellProfileProvisioner.DEFAULT_PROFILE_NAME)
                ?: return@step "无隔离空间"
            val list = apps.installed(id)
            if (!list.isSuccess) error(list.errorOrNull().orEmpty())
            "user=$id count=${list.valueOrNull()?.size}"
        }

        step("策略通道只读查询") {
            val ch = router.available(context, com.opensandbox.dhizuku.channels.PolicyChannel::class.java)
            check(ch.isNotEmpty()) { "没有策略通道" }
            val pkg = context.packageName
            ch.joinToString(";") { "${it.id.label}: blocked=${it.isUninstallBlocked(pkg)}" }
        }

        step("应用列表加载") {
            val catalog = com.opensandbox.dhizuku.apps.AppCatalog()
            val apps = catalog.load(context.packageManager, context.packageName)
            "count=${apps.size} first=${apps.firstOrNull()?.label}"
        }

        Log.i(TAG, "SUMMARY total=${results.size} fail=${results.count { !it.ok }}")
        return results
    }

    private companion object { const val TAG = "YaoxiaTest" }
}
