package io.legado.app.ui.theme

import android.content.res.ColorStateList
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.core.view.forEach
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import io.legado.app.help.config.AppConfig
import io.legado.app.ui.widget.TitleBar
import io.legado.app.utils.applyTint
import io.legado.app.utils.setTintMutate

fun Toolbar.applyLegadoTopAppBarStyle() {
    val tokens = context.legadoComponentTokens().topAppBar
    setBackgroundColor(tokens.container)
    setTitleTextColor(tokens.title)
    setSubtitleTextColor(tokens.subtitle)
    navigationIcon?.setTintMutate(tokens.navigationIcon)
    overflowIcon?.setTintMutate(tokens.actionIcon)
    menu.forEach { item ->
        item.icon?.setTintMutate(tokens.actionIcon)
    }
    if (!AppConfig.isEInkMode) {
        elevation = tokens.elevation
    }
}

fun TitleBar.applyLegadoTopAppBarStyle() {
    if (!AppConfig.isEInkMode) {
        setBackgroundColor(context.legadoComponentTokens().topAppBar.container)
    }
    toolbar.applyLegadoTopAppBarStyle()
}

fun TextInputLayout.applyLegadoInputStyle() {
    val tokens = context.legadoComponentTokens().input
    val boxStrokeColors = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_focused),
            intArrayOf()
        ),
        intArrayOf(tokens.disabled, tokens.focus, tokens.outline)
    )
    val hintColors = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_focused),
            intArrayOf()
        ),
        intArrayOf(tokens.disabled, tokens.focus, tokens.label)
    )
    setBoxStrokeColorStateList(boxStrokeColors)
    defaultHintTextColor = hintColors
    hintTextColor = hintColors
    editText?.applyTint(tokens.cursor)
}

fun TabLayout.applyLegadoTabsStyle() {
    val tokens = context.legadoComponentTokens().tabs
    setBackgroundColor(tokens.container)
    setSelectedTabIndicatorColor(tokens.indicator)
    setTabTextColors(tokens.unselectedText, tokens.selectedText)
    tabIconTint = ColorStateList(
        arrayOf(
            intArrayOf(android.R.attr.state_selected),
            intArrayOf()
        ),
        intArrayOf(tokens.selectedText, tokens.unselectedText)
    )
    tabRippleColor = ColorStateList.valueOf(tokens.indicator)
    elevation = context.legadoComponentTokens().topAppBar.elevation
}

fun FloatingActionButton.applyLegadoFabStyle() {
    val tokens = context.legadoComponentTokens().fab
    backgroundTintList = ColorStateList(
        arrayOf(
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf()
        ),
        intArrayOf(tokens.disabled, tokens.container)
    )
    imageTintList = ColorStateList.valueOf(tokens.content)
    rippleColor = tokens.pressed
    compatElevation = tokens.elevation
}

fun Snackbar.applyLegadoSnackbarStyle(): Snackbar {
    val tokens = context.legadoComponentTokens().snackbar
    setBackgroundTint(tokens.container)
    setTextColor(tokens.content)
    setActionTextColor(tokens.action)
    view.elevation = tokens.elevation
    return this
}

object DialogChromeApplier {

    fun applyRoot(view: View) {
        if (!AppConfig.isEInkMode) {
            view.setBackgroundColor(view.context.legadoComponentTokens().dialog.container)
        }
    }

    fun applyToolbar(toolbar: Toolbar) {
        toolbar.applyLegadoTopAppBarStyle()
    }
}
