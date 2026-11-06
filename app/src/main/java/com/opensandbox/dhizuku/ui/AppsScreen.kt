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
internal fun AppsScreen(state: AppState) {
    var query by rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    val visible =
        state.apps.filter {
            query.isBlank() || it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.apps_count, visible.size),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = state::refreshApps) {
                Text(stringResource(R.string.button_refresh))
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.apps_search)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        if (visible.isEmpty()) {
            Text(
                stringResource(R.string.apps_empty),
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }

        var menuFor by remember { mutableStateOf<String?>(null) }

        LazyColumn(Modifier.fillMaxSize()) {
            items(visible, key = { it.packageName }) { app ->
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(app.label, fontWeight = FontWeight.Medium)
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { state.toggleUninstallBlock(app) }) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = stringResource(R.string.apps_uninstall_block),
                                tint =
                                    if (app.uninstallBlocked) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                            )
                        }
                        SkinSwitch(
                            checked = app.frozen,
                            enabled = app.packageName !in state.busyPackages,
                            onCheckedChange = { target -> state.setFrozen(app, target) },
                        )
                        Box {
                            IconButton(onClick = { menuFor = app.packageName }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = null)
                            }
                            DropdownMenu(
                                expanded = menuFor == app.packageName,
                                onDismissRequest = { menuFor = null },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.apps_menu_install)) },
                                    onClick = {
                                        menuFor = null
                                        state.installIntoProfile(app.packageName)
                                    },
                                )
                            }
                        }
                    }
                    Divider()
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 隔离空间页

