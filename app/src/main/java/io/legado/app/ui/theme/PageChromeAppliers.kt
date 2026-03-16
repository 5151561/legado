package io.legado.app.ui.theme

import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.legado.app.help.config.AppConfig
import io.legado.app.utils.applyBackgroundTint
import io.legado.app.utils.applyTint
import io.legado.app.utils.setEdgeEffectColor
import io.legado.app.utils.setTintMutate

fun View.applyLegadoPageSurfaceStyle(transparent: Boolean = false) {
    if (transparent) {
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        return
    }
    setBackgroundColor(context.legadoComponentTokens().dialog.container)
}

fun SearchView.applyLegadoPageSearchStyle() {
    val tokens = context.legadoComponentTokens()
    applyBackgroundTint(tokens.tabs.container)
    applyTint(tokens.topAppBar.actionIcon)
    findViewById<View?>(androidx.appcompat.R.id.search_mag_icon)?.background = null
    findViewById<View?>(androidx.appcompat.R.id.search_close_btn)?.let {
        it.background = null
    }
    findViewById<TextView?>(androidx.appcompat.R.id.search_src_text)?.apply {
        setTextColor(tokens.topAppBar.title)
        setHintTextColor(tokens.topAppBar.subtitle)
    }
    findViewById<View?>(androidx.appcompat.R.id.search_button)?.background = null
    findViewById<View?>(androidx.appcompat.R.id.search_go_btn)?.background = null
    findViewById<View?>(androidx.appcompat.R.id.search_voice_btn)?.background = null
    findViewById<View?>(androidx.appcompat.R.id.search_close_btn)?.background = null
    findViewById<View?>(androidx.appcompat.R.id.search_plate)?.background?.setTintMutate(tokens.tabs.container)
}

fun RecyclerView.applyLegadoPageListStyle() {
    setBackgroundColor(android.graphics.Color.TRANSPARENT)
    setEdgeEffectColor(context.legadoComponentTokens().shared.accent)
}

fun SwipeRefreshLayout.applyLegadoRefreshStyle() {
    val tokens = context.legadoComponentTokens()
    setBackgroundColor(android.graphics.Color.TRANSPARENT)
    setColorSchemeColors(tokens.shared.accent)
    setProgressBackgroundColorSchemeColor(tokens.tabs.container)
}

fun TextView.applyLegadoPageEmptyStateStyle() {
    val tokens = context.legadoComponentTokens()
    setTextColor(tokens.dialog.supportingText)
    if (!AppConfig.isEInkMode) {
        alpha = 0.92f
    }
}
