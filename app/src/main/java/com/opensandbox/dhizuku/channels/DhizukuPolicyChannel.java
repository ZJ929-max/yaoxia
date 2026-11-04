package com.opensandbox.dhizuku.channels;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import com.opensandbox.dhizuku.SandboxAdmin;
import com.opensandbox.dhizuku.core.Outcome;
import com.opensandbox.dhizuku.delegation.DelegationService;
import com.opensandbox.dhizuku.delegation.DhizukuProtocol;
import com.opensandbox.dhizuku.platform.Capability;
import com.opensandbox.dhizuku.platform.CapabilityProvider;
import com.opensandbox.dhizuku.platform.ProviderId;
import com.rosan.dhizuku.api.Dhizuku;
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 一套实现，同时服务 dhi 与 she —— 因为它们说的是同一个协议，
 * 差别只在"谁是 Device Owner、包名是什么"。
 * 新增同类实现，只要在 ProviderRegistry 里加一行。
 */
public final class DhizukuPolicyChannel implements CapabilityProvider, PolicyChannel, DelegationService {

    private final Context context;
    private final ProviderId providerId;
    private final String ownerPackage;
    private final Set<String> supportedScopes;
    private final ComponentName admin;

    public DhizukuPolicyChannel(Context context, ProviderId providerId,
                                String ownerPackage, Set<String> supportedScopes) {
        this.context = context.getApplicationContext();
        this.providerId = providerId;
        this.ownerPackage = ownerPackage;
        this.supportedScopes = supportedScopes;
        this.admin = new ComponentName(context, SandboxAdmin.class);
    }

    // ------------------------------------------------------------- Provider

    @Override
    public ProviderId id() {
        return providerId;
    }

    @Override
    public Set<Capability> capabilities() {
        return EnumSet.of(Capability.POLICY, Capability.DELEGATION);
    }

    @Override
    public String describe() {
        return providerId.label + " · " + ownerPackage;
    }

    @Override
    public Outcome<Boolean> isAvailable(Context ignored) {
        try {
            DevicePolicyManager policyManager = policyManager();
            if (policyManager == null) {
                return Outcome.ok(false);
            }
            List<ComponentName> admins = policyManager.getActiveAdmins();
            if (admins == null) {
                return Outcome.ok(false);
            }
            for (ComponentName candidate : admins) {
                if (ownerPackage.equals(candidate.getPackageName())
                        && policyManager.isDeviceOwnerApp(ownerPackage)) {
                    return Outcome.ok(true);
                }
            }
            return Outcome.ok(false);
        } catch (Throwable throwable) {
            return Outcome.fail(throwable);
        }
    }

    // ------------------------------------------------------------- Delegation

    @Override
    public Outcome<Session> connect(Context ignored) {
        try {
            if (!Dhizuku.init(context)) {
                return Outcome.fail("初始化失败");
            }
            String version = "";
            try {
                version = Dhizuku.getVersionName();
            } catch (Throwable ignoredVersion) {
                // 版本号仅供展示
            }
            return Outcome.ok(new Session(
                    ownerPackage, version, DhizukuProtocol.providerAuthority(ownerPackage)));
        } catch (Throwable throwable) {
            return Outcome.fail(throwable);
        }
    }

    @Override
    public Outcome<Boolean> isAuthorized() {
        try {
            return Outcome.ok(Dhizuku.isPermissionGranted());
        } catch (Throwable throwable) {
            return Outcome.fail(throwable);
        }
    }

    @Override
    public void requestAuthorization(Activity activity, Consumer<Outcome<Boolean>> callback) {
        try {
            Dhizuku.requestPermission(new DhizukuRequestPermissionListener() {
                @Override
                public void onRequestPermission(int grantResult) {
                    callback.accept(Outcome.ok(grantResult == PackageManager.PERMISSION_GRANTED));
                }
            });
        } catch (Throwable throwable) {
            callback.accept(Outcome.fail(throwable));
        }
    }

    @Override
    public Outcome<Set<String>> grantedScopes() {
        try {
            String[] scopes = Dhizuku.getDelegatedScopes();
            Set<String> result = new LinkedHashSet<>();
            if (scopes != null) {
                result.addAll(Arrays.asList(scopes));
            }
            return Outcome.ok(result);
        } catch (Throwable throwable) {
            return Outcome.fail(throwable);
        }
    }

    @Override
    public Outcome<Set<String>> requestScopes(Collection<String> scopes) {
        try {
            Dhizuku.setDelegatedScopes(scopes.toArray(new String[0]));
        } catch (Throwable throwable) {
            return Outcome.fail(throwable);
        }
        return grantedScopes();
    }

    @Override
    public Set<String> supportedScopes() {
        return supportedScopes;
    }

    // ------------------------------------------------------------- Policy

    @Override
    public boolean isFrozen(String packageName) {
        return query(component -> policyManager().isPackageSuspended(component, packageName));
    }

    @Override
    public Outcome<Void> setFrozen(String packageName, boolean frozen) {
        return perform(component -> policyManager()
                .setPackagesSuspended(component, new String[]{packageName}, frozen));
    }

    @Override
    public boolean isUninstallBlocked(String packageName) {
        return query(component -> policyManager().isUninstallBlocked(component, packageName));
    }

    @Override
    public Outcome<Void> setUninstallBlocked(String packageName, boolean blocked) {
        return perform(component -> policyManager()
                .setUninstallBlocked(component, packageName, blocked));
    }

    // ------------------------------------------------------------- 内部

    private interface Action {
        void run(ComponentName component) throws Exception;
    }

    private interface Query {
        boolean run(ComponentName component) throws Exception;
    }

    private DevicePolicyManager policyManager() {
        return context.getSystemService(DevicePolicyManager.class);
    }

    /** 委派身份下 admin 参数的要求因实现方而异：先带自己的 admin，再退到 null。 */
    private Outcome<Void> perform(Action action) {
        Throwable firstFailure;
        try {
            action.run(admin);
            return Outcome.ok();
        } catch (Throwable throwable) {
            firstFailure = throwable;
        }
        try {
            action.run(null);
            return Outcome.ok();
        } catch (Throwable secondFailure) {
            return Outcome.fail(firstFailure.getClass().getSimpleName()
                    + " / " + secondFailure.getClass().getSimpleName()
                    + ": " + secondFailure.getMessage());
        }
    }

    private boolean query(Query query) {
        try {
            return query.run(admin);
        } catch (Throwable ignored) {
            try {
                return query.run(null);
            } catch (Throwable alsoIgnored) {
                return false;
            }
        }
    }
}
