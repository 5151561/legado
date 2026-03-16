package io.legado.app.ui.main.bookshelf.style1

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SettingsInputComposite
import androidx.compose.material.icons.outlined.Source
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.ui.compose.theme.LegadoPageDefaults
import io.legado.app.ui.compose.theme.LegadoTopBarSurface
import io.legado.app.ui.main.bookshelf.style2.BookGridCard
import io.legado.app.ui.main.bookshelf.style2.BookListCard
import io.legado.app.ui.main.bookshelf.style2.BookshelfBookUi
import io.legado.app.ui.main.bookshelf.style2.BookshelfGroupUi

enum class BookshelfAddAction(
    @param:StringRes val labelRes: Int
) {
    ImportLocal(R.string.book_local),
    AddRemote(R.string.add_remote_book),
    AddUrl(R.string.add_url)
}

enum class BookshelfOverflowAction(
    @param:StringRes val labelRes: Int
) {
    UpdateToc(R.string.update_toc),
    Layout(R.string.bookshelf_layout),
    GroupManage(R.string.group_manage),
    BookshelfManage(R.string.bookshelf_management),
    CacheExport(R.string.cache_export),
    ExportBookshelf(R.string.export_bookshelf),
    ImportBookshelf(R.string.import_bookshelf),
    Log(R.string.log)
}

