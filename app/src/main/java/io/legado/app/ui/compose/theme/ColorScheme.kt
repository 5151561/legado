package io.legado.app.ui.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val MorandiLightPrimary = Color(0xFF6750A4)
private val MorandiLightOnPrimary = Color(0xFFFFFFFF)
private val MorandiLightPrimaryContainer = Color(0xFFEADDFF)
private val MorandiLightOnPrimaryContainer = Color(0xFF21005D)
private val MorandiLightSecondary = Color(0xFF625B71)
private val MorandiLightOnSecondary = Color(0xFFFFFFFF)
private val MorandiLightSecondaryContainer = Color(0xFFE8DEF8)
private val MorandiLightOnSecondaryContainer = Color(0xFF1E192B)
private val MorandiLightTertiary = Color(0xFF7D5260)
private val MorandiLightOnTertiary = Color(0xFFFFFFFF)
private val MorandiLightTertiaryContainer = Color(0xFFFFD8E4)
private val MorandiLightOnTertiaryContainer = Color(0xFF31111D)
private val MorandiLightBackground = Color(0xFFFDF8F6)
private val MorandiLightOnBackground = Color(0xFF1D1B1E)
private val MorandiLightSurface = Color(0xFFFFF8FB)
private val MorandiLightOnSurface = Color(0xFF1D1B1E)
private val MorandiLightSurfaceVariant = Color(0xFFE7E0EC)
private val MorandiLightOnSurfaceVariant = Color(0xFF49454F)
private val MorandiLightOutline = Color(0xFF7A757F)
private val MorandiLightOutlineVariant = Color(0xFFCAC4D0)
private val MorandiLightError = Color(0xFFBA1A1A)
private val MorandiLightOnError = Color(0xFFFFFFFF)
private val MorandiLightErrorContainer = Color(0xFFFFDAD6)
private val MorandiLightOnErrorContainer = Color(0xFF410002)
private val MorandiLightSuccess = Color(0xFF4D8E64)
private val MorandiLightWarning = Color(0xFFAA6F25)

private val MorandiDarkPrimary = Color(0xFFD0BCFF)
private val MorandiDarkOnPrimary = Color(0xFF381E72)
private val MorandiDarkPrimaryContainer = Color(0xFF4F378B)
private val MorandiDarkOnPrimaryContainer = Color(0xFFEADDFF)
private val MorandiDarkSecondary = Color(0xFFCCC2DC)
private val MorandiDarkOnSecondary = Color(0xFF332D41)
private val MorandiDarkSecondaryContainer = Color(0xFF4A4458)
private val MorandiDarkOnSecondaryContainer = Color(0xFFE8DEF8)
private val MorandiDarkTertiary = Color(0xFFEFB8C8)
private val MorandiDarkOnTertiary = Color(0xFF492532)
private val MorandiDarkTertiaryContainer = Color(0xFF633B48)
private val MorandiDarkOnTertiaryContainer = Color(0xFFFFD8E4)
private val MorandiDarkBackground = Color(0xFF141218)
private val MorandiDarkOnBackground = Color(0xFFE6E0E9)
private val MorandiDarkSurface = Color(0xFF141218)
private val MorandiDarkOnSurface = Color(0xFFE6E0E9)
private val MorandiDarkSurfaceVariant = Color(0xFF49454F)
private val MorandiDarkOnSurfaceVariant = Color(0xFFCAC4D0)
private val MorandiDarkOutline = Color(0xFF948F99)
private val MorandiDarkOutlineVariant = Color(0xFF49454F)
private val MorandiDarkError = Color(0xFFFFB4AB)
private val MorandiDarkOnError = Color(0xFF690005)
private val MorandiDarkErrorContainer = Color(0xFF93000A)
private val MorandiDarkOnErrorContainer = Color(0xFFFFDAD6)
private val MorandiDarkSuccess = Color(0xFF84D4A2)
private val MorandiDarkWarning = Color(0xFFFFB95C)

internal fun legadoColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        darkColorScheme(
            primary = MorandiDarkPrimary,
            onPrimary = MorandiDarkOnPrimary,
            primaryContainer = MorandiDarkPrimaryContainer,
            onPrimaryContainer = MorandiDarkOnPrimaryContainer,
            secondary = MorandiDarkSecondary,
            onSecondary = MorandiDarkOnSecondary,
            secondaryContainer = MorandiDarkSecondaryContainer,
            onSecondaryContainer = MorandiDarkOnSecondaryContainer,
            tertiary = MorandiDarkTertiary,
            onTertiary = MorandiDarkOnTertiary,
            tertiaryContainer = MorandiDarkTertiaryContainer,
            onTertiaryContainer = MorandiDarkOnTertiaryContainer,
            background = MorandiDarkBackground,
            onBackground = MorandiDarkOnBackground,
            surface = MorandiDarkSurface,
            onSurface = MorandiDarkOnSurface,
            surfaceVariant = MorandiDarkSurfaceVariant,
            onSurfaceVariant = MorandiDarkOnSurfaceVariant,
            surfaceTint = MorandiDarkPrimary,
            inverseSurface = MorandiLightSurface,
            inverseOnSurface = MorandiLightOnSurface,
            inversePrimary = MorandiLightPrimary,
            outline = MorandiDarkOutline,
            outlineVariant = MorandiDarkOutlineVariant,
            scrim = Color(0x99000000),
            error = MorandiDarkError,
            onError = MorandiDarkOnError,
            errorContainer = MorandiDarkErrorContainer,
            onErrorContainer = MorandiDarkOnErrorContainer
        )
    } else {
        lightColorScheme(
            primary = MorandiLightPrimary,
            onPrimary = MorandiLightOnPrimary,
            primaryContainer = MorandiLightPrimaryContainer,
            onPrimaryContainer = MorandiLightOnPrimaryContainer,
            secondary = MorandiLightSecondary,
            onSecondary = MorandiLightOnSecondary,
            secondaryContainer = MorandiLightSecondaryContainer,
            onSecondaryContainer = MorandiLightOnSecondaryContainer,
            tertiary = MorandiLightTertiary,
            onTertiary = MorandiLightOnTertiary,
            tertiaryContainer = MorandiLightTertiaryContainer,
            onTertiaryContainer = MorandiLightOnTertiaryContainer,
            background = MorandiLightBackground,
            onBackground = MorandiLightOnBackground,
            surface = MorandiLightSurface,
            onSurface = MorandiLightOnSurface,
            surfaceVariant = MorandiLightSurfaceVariant,
            onSurfaceVariant = MorandiLightOnSurfaceVariant,
            surfaceTint = MorandiLightPrimary,
            inverseSurface = MorandiDarkSurface,
            inverseOnSurface = MorandiDarkOnSurface,
            inversePrimary = MorandiDarkPrimary,
            outline = MorandiLightOutline,
            outlineVariant = MorandiLightOutlineVariant,
            scrim = Color(0x66000000),
            error = MorandiLightError,
            onError = MorandiLightOnError,
            errorContainer = MorandiLightErrorContainer,
            onErrorContainer = MorandiLightOnErrorContainer
        )
    }
}

internal fun successColor(darkTheme: Boolean): Color {
    return if (darkTheme) MorandiDarkSuccess else MorandiLightSuccess
}

internal fun warningColor(darkTheme: Boolean): Color {
    return if (darkTheme) MorandiDarkWarning else MorandiLightWarning
}
