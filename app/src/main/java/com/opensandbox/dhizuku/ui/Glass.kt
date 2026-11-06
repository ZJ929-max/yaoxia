package com.opensandbox.dhizuku.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

val LocalGlassEnabled = staticCompositionLocalOf { false }

/** 折射源：只有背景层（官方示例的做法）。玻璃元素都在层外，才能采到东西。 */
internal val LocalBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/** 卡片用：画布折射源，绘制在自身坐标系里，不会因层偏移错位。 */
internal val LocalCardBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * 背景 + 折射源。
 * 关键：`layerBackdrop` 只挂在「背景」这一个节点上，
 * 内容（含玻璃）全部在它之外，避免玻璃采样到自己。
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
    val cardBackdrop = rememberCanvasBackdrop {
        drawRect(brush = gradient)
        drawCircle(
            color = Color(0xFF6D5BFF).copy(alpha = 0.30f),
            radius = size.minDimension * 0.5f,
            center = Offset(size.width * 0.15f, size.height * 0.10f),
        )
        drawCircle(
            color = Color(0xFF00D4B8).copy(alpha = 0.20f),
            radius = size.minDimension * 0.42f,
            center = Offset(size.width * 0.90f, size.height * 0.62f),
        )
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(gradient).layerBackdrop(backdrop)) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFF6D5BFF).copy(alpha = 0.30f),
                    radius = size.minDimension * 0.5f,
                    center = Offset(size.width * 0.15f, size.height * 0.10f),
                )
                drawCircle(
                    color = Color(0xFF00D4B8).copy(alpha = 0.20f),
                    radius = size.minDimension * 0.42f,
                    center = Offset(size.width * 0.90f, size.height * 0.62f),
                )
                drawCircle(
                    color = Color(0xFFFF9F0A).copy(alpha = 0.12f),
                    radius = size.minDimension * 0.30f,
                    center = Offset(size.width * 0.72f, size.height * 0.95f),
                )
            }
        }

        CompositionLocalProvider(
            LocalBackdrop provides backdrop,
            LocalCardBackdrop provides cardBackdrop,
            content = content,
        )
    }
}

/** 统一入口：液态玻璃（折射 + 模糊 + 增艳 + 高光 + 内外阴影）。 */
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color.Transparent,
    blurRadius: Dp = 6.dp,
    lensRadius: Dp = 12.dp,
    lensOffset: Dp = 24.dp,
    highlightAlpha: Float = 0.35f,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        vibrancy()
        blur(blurRadius.toPx())
        lens(lensRadius.toPx(), lensOffset.toPx(), chromaticAberration = true)
    },
    highlight = { Highlight.Default.copy(alpha = highlightAlpha) },
    shadow = { Shadow(alpha = 0.18f) },
    innerShadow = { InnerShadow(radius = 6.dp, alpha = 0.25f) },
    onDrawSurface = { if (tint.alpha > 0f) drawRect(tint) },
)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val enabled = LocalGlassEnabled.current
    val backdrop = LocalBackdrop.current

    if (enabled && backdrop != null) {
        val surface = MaterialTheme.colorScheme.surface
        Box(
            modifier.liquidGlass(
                backdrop = backdrop,
                shape = RoundedCornerShape(24.dp),
                tint = surface.copy(alpha = 0.42f),
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
