package com.opensandbox.dhizuku.delegation;

import android.app.Activity;
import android.content.Context;

import com.opensandbox.dhizuku.core.Outcome;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 「向设备所有者申请能力」这件事的抽象。
 * 上层只依赖这个接口，不依赖 Dhizuku / Shevery 的任何类。
 */
public interface DelegationService {

    /** 连接服务并取回会话信息。 */
    Outcome<Session> connect(Context context);

    /** 当前是否已经拿到授权。 */
    Outcome<Boolean> isAuthorized();

    /** 拉起授权界面；结果通过 callback 在主线程返回。 */
    void requestAuthorization(Activity activity, Consumer<Outcome<Boolean>> callback);

    /** 已经到手的委派范围。 */
    Outcome<Set<String>> grantedScopes();

    /** 请求一组委派范围，返回请求后实际持有的范围。 */
    Outcome<Set<String>> requestScopes(Collection<String> scopes);

    /** 该实现方支持的委派范围（由实现决定，界面不写死）。 */
    Set<String> supportedScopes();

    final class Session {
        public final String ownerPackage;
        public final String serverVersion;
        public final String providerAuthority;

        public Session(String ownerPackage, String serverVersion, String providerAuthority) {
            this.ownerPackage = ownerPackage;
            this.serverVersion = serverVersion;
            this.providerAuthority = providerAuthority;
        }
    }
}
