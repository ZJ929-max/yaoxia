// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
internal fun StatusScreen(state: AppState, topInset: androidx.compose.ui.unit.Dp, bottomInset: androidx.compose.ui.unit.Dp) {
    if (state.busy) {
        SkinLinearProgress(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    ReportScrollProgress(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Metrics.pageHorizontal,
            top = topInset + 4.dp,
            end = Metrics.pageHorizontal,
            bottom = bottomInset + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.status, style = MaterialTheme.typography.bodyMedium)
                    // status 文本本身已经带了会话信息，避免重复打印两遍
                    if (state.session.isNotEmpty() && !state.status.contains("DeviceOwner")) {
                        Text(
                            state.session,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.section_channels), fontWeight = FontWeight.SemiBold)
                    state.channels.forEach { channel ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(channel.name, style = MaterialTheme.typography.bodySmall)
                            Text(
                                stringResource(
                                    if (channel.available) {
                                        R.string.channel_available
                                    } else {
                                        R.string.channel_unavailable
                                    },
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color =
                                    if (channel.available) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                            )
                        }
                    }
                }
            }
        }

        if (state.missingScopes.isNotEmpty()) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.status_missing_scopes, state.missingScopes.joinToString(", ")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SkinButton(onClick = state::connect, enabled = !state.busy) {
                    Text(stringResource(R.string.button_connect))
                }
                SkinOutlinedButton(onClick = state::requestScopes) {
                    Text(stringResource(R.string.button_scopes))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 应用页

