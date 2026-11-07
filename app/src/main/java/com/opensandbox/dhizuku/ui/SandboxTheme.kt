// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.ui

import androidx.compose.foundation.background
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

    val context = androidx.compose.ui.platform.LocalContext.current
    // 「跟随系统」用系统取色（Android 12+），低版本回落到 Material 3 默认色
    val systemColors =
        remember(dark, context) {
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                if (dark) {
                    androidx.compose.material3.dynamicDarkColorScheme(context)
                } else {
                    androidx.compose.material3.dynamicLightColorScheme(context)
                }
            } else {
                base
            }
        }

    val colors =
        when (skin) {
            Skin.SYSTEM -> systemColors

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

    val typography =
        androidx.compose.runtime.remember(fontFamily) {
            androidx.compose.material3.Typography().withFamily(fontFamily)
        }
    MaterialTheme(colorScheme = colors, typography = typography) {
        // 很多 Text 不写 color，靠 LocalContentColor 兜底；它的默认值是纯黑，
        // 深色模式下就变成「黑字画在深色玻璃上」——这里统一钉到 onBackground。
        CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides colors.onBackground,
        ) {
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
