// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.provision

/** 解析 `pm list users` 的输出。抽出来是为了能单独跑单元测试。 */
object UserListParser {

    private val USER_INFO = Regex("""UserInfo\{(\d+):([^:}]+):""")

    /** 按名字找用户 id；找不到返回 null。 */
    fun findId(output: String, name: String): Int? =
        USER_INFO.findAll(output)
            .firstOrNull { it.groupValues[2] == name }
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()

    /** 列出所有用户 id（调试与诊断用）。 */
    fun allIds(output: String): List<Int> =
        USER_INFO.findAll(output).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
}
