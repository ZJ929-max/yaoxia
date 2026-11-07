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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.opensandbox.dhizuku.R
import kotlinx.coroutines.delay

/** 顶栏 / 搜索框 / 通用弹窗：骨架之外的那层「外壳」。 */

/**
 * 顶栏雾气带：只有当内容真的滚到标题下面才出现，浓度跟着滚动距离走。
 * 静止时完全不存在（列表第一行紧贴顶栏下沿），滚动时才把后景糊成玻璃——
 * 比固定的一条渐变遮罩「活」，也不会在没内容可挡的时候凭空压一条色带。
 */
@Composable
internal fun TopFrostBand(
    height: Dp,
    progress: Float,
    backdrop: com.kyant.backdrop.Backdrop?,
) {
    if (backdrop == null || progress <= 0.01f) return
    val density = LocalDensity.current
    // 用户在外观里调的「玻璃模糊」也作用到这条雾气带上，不再是写死的常数
    val blurPx = with(density) { (4.dp + 22.dp * progress + LocalBlurRadius.current.dp * 0.6f).toPx() }
    val tint = MaterialTheme.colorScheme.surface.copy(alpha = 0.30f + 0.55f * progress)

    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .alphaMask(
                Brush.verticalGradient(
                    0f to Color.Black,
                    0.66f to Color.Black,
                    1f to Color.Transparent,
                ),
            ),
    ) {
        // 先把后景糊掉……
        Box(
            Modifier
                .fillMaxSize()
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { com.kyant.shapes.RoundedRectangle(0.dp) },
                    effects = { blur(blurPx) },
                    onDrawSurface = {},
                ),
        )
        // ……再压一层半透明底色，标题区才不会和正文叠字
        Box(Modifier.fillMaxSize().background(tint))
    }
}
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
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(top = topInset)) {
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
