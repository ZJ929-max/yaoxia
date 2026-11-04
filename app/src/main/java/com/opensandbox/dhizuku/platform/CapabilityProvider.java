package com.opensandbox.dhizuku.platform;

import android.content.Context;

import com.opensandbox.dhizuku.core.Outcome;

import java.util.Set;

/** 一切能力提供方的共同契约。 */
public interface CapabilityProvider {

    ProviderId id();

    Set<Capability> capabilities();

    /** 探测当前设备上这一路是否真的可用（只读，不做授权请求）。 */
    Outcome<Boolean> isAvailable(Context context);

    /** 给界面看的一句话描述。 */
    String describe();
}
