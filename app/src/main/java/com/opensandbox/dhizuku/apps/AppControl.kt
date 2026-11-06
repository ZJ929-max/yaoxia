// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.apps

import com.opensandbox.dhizuku.core.Outcome

/**
 * 对单个应用执行管控动作的抽象。
 * 界面依赖它，而不是直接依赖 DevicePolicyManager。
 */
interface AppControl {
    fun isFrozen(packageName: String): Boolean

    fun setFrozen(
        packageName: String,
        frozen: Boolean,
    ): Outcome<Unit>

    fun isUninstallBlocked(packageName: String): Boolean

    fun setUninstallBlocked(
        packageName: String,
        blocked: Boolean,
    ): Outcome<Unit>
}
