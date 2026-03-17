package io.legado.app.base

import androidx.lifecycle.ViewModel
import io.legado.app.constant.Theme

abstract class VMBaseComposeActivity<VM : ViewModel>(
    fullScreen: Boolean = true,
    theme: Theme = Theme.Auto,
    toolBarTheme: Theme = Theme.Auto,
    imageBg: Boolean = true
) : BaseComposeActivity(fullScreen, theme, toolBarTheme, imageBg) {

    protected abstract val viewModel: VM

}
