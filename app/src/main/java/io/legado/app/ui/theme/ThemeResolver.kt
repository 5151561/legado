package io.legado.app.ui.theme

import android.content.Context
import android.os.Build
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.core.content.res.ResourcesCompat
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getPrefString
import splitties.init.appCtx
import kotlin.math.roundToInt

internal data class ThemeInputSnapshot(
    val isDark: Boolean,
    val isEInk: Boolean,
    val useDynamicColor: Boolean,
    val isTransparentStatusBar: Boolean,
    val immNavigationBar: Boolean,
    val isPureBlack: Boolean,
    val primaryColor: Int,
    val accentColor: Int,
    val backgroundColor: Int,
    val bottomBackgroundColor: Int,
    val fallbackBackgroundColor: Int,
    val fallbackBottomBackgroundColor: Int,
    val backgroundImagePath: String?
) {
    val hasBackgroundImage: Boolean
        get() = !backgroundImagePath.isNullOrBlank()
}

internal data class ThemeDraft(
    val primary: Int,
    val primaryContainer: Int,
    val secondary: Int,
    val secondaryContainer: Int,
    val tertiary: Int,
    val tertiaryContainer: Int,
    val surface: Int,
    val surfaceContainer: Int,
    val background: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    val outline: Int,
    val accentCompat: Int,
    val toolbar: Int,
    val statusBar: Int,
    val navigationBar: Int,
    val textPrimaryCompat: Int,
    val textSecondaryCompat: Int,
    val success: Int,
    val warning: Int
)

internal fun interface DynamicThemeProvider {
    fun transform(input: ThemeInputSnapshot, draft: ThemeDraft): ThemeDraft?
}

object ThemeResolver {

    private const val DefaultDarkenFactor = 0.9f

    private const val TRANSPARENT = 0x00000000

    internal var dynamicThemeProvider: DynamicThemeProvider? = MonetDynamicThemeProvider

    fun resolve(context: Context = appCtx): LegadoThemeState {
        return resolve(snapshot(context))
    }

    internal fun resolvePreview(
        context: Context = appCtx,
        darkTheme: Boolean
    ): LegadoThemeState {
        return resolve(snapshot(context).copy(isDark = darkTheme))
    }

    private fun snapshot(context: Context = appCtx): ThemeInputSnapshot {
        val isDark = AppConfig.isNightTheme
        val fallbackBackground = if (isDark) {
            context.getCompatColor(R.color.md_grey_900)
        } else {
            context.getCompatColor(R.color.md_grey_100)
        }
        val fallbackBottomBackground = if (isDark) {
            context.getCompatColor(R.color.md_grey_850)
        } else {
            context.getCompatColor(R.color.md_grey_200)
        }
        return ThemeInputSnapshot(
            isDark = isDark,
            isEInk = AppConfig.isEInkMode,
            useDynamicColor = AppConfig.dynamicColor,
            isTransparentStatusBar = AppConfig.isTransparentStatusBar,
            immNavigationBar = AppConfig.immNavigationBar,
            isPureBlack = context.getPrefBoolean(PreferKey.pureBlack, false),
            primaryColor = if (isDark) {
                context.getPrefInt(
                    PreferKey.cNPrimary,
                    context.getCompatColor(R.color.md_blue_grey_600)
                )
            } else {
                context.getPrefInt(
                    PreferKey.cPrimary,
                    context.getCompatColor(R.color.md_brown_500)
                )
            },
            accentColor = if (isDark) {
                context.getPrefInt(
                    PreferKey.cNAccent,
                    context.getCompatColor(R.color.md_deep_orange_800)
                )
            } else {
                context.getPrefInt(
                    PreferKey.cAccent,
                    context.getCompatColor(R.color.md_red_600)
                )
            },
            backgroundColor = if (isDark) {
                context.getPrefInt(PreferKey.cNBackground, fallbackBackground)
            } else {
                context.getPrefInt(PreferKey.cBackground, fallbackBackground)
            },
            bottomBackgroundColor = if (isDark) {
                context.getPrefInt(PreferKey.cNBBackground, fallbackBottomBackground)
            } else {
                context.getPrefInt(PreferKey.cBBackground, fallbackBottomBackground)
            },
            fallbackBackgroundColor = fallbackBackground,
            fallbackBottomBackgroundColor = fallbackBottomBackground,
            backgroundImagePath = if (isDark) {
                context.getPrefString(PreferKey.bgImageN)
            } else {
                context.getPrefString(PreferKey.bgImage)
            }
        )
    }

