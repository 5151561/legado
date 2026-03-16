package io.legado.app.ui.rss.source.manage

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
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
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.legado.app.data.entities.RssSource
import io.legado.app.ui.compose.theme.LegadoBatchActionSheet
import io.legado.app.ui.compose.theme.LegadoSearchField
import io.legado.app.ui.compose.theme.LegadoSelectionBottomBar
import io.legado.app.ui.compose.theme.LegadoSwitch

enum class RssSourceTopAction {
    Add,
    ImportLocal,
    ImportOnline,
    ImportQr,
    GroupManage,
    ImportDefault,
    Help,
    FilterEnabled,
    FilterDisabled,
    FilterNeedLogin,
    FilterNoGroup
}

enum class RssSourceBatchAction {
    EnableSelected,
    DisableSelected,
    AddGroup,
    RemoveGroup,
    TopSelected,
    BottomSelected,
    ExportSelected,
    ShareSelected,
    CheckSelectedInterval
}

enum class RssSourceItemAction {
    Delete,
    ToTop,
    ToBottom
}

@Composable
fun RssSourceScreen(
    sources: List<RssSource>,
    selectedUrls: Set<String>,
    searchQuery: String,
    groups: List<String>,
    onSearchQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onTopAction: (RssSourceTopAction) -> Unit,
    onGroupFilterClick: (String) -> Unit,
    onToggleSelect: (RssSource) -> Unit,
    onToggleEnable: (RssSource, Boolean) -> Unit,
    onEditClick: (RssSource) -> Unit,
    onItemAction: (RssSource, RssSourceItemAction) -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onBatchAction: (RssSourceBatchAction) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val selectedCount = selectedUrls.size

    Scaffold(
        topBar = {
            TopSearchBar(
                query = searchQuery,
                groups = groups,
                onQueryChange = onSearchQueryChange,
                onBackClick = onBackClick,
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
                    items(
                        items = sources,
                        key = { it.sourceUrl }
                    ) { source ->
                        RssSourceItem(
                            source = source,
                            isSelected = selectedUrls.contains(source.sourceUrl),
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
                    RssSourceBatchAction.EnableSelected to "启用所选",
                    RssSourceBatchAction.DisableSelected to "禁用所选",
                    RssSourceBatchAction.AddGroup to "添加分组",
                    RssSourceBatchAction.RemoveGroup to "移除分组",
                    RssSourceBatchAction.TopSelected to "置顶所选",
                    RssSourceBatchAction.BottomSelected to "置底所选",
                    RssSourceBatchAction.CheckSelectedInterval to "补齐区间",
                    RssSourceBatchAction.ExportSelected to "导出所选",
                    RssSourceBatchAction.ShareSelected to "分享所选"
                ),
                iconFor = {
                    when (it) {
                        RssSourceBatchAction.EnableSelected -> Icons.Default.PlayArrow
                        RssSourceBatchAction.DisableSelected -> Icons.Default.Stop
                        RssSourceBatchAction.AddGroup -> Icons.Default.Folder
                        RssSourceBatchAction.RemoveGroup -> Icons.Default.FolderOff
                        RssSourceBatchAction.TopSelected -> Icons.Default.KeyboardDoubleArrowUp
                        RssSourceBatchAction.BottomSelected -> Icons.Default.KeyboardDoubleArrowDown
                        RssSourceBatchAction.CheckSelectedInterval -> Icons.Default.CheckCircle
                        RssSourceBatchAction.ExportSelected -> Icons.Default.Language
                        RssSourceBatchAction.ShareSelected -> Icons.Default.Language
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
    onQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onTopAction: (RssSourceTopAction) -> Unit,
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
        LegadoSearchField(
            query = query,
            placeholder = "搜索订阅源",
            onQueryChange = onQueryChange,
            modifier = Modifier.weight(1f)
        )
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
                onAction = {
                    onTopAction(it)
                    dismiss()
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupMenuContent(
    groups: List<String>,
    onAction: (RssSourceTopAction) -> Unit,
    onGroupClick: (String) -> Unit
) {
    DropdownMenuItem(
        text = { Text("分组管理") },
        onClick = { onAction(RssSourceTopAction.GroupManage) },
        leadingIcon = { Icon(Icons.Default.Groups, null) }
    )
    DropdownMenuItem(text = { Text("只看启用") }, onClick = { onAction(RssSourceTopAction.FilterEnabled) })
    DropdownMenuItem(text = { Text("只看禁用") }, onClick = { onAction(RssSourceTopAction.FilterDisabled) })
    DropdownMenuItem(text = { Text("需要登录") }, onClick = { onAction(RssSourceTopAction.FilterNeedLogin) })
    DropdownMenuItem(text = { Text("无分组") }, onClick = { onAction(RssSourceTopAction.FilterNoGroup) })
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
    onAction: (RssSourceTopAction) -> Unit
) {
    DropdownMenuItem(
        text = { Text("新建订阅源") },
        onClick = { onAction(RssSourceTopAction.Add) },
        leadingIcon = { Icon(Icons.Default.Add, null) }
    )
    DropdownMenuItem(
        text = { Text("本地导入") },
        onClick = { onAction(RssSourceTopAction.ImportLocal) },
        leadingIcon = { Icon(Icons.Default.Folder, null) }
    )
    DropdownMenuItem(
        text = { Text("网络导入") },
        onClick = { onAction(RssSourceTopAction.ImportOnline) },
        leadingIcon = { Icon(Icons.Default.Language, null) }
    )
    DropdownMenuItem(
        text = { Text("二维码导入") },
        onClick = { onAction(RssSourceTopAction.ImportQr) },
        leadingIcon = { Icon(Icons.Default.QrCodeScanner, null) }
    )
    DropdownMenuItem(
        text = { Text("导入默认") },
        onClick = { onAction(RssSourceTopAction.ImportDefault) },
        leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
    )
    DropdownMenuItem(
        text = { Text("帮助") },
        onClick = { onAction(RssSourceTopAction.Help) },
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
private fun RssSourceItem(
    source: RssSource,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onEnableChange: (Boolean) -> Unit,
    onEditClick: () -> Unit,
    onActionClick: (RssSourceItemAction) -> Unit
) {
    var showItemMenu by remember { mutableStateOf(false) }
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
        } else {
            Color.Transparent
        },
        label = "rss_source_item_background"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
    ) {
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
                    text = source.getDisplayNameGroup(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = source.sourceUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            LegadoSwitch(
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
                    DropdownMenuItem(
                        text = { Text("置顶") },
                        onClick = {
                            onActionClick(RssSourceItemAction.ToTop)
                            showItemMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.KeyboardDoubleArrowUp, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("置底") },
                        onClick = {
                            onActionClick(RssSourceItemAction.ToBottom)
                            showItemMenu = false
                        },
                        leadingIcon = { Icon(Icons.Default.KeyboardDoubleArrowDown, null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("删除") },
                        onClick = {
                            onActionClick(RssSourceItemAction.Delete)
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
            }
        }
    }
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
        Text("没有找到匹配的订阅源", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
