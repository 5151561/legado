package io.legado.app.ui.widget.text

import android.content.Context
import android.util.AttributeSet
import io.legado.app.ui.theme.applyLegadoInputStyle

class TextInputLayout(
    context: Context,
    attrs: AttributeSet?
) : com.google.android.material.textfield.TextInputLayout(context, attrs) {

    init {
        if (!isInEditMode) {
            applyLegadoInputStyle()
        }
    }

}