    internal fun resolve(input: ThemeInputSnapshot): LegadoThemeState {
        val baseDraft = buildDraft(input)
        val draft = dynamicThemeProvider?.transform(input, baseDraft) ?: baseDraft
        val shouldUseTransparentSystemBars = draft.statusBar == TRANSPARENT
        return LegadoThemeState(
            isDark = input.isDark,
            isEInk = input.isEInk,
            isTransparent = shouldUseTransparentSystemBars,
            hasBackgroundImage = input.hasBackgroundImage,
            primary = Color(draft.primary),
            primaryContainer = Color(draft.primaryContainer),
            secondary = Color(draft.secondary),
            secondaryContainer = Color(draft.secondaryContainer),
            tertiary = Color(draft.tertiary),
            tertiaryContainer = Color(draft.tertiaryContainer),
            surface = Color(draft.surface),
            surfaceContainer = Color(draft.surfaceContainer),
            background = Color(draft.background),
            onSurface = Color(draft.onSurface),
            onSurfaceVariant = Color(draft.onSurfaceVariant),
            outline = Color(draft.outline),
            accentCompat = Color(draft.accentCompat),
            toolbar = Color(draft.toolbar),
            statusBar = Color(draft.statusBar),
            navigationBar = Color(draft.navigationBar),
            textPrimaryCompat = Color(draft.textPrimaryCompat),
            textSecondaryCompat = Color(draft.textSecondaryCompat),
            success = Color(draft.success),
            warning = Color(draft.warning)
        )
    }

