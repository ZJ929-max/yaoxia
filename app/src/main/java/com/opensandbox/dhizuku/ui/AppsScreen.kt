package com.opensandbox.dhizuku.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.opensandbox.dhizuku.R

/** 搜索框是浮层：列表从它下面滚过去，才能看到"透过去"的效果。 */
private val HeaderHeight = 196.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppsScreen(state: AppState) {
    var query by rememberSaveable { mutableStateOf("") }
    var selecting by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    val searchFocus = remember { androidx.compose.ui.focus.FocusRequester() }

    val visible = state.apps.filter {
        query.isBlank() || it.label.contains(query, true) || it.packageName.contains(query, true)
    }

    Box(Modifier.fillMaxSize()) {
        if (visible.isEmpty()) {
            Text(
                stringResource(R.string.apps_empty),
                Modifier.padding(top = HeaderHeight + 16.dp, start = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = if (searchExpanded) HeaderHeight else 72.dp,
                    bottom = 176.dp,
                ),
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
                                            selected =
                                                if (checked) selected - app.packageName
                                                else selected + app.packageName
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
                                SkinText(app.label, fontWeight = FontWeight.Medium)
                                SkinText(app.packageName, style = MaterialTheme.typography.bodySmall)
                            }
                            if (!selecting) {
                                IconButton(onClick = { state.toggleUninstallBlock(app) }) {
                                    Icon(
                                        Icons.Filled.Lock,
                                        contentDescription = stringResource(R.string.apps_uninstall_block),
                                        tint = if (app.uninstallBlocked) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
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
                        SkinDivider()
                    }
                }
            }
        }

        // 浮层头部：折叠成右上角搜索按钮；点击展开并自动聚焦
        Column(Modifier.fillMaxWidth()) {
            if (!searchExpanded) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (selecting) stringResource(R.string.apps_selected, selected.size)
                        else stringResource(R.string.apps_count, visible.size),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            androidx.compose.ui.graphics.Color.White.copy(alpha = 0.18f),
                        ),
                        modifier = Modifier.size(44.dp),
                    ) {
                        IconButton(onClick = { searchExpanded = true }) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = stringResource(R.string.apps_search),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            } else {
                GlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (selecting) stringResource(R.string.apps_selected, selected.size)
                            else stringResource(R.string.apps_count, visible.size),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!selecting) {
                                TextButton(onClick = { state.refreshApps() }) {
                                    Text(stringResource(R.string.button_refresh))
                                }
                            }
                            IconButton(onClick = {
                                query = ""
                                searchExpanded = false
                            }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text(stringResource(R.string.apps_search)) },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocus),
                    )
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        searchFocus.requestFocus()
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
