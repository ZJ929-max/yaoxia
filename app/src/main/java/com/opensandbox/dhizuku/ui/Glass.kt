package com.opensandbox.dhizuku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/** 玻璃开关与"背景层"，由根部提供，卡片自己决定要不要玻璃。 */
val LocalGlassEnabled = staticCompositionLocalOf { false }
private val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * 铺一层有颜色的底：液态玻璃必须有东西可"折射"，否则在纯色背景上看不出效果。
 * 背景本身也是 Backdrop 的来源。
 */
@Composable
fun SandboxSurface(content: @Composable () -> Unit) {
    val gradient =
        Brush.linearGradient(
            listOf(
                Color(0xFF6D5BFF),
                Color(0xFF2BB0FF),
                Color(0xFF00D4B8),
            ),
        )

    val backdrop =
        rememberCanvasBackdrop {
            drawRect(brush = gradient)
        }

    Box(Modifier.fillMaxSize().background(gradient)) {
        CompositionLocalProvider(LocalBackdrop provides backdrop, content = content)
    }
}

/**
 * 一张卡片：开玻璃时走折射 + 模糊 + 增艳；关玻璃时就是普通 Material 卡片。
 * 低版本没有 RenderEffect，库会自动退化为普通绘制。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val enabled = LocalGlassEnabled.current
    val backdrop = LocalBackdrop.current

    if (enabled && backdrop != null) {
        Box(
            modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { RoundedCornerShape(24.dp) },
                effects = {
                    vibrancy()
                    blur(6.dp.toPx())
                    lens(16.dp.toPx(), 32.dp.toPx())
                },
            ),
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(modifier) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}
