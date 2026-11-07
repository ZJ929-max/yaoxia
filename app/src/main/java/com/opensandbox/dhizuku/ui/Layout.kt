// SPDX-License-Identifier: MPL-2.0
// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.

package com.opensandbox.dhizuku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 全应用统一的几何与间距。
 *
 * 页面只消费这里的量，不再各自写 132.dp / 176.dp 这类魔法数字：
 * 顶栏由「状态栏 inset + 内容高度」自然撑开，底部留给浮层导航栏的空白也从这里算。
 */
internal object Metrics {
    /** 页面左右留白。 */
    val pageHorizontal: Dp = 16.dp

    /** 卡片 / 列表段的圆角。 */
    val cardCorner: Dp = 22.dp

    /** 顶栏内容高度（不含状态栏）。 */
    val headerHeight: Dp = 64.dp

    /** 悬浮导航胶囊本体高度。 */
    val tabBarHeight: Dp = 64.dp

    /** 导航胶囊上下留白（与 FloatingTabBar 内部 padding 一致）。 */
    val tabBarPadding: Dp = 10.dp

    /** 内容底部需要让出的高度：胶囊本体 + 上下留白。 */
    val tabBarSpace: Dp = tabBarHeight + tabBarPadding * 2

    /** 顶栏雾气带的过渡长度：从顶栏底边往下这么多，玻璃化到完全透明。 */
    val fadeRamp: Dp = 28.dp

    /** 内容往上滚多少就认为「已经完全进到顶栏下面」——雾气的目标浓度。 */
    val frostDistance: Dp = 56.dp

    /** 列表行内边距。 */
    val rowHorizontal: Dp = 14.dp
    val rowVertical: Dp = 10.dp
}

/** 一行在整段列表里的位置：只有首尾是圆角，中间拼成一整张卡。 */
internal enum class RowSlot { Single, First, Middle, Last }

internal fun rowSlot(index: Int, count: Int): RowSlot =
    when {
        count <= 1 -> RowSlot.Single
        index == 0 -> RowSlot.First
        index == count - 1 -> RowSlot.Last
        else -> RowSlot.Middle
    }

internal fun slotShape(slot: RowSlot, corner: Dp = Metrics.cardCorner): Shape =
    when (slot) {
        RowSlot.Single -> RoundedCornerShape(corner)
        RowSlot.First -> RoundedCornerShape(topStart = corner, topEnd = corner)
        RowSlot.Middle -> RoundedCornerShape(0.dp)
        RowSlot.Last -> RoundedCornerShape(bottomStart = corner, bottomEnd = corner)
    }

/**
 * 列表行的底色：浅色下接近实心（保证白底上文字对比度），深色下半透明（透出梦幻泡泡）。
 * 由 [LocalGlassAlpha] 推导，用户拖动透明度滑杆时两端都会跟着动。
 */
@Composable
internal fun listRowColor(): Color {
    val surface = MaterialTheme.colorScheme.surface
    return if (isSystemInDarkTheme()) {
        surface.copy(alpha = (0.14f + LocalGlassAlpha.current * 1.15f).coerceIn(0.14f, 0.60f))
    } else {
        // 浅色：留一点透明，行底能透出背景光斑（磨砂感），文字仍是深色，对比度够
        surface.copy(alpha = (0.34f + LocalGlassAlpha.current * 0.9f).coerceIn(0.34f, 0.86f))
    }
}

/**
 * 一段「设置列表」里的一行：整段共用一张卡片，行与行之间用内缩分隔线。
 * [tint] 用来标记被冻结 / 被选中的行，叠在底色之上，不改变透明度。
 */
@Composable
internal fun ListRow(
    slot: RowSlot,
    modifier: Modifier = Modifier,
    tint: Color = Color.Transparent,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = slotShape(slot)
    val base = listRowColor()
    val background = if (tint.alpha > 0f) tint.compositeOver(base) else base
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background, shape),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Metrics.rowHorizontal, vertical = Metrics.rowVertical),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        if (slot == RowSlot.First || slot == RowSlot.Middle) {
            HorizontalDivider(
                Modifier.padding(start = Metrics.rowHorizontal),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * 顶部渐隐：内容滚到标题区时逐渐化开，而不是被一条硬边切掉。
 *
 * 实现是「透明度遮罩」而不是「盖一层底色」——盖一层底色在极光背景下会变成一条死板的色带，
 * 那正是之前「挡住一切」的观感。这里只把节点自己的绘制结果按垂直渐变扣掉 alpha。
 */
internal fun Modifier.alphaMask(brush: Brush): Modifier =
    drawWithContent {
        val canvas = drawContext.canvas
        canvas.saveLayer(Rect(0f, 0f, size.width, size.height), Paint())
        drawContent()
        drawRect(brush = brush, blendMode = BlendMode.DstIn)
        canvas.restore()
    }

/** 页面把「内容滚进去多少」写回来，顶栏据此决定雾气的浓度——静止时一点雾气都没有。 */
internal val LocalScrollProgress = staticCompositionLocalOf<MutableState<Float>?> { null }

@Composable
internal fun ReportScrollProgress(index: Int, offset: Int) {
    val reporter = LocalScrollProgress.current ?: return
    val threshold = with(LocalDensity.current) { Metrics.frostDistance.toPx() }
    val progress = if (index > 0) 1f else (offset / threshold).coerceIn(0f, 1f)
    SideEffect { reporter.value = progress }
}
