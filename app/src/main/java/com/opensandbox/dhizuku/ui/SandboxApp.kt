// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.opensandbox.dhizuku.R
import kotlinx.coroutines.delay

/**
 * 主界面骨架：
 *
 *   状态栏留白 → 顶栏（标题 + 搜索）→ 页面内容
 *
 * 顶栏是普通布局流的一部分，不再浮在内容之上——那样得靠硬编码高度去补 contentPadding，
 * 一改高度就错位（搜索框压到列表第一行就是这么来的）。底部仍留一颗悬浮导航胶囊，
 * 内容按它的真实高度让位。
 */
@Composable
fun SandboxApp(state: AppState, initialTab: Int = 0) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    val titles =
        listOf(
            stringResource(R.string.tab_status),
            stringResource(R.string.tab_apps),
            stringResource(R.string.tab_profile),
            stringResource(R.string.tab_settings),
        )
    val icons =
        listOf(
            Icons.Filled.Home,
            Icons.Filled.List,
            Icons.Filled.Lock,
            Icons.Filled.Settings,
        )

    state.pendingLaunch?.let { app ->
        AppDialog(
            state = state,
            title = stringResource(R.string.dialog_launch_title),
            message =
                stringResource(
                    R.string.dialog_launch_message,
                    app.label,
                    state.profileUserId ?: 0,
                ),
            onConfirm = state::confirmPendingLaunch,
            onDismiss = state::cancelPendingLaunch,
        )
    }
    if (state.pendingDeleteProfile) {
        AppDialog(
            state = state,
            title = stringResource(R.string.dialog_delete_title),
            message = stringResource(R.string.dialog_delete_message, state.profileUserId ?: 0),
            onConfirm = state::confirmDeleteProfile,
            onDismiss = state::cancelDeleteProfile,
        )
    }
    if (state.pendingSwitchBack) {
        AppDialog(
            state = state,
            title = stringResource(R.string.dialog_switch_title),
            message = stringResource(R.string.dialog_switch_message),
            onConfirm = state::confirmSwitchToOwner,
            onDismiss = state::cancelSwitchToOwner,
        )
    }

    val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset = navBarInset + Metrics.tabBarSpace

    // 顶栏高度是量出来的：搜索框展开 / 收起都会变，内容跟着让位，没有任何写死的高度
    var headerHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    // 页面把「滚进去多少」写回来，顶栏据此决定雾气浓度（静止时为零）
    val scrollProgress = remember { mutableStateOf(0f) }
    val smoothProgress by androidx.compose.animation.core.animateFloatAsState(
        scrollProgress.value,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "topFrost",
    )

    // 内容层：给悬浮导航胶囊做折射源，让它能「透」出下面滚动的列表
    val contentBackdrop = rememberLayerBackdrop()

    SandboxSurface {
        Box(Modifier.fillMaxSize()) {
            // 内容铺满整屏；滚到标题区时由 fadeTopEdge 渐隐化开，而不是被一条硬边切掉
            Box(Modifier.fillMaxSize().layerBackdrop(contentBackdrop)) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalScrollProgress provides scrollProgress,
                ) {
                    val topInset = headerHeight
                    when (tab) {
                        0 -> StatusScreen(state, topInset, bottomInset)
                        1 -> AppsScreen(state, topInset, bottomInset)
                        2 -> ProfileScreen(state, topInset, bottomInset)
                        else -> SettingsScreen(state, topInset, bottomInset)
                    }
                }
            }

            TopFrostBand(
                height = headerHeight + Metrics.fadeRamp,
                progress = smoothProgress,
                backdrop = contentBackdrop,
            )

            AppHeader(
                title = titles[tab],
                topInset = statusBarInset,
                searchable = tab == 1,
                searchExpanded = state.searchExpanded,
                onToggleSearch = {
                    state.searchExpanded = !state.searchExpanded
                    if (!state.searchExpanded) state.appsQuery = ""
                },
                query = state.appsQuery,
                onQueryChange = { state.appsQuery = it },
                modifier = Modifier.onSizeChanged { headerHeight = with(density) { it.height.toDp() } },
            )

            FloatingTabBar(
                items = icons.zip(titles),
                selected = tab,
                onSelect = {
                    tab = it
                    if (it != 1) {
                        state.searchExpanded = false
                        state.appsQuery = ""
                    }
                },
                backdropOverride = contentBackdrop,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = navBarInset),
            )
        }
    }
}
