// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.opensandbox.dhizuku.R
import kotlinx.coroutines.delay

/** 顶栏 / 搜索框 / 通用弹窗：骨架之外的那层「外壳」。 */
@Composable
internal fun AppDialog(
    state: AppState,
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.dialog_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) } },
    )
}

/** 顶栏：标题 + （应用页）搜索开关 + 展开后的搜索框，整块在布局流里，撑多高内容就往下让多少。 */
@Composable
internal fun AppHeader(
    title: String,
    topInset: Dp,
    searchable: Boolean,
    searchExpanded: Boolean,
    onToggleSearch: () -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(top = topInset)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(Metrics.headerHeight)
                .padding(start = Metrics.pageHorizontal, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Spacer(Modifier.weight(1f))
            if (searchable) {
                IconButton(onClick = onToggleSearch) {
                    Icon(
                        if (searchExpanded) Icons.Filled.Close else Icons.Filled.Search,
                        contentDescription = stringResource(R.string.apps_search),
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = searchable && searchExpanded,
            enter = expandVertically() + androidx.compose.animation.fadeIn(),
            exit = shrinkVertically() + androidx.compose.animation.fadeOut(),
        ) {
            SearchField(query = query, onQueryChange = onQueryChange)
        }
    }
}

/** 搜索框：本地立即回显，停手 150ms 才真正过滤，长列表不会每按一键全量扫一遍。 */
@Composable
internal fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    var draft by remember { mutableStateOf(query) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(draft) {
        delay(150)
        if (draft != query) onQueryChange(draft)
    }
    LaunchedEffect(Unit) { focus.requestFocus() }

    OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        placeholder = { Text(stringResource(R.string.apps_search)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (draft.isNotEmpty()) {
                IconButton(onClick = { draft = "" }) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onQueryChange(draft) }),
        colors =
            OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = Metrics.pageHorizontal, end = Metrics.pageHorizontal, bottom = 8.dp)
                .focusRequester(focus),
    )
}
