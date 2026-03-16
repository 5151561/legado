package io.legado.app.lib.theme.view

import android.content.Context
import android.util.AttributeSet
import android.widget.ProgressBar
import io.legado.app.ui.theme.legadoComponentTokens
import io.legado.app.utils.applyTint

class ThemeProgressBar(context: Context, attrs: AttributeSet) : ProgressBar(context, attrs) {

    init {
        if (!isInEditMode) {
            applyTint(context.legadoComponentTokens().input.focus)
        }
    }
}
