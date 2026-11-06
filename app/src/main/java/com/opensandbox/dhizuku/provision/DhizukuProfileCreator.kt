// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.provision

import android.os.IBinder
import android.os.Parcel
import com.opensandbox.dhizuku.core.Outcome
import com.rosan.dhizuku.api.Dhizuku

/**
 * 借 Device Owner 的 binder 创建「工作资料」。
 *
 * 为什么必须这样做：
 *   设备上已有 device owner（Dhizuku）时，Android 会把 `no_add_managed_profile`
 *   置位，任何非 DO 的调用（普通应用、shell、甚至 Shizuku 的 shell）执行
 *   `pm create-user --managed` 都会被拒绝。只有 DO 自己能创建。
 *
 * 做法：
 *   `user` 系统服务的 `createProfileForUser` 走 [Dhizuku.remoteTransact]，
 *   即以 Dhizuku（DO）的身份发起这次 binder 调用。
 *
 * transaction code 5 是在真机（Android 16 / SDK 36）上用 `service call` 标定的：
 *   `service call user 5 s16 <name> s16 <userType> i32 <flags> i32 <parentId>`
 *   返回的正是 "Cannot add user: no_add_managed_profile is enabled"（非 DO 调用时）。
 *
 * 返回值只表示「这次调用成功」；新建资料的 userId 请用公开 API 查：
 *   `UserManager.userProfiles.firstOrNull { it.id != Process.myUserId() }`
 * （`android.content.pm.UserInfo` 是隐藏类，公开 SDK 里引用不到。）
 */
object DhizukuProfileCreator {
    private const val USER_SERVICE = "user"
    private const val DESCRIPTOR = "android.os.IUserManager"
    private const val TRANSACTION_CREATE_PROFILE_FOR_USER = 5
    private const val MANAGED_PROFILE_TYPE = "android.os.usertype.profile.MANAGED"

    /** 创建成功返回 [Outcome.Ok]；失败带上系统给的原因。 */
    fun createManagedProfile(
        name: String,
        parentUserId: Int = 0,
    ): Outcome<Boolean> =
        Outcome.catching {
            check(Dhizuku.isPermissionGranted()) { "Dhizuku 未授权" }

            val binder = userServiceBinder() ?: error("拿不到 user 系统服务")

            val data = Parcel.obtain()
            val reply = Parcel.obtain()
            try {
                data.writeInterfaceToken(DESCRIPTOR)
                data.writeString(name)
                data.writeString(MANAGED_PROFILE_TYPE)
                data.writeInt(0) // flags
                data.writeInt(parentUserId) // profileOf

                check(
                    Dhizuku.remoteTransact(
                        binder,
                        TRANSACTION_CREATE_PROFILE_FOR_USER,
                        data,
                        reply,
                        0,
                    ),
                ) { "remoteTransact 被拒绝（Dhizuku 可能不允许转发该服务）" }

                // DO 被拒时这里抛出 SecurityException / IllegalStateException，
                // 消息就是系统给的原因（例如 no_add_managed_profile）。
                reply.readException()

                true
            } finally {
                data.recycle()
                reply.recycle()
            }
        }

    private fun userServiceBinder(): IBinder? =
        runCatching {
            val serviceManager = Class.forName("android.os.ServiceManager")
            val getService = serviceManager.getMethod("getService", String::class.java)
            getService.invoke(null, USER_SERVICE) as? IBinder
        }.getOrNull()
}
