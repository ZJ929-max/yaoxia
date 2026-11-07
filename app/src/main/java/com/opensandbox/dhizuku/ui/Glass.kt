// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
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
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow

val LocalGlassEnabled = staticCompositionLocalOf { false }

/** 玻璃底色透明度，用户可调（0.08 ~ 0.55）。 */
val LocalGlassAlpha = staticCompositionLocalOf { 0.22f }

/** 背景光晕强度，用户可调（0 ~ 1）。 */
val LocalGlowStrength = staticCompositionLocalOf { 1f }

/** 模糊半径（dp），用户可调。 */
val LocalBlurRadius = staticCompositionLocalOf { 0f }

/** 折射源：只有背景层（官方示例的做法）。玻璃元素都在层外，才能采到东西。 */
internal val LocalBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

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
    val glowStrength = LocalGlowStrength.current
    val backdrop = rememberLayerBackdrop()
    androidx.compose.runtime.LaunchedEffect(Unit) {
        android.util.Log.i(
            "YaoxiaGlass",
            "sdk=${android.os.Build.VERSION.SDK_INT} " +
                "renderEffect=${com.kyant.backdrop.isRenderEffectSupported()} " +
                "runtimeShader=${com.kyant.backdrop.isRuntimeShaderSupported()}",
        )
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(gradient).layerBackdrop(backdrop)) {
            Canvas(Modifier.fillMaxSize()) {
                fun glow(color: Color, cx: Float, cy: Float, r: Float) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color, Color.Transparent),
                            center = Offset(size.width * cx, size.height * cy),
                            radius = size.minDimension * r,
                        ),
                        radius = size.minDimension * r,
                        center = Offset(size.width * cx, size.height * cy),
                    )
                }
                val k = glowStrength
                glow(Color(0xFF6D5BFF).copy(alpha = 0.55f * k), 0.15f, 0.10f, 0.55f)
                glow(Color(0xFF00D4B8).copy(alpha = 0.40f * k), 0.90f, 0.62f, 0.50f)
                glow(Color(0xFFFF9F0A).copy(alpha = 0.30f * k), 0.72f, 0.95f, 0.42f)
            }
        }

        CompositionLocalProvider(
            LocalBackdrop provides backdrop,
            content = content,
        )
    }
}

/** 统一入口：液态玻璃（折射 + 模糊 + 增艳 + 高光 + 内外阴影）。 */
fun Modifier.liquidGlass(
    backdrop: Backdrop,
    shape: Shape = com.kyant.shapes.RoundedRectangle(24.dp),
    tint: Color = Color.Transparent,
    blurRadius: Dp = 6.dp,
    lensRadius: Dp = 12.dp,
    lensOffset: Dp = 24.dp,
    highlightAlpha: Float = 0.35f,
    layerBlock: (androidx.compose.ui.graphics.GraphicsLayerScope.() -> Unit)? = null,
    pulse: () -> Unit = {},
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        // 读一个 snapshot state，让 observeReads 注册并触发 updateEffects
        pulse()
        vibrancy()
        blur(blurRadius.toPx())
        lens(lensRadius.toPx(), lensOffset.toPx(), chromaticAberration = true)
    },
    highlight = { Highlight.Default.copy(alpha = highlightAlpha) },
    shadow = { Shadow(alpha = 0.18f) },
    innerShadow = { InnerShadow(radius = 6.dp, alpha = 0.25f) },
    onDrawSurface = { if (tint.alpha > 0f) drawRect(tint) },
    layerBlock = layerBlock,
)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val enabled = LocalGlassEnabled.current
    val backdrop = LocalBackdrop.current
    val blurRadiusDp = LocalBlurRadius.current

    if (enabled && backdrop != null) {
        val surface = MaterialTheme.colorScheme.surface
        val shape = com.kyant.shapes.RoundedRectangle(24.dp)
        Box(
            modifier
                .clip(shape)
                .background(surface.copy(alpha = LocalGlassAlpha.current), shape)
                .then(
                    if (backdrop != null) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { shape },
                            effects = {
                                if (blurRadiusDp > 0f) blur(blurRadiusDp.dp.toPx())
                                lens(20.dp.toPx(), 24.dp.toPx(), chromaticAberration = true)
                            },
                            highlight = { com.kyant.backdrop.highlight.Highlight.Default.copy(alpha = 0.25f) },
                            onDrawSurface = {},
                        )
                    } else Modifier
                )
                .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            ) {
                Column(Modifier.padding(16.dp), content = content)
            }
        }
    } else {
        Card(modifier, shape = RoundedCornerShape(24.dp)) {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            ) {
                Column(Modifier.padding(16.dp), content = content)
            }
        }
    }
}
