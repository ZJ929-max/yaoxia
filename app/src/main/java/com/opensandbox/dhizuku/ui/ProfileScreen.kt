// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensandbox.dhizuku.BuildConfig
import com.opensandbox.dhizuku.R

@Composable
internal fun ProfileScreen(state: AppState, bottomInset: androidx.compose.ui.unit.Dp) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.profile_explain),
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                stringResource(R.string.profile_kind_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkinButton(onClick = state::createProfile) {
                    Text(stringResource(R.string.button_create_profile))
                }
                SkinOutlinedButton(
                    onClick = state::refreshProfileApps,
                    enabled = !state.profileBusy,
                ) {
                    Text(stringResource(R.string.profile_refresh_apps))
                }
            }
            SkinOutlinedButton(
                onClick = state::switchToOwner,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.profile_switch_back))
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    state.status,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        if (state.profileBusy) {
            SkinLinearProgress(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
        }

        var filter by rememberSaveable { mutableIntStateOf(0) }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(R.string.filter_all, R.string.filter_installed, R.string.filter_not_installed)
                .forEachIndexed { index, res ->
                    FilterChip(
                        selected = filter == index,
                        onClick = { filter = index },
                        label = { Text(stringResource(res)) },
                    )
                }
        }

        if (state.profileUserId == null) {
            Text(
                stringResource(R.string.profile_none),
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }

        val listed =
            state.apps.filter { app ->
                val installed = app.packageName in state.profileApps
                when (filter) {
                    1 -> installed
                    2 -> !installed
                    else -> true
                }
            }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = Metrics.pageHorizontal,
                    top = 8.dp,
                    end = Metrics.pageHorizontal,
                    bottom = bottomInset + 16.dp,
                ),
        ) {
            itemsIndexed(listed, key = { _, app -> app.packageName }) { index, app ->
                val installed = app.packageName in state.profileApps
                ListRow(slot = rowSlot(index, listed.size)) {
                    Column(Modifier.weight(1f)) {
                        SkinText(app.label, fontWeight = FontWeight.Medium)
                        Text(
                            stringResource(
                                if (installed) R.string.profile_state_installed
                                else R.string.profile_state_absent,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (installed) {
                        TextButton(onClick = { state.requestLaunchInProfile(app) }) {
                            Text(stringResource(R.string.profile_open))
                        }
                        TextButton(onClick = { state.removeFromProfile(app.packageName) }) {
                            Text(stringResource(R.string.profile_remove))
                        }
                    } else {
                        TextButton(onClick = { state.installIntoProfile(app.packageName) }) {
                            Text(stringResource(R.string.profile_install))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 设置页

