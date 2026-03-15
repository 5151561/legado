package io.legado.app.ui.compose.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

private val MorandiLightPrimary = Color(0xFF39C5BB)
private val MorandiLightOnPrimary = Color(0xFF042A27)
private val MorandiLightPrimaryContainer = Color(0xFFD6F4F0)
private val MorandiLightOnPrimaryContainer = Color(0xFF0F3D39)
private val MorandiLightSecondary = Color(0xFF7E928C)
private val MorandiLightOnSecondary = Color(0xFFFFFFFF)
private val MorandiLightSecondaryContainer = Color(0xFFDEE8E4)
private val MorandiLightOnSecondaryContainer = Color(0xFF23302D)
private val MorandiLightTertiary = Color(0xFFC99284)
private val MorandiLightOnTertiary = Color(0xFFFFFFFF)
private val MorandiLightTertiaryContainer = Color(0xFFF5DED7)
private val MorandiLightOnTertiaryContainer = Color(0xFF4B2D25)
private val MorandiLightBackground = Color(0xFFF3EFE8)
private val MorandiLightOnBackground = Color(0xFF1C1B18)
private val MorandiLightSurface = Color(0xFFFBF7F1)
private val MorandiLightOnSurface = Color(0xFF1C1B18)
private val MorandiLightSurfaceVariant = Color(0xFFE4DDD3)
private val MorandiLightOnSurfaceVariant = Color(0xFF625D57)
private val MorandiLightOutline = Color(0xFF938C84)
private val MorandiLightOutlineVariant = Color(0xFFC8C1B8)
private val MorandiLightError = Color(0xFFBA1A1A)
private val MorandiLightOnError = Color(0xFFFFFFFF)
private val MorandiLightErrorContainer = Color(0xFFFFDAD6)
private val MorandiLightOnErrorContainer = Color(0xFF410002)
private val MorandiLightSuccess = Color(0xFF4E8E6C)
private val MorandiLightWarning = Color(0xFFB9854E)

private val MorandiDarkPrimary = Color(0xFF66D9D0)
private val MorandiDarkOnPrimary = Color(0xFF003733)
private val MorandiDarkPrimaryContainer = Color(0xFF0B4F4A)
private val MorandiDarkOnPrimaryContainer = Color(0xFFD4F4F0)
private val MorandiDarkSecondary = Color(0xFFA8BAB4)
private val MorandiDarkOnSecondary = Color(0xFF11201D)
private val MorandiDarkSecondaryContainer = Color(0xFF31433F)
private val MorandiDarkOnSecondaryContainer = Color(0xFFDCE7E2)
private val MorandiDarkTertiary = Color(0xFFE0B2A5)
private val MorandiDarkOnTertiary = Color(0xFF442A23)
private val MorandiDarkTertiaryContainer = Color(0xFF5F4037)
private val MorandiDarkOnTertiaryContainer = Color(0xFFFFDBD1)
private val MorandiDarkBackground = Color(0xFF171B1A)
private val MorandiDarkOnBackground = Color(0xFFE8E2DA)
private val MorandiDarkSurface = Color(0xFF1E2322)
private val MorandiDarkOnSurface = Color(0xFFE8E2DA)
private val MorandiDarkSurfaceVariant = Color(0xFF45403A)
private val MorandiDarkOnSurfaceVariant = Color(0xFFCBC4BB)
private val MorandiDarkOutline = Color(0xFF948F87)
private val MorandiDarkOutlineVariant = Color(0xFF49443E)
private val MorandiDarkError = Color(0xFFFFB4AB)
private val MorandiDarkOnError = Color(0xFF690005)
private val MorandiDarkErrorContainer = Color(0xFF93000A)
private val MorandiDarkOnErrorContainer = Color(0xFFFFDAD6)
private val MorandiDarkSuccess = Color(0xFF7FC7A0)
private val MorandiDarkWarning = Color(0xFFE0B27D)

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
