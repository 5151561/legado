package io.legado.app.ui.main.bookshelf

import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import io.legado.app.data.entities.BookGroup
import io.legado.app.ui.main.bookshelf.style1.BookshelfAddAction
import io.legado.app.ui.main.bookshelf.style1.BookshelfOverflowAction
import io.legado.app.ui.main.bookshelf.style2.BookshelfBookUi
import io.legado.app.ui.main.bookshelf.style2.BookshelfGroupUi
import io.legado.app.ui.widget.image.CoverImageView

@Composable
fun BookshelfTabsMaterialScreen(
    title: String,
    selectedTabIndex: Int,
    groups: List<BookshelfGroupUi>,
    books: List<BookshelfBookUi>,
    showUnread: Boolean,
    showLastUpdateTime: Boolean,
    isGrid: Boolean,
    gridColumns: Int,
    scrollRequest: Int,
    onSearchClick: () -> Unit,
    onAddActionClick: (BookshelfAddAction) -> Unit,
    onOverflowActionClick: (BookshelfOverflowAction) -> Unit,
    onTabClick: (Int) -> Unit,
    onTabLongClick: (BookshelfGroupUi) -> Unit,
    onBookClick: (BookshelfBookUi) -> Unit,
    onBookLongClick: (BookshelfBookUi) -> Unit
) {
    var addExpanded by rememberSaveable { mutableStateOf(false) }
    var overflowExpanded by rememberSaveable { mutableStateOf(false) }
    val selectedGroupId = groups.getOrNull(selectedTabIndex)?.groupId

    BookshelfMaterialContent(
        title = title,
        groups = groups,
        selectedGroupId = selectedGroupId,
        books = books,
        showUnread = showUnread,
        showLastUpdateTime = showLastUpdateTime,
        isGrid = isGrid,
        gridColumns = gridColumns,
        scrollRequest = scrollRequest,
        leadingAction = null,
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Outlined.Search, contentDescription = null)
            }
            Box {
                FilledIconButton(onClick = { addExpanded = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                }
                DropdownMenu(
                    expanded = addExpanded,
                    onDismissRequest = { addExpanded = false }
                ) {
                    BookshelfAddAction.entries.forEach { action ->
                        DropdownMenuItem(
                            text = { Text(text = androidx.compose.ui.res.stringResource(action.labelRes)) },
                            onClick = {
                                addExpanded = false
                                onAddActionClick(action)
                            }
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { overflowExpanded = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = null)
                }
                DropdownMenu(
                    expanded = overflowExpanded,
                    onDismissRequest = { overflowExpanded = false }
                ) {
                    BookshelfOverflowAction.entries.forEach { action ->
                        DropdownMenuItem(
                            text = { Text(text = androidx.compose.ui.res.stringResource(action.labelRes)) },
                            onClick = {
                                overflowExpanded = false
                                onOverflowActionClick(action)
                            }
                        )
                    }
                }
            }
        },
        onGroupClick = { group ->
            groups.indexOfFirst { it.groupId == group.groupId }
                .takeIf { it >= 0 }
                ?.let(onTabClick)
        },
        onGroupLongClick = onTabLongClick,
        onBookClick = onBookClick,
        onBookLongClick = onBookLongClick
    )
}

@Composable
fun BookshelfGroupMaterialScreen(
    title: String,
    groupId: Long,
    groups: List<BookshelfGroupUi>,
    books: List<BookshelfBookUi>,
    showUnread: Boolean,
    showLastUpdateTime: Boolean,
    isGrid: Boolean,
    gridColumns: Int,
    scrollRequest: Int,
    onRefresh: () -> Unit,
    onBackToRoot: () -> Unit,
    onGroupClick: (BookshelfGroupUi) -> Unit,
    onGroupLongClick: (BookshelfGroupUi) -> Unit,
    onBookClick: (BookshelfBookUi) -> Unit,
    onBookLongClick: (BookshelfBookUi) -> Unit
) {
    BookshelfMaterialContent(
        title = title,
        groups = groups,
        selectedGroupId = if (groupId == BookGroup.IdRoot) null else groupId,
        books = books,
        showUnread = showUnread,
        showLastUpdateTime = showLastUpdateTime,
        isGrid = isGrid,
        gridColumns = gridColumns,
        scrollRequest = scrollRequest,
                leadingAction = if (groupId == BookGroup.IdRoot) {
            null
        } else {
            {
                IconButton(onClick = onBackToRoot) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                }
            }
        },
        actions = {
            FilledIconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
            }
        },
        onGroupClick = onGroupClick,
        onGroupLongClick = onGroupLongClick,
        onBookClick = onBookClick,
        onBookLongClick = onBookLongClick
    )
}

