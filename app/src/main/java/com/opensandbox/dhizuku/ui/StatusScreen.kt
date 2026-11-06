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
internal fun StatusScreen(state: AppState) {
    if (state.busy) {
        LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp))
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(state.status, style = MaterialTheme.typography.bodyMedium)
                    if (state.session.isNotEmpty()) {
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

