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
    val surface: Color,
    val surfaceVariant: Color,
    val background: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val accentCompat: Color,
    val statusBar: Color,
    val navigationBar: Color
)
