package io.legado.app.ui.main.my

import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import io.legado.app.ui.compose.theme.SettingsItemUi
import io.legado.app.ui.compose.theme.SettingsMaterialScreen
import io.legado.app.ui.compose.theme.SettingsSectionUi

@Composable
fun MyMaterialScreen(
    themeModeLabel: String,
    webServiceEnabled: Boolean,
    webServiceSummary: String,
    onHelpClick: () -> Unit,
    onThemeModeClick: () -> Unit,
    onWebServiceToggle: (Boolean) -> Unit,
    onItemClick: (String) -> Unit
) {
    val sections = listOf(
        SettingsSectionUi(
            items = listOf(
                SettingsItemUi("bookSourceManage", Icons.AutoMirrored.Rounded.MenuBook, "书源管理", "新建、导入、编辑或管理书源"),
                SettingsItemUi("txtTocRuleManage", Icons.Rounded.Code, "TXT 目录规则", "配置 TXT 目录规则"),
                SettingsItemUi("replaceManage", Icons.Rounded.AutoAwesome, "替换净化", "配置替换净化规则"),
                SettingsItemUi("dictRuleManage", Icons.Rounded.Storage, "字典规则", "配置词典与转换规则"),
                SettingsItemUi("themeMode", Icons.Rounded.ColorLens, "主题模式", "选择主题模式", badge = themeModeLabel),
                SettingsItemUi("webService", Icons.Rounded.Web, "Web 服务", webServiceSummary, checked = webServiceEnabled)
            )
        ),
        SettingsSectionUi(
            label = "设置",
            items = listOf(
                SettingsItemUi("web_dav_setting", Icons.Rounded.CloudUpload, "备份与恢复", "WebDav 设置与本地恢复"),
                SettingsItemUi("theme_setting", Icons.Rounded.ColorLens, "主题设置", "调整前台与阅读主题"),
                SettingsItemUi("setting", Icons.Rounded.Settings, "其它设置", "与功能相关的一些设置")
            )
        ),
        SettingsSectionUi(
            label = "其它",
            items = listOf(
                SettingsItemUi("bookmark", Icons.Rounded.Bookmark, "书签", "查看所有书签"),
                SettingsItemUi("readRecord", Icons.Rounded.History, "阅读记录", "阅读统计与历史"),
                SettingsItemUi("fileManage", Icons.Rounded.Folder, "文件管理", "查看导入与导出文件"),
                SettingsItemUi("about", Icons.Rounded.Info, "关于"),
                SettingsItemUi("exit", Icons.AutoMirrored.Rounded.Logout, "退出", destructive = true)
            )
        )
    )

    SettingsMaterialScreen(
        title = "我的",
        subtitle = "设置、同步和阅读偏好",
        sections = sections,
        onBackClick = null,
        actions = {
            IconButton(onClick = onHelpClick) {
                Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null)
            }
        },
        onItemClick = { key ->
            when (key) {
                "themeMode" -> onThemeModeClick()
                else -> onItemClick(key)
            }
        },
        onSwitchChange = { key, checked ->
            if (key == "webService") {
                onWebServiceToggle(checked)
            }
        }
    )
}
