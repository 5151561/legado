@file:Suppress("DEPRECATION")

package io.legado.app.ui.book.toc

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.Bookmark
import io.legado.app.databinding.ActivityChapterListBinding
import io.legado.app.help.book.ContentProcessor
import io.legado.app.help.book.simulatedTotalChapterNum
import io.legado.app.help.config.AppConfig
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.book.bookmark.BookmarkDialog
import io.legado.app.ui.book.toc.rule.TxtTocRuleDialog
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.widget.dialog.WaitDialog
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TocActivity : VMBaseActivity<ActivityChapterListBinding, TocViewModel>(),
    TxtTocRuleDialog.CallBack {

    override val binding by viewBinding(ActivityChapterListBinding::inflate)
    override val viewModel by viewModels<TocViewModel>()

    private var query by mutableStateOf("")
    private var activeTab by mutableIntStateOf(0)
    private var overflowExpanded by mutableStateOf(false)
    private var chapterItems by mutableStateOf<List<TocChapterUi>>(emptyList())
    private var bookmarkItems by mutableStateOf<List<Bookmark>>(emptyList())
    private var bookmarkJob: Job? = null
    private val waitDialog by lazy { WaitDialog(this) }
    private val exportDir = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            when (it.requestCode) {
                1 -> viewModel.saveBookmark(uri)
                2 -> viewModel.saveBookmarkMd(uri)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.composeTocContent.setContent {
            LegadoComposeTheme {
                TocMaterialScreen(
                    query = query,
                    activeTab = activeTab,
                    chapterItems = chapterItems,
                    bookmarkItems = bookmarkItems,
                    currentProgress = progressLabel(),
                    overflowExpanded = overflowExpanded,
                    useReplace = AppConfig.tocUiUseReplace,
                    loadWordCount = AppConfig.tocCountWords,
                    splitLongChapter = viewModel.bookData.value?.getSplitLongChapter() == true,
                    onBack = ::finish,
                    onQueryChange = {
                        query = it
                        refreshComposeData()
                    },
                    onTabSelected = { index ->
                        activeTab = index
                        refreshComposeData()
                    },
                    onOverflowClick = { overflowExpanded = true },
                    onOverflowDismiss = { overflowExpanded = false },
                    onReverseToc = {
                        overflowExpanded = false
                        viewModel.reverseToc { book ->
                            refreshComposeData()
                            setResult(RESULT_OK, Intent().apply {
                                putExtra("index", book.durChapterIndex)
                                putExtra("chapterPos", 0)
                            })
                        }
                    },
                    onToggleUseReplace = {
                        overflowExpanded = false
                        AppConfig.tocUiUseReplace = !AppConfig.tocUiUseReplace
                        refreshChapters()
                    },
                    onToggleLoadWordCount = {
                        overflowExpanded = false
                        AppConfig.tocCountWords = !AppConfig.tocCountWords
                        refreshChapters()
                    },
                    onToggleSplitLongChapter = {
                        overflowExpanded = false
                        viewModel.bookData.value?.let { book ->
                            book.setSplitLongChapter(!book.getSplitLongChapter())
                            upBookAndToc(book)
                        }
                    },
                    onShowTocRegex = {
                        overflowExpanded = false
                        showDialogFragment(TxtTocRuleDialog(viewModel.bookData.value?.tocUrl))
                    },
                    onExportBookmark = {
                        overflowExpanded = false
                        exportDir.launch { requestCode = 1 }
                    },
                    onExportBookmarkMd = {
                        overflowExpanded = false
                        exportDir.launch { requestCode = 2 }
                    },
                    onShowLog = {
                        overflowExpanded = false
                        showDialogFragment<AppLogDialog>()
                    },
                    onChapterClick = { item ->
                        setResult(
                            RESULT_OK,
                            Intent()
                                .putExtra("index", item.index)
                                .putExtra(
                                    "chapterChanged",
                                    item.index != viewModel.bookData.value?.durChapterIndex
                                )
                        )
                        finish()
                    },
                    onBookmarkClick = { bookmark ->
                        setResult(
                            RESULT_OK,
                            Intent().apply {
                                putExtra("index", bookmark.chapterIndex)
                                putExtra("chapterPos", bookmark.chapterPos)
                            }
                        )
                        finish()
                    },
                    onBookmarkLongClick = { bookmark, index ->
                        showDialogFragment(BookmarkDialog(bookmark, index))
                    }
                )
            }
        }
        viewModel.bookData.observe(this) {
            refreshComposeData()
        }
        intent.getStringExtra("bookUrl")?.let(viewModel::initBook)
    }

    override fun onTocRegexDialogResult(tocRegex: String) {
        viewModel.bookData.value?.let { book ->
            book.tocUrl = tocRegex
            upBookAndToc(book)
        }
    }

    private fun refreshComposeData() {
        refreshChapters()
        refreshBookmarks()
    }

    private fun refreshChapters() {
        val book = viewModel.bookData.value ?: return
        lifecycleScope.launch {
            chapterItems = withContext(IO) {
                val end = (book.simulatedTotalChapterNum() - 1).coerceAtLeast(0)
                val rawChapters = if (query.isBlank()) {
                    appDb.bookChapterDao.getChapterList(viewModel.bookUrl, 0, end)
                } else {
                    appDb.bookChapterDao.search(viewModel.bookUrl, query, 0, end)
                }
                val useReplace = AppConfig.tocUiUseReplace && book.getUseReplaceRule()
                val replaceRules = if (useReplace) {
                    ContentProcessor.get(book.name, book.origin).getTitleReplaceRules()
                } else {
                    null
                }
                rawChapters.map { chapter ->
                    val meta = buildList {
                        if (AppConfig.tocCountWords && !chapter.wordCount.isNullOrBlank()) {
                            add(chapter.wordCount.orEmpty())
                        }
                        if (!chapter.tag.isNullOrBlank()) {
                            add(chapter.tag.orEmpty())
                        }
                        if (chapter.isVip && !chapter.isPay) {
                            add("需登录/未购买")
                        }
                    }.joinToString(" · ")
                    TocChapterUi(
                        index = chapter.index,
                        title = chapter.getDisplayTitle(replaceRules, useReplace),
                        meta = if (meta.isBlank()) "第${chapter.index + 1}章" else meta,
                        isCurrent = chapter.index == book.durChapterIndex,
                        isLocked = chapter.isVip && !chapter.isPay
                    )
                }
            }
        }
    }

    private fun refreshBookmarks() {
        bookmarkJob?.cancel()
        val book = viewModel.bookData.value ?: return
        bookmarkJob = lifecycleScope.launch {
            val flow = if (query.isBlank()) {
                appDb.bookmarkDao.flowByBook(book.name, book.author)
            } else {
                appDb.bookmarkDao.flowSearch(book.name, book.author, query)
            }
            flow.catch { bookmarkItems = emptyList() }
                .flowOn(IO)
                .collect { bookmarkItems = it }
        }
    }

    private fun progressLabel(): String {
        val book = viewModel.bookData.value ?: return ""
        return "${book.durChapterTitle}(${book.durChapterIndex + 1}/${book.simulatedTotalChapterNum()})"
    }

    private fun upBookAndToc(book: Book) {
        waitDialog.show()
        viewModel.upBookTocRule(book) {
            waitDialog.dismiss()
            refreshComposeData()
        }
    }
}
