package io.legado.app.ui.compose.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
internal fun LegadoSelectionBottomBar(
    selectedCount: Int,
    totalCount: Int,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onDeleteSelection: () -> Unit,
    onMoreClick: () -> Unit,
    deleteLabel: String = "删除"
) {
    val hasSelection = selectedCount > 0
    val isAllSelected = totalCount > 0 && selectedCount == totalCount
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.Checkbox(
                checked = isAllSelected,
                onCheckedChange = { onSelectAll() }
            )
            Text(
                text = "全选 ($selectedCount/$totalCount)",
                style = MaterialTheme.typography.labelLarge
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onInvertSelection, enabled = totalCount > 0) {
                Text("反选")
            }
            TextButton(
                onClick = onDeleteSelection,
                enabled = hasSelection,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(deleteLabel)
            }
            FilledIconButton(
                onClick = onMoreClick,
                enabled = hasSelection,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            ) {
                Icon(Icons.Rounded.MoreVert, contentDescription = null)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T> LegadoBatchActionSheet(
    title: String,
    actions: List<Pair<T, String>>,
    iconFor: (T) -> ImageVector,
    onDismiss: () -> Unit,
    onAction: (T) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(
                horizontal = LegadoPageDefaults.HorizontalPadding,
                vertical = 8.dp
            )
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
            )
            actions.forEach { (action, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = { onAction(action) },
                    leadingIcon = { Icon(iconFor(action), contentDescription = null) }
                )
            }
        }
    }
}
