package io.legado.app.ui.book.changesource

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import io.legado.app.R
import io.legado.app.base.BaseDialogFragment
import io.legado.app.constant.AppLog
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.data.entities.BookSource
import io.legado.app.data.entities.SearchBook
import io.legado.app.databinding.DialogBookChangeSourceBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.book.read.ReadBookActivity
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.book.source.manage.BookSourceActivity
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.widget.dialog.WaitDialog
import io.legado.app.utils.StartActivityContract
import io.legado.app.utils.setLayout
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding

/**
 * 换源界面
 */
class ChangeBookSourceDialog() : BaseDialogFragment(R.layout.dialog_book_change_source) {

    constructor(name: String, author: String) : this() {
        arguments = Bundle().apply {
            putString("name", name)
            putString("author", author)
        }
    }

    private val binding by viewBinding(DialogBookChangeSourceBinding::bind)
    private val callBack: CallBack? get() = activity as? CallBack
    private val viewModel: ChangeBookSourceViewModel by viewModels()
    private val waitDialog by lazy { WaitDialog(requireContext()) }
    
    private val editSourceResult =
        registerForActivityResult(StartActivityContract(BookSourceEditActivity::class.java)) {
            val origin = it.data?.getStringExtra("origin") ?: return@registerForActivityResult
            viewModel.startSearch(origin)
        }

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.initData(arguments, callBack?.oldBook, activity is ReadBookActivity)
        
        binding.composeView.setContent {
            LegadoComposeTheme {
                ChangeBookSourceScreen(
                    viewModel = viewModel,
                    oldBookUrl = callBack?.oldBook?.bookUrl,
                    onBackClick = { dismissAllowingStateLoss() },
                    onSourceClick = { changeTo(it) },
                    onAction = ::onItemAction,
                    onTopAction = ::onTopAction
                )
            }
        }

        viewModel.searchFinishCallback = { isEmpty ->
            if (isEmpty && AppConfig.searchGroup.isNotEmpty()) {
                alert("搜索结果为空") {
                    setMessage("${AppConfig.searchGroup}分组搜索结果为空,是否切换到全部分组")
                    cancelButton()
                    okButton {
                        AppConfig.searchGroup = ""
                        viewModel.startSearch()
                    }
                }
            }
        }
    }

    private fun onTopAction(action: ChangeBookSourceTopAction) {
        when (action) {
            ChangeBookSourceTopAction.ToggleCheckAuthor -> {
                AppConfig.changeSourceCheckAuthor = !AppConfig.changeSourceCheckAuthor
                viewModel.refresh()
            }
            ChangeBookSourceTopAction.ToggleLoadInfo -> {
                AppConfig.changeSourceLoadInfo = !AppConfig.changeSourceLoadInfo
            }
            ChangeBookSourceTopAction.ToggleLoadToc -> {
                AppConfig.changeSourceLoadToc = !AppConfig.changeSourceLoadToc
            }
            ChangeBookSourceTopAction.ToggleLoadWordCount -> {
                AppConfig.changeSourceLoadWordCount = !AppConfig.changeSourceLoadWordCount
                viewModel.onLoadWordCountChecked(AppConfig.changeSourceLoadWordCount)
            }
            ChangeBookSourceTopAction.RefreshList -> viewModel.startRefreshList()
            ChangeBookSourceTopAction.SourceManage -> startActivity<BookSourceActivity>()
        }
    }

    private fun onItemAction(book: SearchBook, action: ChangeBookSourceAction) {
        when (action) {
            ChangeBookSourceAction.ToTop -> viewModel.topSource(book)
            ChangeBookSourceAction.ToBottom -> viewModel.bottomSource(book)
            ChangeBookSourceAction.Edit -> {
                editSourceResult.launch {
                    putExtra("sourceUrl", book.origin)
                }
            }
            ChangeBookSourceAction.Disable -> viewModel.disableSource(book)
            ChangeBookSourceAction.Delete -> {
                alert(R.string.draw) {
                    setMessage(getString(R.string.sure_del) + "\n" + book.originName)
                    noButton()
                    yesButton {
                        viewModel.del(book)
                        if (callBack?.oldBook?.bookUrl == book.bookUrl) {
                            viewModel.autoChangeSource(callBack?.oldBook?.type) { autoBook, toc, source ->
                                callBack?.changeTo(source, autoBook, toc)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun changeTo(searchBook: SearchBook) {
        val oldBookType = callBack?.oldBook?.type ?: 0
        if (searchBook.sameBookTypeLocal(oldBookType)) {
            changeSource(searchBook) {
                dismissAllowingStateLoss()
            }
        } else {
            alert(
                titleResource = R.string.book_type_different,
                messageResource = R.string.soure_change_source
            ) {
                okButton {
                    changeSource(searchBook) {
                        dismissAllowingStateLoss()
                    }
                }
                cancelButton()
            }
        }
    }

    private fun changeSource(searchBook: SearchBook, onSuccess: (() -> Unit)? = null) {
        waitDialog.setText(R.string.load_toc)
        waitDialog.show()
        val book = viewModel.bookMap[searchBook.primaryStr()] ?: searchBook.toBook()
        val coroutine = viewModel.getToc(book, { toc, source ->
            waitDialog.dismiss()
            callBack?.changeTo(source, book, toc)
            onSuccess?.invoke()
        }, {
            waitDialog.dismiss()
            AppLog.put("换源获取目录出错\n$it", it, true)
        })
        waitDialog.setOnCancelListener {
            coroutine.cancel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.searchFinishCallback = null
    }

    interface CallBack {
        val oldBook: Book?
        fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>)
    }

}
