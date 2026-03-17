package io.legado.app.ui.main.rss

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.legado.app.data.entities.RssSource
import io.legado.app.ui.compose.theme.LegadoPageDefaults
import io.legado.app.ui.compose.theme.LegadoPageHeader
import io.legado.app.ui.compose.theme.LegadoSearchField
import io.legado.app.ui.compose.theme.LegadoSectionCard
import io.legado.app.ui.widget.image.CoverImageView

@Composable
fun RssComposeScreen(
    onSearchQueryChange: (String) -> Unit,
    onRssClick: (RssSource) -> Unit,
    onAction: (RssSource, RssAction) -> Unit,
    onTopAction: (RssTopAction) -> Unit,
    rssSources: List<RssSource>
) {
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            LegadoPageHeader(
                title = "订阅",
                subtitle = "订阅源、收藏和最新内容入口",
                actions = {
                    IconButton(onClick = { onTopAction(RssTopAction.Star) }) {
                        Icon(Icons.Default.Star, contentDescription = "收藏")
                    }
                    IconButton(onClick = { onTopAction(RssTopAction.Config) }) {
                        Icon(Icons.Default.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            contentPadding = PaddingValues(
                start = LegadoPageDefaults.HorizontalPadding,
                top = paddingValues.calculateTopPadding(),
                end = LegadoPageDefaults.HorizontalPadding,
                bottom = 24.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LegadoSectionCard {
                    LegadoSearchField(
                        query = searchQuery,
                        placeholder = "搜索订阅源",
                        onQueryChange = {
                            searchQuery = it
                            onSearchQueryChange(it)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (rssSources.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    LegadoSectionCard {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 72.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "没有找到订阅源",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(rssSources, key = { it.sourceUrl }) { rss ->
                    RssItem(
                        rss = rss,
                        onClick = { onRssClick(rss) },
                        onAction = { onAction(rss, it) }
                    )
                }
            }
        }
    }
}

@Composable
fun RssItem(
    rss: RssSource,
    onClick: () -> Unit,
    onAction: (RssAction) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Surface(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context -> CoverImageView(context) },
                    update = { view ->
                        view.load(
                            path = rss.sourceIcon,
                            name = rss.sourceName,
                            author = null,
                            sourceOrigin = rss.sourceUrl
                        )
                    }
                )
            }
            
            Box(modifier = Modifier.padding(4.dp)) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    )
                }
                
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("置顶") },
                        onClick = { onAction(RssAction.ToTop); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.KeyboardDoubleArrowUp, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        onClick = { onAction(RssAction.Edit); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("禁用") },
                        onClick = { onAction(RssAction.Disable); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Stop, null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = { onAction(RssAction.Delete); showMenu = false },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = rss.sourceName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

enum class RssAction {
    ToTop, Edit, Disable, Delete
}

enum class RssTopAction {
    Star, Config
}
