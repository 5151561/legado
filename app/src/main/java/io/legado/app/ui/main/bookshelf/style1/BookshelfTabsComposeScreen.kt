package io.legado.app.ui.main.bookshelf.style1

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.legado.app.data.entities.BookGroup
import io.legado.app.ui.main.bookshelf.style2.BookGridCard
import io.legado.app.ui.main.bookshelf.style2.BookListCard
import io.legado.app.ui.main.bookshelf.style2.BookshelfActionRow
import io.legado.app.ui.main.bookshelf.style2.BookshelfBookUi
import io.legado.app.ui.main.bookshelf.style2.BookshelfEmptyState
import io.legado.app.ui.main.bookshelf.style2.BookshelfGroupUi

@Composable
fun BookshelfTabsComposeScreen(
    selectedTabIndex: Int,
    groups: List<BookshelfGroupUi>,
    books: List<BookshelfBookUi>,
    showUnread: Boolean,
    showLastUpdateTime: Boolean,
    isGrid: Boolean,
    gridColumns: Int,
    scrollRequest: Int,
    onRefresh: () -> Unit,
    onTabClick: (Int) -> Unit,
    onTabLongClick: (BookshelfGroupUi) -> Unit,
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
        if (groups.isEmpty()) {
            BookshelfEmptyState()
            return@Surface
        }
        if (isGrid) {
            BookshelfTabGrid(
                selectedTabIndex = selectedTabIndex,
                groups = groups,
                books = books,
                showUnread = showUnread,
                state = gridState,
                gridColumns = gridColumns,
                onRefresh = onRefresh,
                onTabClick = onTabClick,
                onTabLongClick = onTabLongClick,
                onBookClick = onBookClick,
                onBookLongClick = onBookLongClick
            )
        } else {
            BookshelfTabList(
                selectedTabIndex = selectedTabIndex,
                groups = groups,
                books = books,
                showUnread = showUnread,
                showLastUpdateTime = showLastUpdateTime,
                onRefresh = onRefresh,
                state = listState,
                onTabClick = onTabClick,
                onTabLongClick = onTabLongClick,
                onBookClick = onBookClick,
                onBookLongClick = onBookLongClick
            )
        }
    }
}

@Composable
private fun BookshelfTabList(
    selectedTabIndex: Int,
    groups: List<BookshelfGroupUi>,
    books: List<BookshelfBookUi>,
    showUnread: Boolean,
    showLastUpdateTime: Boolean,
    onRefresh: () -> Unit,
    state: androidx.compose.foundation.lazy.LazyListState,
    onTabClick: (Int) -> Unit,
    onTabLongClick: (BookshelfGroupUi) -> Unit,
    onBookClick: (BookshelfBookUi) -> Unit,
    onBookLongClick: (BookshelfBookUi) -> Unit
) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BookshelfTabSelector(
                selectedTabIndex = selectedTabIndex,
                groups = groups,
                onTabClick = onTabClick,
                onTabLongClick = onTabLongClick
            )
        }
        item {
            BookshelfActionRow(
                groupId = BookGroup.IdRoot,
                onRefresh = onRefresh,
                onBackToRoot = {}
            )
        }
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

@Composable
private fun BookshelfTabGrid(
    selectedTabIndex: Int,
    groups: List<BookshelfGroupUi>,
    books: List<BookshelfBookUi>,
    showUnread: Boolean,
    state: LazyGridState,
    gridColumns: Int,
    onRefresh: () -> Unit,
    onTabClick: (Int) -> Unit,
    onTabLongClick: (BookshelfGroupUi) -> Unit,
    onBookClick: (BookshelfBookUi) -> Unit,
    onBookLongClick: (BookshelfBookUi) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns.coerceAtLeast(2)),
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            BookshelfTabSelector(
                selectedTabIndex = selectedTabIndex,
                groups = groups,
                onTabClick = onTabClick,
                onTabLongClick = onTabLongClick
            )
        }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
            BookshelfActionRow(
                groupId = BookGroup.IdRoot,
                onRefresh = onRefresh,
                onBackToRoot = {}
            )
        }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookshelfTabSelector(
    selectedTabIndex: Int,
    groups: List<BookshelfGroupUi>,
    onTabClick: (Int) -> Unit,
    onTabLongClick: (BookshelfGroupUi) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = if (groups.isEmpty()) 0 else selectedTabIndex.coerceIn(0, groups.lastIndex),
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            groups.forEachIndexed { index, group ->
                Tab(
                    selected = index == selectedTabIndex,
                    onClick = { onTabClick(index) },
                    text = {
                        Text(
                            text = group.groupName,
                            modifier = Modifier.combinedClickable(
                                onClick = { onTabClick(index) },
                                onLongClick = { onTabLongClick(group) }
                            ),
                            fontWeight = if (index == selectedTabIndex) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            }
                        )
                    },
                    selectedContentColor = MaterialTheme.colorScheme.primary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
