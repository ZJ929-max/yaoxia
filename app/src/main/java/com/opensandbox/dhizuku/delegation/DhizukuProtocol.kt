package com.opensandbox.dhizuku.delegation

/**
 * Dhizuku 协议里跟"名字"有关的东西，只在这里出现一次。
 * 官方 dhi 用固定权威名；兼容实现（如 she）用「包名 + 后缀」的约定。
 */
object DhizukuProtocol {

    const val OFFICIAL_PACKAGE = "com.rosan.dhizuku"
    private const val OFFICIAL_AUTHORITY = "$OFFICIAL_PACKAGE.server.provider"
    private const val AUTHORITY_SUFFIX = ".dhizuku_server.provider"

    /** 由当前 Device Owner 的包名推导出提供方权威名。 */
    fun providerAuthority(ownerPackage: String?): String = when {
        ownerPackage.isNullOrEmpty() -> OFFICIAL_AUTHORITY
        ownerPackage == OFFICIAL_PACKAGE -> OFFICIAL_AUTHORITY
        else -> ownerPackage + AUTHORITY_SUFFIX
    }
}
