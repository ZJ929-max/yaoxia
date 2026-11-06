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

/** 小米皮肤下用 Miuix 的主题包一层，Material 皮肤下不动。 */
@Composable
fun ProvideSkin(
    skin: Skin,
    content: @Composable () -> Unit,
) {
    content()
}

/** 只把 Miuix 组件包进它自己的主题，避免接管整页 Material 的颜色。 */
@Composable
fun MiuixScope(content: @Composable () -> Unit) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val colors = if (dark) top.yukonga.miuix.kmp.theme.darkColorScheme()
    else top.yukonga.miuix.kmp.theme.lightColorScheme()
    top.yukonga.miuix.kmp.theme.MiuixTheme(colors = colors) { content() }
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
fun SkinLabel(text: String) = Text(text)

@Composable
fun SkinCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    if (LocalSkin.current == Skin.MIUI) {
        MiuixScope {
            androidx.compose.foundation.layout.Box(
                modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surface)
                    .padding(16.dp)
            ) { androidx.compose.foundation.layout.Column(content = content) }
        }
    } else {
        androidx.compose.material3.Card(modifier = modifier) {
            androidx.compose.foundation.layout.Column(
                androidx.compose.ui.Modifier.padding(16.dp),
                content = content,
            )
        }
    }
}

@Composable
fun SkinDivider() {
    if (LocalSkin.current == Skin.MIUI) {
        MiuixScope { top.yukonga.miuix.kmp.basic.HorizontalDivider() }
    } else {
        androidx.compose.material3.HorizontalDivider()
    }
}

@Composable
fun SkinSectionTitle(text: String) {
    if (LocalSkin.current == Skin.MIUI) {
        MiuixScope { top.yukonga.miuix.kmp.basic.SmallTitle(text) }
    } else {
        androidx.compose.material3.Text(
            text,
            style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        )
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
                color = color ?: top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onSurface,
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
