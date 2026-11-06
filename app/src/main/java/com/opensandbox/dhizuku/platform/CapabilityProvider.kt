// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.platform

import android.content.Context
import com.opensandbox.dhizuku.core.Outcome

/** 一切能力提供方的共同契约。 */
interface CapabilityProvider {
    val id: ProviderId

    val capabilities: Set<Capability>

    /** 探测当前设备上这一路是否真的可用（只读，不触发授权）。 */
    fun isAvailable(context: Context): Outcome<Boolean>

    /** 给界面看的一句话描述。 */
    fun describe(): String
}
