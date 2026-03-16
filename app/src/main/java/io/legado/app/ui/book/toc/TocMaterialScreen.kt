package io.legado.app.ui.book.toc

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.legado.app.data.entities.Bookmark
import kotlinx.coroutines.launch

data class TocChapterUi(
    val index: Int,
    val title: String,
    val meta: String,
    val isCurrent: Boolean,
    val isLocked: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TocMaterialScreen(
    query: String,
    activeTab: Int,
    chapterItems: List<TocChapterUi>,
    bookmarkItems: List<Bookmark>,
    currentProgress: String,
    overflowExpanded: Boolean,
    useReplace: Boolean,
    loadWordCount: Boolean,
    splitLongChapter: Boolean,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onTabSelected: (Int) -> Unit,
    onOverflowClick: () -> Unit,
    onOverflowDismiss: () -> Unit,
    onReverseToc: () -> Unit,
    onToggleUseReplace: () -> Unit,
    onToggleLoadWordCount: () -> Unit,
    onToggleSplitLongChapter: () -> Unit,
    onShowTocRegex: () -> Unit,
    onExportBookmark: () -> Unit,
    onExportBookmarkMd: () -> Unit,
    onShowLog: () -> Unit,
    onChapterClick: (TocChapterUi) -> Unit,
    onBookmarkClick: (Bookmark) -> Unit,
    onBookmarkLongClick: (Bookmark, Int) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val tabTitles = listOf("目录", "书签")

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "章节目录",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentProgress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = onOverflowClick) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = null)
                }
                TocOverflowMenu(
                    expanded = overflowExpanded,
                    useReplace = useReplace,
                    loadWordCount = loadWordCount,
                    splitLongChapter = splitLongChapter,
                    onDismiss = onOverflowDismiss,
                    onReverseToc = onReverseToc,
                    onToggleUseReplace = onToggleUseReplace,
                    onToggleLoadWordCount = onToggleLoadWordCount,
                    onToggleSplitLongChapter = onToggleSplitLongChapter,
                    onShowTocRegex = onShowTocRegex,
                    onExportBookmark = onExportBookmark,
                    onExportBookmarkMd = onExportBookmarkMd,
                    onShowLog = onShowLog
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            placeholder = { Text("搜索章节或书签") }
        )

        PrimaryTabRow(
            selectedTabIndex = activeTab,
            modifier = Modifier.padding(top = 12.dp)
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { onTabSelected(index) },
                    text = { Text(title) }
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (activeTab == 0) {
                itemsIndexed(chapterItems, key = { _, item -> item.index }) { _, item ->
                    ChapterItemCard(
                        item = item,
                        onClick = { onChapterClick(item) }
                    )
                }
            } else {
                itemsIndexed(bookmarkItems, key = { _, item -> item.time }) { index, item ->
                    BookmarkItemCard(
                        item = item,
                        onClick = { onBookmarkClick(item) },
                        onLongClick = { onBookmarkLongClick(item, index) }
                    )
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "当前进度",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = currentProgress,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        IconButton(onClick = { scope.launch { listState.animateScrollToItem(0) } }) {
                            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        IconButton(onClick = {
                            scope.launch {
                                val count = if (activeTab == 0) chapterItems.size else bookmarkItems.size
                                if (count > 0) {
                                    listState.animateScrollToItem(count - 1)
                                }
                            }
                        }) {
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TocOverflowMenu(
    expanded: Boolean,
    useReplace: Boolean,
    loadWordCount: Boolean,
    splitLongChapter: Boolean,
    onDismiss: () -> Unit,
    onReverseToc: () -> Unit,
    onToggleUseReplace: () -> Unit,
    onToggleLoadWordCount: () -> Unit,
    onToggleSplitLongChapter: () -> Unit,
    onShowTocRegex: () -> Unit,
    onExportBookmark: () -> Unit,
    onExportBookmarkMd: () -> Unit,
    onShowLog: () -> Unit
) {
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        MenuItem("章节倒序", onReverseToc)
        MenuItem("标题替换", onToggleUseReplace, checked = useReplace)
        MenuItem("加载字数", onToggleLoadWordCount, checked = loadWordCount)
        MenuItem("拆分长章节", onToggleSplitLongChapter, checked = splitLongChapter)
        MenuItem("TXT 目录规则", onShowTocRegex)
        MenuItem("导出书签", onExportBookmark)
        MenuItem("导出 Markdown", onExportBookmarkMd)
        MenuItem("日志", onShowLog)
    }
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit, checked: Boolean = false) {
    androidx.compose.material3.DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick,
        trailingIcon = if (checked) {
            { Icon(Icons.Rounded.Check, contentDescription = null) }
        } else {
            null
        }
    )
}

@Composable
private fun ChapterItemCard(
    item: TocChapterUi,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (item.isCurrent) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        },
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (item.isCurrent) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookmarkItemCard(
    item: Bookmark,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = item.chapterName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.content.ifBlank { item.bookText },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
