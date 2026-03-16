package io.legado.app.ui.compose.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * 通用的 One UI 风格折叠布局
 * @param title 界面的标题
 * @param modifier 修饰符
 * @param actions 顶栏右侧操作按钮
 * @param content 界面的可滚动内容（必须是 LazyColumn/LazyVerticalGrid 等）
 */
@Composable
fun LegadoOneUIScaffold(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // 设置最大高度为屏幕高度的 40% (约 2/5)，方便单手操作
    val headerMaxHeight = (configuration.screenHeightDp * 0.4f).dp
    val headerMinHeight = 64.dp
    val maxOffsetPx = with(density) { (headerMaxHeight - headerMinHeight).toPx() }

    var headerOffsetPx by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0) {
                    val prev = headerOffsetPx
                    headerOffsetPx = (headerOffsetPx + delta).coerceIn(-maxOffsetPx, 0f)
                    val consumed = headerOffsetPx - prev
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (delta > 0) {
                    val prev = headerOffsetPx
                    headerOffsetPx = (headerOffsetPx + delta).coerceIn(-maxOffsetPx, 0f)
                    val consumedByHeader = headerOffsetPx - prev
                    return Offset(0f, consumedByHeader)
                }
                return Offset.Zero
            }
        }
    }

    val collapseProgress = -headerOffsetPx / maxOffsetPx

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(nestedScrollConnection)
    ) {
        // --- 滚动内容区域 ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, headerOffsetPx.roundToInt()) }
        ) {
            content(
                PaddingValues(
                    top = headerMaxHeight,
                    bottom = 16.dp 
                )
            )
        }

        // --- 固定的 Header 区域 ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerMaxHeight)
                .offset { IntOffset(0, headerOffsetPx.roundToInt()) }
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 大标题
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer {
                        // 稍微加快渐隐速度，使其在大约滑动一半时完全消失
                        alpha = (1f - (collapseProgress * 2f)).coerceIn(0f, 1f)
                        translationY = -(collapseProgress * 60f)
                    }
            )
        }

        // --- 顶部的常驻导航栏 ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerMinHeight),
            color = if (collapseProgress > 0.9f) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            } else {
                Color.Transparent
            },
            tonalElevation = if (collapseProgress > 0.9f) 4.dp else 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 顶部小标题
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.graphicsLayer {
                        // 在最后 30% 阶段显示
                        alpha = ((collapseProgress - 0.7f) * 3.33f).coerceIn(0f, 1f)
                    }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    actions()
                }
            }
        }
    }
}
