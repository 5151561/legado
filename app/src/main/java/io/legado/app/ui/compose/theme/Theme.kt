package io.legado.app.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import io.legado.app.ui.theme.LegadoThemeState
import io.legado.app.ui.theme.ThemeResolver

@Immutable
data class LegadoExtendedColors(
    val success: androidx.compose.ui.graphics.Color,
    val warning: androidx.compose.ui.graphics.Color
)

val LocalLegadoExtendedColors = staticCompositionLocalOf {
    LegadoExtendedColors(
        success = androidx.compose.ui.graphics.Color.Unspecified,
        warning = androidx.compose.ui.graphics.Color.Unspecified
    )
}

/**
 * 方便在 Compose 中获取当前主题状态
 */
@Composable
fun rememberThemeState(): LegadoThemeState {
    val context = LocalContext.current
    val input = ThemeResolver.snapshot(context)
    return remember(input) { ThemeResolver.resolve(input) }
}

@Composable
fun LegadoComposeTheme(
    content: @Composable () -> Unit
) {
    val themeState = rememberThemeState()
    val colorScheme = legadoColorScheme(themeState)
    val extendedColors = LegadoExtendedColors(
        success = themeState.success,
        warning = themeState.warning
    )

    androidx.compose.runtime.CompositionLocalProvider(
        LocalLegadoExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LegadoTypography,
            shapes = LegadoShapes,
            content = content
        )
    }
}

@Composable
fun LegadoPreviewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val previewState = ThemeResolver.resolve(ThemeResolver.snapshot(context).copy(isDark = darkTheme))
    val colorScheme = legadoColorScheme(previewState)
    val extendedColors = LegadoExtendedColors(
        success = previewState.success,
        warning = previewState.warning
    )

    androidx.compose.runtime.CompositionLocalProvider(
        LocalLegadoExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LegadoTypography,
            shapes = LegadoShapes,
            content = content
        )
    }
}

@Stable
object LegadoTheme {
    val extendedColors: LegadoExtendedColors
        @Composable
        get() = LocalLegadoExtendedColors.current
}
