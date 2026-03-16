package io.legado.app.ui.book.changesource

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.legado.app.data.entities.SearchBook
import io.legado.app.help.config.AppConfig
import io.legado.app.ui.compose.theme.LegadoPageHeader
import io.legado.app.ui.compose.theme.LegadoSearchField

@Composable
fun ChangeBookSourceScreen(
    viewModel: ChangeBookSourceViewModel,
    oldBookUrl: String?,
    onBackClick: () -> Unit,
    onSourceClick: (SearchBook) -> Unit,
    onAction: (SearchBook, ChangeBookSourceAction) -> Unit,
    onTopAction: (ChangeBookSourceTopAction) -> Unit
) {
    val searchBooks by viewModel.searchDataFlow.collectAsState(initial = emptyList())
    val isLoading by viewModel.searchStateData.observeAsState(false)
    val progress by viewModel.changeSourceProgress.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Column {
                LegadoPageHeader(
                    title = viewModel.name,
                    subtitle = viewModel.author,
                    onBackClick = onBackClick,
                    actions = {
                        IconButton(onClick = { viewModel.startOrStopSearch() }) {
                            Icon(
                                if (isLoading) Icons.Default.Stop else Icons.Default.Refresh,
                                contentDescription = null
                            )
                        }
                        TopMenuButton(onTopAction)
                    }
                ) {
                    LegadoSearchField(
                        query = searchQuery,
                        placeholder = "搜索源或书名",
                        onQueryChange = {
                            searchQuery = it
                            viewModel.screen(it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    if (isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        bottomBar = {
            if (progress.first > 0 || isLoading) {
                Surface(tonalElevation = 2.dp) {
                    Text(
                        text = "正在搜索: ${progress.second} (${progress.first}/${viewModel.totalSourceCount})",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(searchBooks, key = { it.bookUrl }) { book ->
                ChangeSourceItem(
                    book = book,
                    isCurrent = book.bookUrl == oldBookUrl,
                    score = viewModel.getBookScore(book),
                    onClick = { onSourceClick(book) },
                    onAction = { onAction(book, it) },
                    onScoreChange = { viewModel.setBookScore(book, it) }
                )
            }
        }
    }
}

@Composable
fun ChangeSourceItem(
    book: SearchBook,
    isCurrent: Boolean,
    score: Int,
    onClick: () -> Unit,
    onAction: (ChangeBookSourceAction) -> Unit,
    onScoreChange: (Int) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val backgroundColor by animateColorAsState(
        targetValue = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent,
        label = "bg"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = book.originName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isCurrent) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = book.getDisplayLastChapterTitle(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onScoreChange(if (score > 0) 0 else 1) }) {
                    Icon(
                        Icons.Default.ThumbUp,
                        contentDescription = null,
                        tint = if (score > 0) Color(0xFFFF5252) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { onScoreChange(if (score < 0) 0 else -1) }) {
                    Icon(
                        Icons.Default.ThumbDown,
                        contentDescription = null,
                        tint = if (score < 0) Color(0xFF448AFF) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("置顶源") },
                            onClick = { onAction(ChangeBookSourceAction.ToTop); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.KeyboardDoubleArrowUp, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("置底源") },
                            onClick = { onAction(ChangeBookSourceAction.ToBottom); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.KeyboardDoubleArrowDown, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("编辑源") },
                            onClick = { onAction(ChangeBookSourceAction.Edit); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Settings, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("禁用源") },
                            onClick = { onAction(ChangeBookSourceAction.Disable); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Stop, null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("删除源") },
                            onClick = { onAction(ChangeBookSourceAction.Delete); showMenu = false },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }
        }
        
        if (AppConfig.changeSourceLoadWordCount && !book.chapterWordCountText.isNullOrBlank()) {
            Text(
                text = book.chapterWordCountText!!,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TopMenuButton(onTopAction: (ChangeBookSourceTopAction) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("校验作者") },
                onClick = { onTopAction(ChangeBookSourceTopAction.ToggleCheckAuthor); expanded = false },
                trailingIcon = { Checkbox(checked = AppConfig.changeSourceCheckAuthor, onCheckedChange = null) }
            )
            DropdownMenuItem(
                text = { Text("加载详情") },
                onClick = { onTopAction(ChangeBookSourceTopAction.ToggleLoadInfo); expanded = false },
                trailingIcon = { Checkbox(checked = AppConfig.changeSourceLoadInfo, onCheckedChange = null) }
            )
            DropdownMenuItem(
                text = { Text("加载目录") },
                onClick = { onTopAction(ChangeBookSourceTopAction.ToggleLoadToc); expanded = false },
                trailingIcon = { Checkbox(checked = AppConfig.changeSourceLoadToc, onCheckedChange = null) }
            )
            DropdownMenuItem(
                text = { Text("加载字数") },
                onClick = { onTopAction(ChangeBookSourceTopAction.ToggleLoadWordCount); expanded = false },
                trailingIcon = { Checkbox(checked = AppConfig.changeSourceLoadWordCount, onCheckedChange = null) }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("刷新列表") },
                onClick = { onTopAction(ChangeBookSourceTopAction.RefreshList); expanded = false }
            )
            DropdownMenuItem(
                text = { Text("源管理") },
                onClick = { onTopAction(ChangeBookSourceTopAction.SourceManage); expanded = false }
            )
        }
    }
}

enum class ChangeBookSourceAction {
    ToTop, ToBottom, Edit, Disable, Delete
}

enum class ChangeBookSourceTopAction {
    ToggleCheckAuthor, ToggleLoadInfo, ToggleLoadToc, ToggleLoadWordCount, RefreshList, SourceManage
}
