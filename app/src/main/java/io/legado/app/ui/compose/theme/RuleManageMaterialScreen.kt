package io.legado.app.ui.compose.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class RuleManageItemUi(
    val key: String,
    val title: String,
    val summary: String,
    val enabled: Boolean,
    val selected: Boolean,
    val badge: String? = null
)

data class RuleManageSheetAction(
    val key: String,
    val title: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleManageMaterialScreen(
    title: String,
    subtitle: String,
    items: List<RuleManageItemUi>,
    selectedCount: Int,
    onBackClick: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onDeleteSelection: () -> Unit,
    onItemClick: (RuleManageItemUi) -> Unit,
    onToggleSelect: (RuleManageItemUi, Boolean) -> Unit,
    onToggleEnabled: (RuleManageItemUi, Boolean) -> Unit,
    onEditItem: (RuleManageItemUi) -> Unit,
    batchActions: List<RuleManageSheetAction>,
    onBatchAction: (String) -> Unit,
    topBarContent: @Composable () -> Unit,
    headerBottomContent: @Composable ColumnScope.() -> Unit = {},
    itemMenuContent: @Composable ((RuleManageItemUi, () -> Unit) -> Unit)
) {
    var showBatchSheet by remember { mutableStateOf(false) }
    val totalCount = items.size
    val isAllSelected = totalCount > 0 && selectedCount == totalCount
    val hasSelection = selectedCount > 0

    Scaffold(
        topBar = {
            Column {
                LegadoPageHeader(
                    title = title,
                    subtitle = subtitle,
                    onBackClick = onBackClick,
                    actions = { topBarContent() },
                    belowTitle = headerBottomContent
                )
            }
        },
        bottomBar = {
            if (totalCount > 0) {
                LegadoSelectionBottomBar(
                    selectedCount = selectedCount,
                    totalCount = totalCount,
                    onSelectAll = onSelectAll,
                    onInvertSelection = onInvertSelection,
                    onDeleteSelection = onDeleteSelection,
                    onMoreClick = { showBatchSheet = true }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (items.isEmpty()) {
                LegadoEmptyState(
                    title = "暂无内容",
                    summary = "当前没有可显示的数据",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        vertical = 0.dp
                    )
                ) {
                    item {
                        Column {
                            items.forEachIndexed { index, item ->
                                RuleManageRow(
                                    item = item,
                                    onClick = { onItemClick(item) },
                                    onToggleSelect = { onToggleSelect(item, it) },
                                    onToggleEnabled = { onToggleEnabled(item, it) },
                                    onEdit = { onEditItem(item) },
                                    menuContent = { dismiss -> itemMenuContent(item, dismiss) }
                                )
                                if (index != items.lastIndex) {
                                    LegadoItemDivider(startIndent = 68)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showBatchSheet) {
            LegadoBatchActionSheet(
                title = "批量操作",
                actions = batchActions.map { it to it.title },
                iconFor = { it.icon },
                onDismiss = { showBatchSheet = false },
                onAction = {
                    onBatchAction(it.key)
                    showBatchSheet = false
                }
            )
        }
    }
}

@Composable
private fun RuleManageRow(
    item: RuleManageItemUi,
    onClick: () -> Unit,
    onToggleSelect: (Boolean) -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    menuContent: @Composable ((() -> Unit)) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    LegadoListRow(
        title = item.title,
        summary = item.summary,
        selected = item.selected,
        leadingContent = {
            Checkbox(
                checked = item.selected,
                onCheckedChange = onToggleSelect
            )
        },
        trailingContent = {
            item.badge?.let {
                LegadoStatusChip(text = it)
            }
            Switch(
                checked = item.enabled,
                onCheckedChange = onToggleEnabled
            )
            IconButton(onClick = onEdit) {
                Icon(Icons.Rounded.Edit, contentDescription = null)
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = null)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    menuContent { showMenu = false }
                }
            }
        },
        onClick = onClick
    )
}