private enum class BookshelfSourceFilter(
    @param:StringRes val labelRes: Int
) {
    All(R.string.all),
    Local(R.string.local)
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onSearchClick: () -> Unit,
    onAddActionClick: (BookshelfAddAction) -> Unit,
    onOverflowActionClick: (BookshelfOverflowAction) -> Unit,
    onRefresh: () -> Unit,
    onTabClick: (Int) -> Unit,
    onTabLongClick: (BookshelfGroupUi) -> Unit,
    onBookClick: (BookshelfBookUi) -> Unit,
    onBookLongClick: (BookshelfBookUi) -> Unit
) {
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    var sourceFilter by rememberSaveable { mutableStateOf(BookshelfSourceFilter.All) }

    LaunchedEffect(scrollRequest, isGrid) {
        if (scrollRequest <= 0) return@LaunchedEffect
        if (isGrid) {
            gridState.animateScrollToItem(0)
        } else {
            listState.animateScrollToItem(0)
        }
    }

    val filteredBooks = remember(books, sourceFilter) {
        when (sourceFilter) {
            BookshelfSourceFilter.All -> books
            BookshelfSourceFilter.Local -> books.filter { it.isLocal }
        }
    }
    val selectedGroup = groups.getOrNull(selectedTabIndex)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BookshelfTopBar(
                sourceFilter = sourceFilter,
                onFilterSelected = { sourceFilter = it },
                onSearchClick = onSearchClick,
                onOverflowActionClick = onOverflowActionClick
            )
        },
        floatingActionButton = {
            BookshelfAddFab(
                onActionClick = onAddActionClick
            )
        }
    ) { innerPadding ->
        if (isGrid) {
            BookshelfTabGrid(
                selectedTabIndex = selectedTabIndex,
                groups = groups,
                books = filteredBooks,
                totalBooks = books.size,
                selectedGroup = selectedGroup,
                showUnread = showUnread,
                state = gridState,
                gridColumns = gridColumns,
                contentPadding = PaddingValues(
                    start = LegadoPageDefaults.HorizontalPadding,
                    top = innerPadding.calculateTopPadding() + LegadoPageDefaults.SectionSpacing,
                    end = LegadoPageDefaults.HorizontalPadding,
                    bottom = innerPadding.calculateBottomPadding() + 120.dp
                ),
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
                books = filteredBooks,
                totalBooks = books.size,
                selectedGroup = selectedGroup,
                showUnread = showUnread,
                showLastUpdateTime = showLastUpdateTime,
                onRefresh = onRefresh,
                state = listState,
                contentPadding = PaddingValues(
                    start = LegadoPageDefaults.HorizontalPadding,
                    top = innerPadding.calculateTopPadding() + LegadoPageDefaults.SectionSpacing,
                    end = LegadoPageDefaults.HorizontalPadding,
                    bottom = innerPadding.calculateBottomPadding() + 120.dp
                ),
                onTabClick = onTabClick,
                onTabLongClick = onTabLongClick,
                onBookClick = onBookClick,
                onBookLongClick = onBookLongClick
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookshelfTopBar(
    sourceFilter: BookshelfSourceFilter,
    onFilterSelected: (BookshelfSourceFilter) -> Unit,
    onSearchClick: () -> Unit,
    onOverflowActionClick: (BookshelfOverflowAction) -> Unit
) {
    var showOverflowMenu by remember { mutableStateOf(false) }
    LegadoTopBarSurface {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = LegadoPageDefaults.HorizontalPadding,
                        end = 8.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "我的书架",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                }
                Box {
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = null
                        )
                    }
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    BookshelfOverflowAction.entries.forEach { action ->
                        DropdownMenuItem(
                            text = {
                                Text(text = androidx.compose.ui.res.stringResource(action.labelRes))
                            },
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
            PrimaryTabRow(
                selectedTabIndex = sourceFilter.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                BookshelfSourceFilter.entries.forEach { filter ->
                    Tab(
                        selected = sourceFilter == filter,
                        onClick = { onFilterSelected(filter) },
                        text = {
                            Text(
                                text = androidx.compose.ui.res.stringResource(filter.labelRes),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (sourceFilter == filter) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Medium
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookshelfAddFab(
    onActionClick: (BookshelfAddAction) -> Unit
) {
    var showAddMenu by remember { mutableStateOf(false) }
    Box {
        LargeFloatingActionButton(
            onClick = { showAddMenu = true },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        }
        DropdownMenu(
            expanded = showAddMenu,
            onDismissRequest = { showAddMenu = false }
        ) {
            BookshelfAddAction.entries.forEach { action ->
                DropdownMenuItem(
                    text = { Text(text = androidx.compose.ui.res.stringResource(action.labelRes)) },
                    leadingIcon = {
                        Icon(
                            imageVector = addActionIcon(action),
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showAddMenu = false
                        onActionClick(action)
                    }
                )
            }
        }
    }
}

@Composable
private fun BookshelfTabList(
    selectedTabIndex: Int,
    groups: List<BookshelfGroupUi>,
    books: List<BookshelfBookUi>,
    totalBooks: Int,
    selectedGroup: BookshelfGroupUi?,
    showUnread: Boolean,
    showLastUpdateTime: Boolean,
    onRefresh: () -> Unit,
    state: androidx.compose.foundation.lazy.LazyListState,
    contentPadding: PaddingValues,
    onTabClick: (Int) -> Unit,
    onTabLongClick: (BookshelfGroupUi) -> Unit,
    onBookClick: (BookshelfBookUi) -> Unit,
    onBookLongClick: (BookshelfBookUi) -> Unit
) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BookshelfOverviewCard(
                selectedGroup = selectedGroup,
                visibleCount = books.size,
                totalCount = totalBooks,
                onRefresh = onRefresh
            )
        }
        if (groups.isNotEmpty()) {
            item {
                BookshelfGroupSelector(
                    selectedTabIndex = selectedTabIndex,
                    groups = groups,
                    onTabClick = onTabClick,
                    onTabLongClick = onTabLongClick
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
private fun BookshelfTabGrid(
    selectedTabIndex: Int,
    groups: List<BookshelfGroupUi>,
    books: List<BookshelfBookUi>,
    totalBooks: Int,
    selectedGroup: BookshelfGroupUi?,
    showUnread: Boolean,
    state: LazyGridState,
    gridColumns: Int,
    contentPadding: PaddingValues,
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
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            BookshelfOverviewCard(
                selectedGroup = selectedGroup,
                visibleCount = books.size,
                totalCount = totalBooks,
                onRefresh = onRefresh
            )
        }
        if (groups.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                BookshelfGroupSelector(
                    selectedTabIndex = selectedTabIndex,
                    groups = groups,
                    onTabClick = onTabClick,
                    onTabLongClick = onTabLongClick
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
private fun BookshelfOverviewCard(
    selectedGroup: BookshelfGroupUi?,
    visibleCount: Int,
    totalCount: Int,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = selectedGroup?.groupName ?: "全部书籍",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "当前展示 $visibleCount 本，书架共 $totalCount 本",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        FilledTonalButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Outlined.Update,
                contentDescription = null
            )
            Text(
                text = "刷新书架",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookshelfGroupSelector(
    selectedTabIndex: Int,
    groups: List<BookshelfGroupUi>,
    onTabClick: (Int) -> Unit,
    onTabLongClick: (BookshelfGroupUi) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "分组",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(groups, key = { it.groupId }) { group ->
                val index = groups.indexOf(group)
                val selected = index == selectedTabIndex
                Surface(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.extraLarge)
                        .combinedClickable(
                            onClick = { onTabClick(index) },
                            onLongClick = { onTabLongClick(group) }
                        ),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    tonalElevation = if (selected) 2.dp else 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = group.groupName,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun BookshelfInlineEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
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
                text = "这个分组里还没有书",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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
