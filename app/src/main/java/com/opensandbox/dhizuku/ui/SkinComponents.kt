package com.opensandbox.dhizuku.ui

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/** 当前皮肤，由根部提供。 */
val LocalSkin = staticCompositionLocalOf { Skin.MATERIAL }

/** 小米皮肤下用 Miuix 的主题包一层，Material 皮肤下不动。 */
@Composable
fun ProvideSkin(skin: Skin, content: @Composable () -> Unit) {
    if (skin == Skin.MIUI) {
        val controller = remember { ThemeController() }
        MiuixTheme(controller = controller) { content() }
    } else {
        content()
    }
}

@Composable
fun SkinButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val sized = modifier.height(48.dp)
    if (LocalSkin.current == Skin.MIUI) {
        MiuixButton(onClick = onClick, enabled = enabled, modifier = sized) { content() }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = sized,
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) { content() }
    }
}

@Composable
fun SkinOutlinedButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val sized = modifier.height(48.dp)
    if (LocalSkin.current == Skin.MIUI) {
        MiuixButton(onClick = onClick, enabled = enabled, modifier = sized) { content() }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = sized,
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) { content() }
    }
}

@Composable
fun SkinSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    if (LocalSkin.current == Skin.MIUI) {
        MiuixSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    } else {
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
fun SkinLabel(text: String) = Text(text)
