package io.legado.app.ui.main.bookshelf.style2

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.constant.AppLog
import io.legado.app.constant.EventBus
import io.legado.app.data.AppDatabase
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.databinding.FragmentBookshelf2Binding
import io.legado.app.help.book.isLocal
import io.legado.app.help.config.AppConfig
import io.legado.app.ui.book.group.GroupEditDialog
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.main.bookshelf.BookshelfGroupMaterialScreen
import io.legado.app.ui.main.bookshelf.BaseBookshelfFragment
import io.legado.app.utils.flowWithLifecycleAndDatabaseChangeFirst
import io.legado.app.utils.observeEvent
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity
import io.legado.app.utils.startActivityForBook
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.max

/**
 * 书架界面
 */
class BookshelfFragment2() : BaseBookshelfFragment(R.layout.fragment_bookshelf2) {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    private val binding by viewBinding(FragmentBookshelf2Binding::bind)
    private var bookGroups: List<BookGroup> = emptyList()
    private var bookGroupMap: Map<Long, BookGroup> = emptyMap()
    private var booksFlowJob: Job? = null
    override var groupId = BookGroup.IdRoot
    override var books: List<Book> = emptyList()
    private var enableRefresh = true
    private var booksMap: Map<String, Book> = emptyMap()

    private var composeGroups by mutableStateOf(emptyList<BookshelfGroupUi>())
    private var composeBooks by mutableStateOf(emptyList<BookshelfBookUi>())
    private var composeGroupId by mutableLongStateOf(BookGroup.IdRoot)
    private var scrollRequest by mutableIntStateOf(0)
    private var showUnread by mutableStateOf(AppConfig.showUnread)
    private var showLastUpdateTime by mutableStateOf(AppConfig.showLastUpdateTime)
    private var isGridLayout by mutableStateOf(AppConfig.bookshelfLayout != 0)
    private var gridColumns by mutableIntStateOf((AppConfig.bookshelfLayout + 2).coerceAtLeast(2))
    private var composeTitle by mutableStateOf("")

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initComposeContent()
        initBookGroupData()
        initBooksData()
    }

    private fun initComposeContent() {
        binding.composeBookshelfContent.setContent {
            LegadoComposeTheme {
                BookshelfGroupMaterialScreen(
                    title = composeTitle,
                    groupId = composeGroupId,
                    groups = composeGroups,
                    books = composeBooks,
                    showUnread = showUnread,
                    showLastUpdateTime = showLastUpdateTime,
                    isGrid = isGridLayout,
                    gridColumns = gridColumns,
                    scrollRequest = scrollRequest,
                    onRefresh = {
                        if (enableRefresh) {
                            activityViewModel.upToc(books)
                        }
                    },
                    onBackToRoot = {
                        groupId = BookGroup.IdRoot
                        composeGroupId = groupId
                        initBooksData()
                    },
                    onBookClick = { item ->
                        booksMap[item.bookUrl]?.let(::startActivityForBook)
                    },
                    onBookLongClick = { item ->
                        booksMap[item.bookUrl]?.let { book ->
                            startActivity<BookInfoActivity> {
                                putExtra("name", book.name)
                                putExtra("author", book.author)
                            }
                        }
                    },
                    onGroupClick = { item ->
                        groupId = item.groupId
                        composeGroupId = groupId
                        initBooksData()
                    },
                    onGroupLongClick = { item ->
                        bookGroupMap[item.groupId]?.let { group ->
                            showDialogFragment(GroupEditDialog(group))
                        }
                    }
                )
            }
        }
    }

    override fun upGroup(data: List<BookGroup>) {
        if (data != bookGroups) {
            bookGroups = data
            bookGroupMap = data.associateBy { it.groupId }
            composeGroups = data.map {
                BookshelfGroupUi(
                    groupId = it.groupId,
                    groupName = it.groupName,
                    cover = it.cover
                )
            }
            if (groupId != BookGroup.IdRoot) {
                updateTitle()
            }
        }
    }

    override fun upSort() {
        initBooksData()
    }

    private fun initBooksData() {
        updateSettings()
        composeGroupId = groupId
        updateTitle()
        booksFlowJob?.cancel()
        booksFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            appDb.bookDao.flowByGroup(groupId).map { list ->
                when (AppConfig.getBookSortByGroupId(groupId)) {
                    1 -> list.sortedByDescending { it.latestChapterTime }
                    2 -> list.sortedBy { it.name.lowercase() }
                    3 -> list.sortedBy { it.order }
                    4 -> list.sortedByDescending { max(it.latestChapterTime, it.durChapterTime) }
                    else -> list.sortedByDescending { it.durChapterTime }
                }
            }.flowWithLifecycleAndDatabaseChangeFirst(
                viewLifecycleOwner.lifecycle,
                Lifecycle.State.RESUMED,
                AppDatabase.BOOK_TABLE_NAME
            ).catch {
                AppLog.put("书架更新出错", it)
            }.conflate().flowOn(Dispatchers.Default).collect { list ->
                books = list
                booksMap = list.associateBy { it.bookUrl }
                composeBooks = list.map { book ->
                    BookshelfBookUi(
                        bookUrl = book.bookUrl,
                        name = book.name,
                        author = book.author,
                        origin = book.origin,
                        cover = book.getDisplayCover(),
                        currentChapter = book.durChapterTitle,
                        latestChapter = book.latestChapterTitle,
                        latestChapterTime = book.latestChapterTime,
                        unreadCount = book.getUnreadChapterNum(),
                        lastCheckCount = book.lastCheckCount,
                        isUpdating = activityViewModel.isUpdate(book.bookUrl),
                        isLocal = book.isLocal
                    )
                }
                delay(100)
            }
        }
    }

    private fun updateTitle() {
        if (groupId == BookGroup.IdRoot) {
            composeTitle = getString(R.string.bookshelf)
            enableRefresh = true
        } else {
            bookGroups.firstOrNull { it.groupId == groupId }?.let {
                composeTitle = it.groupName
                enableRefresh = it.enableRefresh
            }
        }
    }

    private fun updateSettings() {
        showUnread = AppConfig.showUnread
        showLastUpdateTime = AppConfig.showLastUpdateTime
        isGridLayout = AppConfig.bookshelfLayout != 0
        gridColumns = (AppConfig.bookshelfLayout + 2).coerceAtLeast(2)
    }

    private fun refreshUpdatingState() {
        composeBooks = composeBooks.map {
            it.copy(isUpdating = activityViewModel.isUpdate(it.bookUrl))
        }
    }

    fun back(): Boolean {
        if (groupId != BookGroup.IdRoot) {
            groupId = BookGroup.IdRoot
            composeGroupId = groupId
            initBooksData()
            return true
        }
        return false
    }

    override fun gotoTop() {
        scrollRequest++
    }

    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<String>(EventBus.UP_BOOKSHELF) {
            refreshUpdatingState()
        }
        observeEvent<String>(EventBus.BOOKSHELF_REFRESH) {
            updateSettings()
            refreshUpdatingState()
        }
    }
}
