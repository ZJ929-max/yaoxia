// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.channels

import com.opensandbox.dhizuku.core.Outcome
import com.opensandbox.dhizuku.platform.CapabilityProvider

/**
 * shell 通道：工作资料的创建、把应用装进资料、指定 Profile Owner
 * 都只能经这条通道完成（需要 shell 身份，普通应用和 Device Owner 都做不到）。
 */
interface ShellChannel : CapabilityProvider {
    fun execute(vararg command: String): Outcome<String>

    /** 不等待结果，避免长命令把工作线程占死。 */
    fun executeDetached(vararg command: String): Outcome<Unit>

    fun requestPermission(): Outcome<Unit>
}
