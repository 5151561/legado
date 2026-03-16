package io.legado.app.ui.book.read

import android.content.Context
import android.util.AttributeSet
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
import android.widget.FrameLayout
import androidx.core.graphics.toColorInt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import io.legado.app.R
import io.legado.app.constant.PreferKey
import io.legado.app.help.AppWebDav
import io.legado.app.help.book.isEpub
import io.legado.app.help.book.isLocal
import io.legado.app.help.book.isLocalTxt
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.LocalConfig
import io.legado.app.help.config.ReadBookConfig
import io.legado.app.help.config.ThemeConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.source.getSourceType
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.theme.bottomBackground
import io.legado.app.lib.theme.getPrimaryTextColor
import io.legado.app.model.ReadBook
import io.legado.app.ui.browser.WebViewActivity
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.activity
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.openUrl
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.startActivity

class ReadMenu @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var canShowMenu: Boolean = false
    private val callBack: CallBack get() = activity as CallBack
    private var uiState by mutableStateOf(ReadMenuUiState())
    private var isMenuOutAnimating = false
    private var onMenuOutEnd: (() -> Unit)? = null
    private var confirmSkipToChapter: Boolean = false
    private var autoPageEnabled: Boolean = false
    private var overflowVisible by mutableStateOf(false)

    private val composeView = ComposeView(context).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            io.legado.app.ui.compose.theme.LegadoComposeTheme {
                ReadMenuMaterialContent(
                    state = uiState,
                    sourceActions = sourceActions(),
                    overflowActions = overflowActions(),
                    overflowVisible = overflowVisible,
                    onDismiss = { runMenuOut() },
                    onOverflowClick = { overflowVisible = true },
                    onOverflowDismiss = { overflowVisible = false },
                    onTitleClick = { callBack.openBookInfoActivity() },
                    onChapterNameClick = ::openChapterLink,
                    onChapterNameLongClick = ::showChapterLinkModeDialog,
                    onSourceActionClick = {},
                    onBrightnessAutoClick = {
                        context.putPrefBoolean("brightnessAuto", !brightnessAuto())
                        upBrightnessState()
                    },
                    onBrightnessChange = { setScreenBrightness(it) },
                    onBrightnessChangeFinished = {
                        AppConfig.readBrightness = it.toInt()
                        syncUiState()
                    },
                    onBrightnessPositionToggle = {
                        AppConfig.brightnessVwPos = !AppConfig.brightnessVwPos
                        syncUiState()
                    },
                    onSeekChange = {},
                    onSeekChangeFinished = ::handleSeekFinished,
                    onPrevClick = { ReadBook.moveToPrevChapter(upContent = true, toLast = false) },
                    onNextClick = { ReadBook.moveToNextChapter(true) },
                    onSearchClick = {
                        runMenuOut {
                            callBack.openSearchActivity(null)
                        }
                    },
                    onAutoPageClick = {
                        runMenuOut {
                            callBack.autoPage()
                        }
                    },
                    onReplaceClick = { callBack.openReplaceRule() },
                    onNightModeClick = {
                        AppConfig.isNightTheme = !AppConfig.isNightTheme
                        ThemeConfig.applyDayNight(context)
                        syncUiState()
                    },
                    onCatalogClick = {
                        runMenuOut {
                            callBack.openChapterList()
                        }
                    },
                    onReadAloudClick = {
                        runMenuOut {
                            callBack.onClickReadAloud()
                        }
                    },
                    onReadAloudLongClick = {
                        runMenuOut {
                            callBack.showReadAloudDialog()
                        }
                    },
                    onLayoutClick = {
                        runMenuOut {
                            callBack.showReadStyle()
                        }
                    },
                    onSettingClick = {
                        runMenuOut {
                            callBack.showMoreSetting()
                        }
                    }
                )
            }
        }
    }

    init {
        addView(
            composeView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )
        visibility = GONE
        upBrightnessState()
        syncUiState()
    }

    private val immersiveMenu: Boolean
        get() = AppConfig.readBarStyleFollowPage && ReadBookConfig.durConfig.curBgType() == 0

    private fun containerColorInt(): Int {
        return if (immersiveMenu) {
            runCatching {
                ReadBookConfig.durConfig.curBgStr().toColorInt()
            }.getOrDefault(context.bottomBackground)
        } else {
            context.bottomBackground
        }
    }

    private fun contentColorInt(containerColor: Int): Int {
        return if (immersiveMenu) {
            ReadBookConfig.durConfig.curTextColor()
        } else {
            context.getPrimaryTextColor(ColorUtils.isColorLight(containerColor))
        }
    }

    private fun brightnessAuto(): Boolean {
        return context.getPrefBoolean("brightnessAuto", true) || !showBrightnessView()
    }

    private fun showBrightnessView(): Boolean {
        return context.getPrefBoolean(PreferKey.showBrightnessView, true)
    }

    fun reset() {
        syncUiState()
    }

    fun refreshMenuColorFilter() {
        syncUiState()
    }

    fun upBrightnessState() {
        setScreenBrightness(AppConfig.readBrightness.toFloat())
        syncUiState()
    }

    fun setScreenBrightness(value: Float) {
        activity?.run {
            var brightness = BRIGHTNESS_OVERRIDE_NONE
            if (!brightnessAuto() && value != BRIGHTNESS_OVERRIDE_NONE) {
                brightness = value
                if (brightness < 1f) brightness = 1f
                brightness /= 255f
            }
            val params = window.attributes
            params.screenBrightness = brightness
            window.attributes = params
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun runMenuIn(anim: Boolean = !AppConfig.isEInkMode) {
        visibility = VISIBLE
        callBack.onMenuShow()
        canShowMenu = false
        overflowVisible = false
        syncUiState(visible = true)
        callBack.upSystemUiVisibility()
        if (!LocalConfig.readMenuHelpVersionIsLast) {
            callBack.showHelp()
        }
    }

    fun runMenuOut(anim: Boolean = !AppConfig.isEInkMode, onMenuOutEnd: (() -> Unit)? = null) {
        if (isMenuOutAnimating) return
        isMenuOutAnimating = true
        callBack.onMenuHide()
        this.onMenuOutEnd = onMenuOutEnd
        overflowVisible = false
        syncUiState(visible = false)
        postDelayed({
            if (!uiState.visible) {
                visibility = INVISIBLE
                canShowMenu = false
                isMenuOutAnimating = false
                onMenuOutEnd?.invoke()
                callBack.upSystemUiVisibility()
            }
        }, if (anim) 220L else 0L)
    }

    fun upBookView() {
        syncUiState()
    }

    fun upSeekBar() {
        syncUiState()
    }

    fun setSeekPage(seek: Int) {
        uiState = uiState.copy(seekProgress = seek)
    }

    fun setAutoPage(autoPage: Boolean) {
        autoPageEnabled = autoPage
        syncUiState()
    }

    private fun syncUiState(visible: Boolean = uiState.visible) {
        val container = containerColorInt()
        val content = contentColorInt(container)
        val currentChapter = ReadBook.curTextChapter
        val chapterUrl = if (!ReadBook.isLocalBook) {
            currentChapter?.chapter?.getAbsoluteURL()
        } else {
            null
        }
        uiState = ReadMenuUiState(
            visible = visible,
            containerColor = Color(container),
            contentColor = Color(content),
            title = ReadBook.book?.name.orEmpty(),
            chapterName = currentChapter?.title,
            chapterUrl = chapterUrl,
            sourceName = if (ReadBook.isLocalBook) null else ReadBook.bookSource?.bookSourceName,
            showTitleAddition = AppConfig.showReadTitleBarAddition,
            showBrightnessControl = showBrightnessView(),
            brightnessAuto = brightnessAuto(),
            brightnessProgress = AppConfig.readBrightness,
            brightnessOnRight = AppConfig.brightnessVwPos,
            seekProgress = currentSeekProgress(),
            seekMax = currentSeekMax(),
            preEnabled = ReadBook.durChapterIndex != 0,
            nextEnabled = ReadBook.durChapterIndex != ReadBook.simulatedChapterSize - 1,
            autoPageEnabled = autoPageEnabled
        )
    }

    private fun currentSeekProgress(): Int {
        return when (AppConfig.progressBarBehavior) {
            "page" -> ReadBook.durPageIndex
            else -> ReadBook.durChapterIndex
        }
    }

    private fun currentSeekMax(): Int {
        return when (AppConfig.progressBarBehavior) {
            "page" -> ReadBook.curTextChapter?.pageSize?.minus(1)?.coerceAtLeast(0) ?: 0
            else -> (ReadBook.simulatedChapterSize - 1).coerceAtLeast(0)
        }
    }

    private fun handleSeekFinished(value: Float) {
        val seek = value.toInt()
        when (AppConfig.progressBarBehavior) {
            "page" -> ReadBook.skipToPage(seek)
            "chapter" -> {
                if (confirmSkipToChapter) {
                    callBack.skipToChapter(seek)
                } else {
                    context.alert("章节跳转确认", "确定要跳转章节吗？") {
                        yesButton {
                            confirmSkipToChapter = true
                            callBack.skipToChapter(seek)
                        }
                        noButton {
                            upSeekBar()
                        }
                        onCancelled {
                            upSeekBar()
                        }
                    }
                }
            }
        }
    }

    private fun openChapterLink() {
        if (ReadBook.isLocalBook) return
        val url = uiState.chapterUrl ?: return
        if (AppConfig.readUrlInBrowser) {
            context.openUrl(url.substringBefore(",{"))
        } else {
            Coroutine.async {
                context.startActivity<WebViewActivity> {
                    val bookSource = ReadBook.bookSource
                    putExtra("title", uiState.chapterName)
                    putExtra("url", url)
                    putExtra("sourceOrigin", bookSource?.bookSourceUrl)
                    putExtra("sourceName", bookSource?.bookSourceName)
                    putExtra("sourceType", bookSource?.getSourceType())
                }
            }
        }
    }

    private fun showChapterLinkModeDialog() {
        if (ReadBook.isLocalBook) return
        context.alert(R.string.open_fun) {
            setMessage(R.string.use_browser_open)
            okButton {
                AppConfig.readUrlInBrowser = true
            }
            noButton {
                AppConfig.readUrlInBrowser = false
            }
        }
    }

    private fun sourceActions(): List<ReadMenuSourceAction> {
        if (ReadBook.isLocalBook) return emptyList()
        val actions = mutableListOf<ReadMenuSourceAction>()
        if (!ReadBook.bookSource?.loginUrl.isNullOrEmpty()) {
            actions += ReadMenuSourceAction(context.getString(R.string.login)) {
                callBack.showLogin()
            }
            if (ReadBook.curTextChapter?.isVip == true && ReadBook.curTextChapter?.isPay != true) {
                actions += ReadMenuSourceAction(context.getString(R.string.chapter_pay)) {
                    callBack.payAction()
                }
            }
        }
        actions += ReadMenuSourceAction(context.getString(R.string.edit_book_source)) {
            callBack.openSourceEditActivity()
        }
        actions += ReadMenuSourceAction(context.getString(R.string.disable_book_source)) {
            callBack.disableSource()
        }
        return actions
    }

    private fun overflowActions(): List<ReadMenuOverflowAction> {
        val book = ReadBook.book ?: return emptyList()
        val online = !book.isLocal
        val actions = mutableListOf<ReadMenuOverflowAction>()
        if (online) {
            actions += ReadMenuOverflowAction(
                label = context.getString(R.string.change_origin),
                supportingText = context.getString(R.string.book_change_source)
            ) { callBack.dispatchReadMenuAction(R.id.menu_book_change_source) }
            actions += ReadMenuOverflowAction(
                label = context.getString(R.string.chapter_change_source)
            ) { callBack.dispatchReadMenuAction(R.id.menu_chapter_change_source) }
            actions += ReadMenuOverflowAction(
                label = context.getString(R.string.refresh),
                supportingText = context.getString(R.string.menu_refresh_dur)
            ) { callBack.dispatchReadMenuAction(R.id.menu_refresh_dur) }
            actions += ReadMenuOverflowAction(
                label = context.getString(R.string.menu_refresh_after)
            ) { callBack.dispatchReadMenuAction(R.id.menu_refresh_after) }
            actions += ReadMenuOverflowAction(
                label = context.getString(R.string.menu_refresh_all)
            ) { callBack.dispatchReadMenuAction(R.id.menu_refresh_all) }
            actions += ReadMenuOverflowAction(
                label = context.getString(R.string.offline_cache)
            ) { callBack.dispatchReadMenuAction(R.id.menu_download) }
            actions += ReadMenuOverflowAction(
                label = context.getString(R.string.get_book_progress),
                enabled = ReadBook.inBookshelf && AppWebDav.isOk
            ) { callBack.dispatchReadMenuAction(R.id.menu_get_progress) }
            actions += ReadMenuOverflowAction(
                label = context.getString(R.string.cover_book_progress),
                enabled = ReadBook.inBookshelf && AppWebDav.isOk
            ) { callBack.dispatchReadMenuAction(R.id.menu_cover_progress) }
            actions += ReadMenuOverflowAction(
                label = context.getString(R.string.reverse_content)
            ) { callBack.dispatchReadMenuAction(R.id.menu_reverse_content) }
        }
        if (book.isLocalTxt) {
            actions += ReadMenuOverflowAction(
                label = context.getString(R.string.txt_toc_rule)
            ) { callBack.dispatchReadMenuAction(R.id.menu_toc_regex) }
        }
        if (book.isLocal) {
            actions += ReadMenuOverflowAction(
                label = context.getString(R.string.set_charset)
            ) { callBack.dispatchReadMenuAction(R.id.menu_set_charset) }
        }
        actions += ReadMenuOverflowAction(
            label = context.getString(R.string.bookmark_add)
        ) { callBack.dispatchReadMenuAction(R.id.menu_add_bookmark) }
        actions += ReadMenuOverflowAction(
            label = context.getString(R.string.edit_content)
        ) { callBack.dispatchReadMenuAction(R.id.menu_edit_content) }
        actions += ReadMenuOverflowAction(
            label = context.getString(R.string.book_page_anim)
        ) { callBack.dispatchReadMenuAction(R.id.menu_page_anim) }
        actions += ReadMenuOverflowAction(
            label = context.getString(R.string.simulated_reading)
        ) { callBack.dispatchReadMenuAction(R.id.menu_simulated_reading) }
        actions += ReadMenuOverflowAction(
            label = context.getString(R.string.replace_rule_title),
            supportingText = if (book.getUseReplaceRule()) "已启用" else "已关闭"
        ) { callBack.dispatchReadMenuAction(R.id.menu_enable_replace) }
        actions += ReadMenuOverflowAction(
            label = context.getString(R.string.same_title_removed),
            supportingText = if (ReadBook.curTextChapter?.sameTitleRemoved == true) "已启用" else "可切换"
        ) { callBack.dispatchReadMenuAction(R.id.menu_same_title_removed) }
        actions += ReadMenuOverflowAction(
            label = context.getString(R.string.re_segment),
            supportingText = if (book.getReSegment()) "已启用" else "已关闭"
        ) { callBack.dispatchReadMenuAction(R.id.menu_re_segment) }
        if (book.isEpub) {
            actions += ReadMenuOverflowAction(
                label = context.getString(R.string.del_ruby_tag),
                supportingText = if (book.getDelTag(io.legado.app.data.entities.Book.rubyTag)) "已启用" else "已关闭"
            ) { callBack.dispatchReadMenuAction(R.id.menu_del_ruby_tag) }
            actions += ReadMenuOverflowAction(
                label = context.getString(R.string.del_h_tag),
                supportingText = if (book.getDelTag(io.legado.app.data.entities.Book.hTag)) "已启用" else "已关闭"
            ) { callBack.dispatchReadMenuAction(R.id.menu_del_h_tag) }
        }
        actions += ReadMenuOverflowAction(
            label = context.getString(R.string.image_style)
        ) { callBack.dispatchReadMenuAction(R.id.menu_image_style) }
        actions += ReadMenuOverflowAction(
            label = context.getString(R.string.update_toc)
        ) { callBack.dispatchReadMenuAction(R.id.menu_update_toc) }
        actions += ReadMenuOverflowAction(
            label = context.getString(R.string.effective_replaces)
        ) { callBack.dispatchReadMenuAction(R.id.menu_effective_replaces) }
        actions += ReadMenuOverflowAction(
            label = context.getString(R.string.log)
        ) { callBack.dispatchReadMenuAction(R.id.menu_log) }
        actions += ReadMenuOverflowAction(
            label = context.getString(R.string.help)
        ) { callBack.dispatchReadMenuAction(R.id.menu_help) }
        return actions
    }

    interface CallBack {
        fun autoPage()
        fun openReplaceRule()
        fun openChapterList()
        fun openSearchActivity(searchWord: String?)
        fun openSourceEditActivity()
        fun openBookInfoActivity()
        fun showReadStyle()
        fun showMoreSetting()
        fun showReadAloudDialog()
        fun upSystemUiVisibility()
        fun onClickReadAloud()
        fun showHelp()
        fun showLogin()
        fun payAction()
        fun disableSource()
        fun dispatchReadMenuAction(itemId: Int)
        fun skipToChapter(index: Int)
        fun onMenuShow()
        fun onMenuHide()
    }
}
