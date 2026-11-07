// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp
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
import com.kyant.shapes.Capsule
import com.kyant.shapes.RoundedRectangle

val LocalGlassEnabled = staticCompositionLocalOf { false }

/**
 * 梦幻泡泡的调色板：底色、光斑、描边、玻璃色全部由 colorScheme 推导。
 * 浅色也要有颜色——玻璃得有东西可折射，否则只剩一块白板。
 */
data class GlassPalette(
    val base: List<Color>,
    val glows: List<Color>,
    val border: Color,
    val tint: Color,
)

@Composable
fun rememberGlassPalette(): GlassPalette {
    val cs = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    return remember(cs, dark) {
        if (dark) {
            GlassPalette(
                base = listOf(cs.background, cs.surfaceVariant.copy(alpha = 0.45f), cs.background),
                glows =
                    listOf(
                        cs.primary.copy(alpha = 0.42f),
                        cs.tertiary.copy(alpha = 0.30f),
                        cs.secondary.copy(alpha = 0.22f),
                    ),
                border = cs.onSurface.copy(alpha = 0.16f),
                tint = cs.surface.copy(alpha = 0.22f),
            )
        } else {
            // 浅色：近白底 + 淡紫/淡青染色，光斑肉眼可见但不脏
            GlassPalette(
                base =
                    listOf(
                        cs.primary.copy(alpha = 0.28f).compositeOver(cs.surface),
                        cs.surface,
                        cs.tertiary.copy(alpha = 0.30f).compositeOver(cs.surface),
                    ),
                glows =
                    listOf(
                        cs.primary.copy(alpha = 0.58f),
                        cs.tertiary.copy(alpha = 0.46f),
                        cs.secondary.copy(alpha = 0.34f),
                    ),
                border = Color.White.copy(alpha = 0.85f),
                tint = cs.surface.copy(alpha = 0.30f),
            )
        }
    }
}

internal val LocalGlassPalette = staticCompositionLocalOf<GlassPalette?> { null }

/** 玻璃底色透明度，用户可调（0.08 ~ 0.55）。 */
val LocalGlassAlpha = staticCompositionLocalOf { 0.22f }

/** 背景光晕强度，用户可调（0 ~ 1）。 */
val LocalGlowStrength = staticCompositionLocalOf { 1f }

/** 额外模糊半径（dp），用户可调；0 时用每套配色的默认值。 */
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
    val palette = rememberGlassPalette()
    val gradient = Brush.linearGradient(palette.base)
    val glowStrength = LocalGlowStrength.current
    val backdrop = rememberLayerBackdrop()
    LaunchedEffect(Unit) {
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
                    val center = Offset(size.width * cx, size.height * cy)
                    val radius = size.minDimension * r
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color, Color.Transparent),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
                val k = glowStrength
                glow(palette.glows[0].copy(alpha = palette.glows[0].alpha * k), 0.16f, 0.10f, 0.40f)
                glow(palette.glows[1].copy(alpha = palette.glows[1].alpha * k), 0.88f, 0.56f, 0.36f)
                glow(palette.glows[2].copy(alpha = palette.glows[2].alpha * k), 0.66f, 0.94f, 0.30f)
            }
        }

        CompositionLocalProvider(
            LocalBackdrop provides backdrop,
            LocalGlassPalette provides palette,
            content = content,
        )
    }
}
