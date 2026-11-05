package com.opensandbox.dhizuku.delegation

import android.app.Activity
import android.content.Context
import com.opensandbox.dhizuku.core.Outcome

/**
 * 「向设备所有者申请能力」这件事的抽象。
 * 上层只依赖这个接口，不依赖 dhi / she / shi 的任何具体类。
 */
interface DelegationService {

    fun connect(context: Context): Outcome<Session>

    fun isAuthorized(): Outcome<Boolean>

    fun requestAuthorization(activity: Activity, callback: (Outcome<Boolean>) -> Unit)

    fun grantedScopes(): Outcome<Set<String>>

    fun requestScopes(scopes: Collection<String>): Outcome<Set<String>>

    fun supportedScopes(): Set<String>

    data class Session(
        val ownerPackage: String,
        val serverVersion: String,
        val providerAuthority: String
    )
}
