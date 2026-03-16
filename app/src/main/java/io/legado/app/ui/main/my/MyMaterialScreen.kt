package io.legado.app.ui.main.my

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Web
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.legado.app.ui.compose.theme.LegadoPageDefaults
import io.legado.app.ui.compose.theme.LegadoSectionCard
import io.legado.app.ui.compose.theme.LegadoSectionLabel

data class MySettingItemUi(
    val key: String,
    val icon: ImageVector,
    val title: String,
    val desc: String? = null,
    val badge: String? = null,
    val switchChecked: Boolean? = null,
    val destructive: Boolean = false
)

@Composable
fun MyMaterialScreen(
    themeModeLabel: String,
    webServiceEnabled: Boolean,
    webServiceSummary: String,
    onHelpClick: () -> Unit,
    onThemeModeClick: () -> Unit,
    onWebServiceToggle: (Boolean) -> Unit,
    onWebServiceLongClick: () -> Unit,
    onItemClick: (String) -> Unit
) {
    val primaryItems = listOf(
        MySettingItemUi("bookSourceManage", Icons.AutoMirrored.Rounded.MenuBook, "书源管理", "新建、导入、编辑或管理书源"),
        MySettingItemUi("txtTocRuleManage", Icons.Rounded.Code, "TXT 目录规则", "配置 TXT 目录规则"),
        MySettingItemUi("replaceManage", Icons.Rounded.AutoAwesome, "替换净化", "配置替换净化规则"),
        MySettingItemUi("dictRuleManage", Icons.Rounded.Storage, "字典规则", "配置词典与转换规则"),
        MySettingItemUi("themeMode", Icons.Rounded.ColorLens, "主题模式", "选择主题模式", badge = themeModeLabel),
        MySettingItemUi("webService", Icons.Rounded.Web, "Web 服务", webServiceSummary, switchChecked = webServiceEnabled)
    )
    val settingItems = listOf(
        MySettingItemUi("web_dav_setting", Icons.Rounded.CloudUpload, "备份与恢复", "WebDav 设置/导入旧版本数据"),
        MySettingItemUi("theme_setting", Icons.Rounded.ColorLens, "主题设置", "调整前台与阅读主题"),
        MySettingItemUi("setting", Icons.Rounded.Settings, "其它设置", "与功能相关的一些设置")
    )
    val otherItems = listOf(
        MySettingItemUi("bookmark", Icons.Rounded.Bookmark, "书签", "查看所有书签"),
        MySettingItemUi("readRecord", Icons.Rounded.History, "阅读记录", "阅读统计与历史"),
        MySettingItemUi("fileManage", Icons.Rounded.Folder, "文件管理", "查看导入与导出文件"),
        MySettingItemUi("about", Icons.Rounded.Info, "关于"),
        MySettingItemUi("exit", Icons.AutoMirrored.Rounded.Logout, "退出", destructive = true)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = LegadoPageDefaults.HorizontalPadding,
            end = LegadoPageDefaults.HorizontalPadding,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(LegadoPageDefaults.SectionSpacing)
    ) {
        item {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "我的",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "设置、同步和阅读偏好",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onHelpClick) {
                    Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null)
                }
            }
        }
        item { SettingsSection(items = primaryItems, onItemClick = onItemClick, onItemLongClick = { key ->
            when (key) {
                "themeMode" -> onThemeModeClick()
                "webService" -> onWebServiceLongClick()
                else -> Unit
            }
        }, onSwitchChange = { key, checked ->
            if (key == "webService") {
                onWebServiceToggle(checked)
            }
        }) }
        item { LegadoSectionLabel("设置") }
        item { SettingsSection(items = settingItems, onItemClick = onItemClick) }
        item { LegadoSectionLabel("其它") }
        item { SettingsSection(items = otherItems, onItemClick = onItemClick) }
    }
}

@Composable
private fun SettingsSection(
    items: List<MySettingItemUi>,
    onItemClick: (String) -> Unit,
    onItemLongClick: (String) -> Unit = {},
    onSwitchChange: (String, Boolean) -> Unit = { _, _ -> }
) {
    LegadoSectionCard(contentPadding = PaddingValues(vertical = 4.dp)) {
        items.forEachIndexed { index, item ->
            SettingsItemRow(
                item = item,
                onClick = { onItemClick(item.key) },
                onLongClick = { onItemLongClick(item.key) },
                onSwitchChange = { onSwitchChange(item.key, it) }
            )
            if (index != items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 88.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsItemRow(
    item: MySettingItemUi,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwitchChange: (Boolean) -> Unit
) {
    val contentColor = if (item.destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val iconTint = if (item.destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (item.destructive) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(item.icon, contentDescription = null, tint = iconTint)
            }
        }
        Spacer(modifier = Modifier.size(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            item.desc?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        when {
            item.switchChecked != null -> {
                Switch(
                    checked = item.switchChecked,
                    onCheckedChange = onSwitchChange
                )
            }

            !item.badge.isNullOrBlank() -> {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape
                ) {
                    Text(
                        text = item.badge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            else -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                )
            }
        }
    }
}
