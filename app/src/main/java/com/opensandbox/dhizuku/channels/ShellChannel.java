package com.opensandbox.dhizuku.channels;

import com.opensandbox.dhizuku.core.Outcome;
import com.opensandbox.dhizuku.platform.CapabilityProvider;

/**
 * shell 通道：工作资料的创建、把应用装进资料、指定 Profile Owner
 * 都只能经这条通道完成（需要 shell 身份，普通应用和 Device Owner 都做不到）。
 */
public interface ShellChannel extends CapabilityProvider {

    Outcome<String> execute(String... command);

    Outcome<Void> requestPermission();
}
