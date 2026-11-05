package com.opensandbox.dhizuku.provision

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import com.opensandbox.dhizuku.core.Outcome

/**
 * 走系统自带的「配置工作资料」流程（Island 用的就是这条，Apache-2.0）：
 *
 *   isProvisioningAllowed → startActivityForResult(ACTION_PROVISION_MANAGED_PROFILE)
 *
 * 系统界面会建好工作资料，并把我们的 DeviceAdminReceiver 设为资料所有者。
 * 比 `pm create-user` 稳，因为不需要 shell，也不受 DO 占用影响。
 */
class SystemProfileProvisioner(
    private val activity: Activity,
    private val admin: ComponentName
) {

    companion object {
        const val REQUEST_PROVISION = 3001
    }

    private val policyManager: DevicePolicyManager?
        get() = activity.getSystemService(DevicePolicyManager::class.java)

    fun isAllowed(): Boolean = runCatching {
        policyManager?.isProvisioningAllowed(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE) == true
    }.getOrDefault(false)

    fun launch(): Outcome<Unit> = Outcome.catching {
        check(isAllowed()) { "系统当前不允许创建工作资料（可能已存在，或被设备所有者禁止）" }

        val intent = Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE).apply {
            putExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME,
                admin
            )
            putExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_PACKAGE_NAME,
                admin.packageName
            )
        }
        activity.startActivityForResult(intent, REQUEST_PROVISION)
    }

    /** 系统流程返回后调用；resultOk 表示工作资料已建好并归我们管。 */
    fun onResult(resultCode: Int): Outcome<Unit> =
        if (resultCode == Activity.RESULT_OK) Outcome.ok(Unit)
        else Outcome.fail("配置被取消或失败（resultCode=$resultCode）")
}
