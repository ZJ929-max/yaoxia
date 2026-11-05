package com.opensandbox.dhizuku.platform

import android.content.Context
import com.opensandbox.dhizuku.core.Outcome

/**
 * 能力路由：按「接口类型 + 当前可用性」挑通道。
 * 不同能力可以落到不同提供方——这就是混合调用：
 * 例如策略走 she、shell 走 shi，各取所长。
 */
class CapabilityRouter(private val providers: List<CapabilityProvider>) {

    fun providers(): List<CapabilityProvider> = providers.toList()

    /** 解析出第一个可用的通道；全部不可用时返回失败原因。 */
    fun <T : Any> resolve(context: Context, channelType: Class<T>): Outcome<T> {
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
            if (unavailable.isEmpty()) "没有任何实现该能力的通道"
            else "通道均不可用：" + unavailable.joinToString(", ")
        )
    }
}
