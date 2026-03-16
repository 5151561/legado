package io.legado.app.ui.compose.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class SettingsSectionUi(
    val label: String? = null,
    val items: List<SettingsItemUi>
)

data class SettingsItemUi(
    val key: String,
    val icon: ImageVector,
    val title: String,
    val summary: String? = null,
    val badge: String? = null,
    val checked: Boolean? = null,
    val destructive: Boolean = false
)

@Composable
fun SettingsMaterialScreen(
    title: String,
    subtitle: String,
    sections: List<SettingsSectionUi>,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    onItemClick: (String) -> Unit,
    onSwitchChange: (String, Boolean) -> Unit = { _, _ -> }
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = LegadoPageDefaults.HorizontalPadding,
            end = LegadoPageDefaults.HorizontalPadding,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(LegadoPageDefaults.SectionSpacing)
    ) {
        item {
            LegadoPageHeader(
                title = title,
                subtitle = subtitle,
                onBackClick = onBackClick,
                actions = { actions() }
            )
        }
        sections.forEach { section ->
            if (!section.label.isNullOrBlank()) {
                item { LegadoSectionLabel(section.label) }
            }
            item {
                LegadoSectionCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                    section.items.forEachIndexed { index, item ->
                        LegadoListRow(
                            title = item.title,
                            summary = item.summary,
                            destructive = item.destructive,
                            leadingContent = {
                                LegadoLeadingIcon(
                                    icon = item.icon,
                                    tint = if (item.destructive) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    containerColor = if (item.destructive) {
                                        MaterialTheme.colorScheme.errorContainer
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    }
                                )
                            },
                            trailingContent = {
                                when {
                                    item.checked != null -> {
                                        LegadoSwitch(
                                            checked = item.checked,
                                            onCheckedChange = { onSwitchChange(item.key, it) }
                                        )
                                    }

                                    !item.badge.isNullOrBlank() -> {
                                        LegadoStatusChip(text = item.badge)
                                    }

                                    else -> {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                        )
                                    }
                                }
                            },
                            onClick = { onItemClick(item.key) }
                        )
                        if (index != section.items.lastIndex) {
                            LegadoItemDivider()
                        }
                    }
                }
            }
        }
    }
}
