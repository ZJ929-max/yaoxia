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
    val blobs: List<Glow>,
    val border: Color,
    val tint: Color,
)

/** 一颗光斑：颜色 + 圆心（相对屏幕宽高的比例）+ 半径（相对短边的比例）。 */
data class Glow(
    val color: Color,
    val cx: Float,
    val cy: Float,
    val r: Float,
)

/** 梦幻泡泡的三个品牌色：青紫 / 青绿 / 琥珀。这是设计标识，不随皮肤变。 */
private val BubbleIndigo = Color(0xFF6D5BFF)
private val BubbleTeal = Color(0xFF00D4B8)
private val BubbleAmber = Color(0xFFFF9F0A)

@Composable
fun rememberGlassPalette(): GlassPalette {
    val cs = MaterialTheme.colorScheme
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    return remember(cs, dark) {
        if (dark) {
            // 原版「梦幻泡泡」：近黑底 + 三颗高饱和光斑
            GlassPalette(
                base =
                    listOf(
                        Color(0xFF0B0D0A),
                        Color(0xFF151A12),
                        Color(0xFF10140F),
                    ),
                blobs =
                    listOf(
                        Glow(BubbleIndigo.copy(alpha = 0.55f), 0.15f, 0.10f, 0.55f),
                        Glow(BubbleTeal.copy(alpha = 0.40f), 0.90f, 0.62f, 0.50f),
                        Glow(BubbleAmber.copy(alpha = 0.30f), 0.72f, 0.95f, 0.42f),
                    ),
                border = cs.onSurface.copy(alpha = 0.16f),
                tint = cs.surface.copy(alpha = 0.22f),
            )
        } else {
            // 浅色：同一组色相，压低浓度，底色保持近白
            GlassPalette(
                base =
                    listOf(
                        Color(0xFFFDFCFA),
                        Color(0xFFF3F1F7),
                        Color(0xFFFBF9F6),
                    ),
                blobs =
                    listOf(
                        Glow(BubbleIndigo.copy(alpha = 0.22f), 0.15f, 0.10f, 0.55f),
                        Glow(BubbleTeal.copy(alpha = 0.16f), 0.90f, 0.62f, 0.50f),
                        Glow(BubbleAmber.copy(alpha = 0.12f), 0.72f, 0.95f, 0.42f),
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
                val k = glowStrength
                palette.blobs.forEach { blob ->
                    val center = Offset(size.width * blob.cx, size.height * blob.cy)
                    val radius = size.minDimension * blob.r
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(blob.color.copy(alpha = blob.color.alpha * k), Color.Transparent),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
            }
        }

        CompositionLocalProvider(
            LocalBackdrop provides backdrop,
            LocalGlassPalette provides palette,
            content = content,
        )
    }
}
