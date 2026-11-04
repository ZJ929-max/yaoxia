package com.opensandbox.dhizuku.platform;

import android.content.Context;

import com.opensandbox.dhizuku.core.Outcome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 能力路由：按「接口类型 + 当前可用性」挑通道。
 * 不同能力可以落到不同提供方——这就是"混合调用"：
 * 例如策略走 she、shell 走 shi，各取所长。
 */
public final class CapabilityRouter {

    private final List<CapabilityProvider> providers;

    public CapabilityRouter(List<CapabilityProvider> providers) {
        this.providers = new ArrayList<>(providers);
    }

    public List<CapabilityProvider> providers() {
        return Collections.unmodifiableList(providers);
    }

    /** 按接口类型解析出第一个可用的通道；全部不可用时返回失败原因。 */
    public <T> Outcome<T> resolve(Context context, Class<T> channelType) {
        List<String> unavailable = new ArrayList<>();
        for (CapabilityProvider provider : providers) {
            if (!channelType.isInstance(provider)) {
                continue;
            }
            Outcome<Boolean> availability = provider.isAvailable(context);
            if (availability.isSuccess() && Boolean.TRUE.equals(availability.value())) {
                return Outcome.ok(channelType.cast(provider));
            }
            unavailable.add(provider.id().label);
        }
        return Outcome.fail(unavailable.isEmpty()
                ? "没有任何实现该能力的通道"
                : "通道均不可用：" + String.join(", ", unavailable));
    }
}
