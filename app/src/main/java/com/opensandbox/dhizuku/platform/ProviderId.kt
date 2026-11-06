// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.platform

/** 能力提供方代号。项目内固定叫法：dhi / she / shi。 */
enum class ProviderId(
    val label: String,
) {
    DHI("dhi"),
    SHE("she"),
    SHI("shi"),
}
