package io.legado.app.ui.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import io.legado.app.lib.theme.ThemeStore
import io.legado.app.utils.ColorUtils

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

@Composable
fun LegadoComposeTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = !ColorUtils.isColorLight(ThemeStore.backgroundColor(context))
    val colorScheme = legadoColorScheme(darkTheme)
    val extendedColors = LegadoExtendedColors(
        success = successColor(darkTheme),
        warning = warningColor(darkTheme)
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
    val colorScheme = legadoColorScheme(darkTheme)
    val extendedColors = LegadoExtendedColors(
        success = successColor(darkTheme),
        warning = warningColor(darkTheme)
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
