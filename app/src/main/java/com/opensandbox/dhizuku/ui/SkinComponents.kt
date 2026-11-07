// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch

/** 当前皮肤，由根部提供。 */
val LocalSkin = staticCompositionLocalOf { Skin.MATERIAL }

/** 只把 Miuix 组件包进它自己的主题，避免接管整页 Material 的颜色。 */
@Composable
fun MiuixScope(content: @Composable () -> Unit) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val colors = if (dark) top.yukonga.miuix.kmp.theme.darkColorScheme()
    else top.yukonga.miuix.kmp.theme.lightColorScheme()
    val styles = top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.withFamily(LocalFontFamily.current)
    top.yukonga.miuix.kmp.theme.MiuixTheme(colors = colors, textStyles = styles) { content() }
}

@Composable
fun SkinButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val sized = modifier.height(48.dp)
    if (LocalSkin.current == Skin.MIUI) {
        MiuixScope { MiuixButton(onClick = onClick, enabled = enabled, modifier = sized) { content() } }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = sized,
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) { content() }
    }
}

@Composable
fun SkinOutlinedButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val sized = modifier.height(48.dp)
    if (LocalSkin.current == Skin.MIUI) {
        MiuixScope { MiuixButton(onClick = onClick, enabled = enabled, modifier = sized) { content() } }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = sized,
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) { content() }
    }
}

@Composable
fun SkinSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    if (LocalSkin.current == Skin.MIUI) {
        MiuixScope { MiuixSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled) }
    } else {
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
fun SkinDivider() {
    if (LocalSkin.current == Skin.MIUI) {
        MiuixScope {
            top.yukonga.miuix.kmp.basic.HorizontalDivider(
                color = androidx.compose.material3.MaterialTheme.colorScheme.outlineVariant,
            )
        }
    } else {
        androidx.compose.material3.HorizontalDivider()
    }
}

@Composable
fun SkinLinearProgress(modifier: Modifier = Modifier, progress: Float? = null) {
    if (LocalSkin.current == Skin.MIUI) {
        MiuixScope { top.yukonga.miuix.kmp.basic.LinearProgressIndicator(modifier = modifier, progress = progress) }
    } else {
        androidx.compose.material3.LinearProgressIndicator(progress = { progress ?: 0f }, modifier = modifier)
    }
}

@Composable
fun SkinText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle? = null,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    color: androidx.compose.ui.graphics.Color? = null,
) {
    if (LocalSkin.current == Skin.MIUI) {
        MiuixScope {
            top.yukonga.miuix.kmp.basic.Text(
                text = text,
                modifier = modifier,
                color = color ?: androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                style = style ?: top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles.main,
                fontWeight = fontWeight,
            )
        }
    } else {
        androidx.compose.material3.Text(
            text = text,
            modifier = modifier,
            style = style ?: androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            fontWeight = fontWeight,
            color = color ?: androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
fun SkinCheckbox(checked: Boolean, enabled: Boolean = true, onClick: (() -> Unit)? = null) {
    if (LocalSkin.current == Skin.MIUI) {
        MiuixScope {
            top.yukonga.miuix.kmp.basic.Checkbox(
                state = if (checked) androidx.compose.ui.state.ToggleableState.On
                else androidx.compose.ui.state.ToggleableState.Off,
                onClick = onClick,
                enabled = enabled,
            )
        }
    } else {
        androidx.compose.material3.Checkbox(
            checked = checked,
            onCheckedChange = { onClick?.invoke() },
            enabled = enabled,
        )
    }
}

@Composable
fun SkinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
) {
    if (LocalSkin.current == Skin.MIUI) {
        MiuixScope {
            top.yukonga.miuix.kmp.basic.Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = modifier,
            )
        }
    } else {
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = modifier,
        )
    }
}
