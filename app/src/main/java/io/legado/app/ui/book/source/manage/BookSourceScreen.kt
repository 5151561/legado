package io.legado.app.ui.book.source.manage

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.ManageSearch
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.ui.compose.theme.LegadoBatchActionSheet
import io.legado.app.ui.compose.theme.LegadoSelectionBottomBar
import io.legado.app.ui.compose.theme.LegadoTheme

enum class BookSourceTopAction {
    Add,
    ImportLocal,
    ImportOnline,
    ImportQr,
    GroupManage,
    ToggleDomainGroup,
    Help,
    FilterEnabled,
    FilterDisabled,
    FilterNeedLogin,
    FilterNoGroup,
    FilterEnabledExplore,
    FilterDisabledExplore
}

enum class BookSourceBatchAction {
    EnableSelected,
    DisableSelected,
    EnableExploreSelected,
    DisableExploreSelected,
    AddGroup,
    RemoveGroup,
    TopSelected,
    BottomSelected,
    ExportSelected,
    ShareSelected,
    CheckSelected,
    CheckSelectedInterval
}

enum class BookSourceItemAction {
    Delete,
    ToggleExplore,
    ToTop,
    ToBottom,
    Search,
    Debug,
    Login
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSourceScreen(
    sources: List<BookSourcePart>,
    selectedUrls: Set<String>,
    searchQuery: String,
    groups: List<String>,
    sort: BookSourceSort,
    sortAscending: Boolean,
    groupSourcesByDomain: Boolean,
    debugMessageVersion: Int,
    onSearchQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSortChange: (BookSourceSort) -> Unit,
    onToggleSortAscending: () -> Unit,
    onTopAction: (BookSourceTopAction) -> Unit,
    onGroupFilterClick: (String) -> Unit,
    onToggleSelect: (BookSourcePart) -> Unit,
    onToggleEnable: (BookSourcePart, Boolean) -> Unit,
    onEditClick: (BookSourcePart) -> Unit,
    onItemAction: (BookSourcePart, BookSourceItemAction) -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onBatchAction: (BookSourceBatchAction) -> Unit,
    sourceHostProvider: (BookSourcePart) -> String,
    debugMessageProvider: (BookSourcePart) -> String
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val selectedCount = selectedUrls.size
    val isAllSelected = selectedCount == sources.size && sources.isNotEmpty()

    Scaffold(
        topBar = {
            TopSearchBar(
                query = searchQuery,
                groups = groups,
                sort = sort,
                sortAscending = sortAscending,
                groupSourcesByDomain = groupSourcesByDomain,
                onQueryChange = onSearchQueryChange,
                onBackClick = onBackClick,
                onSortChange = onSortChange,
                onToggleSortAscending = onToggleSortAscending,
                onTopAction = onTopAction,
                onGroupFilterClick = onGroupFilterClick
            )
        },
        bottomBar = {
                LegadoSelectionBottomBar(
                    selectedCount = selectedCount,
                    totalCount = sources.size,
                    onSelectAll = onSelectAll,
                    onInvertSelection = onInvertSelection,
                    onDeleteSelection = onDeleteSelected,
                    onMoreClick = { showBottomSheet = true }
                )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (sources.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(
                        items = sources,
                        key = { _, item -> item.bookSourceUrl }
                    ) { index, source ->
                        val host = sourceHostProvider(source)
                        val hostLabel = if (
                            groupSourcesByDomain &&
                            (index == 0 || sourceHostProvider(sources[index - 1]) != host)
                        ) {
                            host
                        } else {
                            null
                        }
                        BookSourceItem(
                            source = source,
                            hostLabel = hostLabel,
                            debugMessage = debugMessageProvider(source) + debugMessageVersion.let { "" },
                            isSelected = selectedUrls.contains(source.bookSourceUrl),
                            onCheckedChange = { onToggleSelect(source) },
                            onEnableChange = { checked -> onToggleEnable(source, checked) },
                            onEditClick = { onEditClick(source) },
                            onActionClick = { action -> onItemAction(source, action) }
                        )
                    }
                }
            }

        }

        if (showBottomSheet) {
            LegadoBatchActionSheet(
                title = "批量操作",
                actions = listOf(
                    BookSourceBatchAction.EnableSelected to "启用所选",
                    BookSourceBatchAction.DisableSelected to "禁用所选",
                    BookSourceBatchAction.EnableExploreSelected to "启用发现",
                    BookSourceBatchAction.DisableExploreSelected to "禁用发现",
                    BookSourceBatchAction.AddGroup to "添加分组",
                    BookSourceBatchAction.RemoveGroup to "移除分组",
                    BookSourceBatchAction.TopSelected to "置顶所选",
                    BookSourceBatchAction.BottomSelected to "置底所选",
                    BookSourceBatchAction.CheckSelected to "校验所选",
                    BookSourceBatchAction.CheckSelectedInterval to "补齐区间",
                    BookSourceBatchAction.ExportSelected to "导出所选",
                    BookSourceBatchAction.ShareSelected to "分享所选"
                ),
                iconFor = {
                    when (it) {
                        BookSourceBatchAction.EnableSelected -> Icons.Default.PlayArrow
                        BookSourceBatchAction.DisableSelected -> Icons.Default.Stop
                        BookSourceBatchAction.EnableExploreSelected -> Icons.Outlined.Explore
                        BookSourceBatchAction.DisableExploreSelected -> Icons.Outlined.Explore
                        BookSourceBatchAction.AddGroup -> Icons.Default.Folder
                        BookSourceBatchAction.RemoveGroup -> Icons.Default.FolderOff
                        BookSourceBatchAction.TopSelected -> Icons.Default.KeyboardDoubleArrowUp
                        BookSourceBatchAction.BottomSelected -> Icons.Default.KeyboardDoubleArrowDown
                        BookSourceBatchAction.CheckSelected -> Icons.Default.CheckCircle
                        BookSourceBatchAction.CheckSelectedInterval -> Icons.Default.CheckCircle
                        BookSourceBatchAction.ExportSelected -> Icons.Default.Language
                        BookSourceBatchAction.ShareSelected -> Icons.Default.Language
                    }
                },
                onDismiss = { showBottomSheet = false },
                onAction = {
                    onBatchAction(it)
                    showBottomSheet = false
                }
            )
        }
    }
}

