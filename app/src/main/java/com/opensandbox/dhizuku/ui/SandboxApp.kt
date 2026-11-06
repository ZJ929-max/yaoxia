package com.opensandbox.dhizuku.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Typography
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.opensandbox.dhizuku.BuildConfig
import com.opensandbox.dhizuku.R

/** 主题：只换配色，布局不变——所以"外观"和"玻璃"是两个独立开关。 */
@Composable
fun SandboxTheme(
    skin: Skin,
    glassEnabled: Boolean,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    glassAlpha: Float = 0.22f,
    glowStrength: Float = 1f,
    blurRadius: Float = 0f,
    content: @Composable () -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val base = if (dark) darkColorScheme() else lightColorScheme()

    val colors =
        when (skin) {
            Skin.SYSTEM -> base

            Skin.MATERIAL -> {
                base
            }

            Skin.MIUI -> {
                if (dark) {
                    base.copy(
                        primary = Color(0xFF9EC0FF),
                        primaryContainer = Color(0xFF00306B),
                        onPrimaryContainer = Color(0xFFDCE8FF),
                    )
                } else {
                    base.copy(
                        primary = Color(0xFF3482FF),
                        primaryContainer = Color(0xFFDCE8FF),
                        onPrimaryContainer = Color(0xFF00174A),
                    )
                }
            }
        }

    MaterialTheme(colorScheme = colors, typography = androidx.compose.material3.Typography().withFamily(fontFamily)) {
        ProvideSkin(skin) {
            CompositionLocalProvider(
                LocalGlassEnabled provides glassEnabled,
                LocalGlassAlpha provides glassAlpha,
                LocalGlowStrength provides glowStrength,
                LocalBlurRadius provides blurRadius,
                LocalFontFamily provides fontFamily,
                LocalSkin provides skin,
                content = content,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SandboxApp(state: AppState, initialTab: Int = 0) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var logged by remember { androidx.compose.runtime.mutableStateOf(false) }
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
        AlertDialog(
            onDismissRequest = state::cancelPendingLaunch,
            title = { Text(stringResource(R.string.dialog_launch_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dialog_launch_message,
                        app.label,
                        state.profileUserId ?: 0,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = state::confirmPendingLaunch) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = state::cancelPendingLaunch) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    if (state.pendingDeleteProfile) {
        AlertDialog(
            onDismissRequest = state::cancelDeleteProfile,
            title = { Text(stringResource(R.string.dialog_delete_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dialog_delete_message,
                        state.profileUserId ?: 0,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = state::confirmDeleteProfile) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = state::cancelDeleteProfile) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    if (state.pendingSwitchBack) {
        AlertDialog(
            onDismissRequest = state::cancelSwitchToOwner,
            title = { Text(stringResource(R.string.dialog_switch_title)) },
            text = { Text(stringResource(R.string.dialog_switch_message)) },
            confirmButton = {
                TextButton(onClick = state::confirmSwitchToOwner) {
                    Text(stringResource(R.string.dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = state::cancelSwitchToOwner) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    val contentBackdrop = rememberLayerBackdrop(
        onDraw = {
            drawContent()
            if (!logged) {
                logged = true
                android.util.Log.i("YaoxiaGlass", "content layer recorded ${size.width}x${size.height}")
            }
        },
    )

    SandboxSurface {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .layerBackdrop(contentBackdrop)
        ) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                // 透明顶栏：否则会盖住 SandboxSurface 的背景光斑（梦幻泡泡的底色来源）
                TopAppBar(
                    title = {
                        Text(
                            if (tab == 1) stringResource(R.string.apps_count, state.apps.size)
                            else titles[tab],
                        )
                    },
                    actions = {
                        if (tab == 1) {
                            IconButton(onClick = { state.searchExpanded = true }) {
                                Icon(
                                    Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.apps_search),
                                )
                            }
                        }
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
                    ),
                )
            },
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(
                    if (tab == 1) PaddingValues(bottom = padding.calculateBottomPadding())
                    else padding,
                ),
            ) {
                when (tab) {
                    0 -> StatusScreen(state)
                    1 -> AppsScreen(state)
                    2 -> ProfileScreen(state)
                    else -> SettingsScreen(state)
                }
            }
        }
        }

        FloatingTabBar(
            backdropOverride = contentBackdrop,
            items = icons.zip(titles),
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
    }

}

// ---------------------------------------------------------------- 状态页

