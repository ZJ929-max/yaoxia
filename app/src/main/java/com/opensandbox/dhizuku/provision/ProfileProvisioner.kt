// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.provision

import com.opensandbox.dhizuku.core.Outcome

/**
 * 「建一个隔离空间」的抽象。
 * 真正的实现只能走 shell 通道——创建受管用户与指定 Profile Owner 都是 shell 专属能力。
 */
interface ProfileProvisioner {
    fun ensureManagedProfile(profileName: String): Outcome<ProfileInfo>

    data class ProfileInfo(
        val userId: Int,
        val name: String,
        val createdNow: Boolean,
    )
}
