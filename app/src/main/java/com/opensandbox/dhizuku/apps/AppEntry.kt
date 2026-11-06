// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.apps

/** 一个可被管控的应用：只描述"是谁"，不描述"状态"。 */
data class AppEntry(
    val packageName: String,
    val label: String,
)
