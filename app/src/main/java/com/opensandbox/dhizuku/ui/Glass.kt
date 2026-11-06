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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

val LocalGlassEnabled = staticCompositionLocalOf { false }
internal val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * 背景 + 折射源。
 * 关键点：backdrop 来自「真实内容层」（layerBackdrop），
 * 所以玻璃读到的是栏后面的界面内容，而不是一张静态渐变。
 */
@Composable
fun SandboxSurface(content: @Composable () -> Unit) {
    val gradient = Brush.linearGradient(
        listOf(
            Color(0xFF0B0D0A),
            Color(0xFF151A12),
            Color(0xFF10140F),
        )
    )

    val backdrop = rememberLayerBackdrop()

    Box(
        Modifier
            .fillMaxSize()
            .background(gradient)
            .layerBackdrop(backdrop)
    ) {
        CompositionLocalProvider(LocalBackdrop provides backdrop, content = content)
    }
}

/**
 * 一张卡片：开玻璃时走「折射 + 模糊 + 增艳」；关玻璃时退回普通 Material 卡片。
 * 低版本没有 RenderEffect 时库会自动退化。
 */
/** 统一入口：给任意容器套液态玻璃。shape 决定边缘折射形状。 */
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    shape: Shape = RoundedCornerShape(24.dp),
    blurRadius: Dp = 6.dp,
    lensRadius: Dp = 16.dp,
    lensOffset: Dp = 32.dp,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        vibrancy()
        blur(blurRadius.toPx())
        lens(lensRadius.toPx(), lensOffset.toPx())
    },
)

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
            )
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(modifier, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}
