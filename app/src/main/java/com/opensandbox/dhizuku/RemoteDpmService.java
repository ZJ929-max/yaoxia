package com.opensandbox.dhizuku;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 这个类的实例不由本应用创建，而是由 Dhizuku 在它自己的进程里
 * 用 Class.forName + new RemoteDpmService(dhizukuContext) 反射创建。
 * 因此这里拿到的 context 是 Dhizuku 的，调用 DevicePolicyManager
 * 时的身份也是 Dhizuku（真正的 Device Owner）——权限检查才能通过。
 */
public class RemoteDpmService extends IRemoteDpm.Stub {

    private final Context context;

    public RemoteDpmService(Context context) {
        this.context = context;
    }

    private DevicePolicyManager dpm() {
        DevicePolicyManager dpm = context.getSystemService(DevicePolicyManager.class);
        if (dpm == null) throw new IllegalStateException("DevicePolicyManager 不可用");
        return dpm;
    }

    /** 找到 Dhizuku 自己的 admin 组件：所有特权调用都要带上它。 */
    private ComponentName admin() {
        DevicePolicyManager dpm = dpm();
        List<ComponentName> admins = dpm.getActiveAdmins();
        if (admins == null || admins.isEmpty()) {
            throw new IllegalStateException("当前没有任何活跃的设备管理器，Dhizuku 可能未激活");
        }
        for (ComponentName candidate : admins) {
            if (dpm.isDeviceOwnerApp(candidate.getPackageName())) return candidate;
        }
        return admins.get(0);
    }

    @Override
    public int getApiLevel() {
        return Build.VERSION.SDK_INT;
    }

    @Override
    public void onServiceCreated() {
    }

    @Override
    public void onServiceDestroyed() {
    }

    @Override
    public void setSuspended(String[] packages, boolean suspended) {
        if (packages == null || packages.length == 0) return;
        String[] failed = dpm().setPackagesSuspended(admin(), packages, suspended);
        if (failed != null && failed.length > 0) {
            throw new IllegalStateException("以下应用无法处理：" + TextUtils.join(", ", failed));
        }
    }

    @Override
    public String[] filterSuspended(String[] packages) {
        if (packages == null || packages.length == 0) return new String[0];
        DevicePolicyManager dpm = dpm();
        ComponentName admin = admin();
        List<String> suspended = new ArrayList<>();
        for (String pkg : packages) {
            if (pkg == null) continue;
            try {
                if (dpm.isPackageSuspended(admin, pkg)) suspended.add(pkg);
            } catch (PackageManager.NameNotFoundException ignored) {
                // 应用已卸载，跳过
            } catch (SecurityException ignored) {
                // 该包不在本用户下，跳过
            }
        }
        return suspended.toArray(new String[0]);
    }

    @Override
    public boolean isDeviceOwner() {
        DevicePolicyManager dpm = dpm();
        return dpm.isDeviceOwnerApp(context.getPackageName());
    }
}
