package io.legado.app.ui.main.bookshelf.style2

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkAdded
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SettingsInputComposite
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SubdirectoryArrowLeft
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import io.legado.app.R
import io.legado.app.data.entities.BookGroup
import io.legado.app.ui.compose.theme.LegadoPageDefaults
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.ui.main.bookshelf.style1.BookshelfAddAction
import io.legado.app.ui.main.bookshelf.style1.BookshelfOverflowAction
import io.legado.app.ui.widget.image.CoverImageView

data class BookshelfBookUi(
    val bookUrl: String,
    val name: String,
    val author: String,
    val origin: String,
    val cover: String?,
    val currentChapter: String?,
    val latestChapter: String?,
    val latestChapterTime: Long,
    val unreadCount: Int,
    val lastCheckCount: Int,
    val isUpdating: Boolean,
    val isLocal: Boolean
)

data class BookshelfGroupUi(
    val groupId: Long,
    val groupName: String,
    val cover: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfComposeScreen(
    groupId: Long,
    groups: List<BookshelfGroupUi>,
    books: List<BookshelfBookUi>,
    showUnread: Boolean,
    showLastUpdateTime: Boolean,
    isGrid: Boolean,
    gridColumns: Int,
    scrollRequest: Int,
    isRefreshing: Boolean,
    onSearchClick: () -> Unit,
    onAddActionClick: (BookshelfAddAction) -> Unit,
    onOverflowActionClick: (BookshelfOverflowAction) -> Unit,
    onRefresh: () -> Unit,
    onBackToRoot: () -> Unit,
    onBookClick: (BookshelfBookUi) -> Unit,
    onBookLongClick: (BookshelfBookUi) -> Unit,
    onGroupClick: (BookshelfGroupUi) -> Unit,
    onGroupLongClick: (BookshelfGroupUi) -> Unit
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    LaunchedEffect(scrollRequest, isGrid) {
        if (scrollRequest <= 0) return@LaunchedEffect
        if (isGrid) {
            gridState.animateScrollToItem(0)
        } else {
            listState.animateScrollToItem(0)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            if (isGrid) {
                BookshelfGrid(
                    groupId = groupId,
                    groups = groups,
                    books = books,
                    showUnread = showUnread,
                    state = gridState,
                    gridColumns = gridColumns,
                    onSearchClick = onSearchClick,
                    onAddActionClick = onAddActionClick,
                    onOverflowActionClick = onOverflowActionClick,
                    onBackToRoot = onBackToRoot,
                    onBookClick = onBookClick,
                    onBookLongClick = onBookLongClick,
                    onGroupClick = onGroupClick,
                    onGroupLongClick = onGroupLongClick
                )
            } else {
                BookshelfList(
                    groupId = groupId,
                    groups = groups,
                    books = books,
                    showUnread = showUnread,
                    showLastUpdateTime = showLastUpdateTime,
                    state = listState,
                    onSearchClick = onSearchClick,
                    onAddActionClick = onAddActionClick,
                    onOverflowActionClick = onOverflowActionClick,
                    onBackToRoot = onBackToRoot,
                    onBookClick = onBookClick,
                    onBookLongClick = onBookLongClick,
                    onGroupClick = onGroupClick,
                    onGroupLongClick = onGroupLongClick
                )
            }
        }
    }
}

@Composable
private fun BookshelfFolderHeader(
    groupId: Long,
    groups: List<BookshelfGroupUi>,
    onSearchClick: () -> Unit,
    onAddActionClick: (BookshelfAddAction) -> Unit,
    onOverflowActionClick: (BookshelfOverflowAction) -> Unit
) {
    val currentGroup = remember(groupId, groups) {
        groups.firstOrNull { it.groupId == groupId }
    }
    var showAddMenu by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.bookshelf),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (groupId == BookGroup.IdRoot) {
                    "${groups.size} ${stringResource(R.string.group)}"
                } else {
                    currentGroup?.groupName.orEmpty()
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null
            )
        }
        Box {
            IconButton(onClick = { showAddMenu = true }) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = showAddMenu,
                onDismissRequest = { showAddMenu = false }
            ) {
                BookshelfAddAction.entries.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(text = stringResource(action.labelRes)) },
                        leadingIcon = {
                            Icon(
                                imageVector = addActionIcon(action),
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showAddMenu = false
                            onAddActionClick(action)
                        }
                    )
                }
            }
        }
        Box {
            IconButton(onClick = { showOverflowMenu = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = null
                )
            }
            DropdownMenu(
                expanded = showOverflowMenu,
                onDismissRequest = { showOverflowMenu = false }
            ) {
                BookshelfOverflowAction.entries.forEach { action ->
                    DropdownMenuItem(
                        text = { Text(text = stringResource(action.labelRes)) },
                        leadingIcon = {
                            Icon(
                                imageVector = overflowActionIcon(action),
                                contentDescription = null
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onOverflowActionClick(action)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookshelfList(
    groupId: Long,
    groups: List<BookshelfGroupUi>,
    books: List<BookshelfBookUi>,
    showUnread: Boolean,
    showLastUpdateTime: Boolean,
    state: androidx.compose.foundation.lazy.LazyListState,
    onSearchClick: () -> Unit,
    onAddActionClick: (BookshelfAddAction) -> Unit,
    onOverflowActionClick: (BookshelfOverflowAction) -> Unit,
    onBackToRoot: () -> Unit,
    onBookClick: (BookshelfBookUi) -> Unit,
    onBookLongClick: (BookshelfBookUi) -> Unit,
    onGroupClick: (BookshelfGroupUi) -> Unit,
    onGroupLongClick: (BookshelfGroupUi) -> Unit
) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(LegadoPageDefaults.HorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(LegadoPageDefaults.SectionSpacing)
    ) {
        item {
            BookshelfFolderHeader(
                groupId = groupId,
                groups = groups,
                onSearchClick = onSearchClick,
                onAddActionClick = onAddActionClick,
                onOverflowActionClick = onOverflowActionClick
            )
        }
        if (groupId != BookGroup.IdRoot) {
            item {
                BookshelfActionRow(onBackToRoot = onBackToRoot)
            }
        }
        if (groupId == BookGroup.IdRoot && groups.isNotEmpty()) {
            item {
                BookshelfGroupRow(
                    groups = groups,
                    onGroupClick = onGroupClick,
                    onGroupLongClick = onGroupLongClick
                )
            }
        }
        if (books.isEmpty()) {
            item {
                BookshelfInlineEmptyState()
            }
        } else {
            items(books, key = { it.bookUrl }) { book ->
                BookListCard(
                    book = book,
                    showUnread = showUnread,
                    showLastUpdateTime = showLastUpdateTime,
                    onClick = { onBookClick(book) },
                    onLongClick = { onBookLongClick(book) }
                )
            }
        }
    }
}

@Composable
private fun BookshelfGrid(
    groupId: Long,
    groups: List<BookshelfGroupUi>,
    books: List<BookshelfBookUi>,
    showUnread: Boolean,
    state: LazyGridState,
    gridColumns: Int,
    onSearchClick: () -> Unit,
    onAddActionClick: (BookshelfAddAction) -> Unit,
    onOverflowActionClick: (BookshelfOverflowAction) -> Unit,
    onBackToRoot: () -> Unit,
    onBookClick: (BookshelfBookUi) -> Unit,
    onBookLongClick: (BookshelfBookUi) -> Unit,
    onGroupClick: (BookshelfGroupUi) -> Unit,
    onGroupLongClick: (BookshelfGroupUi) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns.coerceAtLeast(2)),
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(LegadoPageDefaults.HorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(LegadoPageDefaults.SectionSpacing),
        verticalArrangement = Arrangement.spacedBy(LegadoPageDefaults.SectionSpacing)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            BookshelfFolderHeader(
                groupId = groupId,
                groups = groups,
                onSearchClick = onSearchClick,
                onAddActionClick = onAddActionClick,
                onOverflowActionClick = onOverflowActionClick
            )
        }
        if (groupId != BookGroup.IdRoot) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BookshelfActionRow(onBackToRoot = onBackToRoot)
            }
        }
        if (groupId == BookGroup.IdRoot && groups.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BookshelfGroupRow(
                    groups = groups,
                    onGroupClick = onGroupClick,
                    onGroupLongClick = onGroupLongClick
                )
            }
        }
        if (books.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BookshelfInlineEmptyState()
            }
        } else {
            items(books, key = { it.bookUrl }) { book ->
                BookGridCard(
                    book = book,
                    showUnread = showUnread,
                    onClick = { onBookClick(book) },
                    onLongClick = { onBookLongClick(book) }
                )
            }
        }
    }
}

