package io.legado.app.ui.theme

import android.content.Context
import androidx.annotation.ColorInt
import androidx.compose.ui.graphics.toArgb
import io.legado.app.lib.theme.elevation
import io.legado.app.utils.ColorUtils

data class LegadoComponentTokens(
    val topAppBar: TopAppBarTokens,
    val dialog: DialogTokens,
    val input: InputTokens,
    val tabs: TabTokens,
    val fab: FabTokens,
    val snackbar: SnackbarTokens
)

data class TopAppBarTokens(
    @ColorInt val container: Int,
    @ColorInt val title: Int,
    @ColorInt val subtitle: Int,
    @ColorInt val navigationIcon: Int,
    @ColorInt val actionIcon: Int,
    @ColorInt val divider: Int,
    val elevation: Float
)

data class DialogTokens(
    @ColorInt val container: Int,
    @ColorInt val onContainer: Int,
    @ColorInt val headline: Int,
    @ColorInt val supportingText: Int,
    @ColorInt val action: Int,
    @ColorInt val scrim: Int,
    val elevation: Float
)

data class InputTokens(
    @ColorInt val focus: Int,
    @ColorInt val outline: Int,
    @ColorInt val cursor: Int,
    @ColorInt val label: Int,
    @ColorInt val supportingText: Int,
    @ColorInt val error: Int,
    @ColorInt val disabled: Int
)

data class TabTokens(
    @ColorInt val container: Int,
    @ColorInt val indicator: Int,
    @ColorInt val selectedText: Int,
    @ColorInt val unselectedText: Int,
    @ColorInt val divider: Int
)

data class FabTokens(
    @ColorInt val container: Int,
    @ColorInt val content: Int,
    @ColorInt val pressed: Int,
    @ColorInt val disabled: Int,
    val elevation: Float
)

data class SnackbarTokens(
    @ColorInt val container: Int,
    @ColorInt val content: Int,
    @ColorInt val action: Int,
    @ColorInt val dismiss: Int,
    val elevation: Float
)

fun Context.legadoComponentTokens(): LegadoComponentTokens {
    val state = ThemeResolver.resolve(this)
    val onToolbar = contentColorFor(state.toolbar.toArgb())
    val topAppBar = TopAppBarTokens(
        container = state.toolbar.toArgb(),
        title = onToolbar,
        subtitle = withAlpha(onToolbar, 0.75f),
        navigationIcon = onToolbar,
        actionIcon = onToolbar,
        divider = withAlpha(state.onSurfaceVariant.toArgb(), 0.18f),
        elevation = elevation
    )
    val dialog = DialogTokens(
        container = state.surface.toArgb(),
        onContainer = state.onSurface.toArgb(),
        headline = state.onSurface.toArgb(),
        supportingText = state.onSurfaceVariant.toArgb(),
        action = state.secondary.toArgb(),
        scrim = withAlpha(state.onSurface.toArgb(), 0.32f),
        elevation = elevation
    )
    val input = InputTokens(
        focus = state.secondary.toArgb(),
        outline = state.outline.toArgb(),
        cursor = state.secondary.toArgb(),
        label = state.onSurfaceVariant.toArgb(),
        supportingText = state.onSurfaceVariant.toArgb(),
        error = state.warning.toArgb(),
        disabled = withAlpha(state.onSurfaceVariant.toArgb(), 0.38f)
    )
    val tabs = TabTokens(
        container = state.surfaceContainer.toArgb(),
        indicator = state.secondary.toArgb(),
        selectedText = state.onSurface.toArgb(),
        unselectedText = state.onSurfaceVariant.toArgb(),
        divider = withAlpha(state.outline.toArgb(), 0.3f)
    )
    val fab = FabTokens(
        container = state.secondaryContainer.toArgb(),
        content = contentColorFor(state.secondaryContainer.toArgb()),
        pressed = state.secondary.toArgb(),
        disabled = withAlpha(state.onSurfaceVariant.toArgb(), 0.38f),
        elevation = elevation
    )
    val snackbar = SnackbarTokens(
        container = state.surfaceContainer.toArgb(),
        content = state.onSurface.toArgb(),
        action = state.secondary.toArgb(),
        dismiss = state.onSurfaceVariant.toArgb(),
        elevation = elevation
    )
    return LegadoComponentTokens(
        topAppBar = topAppBar,
        dialog = dialog,
        input = input,
        tabs = tabs,
        fab = fab,
        snackbar = snackbar
    )
}

@ColorInt
private fun contentColorFor(@ColorInt color: Int): Int {
    return if (ColorUtils.isColorLight(color)) {
        0xFF000000.toInt()
    } else {
        0xFFFFFFFF.toInt()
    }
}

@ColorInt
private fun withAlpha(@ColorInt color: Int, alphaFraction: Float): Int {
    return ColorUtils.adjustAlpha(color, alphaFraction)
}
