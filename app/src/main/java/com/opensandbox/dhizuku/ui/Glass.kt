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

/**
 * 玻璃底色：浅色下也保持半透明，让背景光斑透上来；
 * 用户拖动透明度滑杆时两端都跟着动，但都不会把玻璃糊成一块实心白。
 */
@Composable
internal fun glassTint(): Color {
    val cs = MaterialTheme.colorScheme
    return if (isSystemInDarkTheme()) {
        cs.surface.copy(alpha = (0.16f + LocalGlassAlpha.current * 0.95f).coerceIn(0.16f, 0.62f))
    } else {
        cs.surface.copy(alpha = (0.08f + LocalGlassAlpha.current * 0.62f).coerceIn(0.08f, 0.48f))
    }
}

/** 模糊半径：浅色默认略重一点，才看得出「磨砂」而不是「透明」。 */
@Composable
internal fun glassBlurDp(): Float {
    val base = if (isSystemInDarkTheme()) 12f else 24f
    return base + LocalBlurRadius.current
}

/**
 * 卡片：真玻璃——采样背景层 → 模糊 → 折射 → 高光 + 内外阴影 → 半透明底色。
 *
 * 形状必须用 com.kyant.shapes 的形状，`lens()` 才会生效（换成 RoundedCornerShape 会静默失效）。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val enabled = LocalGlassEnabled.current
    val backdrop = LocalBackdrop.current
    val palette = LocalGlassPalette.current
    val cs = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val shape = RoundedRectangle(Metrics.cardCorner)
    val clip = RoundedCornerShape(Metrics.cardCorner)
    val tint = glassTint()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val blurPx = with(density) { glassBlurDp().dp.toPx() }
    val lensPx = with(density) { 18.dp.toPx() }
    val lensOffsetPx = with(density) { 26.dp.toPx() }
    val repaint = remember { mutableIntStateOf(0) }

    val glass = enabled && backdrop != null

    Box(
        modifier
            .then(
                if (glass) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop!!,
                        shape = { shape },
                        effects = {
                            // 读一次 state，注册快照观察：透明度/模糊变化会即时重绘
                            repaint.intValue
                            if (dark) vibrancy()
                            blur(blurPx)
                            lens(lensPx, lensOffsetPx, chromaticAberration = true)
                        },
                        highlight = { Highlight.Default.copy(alpha = if (dark) 0.34f else 0.62f) },
                        shadow = { Shadow(alpha = if (dark) 0.24f else 0.10f) },
                        innerShadow = { InnerShadow(radius = 6.dp, alpha = if (dark) 0.24f else 0.12f) },
                        onDrawSurface = {
                            drawRect(tint)
                            // 顶部斜向高光：玻璃的「反光」，画在内容之下不挡字
                            drawRect(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = if (dark) 0.10f else 0.26f),
                                        Color.Transparent,
                                    ),
                                    startY = 0f,
                                    endY = size.height * 0.42f,
                                ),
                            )
                        },
                    )
                } else {
                    Modifier.background(cs.surface, clip)
                }
            )
            .border(
                1.dp,
                palette?.border ?: cs.outlineVariant,
                clip,
            ),
    ) {
        CompositionLocalProvider(LocalContentColor provides cs.onSurface) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

/**
 * 悬浮元素的玻璃（导航胶囊等）：和卡片同一套参数，形状可传 Capsule。
 * 采样的是「内容层」，所以能折射出下面滚动的列表。
 */
@Composable
fun Modifier.glassCapsule(
    backdrop: com.kyant.backdrop.Backdrop?,
    corner: androidx.compose.ui.unit.Dp = Metrics.cardCorner,
    capsule: Boolean = false,
): Modifier {
    if (!LocalGlassEnabled.current || backdrop == null) return this
    val tint = glassTint()
    val dark = isSystemInDarkTheme()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val blurPx = with(density) { (glassBlurDp() * 0.6f).dp.toPx() }
    val lensPx = with(density) { 16.dp.toPx() }
    val lensOffsetPx = with(density) { 22.dp.toPx() }
    val kShape: androidx.compose.ui.graphics.Shape =
        if (capsule) Capsule() else RoundedRectangle(corner)
    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { kShape },
        effects = {
            if (dark) vibrancy()
            blur(blurPx)
            lens(lensPx, lensOffsetPx, chromaticAberration = true)
        },
        highlight = { Highlight.Default.copy(alpha = if (dark) 0.30f else 0.55f) },
        shadow = { Shadow(alpha = 0.16f) },
        innerShadow = { InnerShadow(radius = 5.dp, alpha = 0.14f) },
        onDrawSurface = { drawRect(tint) },
    )
}