@Composable
private fun TopSearchBar(
    query: String,
    groups: List<String>,
    sort: BookSourceSort,
    sortAscending: Boolean,
    groupSourcesByDomain: Boolean,
    onQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSortChange: (BookSourceSort) -> Unit,
    onToggleSortAscending: () -> Unit,
    onTopAction: (BookSourceTopAction) -> Unit,
    onGroupFilterClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = true,
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "搜索书源",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            )
        }
        AnchoredActionMenuButton(
            icon = {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "排序")
            }
        ) { dismiss ->
            SortMenuContent(
                currentSort = sort,
                sortAscending = sortAscending,
                onToggleAscending = {
                    onToggleSortAscending()
                    dismiss()
                },
                onSortChange = {
                    onSortChange(it)
                    dismiss()
                }
            )
        }
        AnchoredActionMenuButton(
            icon = {
                Icon(Icons.Default.Groups, contentDescription = "分组")
            }
        ) { dismiss ->
            GroupMenuContent(
                groups = groups,
                onAction = {
                    onTopAction(it)
                    dismiss()
                },
                onGroupClick = {
                    onGroupFilterClick(it)
                    dismiss()
                }
            )
        }
        AnchoredActionMenuButton(
            icon = {
                Icon(Icons.Default.MoreVert, contentDescription = "更多")
            }
        ) { dismiss ->
            MoreMenuContent(
                groupSourcesByDomain = groupSourcesByDomain,
                onAction = {
                    onTopAction(it)
                    dismiss()
                }
            )
        }
    }
}

