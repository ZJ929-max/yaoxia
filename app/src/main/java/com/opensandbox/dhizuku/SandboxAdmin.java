package com.opensandbox.dhizuku;

import android.app.admin.DeviceAdminReceiver;

/**
 * 只是个"身份证"：DevicePolicyManager 的多数方法都需要一个 ComponentName 参数，
 * 被委派的应用也要靠它来标识自己。不需要用户去设置里手动激活。
 */
public class SandboxAdmin extends DeviceAdminReceiver {
}
