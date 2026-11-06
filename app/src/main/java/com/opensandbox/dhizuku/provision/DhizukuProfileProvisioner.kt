// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.provision

import android.content.Context
import android.os.Build
import android.os.UserManager
import com.opensandbox.dhizuku.core.Outcome

/**
 * 走 Device Owner 的 binder 创建「工作资料」——不需要 shell 通道。
 *
 * 这是本机唯一能建出 managed profile 的路：设备已有 DO 时，
 * `pm create-user --managed` 会被 `no_add_managed_profile` 拒绝，
 * 只有 DO 自己能调 createProfileForUser。
 *
 * 注意：这里只碰 `getEnabledProfileIds()`（返回 int[]），
 * 不碰 `userProfiles`（返回隐藏类 `UserInfo`，公开 SDK 里引用不到）。
 */
class DhizukuProfileProvisioner(
    private val context: Context,
) : ProfileProvisioner {
    private val userManager: UserManager?
        get() = context.getSystemService(UserManager::class.java)

    /** 当前用户之外的、已启用的资料 id。API 30+ 才有；更老的系统直接返回空。 */
    private fun profileIds(me: Int): IntArray {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return IntArray(0)
        val manager = userManager ?: return IntArray(0)
        // getEnabledProfileIds 是 @SystemApi，公开 SDK 里没有；反射调用，
        // 被隐藏 API 策略拦下时返回空数组，由调用方按"没建出来"处理。
        return runCatching {
            val method = manager.javaClass.getMethod("getEnabledProfileIds", Int::class.javaPrimitiveType)
            method.invoke(manager, me) as? IntArray ?: IntArray(0)
        }.getOrDefault(IntArray(0))
    }

    override fun ensureManagedProfile(profileName: String): Outcome<ProfileProvisioner.ProfileInfo> =
        Outcome.catching {
            val me = android.os.Process.myUid() / 100000

            profileIds(me).firstOrNull()?.let { existing ->
                return@catching ProfileProvisioner.ProfileInfo(existing, profileName, createdNow = false)
            }

            val created = DhizukuProfileCreator.createManagedProfile(profileName, me)
            if (!created.isSuccess) error(created.errorOrNull().orEmpty())

            val newId =
                profileIds(me).firstOrNull()
                    ?: error("调用成功但系统还没返回工作资料（可能需要等几秒再刷新）")

            ProfileProvisioner.ProfileInfo(newId, profileName, createdNow = true)
        }
}
