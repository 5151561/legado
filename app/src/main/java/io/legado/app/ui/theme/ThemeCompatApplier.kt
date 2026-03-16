package io.legado.app.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import io.legado.app.lib.theme.ThemeStore

internal object ThemeCompatApplier {

    fun apply(context: Context, state: LegadoThemeState) {
        val primary = state.primary.toArgb()
        val onSurface = state.onSurface.toArgb()
        val onSurfaceVariant = state.onSurfaceVariant.toArgb()
        ThemeStore.editTheme(context)
            .primaryColor(primary)
            .primaryColorDark(state.statusBar.toArgb())
            .accentColor(state.accentCompat.toArgb())
            .backgroundColor(state.background.toArgb())
            .bottomBackground(state.surfaceContainer.toArgb())
            .statusBarColor(state.statusBar.toArgb())
            .navigationBarColor(state.navigationBar.toArgb())
            .textColorPrimary(state.textPrimaryCompat.toArgb())
            .textColorPrimaryInverse(inverseOf(onSurface))
            .textColorSecondary(state.textSecondaryCompat.toArgb())
            .textColorSecondaryInverse(inverseOf(onSurfaceVariant))
            .apply()
    }

    private fun inverseOf(color: Int): Int {
        return color xor 0x00FFFFFF
    }
}
