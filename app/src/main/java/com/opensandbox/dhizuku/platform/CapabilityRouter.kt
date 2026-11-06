// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.platform

import android.content.Context
import com.opensandbox.dhizuku.core.Outcome

/**
 * 能力路由：按「接口类型 + 当前可用性」挑通道。
 * 不同能力可以落到不同提供方——这就是混合调用：
 * 例如策略走 she、shell 走 shi，各取所长。
 */
class CapabilityRouter(
    private val providers: List<CapabilityProvider>,
) {
    fun providers(): List<CapabilityProvider> = providers.toList()

    /** 按优先级列出所有可用通道，失败时可依次重试。 */
    fun <T : Any> available(
        context: Context,
        channelType: Class<T>,
    ): List<T> =
        providers
            .filter { channelType.isInstance(it) }
            .filter { provider ->
                val availability = provider.isAvailable(context)
                availability.isSuccess && availability.valueOrNull() == true
            }.map { channelType.cast(it) }

    /** 解析出第一个可用的通道；全部不可用时返回失败原因。 */
    fun <T : Any> resolve(
        context: Context,
        channelType: Class<T>,
    ): Outcome<T> {
        val unavailable = mutableListOf<String>()
        for (provider in providers) {
            if (!channelType.isInstance(provider)) continue

            val availability = provider.isAvailable(context)
            if (availability.isSuccess && availability.valueOrNull() == true) {
                return Outcome.ok(channelType.cast(provider))
            }
            unavailable += provider.id.label
        }
        return Outcome.fail(
            if (unavailable.isEmpty()) {
                "没有任何实现该能力的通道"
            } else {
                "通道均不可用：" + unavailable.joinToString(", ")
            },
        )
    }
}
