// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.opensandbox.dhizuku.R

/**
 * 应用列表：一整段「设置列表」式卡片，长按进入批量选择。
 * 顶部那条是状态条（数量 / 已选数量），不是第二个标题——页面标题只在顶栏出现一次。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppsScreen(state: AppState, topInset: Dp, bottomInset: Dp) {
    var selecting by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var menuFor by remember { mutableStateOf<String?>(null) }

    val query = state.appsQuery
    val visible =
        remember(state.apps, query) {
            if (query.isBlank()) {
                state.apps
            } else {
                state.apps.filter {
                    it.label.contains(query, true) || it.packageName.contains(query, true)
                }
            }
        }

    fun leaveSelection() {
        selecting = false
        selected = emptySet()
    }

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    ReportScrollProgress(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding =
            PaddingValues(
                start = Metrics.pageHorizontal,
                top = topInset + 4.dp,
                end = Metrics.pageHorizontal,
                bottom = bottomInset + 16.dp,
            ),
    ) {
        item(key = "status-bar") {
            Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (selecting) {
                            stringResource(R.string.apps_selected, selected.size)
                        } else {
                            stringResource(R.string.apps_count, visible.size)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    if (selecting) {
                        TextButton(onClick = ::leaveSelection) {
                            Text(stringResource(R.string.apps_cancel_select))
                        }
                    } else if (visible.isNotEmpty()) {
                        TextButton(onClick = { selecting = true }) {
                            Text(stringResource(R.string.apps_batch))
                        }
                    }
                }
                if (selecting) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val enabled = selected.isNotEmpty() && state.busyPackages.isEmpty()
                        TextButton(
                            enabled = enabled,
                            onClick = {
                                state.freezeBatch(selected, true)
                                leaveSelection()
                            },
                        ) { Text(stringResource(R.string.apps_freeze_selected)) }
                        TextButton(
                            enabled = enabled,
                            onClick = {
                                state.freezeBatch(selected, false)
                                leaveSelection()
                            },
                        ) { Text(stringResource(R.string.apps_unfreeze_selected)) }
                        TextButton(
                            enabled = enabled,
                            onClick = {
                                state.installBatch(selected)
                                leaveSelection()
                            },
                        ) { Text(stringResource(R.string.apps_install_selected)) }
                    }
                }
            }
        }

        if (visible.isEmpty()) {
            item(key = "empty") {
                Text(
                    stringResource(R.string.apps_empty),
                    Modifier.padding(horizontal = 4.dp, vertical = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            itemsIndexed(visible, key = { _, app -> app.packageName }) { index, app ->
                val checked = app.packageName in selected
                AppRow(
                    app = app,
                    slot = rowSlot(index, visible.size),
                    selecting = selecting,
                    checked = checked,
                    busy = app.packageName in state.busyPackages,
                    menuOpen = menuFor == app.packageName,
                    onToggleSelect = {
                        selected = if (checked) selected - app.packageName else selected + app.packageName
                    },
                    onLongPress = {
                        selecting = true
                        selected = selected + app.packageName
                    },
                    onToggleFreeze = { target -> state.setFrozen(app, target) },
                    onToggleLock = { state.toggleUninstallBlock(app) },
                    onOpenMenu = { menuFor = app.packageName },
                    onDismissMenu = { menuFor = null },
                    onInstall = {
                        menuFor = null
                        state.installIntoProfile(app.packageName)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(
    app: AppUi,
    slot: RowSlot,
    selecting: Boolean,
    checked: Boolean,
    busy: Boolean,
    menuOpen: Boolean,
    onToggleSelect: () -> Unit,
    onLongPress: () -> Unit,
    onToggleFreeze: (Boolean) -> Unit,
    onToggleLock: () -> Unit,
    onOpenMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onInstall: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val tint =
        when {
            selecting && checked -> cs.primary.copy(alpha = 0.14f)
            app.frozen -> cs.primary.copy(alpha = 0.06f)
            else -> Color.Transparent
        }

    ListRow(
        slot = slot,
        tint = tint,
        modifier =
            Modifier.combinedClickable(
                onClick = {
                    if (selecting) {
                        onToggleSelect()
                    }
                },
                onLongClick = onLongPress,
            ),
    ) {
        if (selecting) {
            SkinCheckbox(checked = checked, onClick = onToggleSelect)
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                app.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!selecting) {
            IconButton(onClick = onToggleLock) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = stringResource(R.string.apps_uninstall_block),
                    tint = if (app.uninstallBlocked) cs.primary else cs.outline,
                )
            }
            SkinSwitch(
                checked = app.frozen,
                enabled = !busy,
                onCheckedChange = onToggleFreeze,
            )
            Box {
                IconButton(onClick = onOpenMenu) {
                    Icon(Icons.Filled.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = onDismissMenu) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.apps_menu_install)) },
                        onClick = onInstall,
                    )
                }
            }
        }
    }
}
