package io.legado.app.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.theme.ThemeStore
import splitties.init.appCtx

/**
 * 主题解析器，将现有的 ThemeStore 和 AppConfig 状态映射为 LegadoThemeState。
 */
object ThemeResolver {

    fun resolve(context: Context = appCtx): LegadoThemeState {
        val isDark = AppConfig.isNightTheme
        val isEInk = AppConfig.isEInkMode
        
        // 从 ThemeStore 获取当前颜色值
        val primary = Color(ThemeStore.primaryColor(context))
        val background = Color(ThemeStore.backgroundColor(context))
        val bottomBackground = Color(ThemeStore.bottomBackground(context))
        val accent = Color(ThemeStore.accentColor(context))
        
        // 映射逻辑（后续可根据统一设计系统调整）
        // 这里暂时使用 ThemeStore 的值映射到 Compose 体系
        return LegadoThemeState(
            isDark = isDark,
            isEInk = isEInk,
            isTransparent = false, // 待接入透明逻辑
            hasBackgroundImage = false, // 待接入背景图逻辑
            primary = primary,
            surface = background, // 暂时映射为 background
            surfaceVariant = bottomBackground,
            background = background,
            onSurface = if (isDark) Color.White else Color.Black, // 待接入更准确的文本色逻辑
            onSurfaceVariant = if (isDark) Color.LightGray else Color.DarkGray,
            outline = Color.Gray,
            accentCompat = accent,
            statusBar = Color(ThemeStore.statusBarColor(context, false)),
            navigationBar = Color(ThemeStore.navigationBarColor(context))
        )
    }
}
