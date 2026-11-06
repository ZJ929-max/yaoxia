// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.opensandbox.dhizuku.R
import com.opensandbox.dhizuku.apps.AppControl
import com.opensandbox.dhizuku.apps.PolicyAppControl
import com.opensandbox.dhizuku.channels.PolicyChannel
import com.opensandbox.dhizuku.channels.ShellChannel
import com.opensandbox.dhizuku.core.AsyncTaskRunner
import com.opensandbox.dhizuku.core.Outcome
import com.opensandbox.dhizuku.delegation.DelegationService
import com.opensandbox.dhizuku.platform.CapabilityProvider
import com.opensandbox.dhizuku.platform.CapabilityRouter

/** 连接、授权、委派范围、通道探测。从 AppState 拆出来的第一块。 */
class ConnectionFlow(
    private val context: Context,
    private val async: AsyncTaskRunner,
    private val router: CapabilityRouter,
    private val report: (String) -> Unit,
    private val onReady: () -> Unit,
) {
    var session by mutableStateOf("")
        private set
    var channels by mutableStateOf<List<ChannelUi>>(emptyList())
        private set
    var scopes by mutableStateOf<Set<String>>(emptySet())
        private set
    var busy by mutableStateOf(false)
        private set
    var shellLabel by mutableStateOf("")
        private set

    var delegation: DelegationService? = null
        private set
    var control: AppControl? = null
        private set
    var policyChannels: List<PolicyChannel> = emptyList()
        private set
    var shell: ShellChannel? = null
        private set

    /** 还没拿到的委派范围（用于界面提示）。 */
    fun missingScopes(): List<String> =
        com.opensandbox.dhizuku.delegation.DelegationScopes.ALL.filter { it !in scopes }

    data class ChannelUi(val name: String, val available: Boolean)

    fun connect() {
        report(context.getString(R.string.status_connecting))
        refreshChannels()
        busy = true

        async.execute({
            val resolved = router.resolve(context, DelegationService::class.java)
            if (!resolved.isSuccess) {
                return@execute Outcome.fail<Handshake>(resolved.errorOrNull().orEmpty())
            }
            val service = resolved.valueOrNull()!!

            val connected = service.connect(context)
            if (!connected.isSuccess) {
                return@execute Outcome.fail<Handshake>(connected.errorOrNull().orEmpty())
            }
            val authorized = service.isAuthorized()
            if (!authorized.isSuccess) {
                return@execute Outcome.fail<Handshake>(authorized.errorOrNull().orEmpty())
            }
            Outcome.ok(
                Handshake(service, connected.valueOrNull()!!, authorized.valueOrNull() == true),
            )
        }, ::onConnected)
    }

    private fun onConnected(outcome: Outcome<Handshake>) {
        busy = false
        if (!outcome.isSuccess) {
            report(context.getString(R.string.status_connect_failed, outcome.errorOrNull().orEmpty()))
            return
        }
        val handshake = outcome.valueOrNull() ?: return
        delegation = handshake.service
        session = context.getString(
            R.string.session_summary,
            handshake.session.ownerPackage,
            handshake.session.serverVersion,
            handshake.session.providerAuthority,
        )

        if (handshake.authorized) {
            report(context.getString(R.string.status_authorized, session))
            resolvePolicy()
        } else {
            report(context.getString(R.string.status_waiting_authorization, session))
            requestAuthorization()
        }
    }

    private fun requestAuthorization() {
        delegation?.requestAuthorization(context as android.app.Activity) { result ->
            if (result.isSuccess && result.valueOrNull() == true) {
                resolvePolicy()
            } else {
                report(context.getString(R.string.status_authorization_denied))
            }
        }
    }

    private fun resolvePolicy() {
        async.execute({ router.resolve(context, PolicyChannel::class.java) }, { outcome ->
            if (!outcome.isSuccess) {
                report(
                    context.getString(
                        R.string.status_no_policy_channel,
                        outcome.errorOrNull().orEmpty(),
                    ),
                )
                return@execute
            }
            control = PolicyAppControl(outcome.valueOrNull()!!)
            policyChannels = router.available(context, PolicyChannel::class.java)
            resolveShell()
            loadScopes()
        })
    }

    private fun resolveShell() {
        async.execute({ router.resolve(context, ShellChannel::class.java) }, { outcome ->
            shell = outcome.valueOrNull()
            shellLabel = shell?.describe().orEmpty()
            if (!outcome.isSuccess) {
                // 没有任何 shell 通道时，若 shi 装了但没授权，主动拉起授权
                router.providers()
                    .filterIsInstance<ShellChannel>()
                    .forEach { runCatching { it.requestPermission() } }
            }
        })
    }

    fun refreshChannels() {
        async.execute({
            val list = router.providers().map { provider: CapabilityProvider ->
                val availability = provider.isAvailable(context)
                ChannelUi(
                    provider.describe(),
                    availability.isSuccess && availability.valueOrNull() == true,
                )
            }
            Outcome.ok(list)
        }, { outcome -> channels = outcome.valueOrNull().orEmpty() })
    }

    private fun loadScopes() {
        val service = delegation ?: return
        async.execute({ service.grantedScopes() }, { outcome ->
            if (!outcome.isSuccess) {
                report(context.getString(R.string.status_scopes_failed, outcome.errorOrNull().orEmpty()))
                return@execute
            }
            scopes = outcome.valueOrNull().orEmpty()
            report(
                if (scopes.isEmpty()) {
                    context.getString(R.string.status_no_scopes, session)
                } else {
                    context.getString(R.string.status_scopes, session, scopes.toString())
                },
            )
            onReady()
        })
    }

    fun requestScopes() {
        val service = delegation ?: return
        report(context.getString(R.string.status_requesting_scopes))
        async.execute({ service.requestScopes(service.supportedScopes()) }, { outcome ->
            report(
                if (outcome.isSuccess) {
                    context.getString(R.string.status_scopes_updated, outcome.valueOrNull().toString())
                } else {
                    context.getString(R.string.status_scope_request_failed, outcome.errorOrNull().orEmpty())
                },
            )
            loadScopes()
        })
    }

    private data class Handshake(
        val service: DelegationService,
        val session: DelegationService.Session,
        val authorized: Boolean,
    )
}