    private fun buildDraft(input: ThemeInputSnapshot): ThemeDraft {
        if (input.isEInk) {
            return ThemeDraft(
                primary = 0xFFFFFFFF.toInt(),
                primaryContainer = 0xFFF2F2F2.toInt(),
                secondary = 0xFF000000.toInt(),
                secondaryContainer = 0xFFE0E0E0.toInt(),
                tertiary = 0xFF4A4A4A.toInt(),
                tertiaryContainer = 0xFFE5E5E5.toInt(),
                surface = 0xFFFFFFFF.toInt(),
                surfaceContainer = 0xFFFFFFFF.toInt(),
                background = 0xFFFFFFFF.toInt(),
                onSurface = 0xFF000000.toInt(),
                onSurfaceVariant = 0xFF4A4A4A.toInt(),
                outline = 0xFF7A7A7A.toInt(),
                accentCompat = 0xFF000000.toInt(),
                toolbar = 0xFFFFFFFF.toInt(),
                statusBar = 0xFFFFFFFF.toInt(),
                navigationBar = 0xFFFFFFFF.toInt(),
                textPrimaryCompat = 0xFF000000.toInt(),
                textSecondaryCompat = 0xFF4A4A4A.toInt(),
                success = 0xFF000000.toInt(),
                warning = 0xFF4A4A4A.toInt()
            )
        }

        val primary = opaque(input.primaryColor)
        val accent = opaque(input.accentColor)
        val background = sanitizeBackground(
            rawColor = opaque(input.backgroundColor),
            fallbackColor = opaque(input.fallbackBackgroundColor),
            shouldBeLight = !input.isDark
        )
        val bottomBackground = sanitizeBackground(
            rawColor = opaque(input.bottomBackgroundColor),
            fallbackColor = opaque(input.fallbackBottomBackgroundColor),
            shouldBeLight = !input.isDark
        )

        val onSurface = if (isLight(background)) {
            0xFF000000.toInt()
        } else {
            0xFFFFFFFF.toInt()
        }
        val onSurfaceVariant = blend(onSurface, background, 0.45f)
        val outline = blend(onSurface, background, 0.7f)

        var surface = background
        var surfaceContainer = bottomBackground
        var resolvedBackground = background

        if (input.isDark && input.isPureBlack) {
            resolvedBackground = 0xFF000000.toInt()
            surface = 0xFF000000.toInt()
            surfaceContainer = 0xFF121212.toInt()
        }

        val primaryContainer = blend(primary, resolvedBackground, if (input.isDark) 0.55f else 0.82f)
        val secondary = accent
        val secondaryContainer = blend(secondary, resolvedBackground, if (input.isDark) 0.55f else 0.86f)
        val tertiary = blend(primary, secondary, if (input.isDark) 0.35f else 0.5f)
        val tertiaryContainer = blend(tertiary, resolvedBackground, if (input.isDark) 0.55f else 0.86f)

        if (input.hasBackgroundImage) {
            resolvedBackground = TRANSPARENT
            surface = TRANSPARENT
            surfaceContainer = TRANSPARENT
        }

        val toolbar = primary
        val statusBar = if (input.isTransparentStatusBar || input.hasBackgroundImage) {
            TRANSPARENT
        } else if (input.isDark) {
            darken(primary)
        } else {
            primary
        }
        val navigationBar = when {
            input.hasBackgroundImage && input.immNavigationBar -> TRANSPARENT
            input.immNavigationBar -> bottomBackground
            else -> darken(bottomBackground)
        }

        return ThemeDraft(
            primary = primary,
            primaryContainer = primaryContainer,
            secondary = secondary,
            secondaryContainer = secondaryContainer,
            tertiary = tertiary,
            tertiaryContainer = tertiaryContainer,
            surface = surface,
            surfaceContainer = surfaceContainer,
            background = resolvedBackground,
            onSurface = onSurface,
            onSurfaceVariant = onSurfaceVariant,
            outline = outline,
            accentCompat = accent,
            toolbar = toolbar,
            statusBar = statusBar,
            navigationBar = navigationBar,
            textPrimaryCompat = onSurface,
            textSecondaryCompat = onSurfaceVariant,
            success = if (input.isDark) 0xFF84D4A2.toInt() else 0xFF4D8E64.toInt(),
            warning = if (input.isDark) 0xFFFFB95C.toInt() else 0xFFAA6F25.toInt()
        )
    }

    @ColorInt
    private fun sanitizeBackground(
        @ColorInt rawColor: Int,
        @ColorInt fallbackColor: Int,
        shouldBeLight: Boolean
    ): Int {
        return if (shouldBeLight == isLight(rawColor)) rawColor else fallbackColor
    }

    @ColorInt
    private fun darken(@ColorInt color: Int, factor: Float = DefaultDarkenFactor): Int {
        val alpha = alpha(color)
        val r = (red(color) * factor).roundToInt().coerceIn(0, 255)
        val g = (green(color) * factor).roundToInt().coerceIn(0, 255)
        val b = (blue(color) * factor).roundToInt().coerceIn(0, 255)
        return argb(alpha, r, g, b)
    }

    private fun isLight(@ColorInt color: Int): Boolean {
        fun channel(v: Int): Double {
            val normalized = v / 255.0
            return if (normalized <= 0.03928) {
                normalized / 12.92
            } else {
                Math.pow((normalized + 0.055) / 1.055, 2.4)
            }
        }
        val luminance =
            0.2126 * channel(red(color)) +
                0.7152 * channel(green(color)) +
                0.0722 * channel(blue(color))
        return luminance >= 0.5
    }

