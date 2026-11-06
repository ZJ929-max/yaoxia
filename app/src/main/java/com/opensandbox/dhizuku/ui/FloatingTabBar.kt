package com.opensandbox.dhizuku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/**
 * 悬浮胶囊导航栏——照着 InstallerX Revived 的观感做：
 * 半透明底 + 细高光边 + 大圆角，浮在内容之上，而不是贴边的一条实色栏。
 */
@Composable
fun FloatingTabBar(
    items: List<Pair<ImageVector, String>>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(32.dp)
    val surface = MaterialTheme.colorScheme.surface

    val backdrop = LocalBackdrop.current
    val glass = LocalGlassEnabled.current && backdrop != null

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(64.dp)
            .then(
                if (glass) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop!!,
                        shape = { shape },
                        effects = {
                            vibrancy()
                            blur(8.dp.toPx())
                            lens(12.dp.toPx(), 24.dp.toPx())
                        },
                    )
                } else {
                    Modifier.clip(shape).background(surface.copy(alpha = 0.92f))
                }
            )
            .border(1.dp, Color.White.copy(alpha = 0.10f), shape),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, (icon, label) ->
            val active = index == selected
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable { onSelect(index) }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
            ) {
                Box(
                    Modifier
                        .size(width = 48.dp, height = 28.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
