package com.opensandbox.dhizuku.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensandbox.dhizuku.R

/** 应用页：支持长按进入多选，批量冻结/解冻/装进隔离。 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun AppsScreen(state: AppState) {
    var query by rememberSaveable { mutableStateOf("") }
    var selecting by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }

    val visible = state.apps.filter {
        query.isBlank() ||
            it.label.contains(query, ignoreCase = true) ||
            it.packageName.contains(query, ignoreCase = true)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (selecting) {
                    stringResource(R.string.apps_selected, selected.size)
                } else {
                    stringResource(R.string.apps_count, visible.size)
                },
                style = MaterialTheme.typography.bodySmall,
            )
            if (!selecting) {
                TextButton(onClick = state::refreshApps) {
                    Text(stringResource(R.string.button_refresh))
                }
            }
        }

        if (selecting) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = { state.freezeBatch(selected, true) }) {
                    Text(stringResource(R.string.apps_freeze_selected))
                }
                TextButton(onClick = { state.freezeBatch(selected, false) }) {
                    Text(stringResource(R.string.apps_unfreeze_selected))
                }
                TextButton(onClick = { state.installBatch(selected) }) {
                    Text(stringResource(R.string.apps_install_selected))
                }
                TextButton(onClick = {
                    selecting = false
                    selected = emptySet()
                }) { Text(stringResource(R.string.apps_cancel_select)) }
            }
        } else {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.apps_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
        }

        if (visible.isEmpty()) {
            Text(
                stringResource(R.string.apps_empty),
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            return@Column
        }

        var menuFor by remember { mutableStateOf<String?>(null) }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            items(visible, key = { it.packageName }) { app ->
                val checked = app.packageName in selected
                Column(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (selecting) {
                                        selected = if (checked) {
                                            selected - app.packageName
                                        } else {
                                            selected + app.packageName
                                        }
                                    }
                                },
                                onLongClick = {
                                    selecting = true
                                    selected = selected + app.packageName
                                },
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (selecting) {
                            Checkbox(checked = checked, onCheckedChange = null)
                            Spacer(Modifier.width(8.dp))
                        }

                        Column(Modifier.weight(1f)) {
                            Text(app.label, fontWeight = FontWeight.Medium)
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (!selecting) {
                            IconButton(onClick = { state.toggleUninstallBlock(app) }) {
                                Icon(
                                    Icons.Filled.Lock,
                                    contentDescription = stringResource(R.string.apps_uninstall_block),
                                    tint = if (app.uninstallBlocked) {
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
                    }
                    Divider()
                }
            }
        }
    }
}