    @ColorInt
    private fun blend(@ColorInt from: Int, @ColorInt to: Int, ratio: Float): Int {
        val clamped = ratio.coerceIn(0f, 1f)
        val inverse = 1f - clamped
        return argb(
            (alpha(from) * inverse + alpha(to) * clamped).roundToInt(),
            (red(from) * inverse + red(to) * clamped).roundToInt(),
            (green(from) * inverse + green(to) * clamped).roundToInt(),
            (blue(from) * inverse + blue(to) * clamped).roundToInt()
        )
    }

    @ColorInt
    private fun opaque(@ColorInt color: Int): Int {
        return argb(255, red(color), green(color), blue(color))
    }

    @ColorInt
    private fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int {
        return ((alpha and 0xFF) shl 24) or
            ((red and 0xFF) shl 16) or
            ((green and 0xFF) shl 8) or
            (blue and 0xFF)
    }

    private fun alpha(@ColorInt color: Int): Int = color ushr 24 and 0xFF

    private fun red(@ColorInt color: Int): Int = color ushr 16 and 0xFF

    private fun green(@ColorInt color: Int): Int = color ushr 8 and 0xFF

    private fun blue(@ColorInt color: Int): Int = color and 0xFF

    private object MonetDynamicThemeProvider : DynamicThemeProvider {
        override fun transform(input: ThemeInputSnapshot, draft: ThemeDraft): ThemeDraft? {
            if (!input.useDynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                return null
            }
            val palette = DynamicPalette.fromSystem(input.isDark)
            return draft.copy(
                primary = palette.primary,
                primaryContainer = palette.primaryContainer,
                secondary = palette.secondary,
                secondaryContainer = palette.secondaryContainer,
                tertiary = palette.tertiary,
                tertiaryContainer = palette.tertiaryContainer,
                onSurface = palette.onSurface,
                onSurfaceVariant = palette.onSurfaceVariant,
                outline = palette.outline,
                accentCompat = palette.secondary,
                toolbar = palette.primary,
                textPrimaryCompat = palette.onSurface,
                textSecondaryCompat = palette.onSurfaceVariant
            )
        }
    }

    private data class DynamicPalette(
        @param:ColorInt val primary: Int,
        @param:ColorInt val primaryContainer: Int,
        @param:ColorInt val secondary: Int,
        @param:ColorInt val secondaryContainer: Int,
        @param:ColorInt val tertiary: Int,
        @param:ColorInt val tertiaryContainer: Int,
        @param:ColorInt val onSurface: Int,
        @param:ColorInt val onSurfaceVariant: Int,
        @param:ColorInt val outline: Int
    ) {
        companion object {
            @RequiresApi(Build.VERSION_CODES.S)
            fun fromSystem(darkTheme: Boolean): DynamicPalette {
                return DynamicPalette(
                    primary = systemColor(if (darkTheme) android.R.color.system_accent1_200 else android.R.color.system_accent1_600),
                    primaryContainer = systemColor(if (darkTheme) android.R.color.system_accent1_700 else android.R.color.system_accent1_100),
                    secondary = systemColor(if (darkTheme) android.R.color.system_accent2_200 else android.R.color.system_accent2_600),
                    secondaryContainer = systemColor(if (darkTheme) android.R.color.system_accent2_700 else android.R.color.system_accent2_100),
                    tertiary = systemColor(if (darkTheme) android.R.color.system_accent3_200 else android.R.color.system_accent3_600),
                    tertiaryContainer = systemColor(if (darkTheme) android.R.color.system_accent3_700 else android.R.color.system_accent3_100),
                    onSurface = systemColor(if (darkTheme) android.R.color.system_neutral1_100 else android.R.color.system_neutral1_900),
                    onSurfaceVariant = systemColor(if (darkTheme) android.R.color.system_neutral2_300 else android.R.color.system_neutral2_700),
                    outline = systemColor(android.R.color.system_neutral2_500)
                )
            }

            @RequiresApi(Build.VERSION_CODES.S)
            @ColorInt
            private fun systemColor(id: Int): Int {
                return ResourcesCompat.getColor(appCtx.resources, id, appCtx.theme)
            }
        }
    }

}
