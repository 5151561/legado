package io.legado.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 统一主题模型，作为全应用主题真值的唯一来源。
 */
data class LegadoThemeState(
    val isDark: Boolean,
    val isEInk: Boolean,
    val isTransparent: Boolean,
    val hasBackgroundImage: Boolean,
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val tertiary: Color,
    val tertiaryContainer: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val background: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val accentCompat: Color,
    val toolbar: Color,
    val statusBar: Color,
    val navigationBar: Color,
    val textPrimaryCompat: Color,
    val textSecondaryCompat: Color,
    val success: Color,
    val warning: Color
)