@Composable
internal fun BookshelfActionRow(
    onBackToRoot: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        OutlinedButton(onClick = onBackToRoot) {
            Icon(Icons.Outlined.SubdirectoryArrowLeft, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.all))
        }
    }
}

@Composable
private fun BookshelfGroupRow(
    groups: List<BookshelfGroupUi>,
    onGroupClick: (BookshelfGroupUi) -> Unit,
    onGroupLongClick: (BookshelfGroupUi) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.group),
            style = MaterialTheme.typography.titleMedium
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(groups, key = { it.groupId }) { group ->
                GroupCard(
                    group = group,
                    onClick = { onGroupClick(group) },
                    onLongClick = { onGroupLongClick(group) }
                )
            }
        }
    }
}

@Composable
private fun BookshelfInlineEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoStories,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.bookshelf_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BookListCard(
    book: BookshelfBookUi,
    showUnread: Boolean,
    showLastUpdateTime: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BookCover(
                cover = book.cover,
                name = book.name,
                author = book.author,
                sourceOrigin = book.origin,
                modifier = Modifier.size(width = 92.dp, height = 128.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = book.name,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    BookStatusBadge(
                        isUpdating = book.isUpdating,
                        showUnread = showUnread,
                        unreadCount = book.unreadCount,
                        highlight = book.lastCheckCount > 0
                    )
                }
                InfoLine(
                    icon = Icons.Outlined.PersonOutline,
                    text = stringResource(R.string.author_show, book.author)
                )
                if (showLastUpdateTime && !book.isLocal) {
                    InfoLine(
                        icon = Icons.Outlined.Schedule,
                        text = DateUtils.getRelativeTimeSpanString(book.latestChapterTime).toString(),
                    )
                }
                InfoLine(
                    icon = Icons.Outlined.History,
                    text = stringResource(
                        R.string.read_dur_progress,
                        book.currentChapter.orEmpty().ifBlank { "-" }
                    )
                )
                InfoLine(
                    icon = Icons.Outlined.BookmarkAdded,
                    text = stringResource(
                        R.string.lasted_show,
                        book.latestChapter.orEmpty().ifBlank { "-" }
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BookGridCard(
    book: BookshelfBookUi,
    showUnread: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box {
                BookCover(
                    cover = book.cover,
                    name = book.name,
                    author = book.author,
                    sourceOrigin = book.origin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(5f / 7f)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    BookStatusBadge(
                        isUpdating = book.isUpdating,
                        showUnread = showUnread,
                        unreadCount = book.unreadCount,
                        highlight = book.lastCheckCount > 0
                    )
                }
            }
            Text(
                text = book.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GroupCard(
    group: BookshelfGroupUi,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(width = 108.dp, height = 160.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BookCover(
                cover = group.cover,
                name = group.groupName,
                author = null,
                sourceOrigin = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(5f / 7f)
            )
            Text(
                text = group.groupName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InfoLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BookStatusBadge(
    isUpdating: Boolean,
    showUnread: Boolean,
    unreadCount: Int,
    highlight: Boolean
) {
    when {
        isUpdating -> {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        }

        showUnread && unreadCount > 0 -> {
            val badgeColor = if (highlight) {
                LegadoTheme.extendedColors.warning
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
            val badgeTextColor = if (highlight) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSecondaryContainer
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(badgeColor)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = unreadCount.toString(),
                    color = badgeTextColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun BookCover(
    cover: String?,
    name: String?,
    author: String?,
    sourceOrigin: String?,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.clip(MaterialTheme.shapes.medium),
        factory = { context -> CoverImageView(context) },
        update = { view ->
            view.load(
                path = cover,
                name = name,
                author = author,
                sourceOrigin = sourceOrigin
            )
        }
    )
}

private fun addActionIcon(action: BookshelfAddAction) = when (action) {
    BookshelfAddAction.ImportLocal -> Icons.Outlined.LibraryAdd
    BookshelfAddAction.AddRemote -> Icons.Outlined.CloudDownload
    BookshelfAddAction.AddUrl -> Icons.Outlined.Source
}

private fun overflowActionIcon(action: BookshelfOverflowAction) = when (action) {
    BookshelfOverflowAction.UpdateToc -> Icons.Outlined.Update
    BookshelfOverflowAction.Layout -> Icons.Outlined.Widgets
    BookshelfOverflowAction.GroupManage -> Icons.Outlined.Storage
    BookshelfOverflowAction.BookshelfManage -> Icons.Outlined.SettingsInputComposite
    BookshelfOverflowAction.CacheExport -> Icons.Outlined.Download
    BookshelfOverflowAction.ExportBookshelf -> Icons.Outlined.FileDownload
    BookshelfOverflowAction.ImportBookshelf -> Icons.Outlined.CloudDownload
    BookshelfOverflowAction.Log -> Icons.Outlined.FindInPage
}