@Composable
private fun BookshelfMaterialContent(
    title: String,
    groups: List<BookshelfGroupUi>,
    selectedGroupId: Long?,
    books: List<BookshelfBookUi>,
    showUnread: Boolean,
    showLastUpdateTime: Boolean,
    isGrid: Boolean,
    gridColumns: Int,
    scrollRequest: Int,
    leadingAction: (@Composable (() -> Unit))?,
    actions: @Composable RowScope.() -> Unit,
    onGroupClick: (BookshelfGroupUi) -> Unit,
    onGroupLongClick: (BookshelfGroupUi) -> Unit,
    onBookClick: (BookshelfBookUi) -> Unit,
    onBookLongClick: (BookshelfBookUi) -> Unit
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
        if (isGrid) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns.coerceAtLeast(2)),
                state = gridState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    BookshelfHeader(
                        title = title,
                        groups = groups,
                        selectedGroupId = selectedGroupId,
                        leadingAction = leadingAction,
                        actions = actions,
                        onGroupClick = onGroupClick,
                        onGroupLongClick = onGroupLongClick
                    )
                }
                if (books.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        BookshelfEmptyState()
                    }
                } else {
                    items(books, key = { it.bookUrl }) { book ->
                        CreamBookGridCard(
                            book = book,
                            showUnread = showUnread,
                            onClick = { onBookClick(book) },
                            onLongClick = { onBookLongClick(book) }
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    BookshelfHeader(
                        title = title,
                        groups = groups,
                        selectedGroupId = selectedGroupId,
                        leadingAction = leadingAction,
                        actions = actions,
                        onGroupClick = onGroupClick,
                        onGroupLongClick = onGroupLongClick
                    )
                }
                if (books.isEmpty()) {
                    item {
                        BookshelfEmptyState()
                    }
                } else {
                    items(books, key = { it.bookUrl }) { book ->
                        CreamBookListCard(
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
    }
}

@Composable
private fun BookshelfHeader(
    title: String,
    groups: List<BookshelfGroupUi>,
    selectedGroupId: Long?,
    leadingAction: (@Composable (() -> Unit))?,
    actions: @Composable RowScope.() -> Unit,
    onGroupClick: (BookshelfGroupUi) -> Unit,
    onGroupLongClick: (BookshelfGroupUi) -> Unit
) {
    Column {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingAction?.invoke()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = if (leadingAction == null) 8.dp else 4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        text = "${groups.size} 个分组",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
        if (groups.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(groups, key = { it.groupId }) { group ->
                    val isSelected = selectedGroupId == null && groups.size == 1 ||
                        selectedGroupId == group.groupId
                    BookshelfGroupChip(
                        group = group,
                        selected = isSelected,
                        onClick = { onGroupClick(group) },
                        onLongClick = { onGroupLongClick(group) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookshelfGroupChip(
    group: BookshelfGroupUi,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = group.groupName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CreamBookListCard(
    book: BookshelfBookUi,
    showUnread: Boolean,
    showLastUpdateTime: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        ),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BookCover(
                cover = book.cover,
                name = book.name,
                author = book.author,
                sourceOrigin = book.origin,
                modifier = Modifier.size(width = 84.dp, height = 116.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = book.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (book.lastCheckCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp, top = 6.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error)
                        )
                    }
                }
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                InfoLine(
                    icon = Icons.Outlined.Schedule,
                    text = buildString {
                        append("读至：")
                        append(book.currentChapter.orEmpty().ifBlank { "-" })
                    },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                InfoLine(
                    icon = Icons.Outlined.Refresh,
                    text = buildString {
                        append("最新：")
                        append(book.latestChapter.orEmpty().ifBlank { "-" })
                        if (showLastUpdateTime && !book.isLocal) {
                            append(" · ")
                            append(
                                DateUtils.getRelativeTimeSpanString(book.latestChapterTime).toString()
                            )
                        }
                    },
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            StatusPill(
                showUnread = showUnread,
                unreadCount = book.unreadCount,
                isUpdating = book.isUpdating
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CreamBookGridCard(
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
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                    StatusPill(
                        showUnread = showUnread,
                        unreadCount = book.unreadCount,
                        isUpdating = book.isUpdating
                    )
                }
            }
            Text(
                text = book.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = book.author,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusPill(
    showUnread: Boolean,
    unreadCount: Int,
    isUpdating: Boolean
) {
    when {
        isUpdating -> {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "…",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }

        showUnread && unreadCount > 0 -> {
            Badge(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Text(text = unreadCount.toString())
            }
        }
    }
}

@Composable
private fun InfoLine(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tint
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        modifier = modifier.clip(RoundedCornerShape(20.dp)),
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

@Composable
private fun BookshelfEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "书",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
            Text(
                text = "当前书架还是空的",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        Text(
            text = "导入书籍后会出现在这里",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