@Composable
private fun SortMenuContent(
    currentSort: BookSourceSort,
    sortAscending: Boolean,
    onToggleAscending: () -> Unit,
    onSortChange: (BookSourceSort) -> Unit
) {
    DropdownMenuItem(
        text = { Text(if (sortAscending) "当前: 升序" else "当前: 降序") },
        onClick = onToggleAscending,
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, null) }
    )
    HorizontalDivider()
    listOf(
        BookSourceSort.Default to "手动排序",
        BookSourceSort.Weight to "智能排序",
        BookSourceSort.Name to "按名称",
        BookSourceSort.Url to "按链接",
        BookSourceSort.Update to "按更新时间",
        BookSourceSort.Respond to "按响应时间",
        BookSourceSort.Enable to "按启用状态"
    ).forEach { (sort, label) ->
        DropdownMenuItem(
            text = {
                Text(if (currentSort == sort) "✓ $label" else label)
            },
            onClick = { onSortChange(sort) }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupMenuContent(
    groups: List<String>,
    onAction: (BookSourceTopAction) -> Unit,
    onGroupClick: (String) -> Unit
) {
    DropdownMenuItem(
        text = { Text("分组管理") },
        onClick = { onAction(BookSourceTopAction.GroupManage) },
        leadingIcon = { Icon(Icons.Default.Groups, null) }
    )
    DropdownMenuItem(text = { Text("只看启用") }, onClick = { onAction(BookSourceTopAction.FilterEnabled) })
    DropdownMenuItem(text = { Text("只看禁用") }, onClick = { onAction(BookSourceTopAction.FilterDisabled) })
    DropdownMenuItem(text = { Text("需要登录") }, onClick = { onAction(BookSourceTopAction.FilterNeedLogin) })
    DropdownMenuItem(text = { Text("无分组") }, onClick = { onAction(BookSourceTopAction.FilterNoGroup) })
    DropdownMenuItem(text = { Text("启用发现") }, onClick = { onAction(BookSourceTopAction.FilterEnabledExplore) })
    DropdownMenuItem(text = { Text("禁用发现") }, onClick = { onAction(BookSourceTopAction.FilterDisabledExplore) })
    if (groups.isNotEmpty()) {
        HorizontalDivider()
        FlowRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groups.forEach { group ->
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.clickable { onGroupClick(group) }
                ) {
                    Text(
                        text = group,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MoreMenuContent(
    groupSourcesByDomain: Boolean,
    onAction: (BookSourceTopAction) -> Unit
) {
    DropdownMenuItem(
        text = { Text("新建书源") },
        onClick = { onAction(BookSourceTopAction.Add) },
        leadingIcon = { Icon(Icons.Default.Add, null) }
    )
    DropdownMenuItem(
        text = { Text("本地导入") },
        onClick = { onAction(BookSourceTopAction.ImportLocal) },
        leadingIcon = { Icon(Icons.Default.Folder, null) }
    )
    DropdownMenuItem(
        text = { Text("网络导入") },
        onClick = { onAction(BookSourceTopAction.ImportOnline) },
        leadingIcon = { Icon(Icons.Default.Language, null) }
    )
    DropdownMenuItem(
        text = { Text("二维码导入") },
        onClick = { onAction(BookSourceTopAction.ImportQr) },
        leadingIcon = { Icon(Icons.Default.QrCodeScanner, null) }
    )
    DropdownMenuItem(
        text = { Text(if (groupSourcesByDomain) "取消域名分组" else "按域名分组") },
        onClick = { onAction(BookSourceTopAction.ToggleDomainGroup) },
        leadingIcon = { Icon(Icons.Default.Language, null) }
    )
    DropdownMenuItem(
        text = { Text("帮助") },
        onClick = { onAction(BookSourceTopAction.Help) },
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Help, null) }
    )
}

@Composable
private fun AnchoredActionMenuButton(
    icon: @Composable () -> Unit,
    content: @Composable ((() -> Unit)) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            icon()
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            content { expanded = false }
        }
    }
}

@Composable
private fun BookSourceItem(
    source: BookSourcePart,
    hostLabel: String?,
    debugMessage: String,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onEnableChange: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onActionClick: (BookSourceItemAction) -> Unit
) {
    var showItemMenu by remember { mutableStateOf(false) }
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
        } else {
            Color.Transparent
        },
        label = "book_source_item_background"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
        hostLabel?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!isSelected) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onCheckedChange
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = source.getDisPlayNameGroup(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (debugMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = debugMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if ("成功" in debugMessage) {
                            LegadoTheme.extendedColors.success
                        } else if ("失败" in debugMessage) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Switch(
                checked = source.enabled,
                onCheckedChange = onEnableChange
            )
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "编辑")
            }
            Box {
                IconButton(onClick = { showItemMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
                DropdownMenu(
                    expanded = showItemMenu,
                    onDismissRequest = { showItemMenu = false }
                ) {
                    if (source.hasExploreUrl) {
                        DropdownMenuItem(
                            text = { Text(if (source.enabledExplore) "禁用发现" else "启用发现") },
                            onClick = {
                                onActionClick(BookSourceItemAction.ToggleExplore)
                                showItemMenu = false
                            },
                            leadingIcon = { Icon(Icons.Outlined.Explore, null) }
                        )
                    }
                    if (source.hasLoginUrl) {
                        DropdownMenuItem(
                            text = { Text("登录") },
                            onClick = {
                                onActionClick(BookSourceItemAction.Login)
                                showItemMenu = false
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Login, null) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("搜索") },
                        onClick = {
                            onActionClick(BookSourceItemAction.Search)
                            showItemMenu = false
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.ManageSearch, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("调试") },
                        onClick = {
                            onActionClick(BookSourceItemAction.Debug)
                            showItemMenu = false
                        },
                        leadingIcon = { Icon(Icons.Outlined.BugReport, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("置顶") },
                        onClick = {
                            onActionClick(BookSourceItemAction.ToTop)
                            showItemMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.KeyboardDoubleArrowUp, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("置底") },
                        onClick = {
                            onActionClick(BookSourceItemAction.ToBottom)
                            showItemMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.KeyboardDoubleArrowDown, null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = {
                            onActionClick(BookSourceItemAction.Delete)
                            showItemMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
                ExploreStatusDot(
                    visible = source.hasExploreUrl,
                    enabled = source.enabledExplore,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun ExploreStatusDot(
    visible: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(
                if (enabled) LegadoTheme.extendedColors.success
                else MaterialTheme.colorScheme.error
            )
    )
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.SearchOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("没有找到匹配的书源", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
