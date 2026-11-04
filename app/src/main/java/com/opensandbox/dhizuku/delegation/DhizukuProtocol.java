package com.opensandbox.dhizuku.delegation;

/**
 * Dhizuku 协议里跟"名字"有关的东西，只在这里出现一次。
 * 官方 Dhizuku 用固定权威名；兼容实现（如 Shevery）用「包名 + 后缀」的约定。
 */
public final class DhizukuProtocol {

    private static final String OFFICIAL_PACKAGE = "com.rosan.dhizuku";
    private static final String OFFICIAL_AUTHORITY = OFFICIAL_PACKAGE + ".server.provider";
    private static final String AUTHORITY_SUFFIX = ".dhizuku_server.provider";

    private DhizukuProtocol() {
    }

    /** 由当前 Device Owner 的包名推导出提供方权威名。 */
    public static String providerAuthority(String ownerPackage) {
        if (ownerPackage == null || ownerPackage.isEmpty()) {
            return OFFICIAL_AUTHORITY;
        }
        return OFFICIAL_PACKAGE.equals(ownerPackage)
                ? OFFICIAL_AUTHORITY
                : ownerPackage + AUTHORITY_SUFFIX;
    }
}
