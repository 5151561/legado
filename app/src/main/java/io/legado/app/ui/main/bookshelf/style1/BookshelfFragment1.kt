package io.legado.app.ui.main.bookshelf.style1

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import io.legado.app.databinding.FragmentBookshelf1Binding
import io.legado.app.help.book.isLocal
import io.legado.app.help.config.AppConfig
import io.legado.app.ui.book.group.GroupEditDialog
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.main.bookshelf.BaseBookshelfFragment
import io.legado.app.ui.main.bookshelf.style2.BookshelfBookUi
import io.legado.app.ui.main.bookshelf.style2.BookshelfGroupUi
import io.legado.app.utils.cnCompare
import io.legado.app.utils.flowWithLifecycleAndDatabaseChangeFirst
import io.legado.app.utils.observeEvent
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity
import io.legado.app.utils.startActivityForBook
import io.legado.app.utils.toastOnUi
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
class BookshelfFragment1() : BaseBookshelfFragment(R.layout.fragment_bookshelf1) {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    private val binding by viewBinding(FragmentBookshelf1Binding::bind)
    private val bookGroups = mutableListOf<BookGroup>()
    private var booksFlowJob: Job? = null
    private var booksMap: Map<String, Book> = emptyMap()

    private var selectedTabIndex by mutableIntStateOf(0)
    private var composeGroups by mutableStateOf(emptyList<BookshelfGroupUi>())
    private var composeBooks by mutableStateOf(emptyList<BookshelfBookUi>())
    private var scrollRequest by mutableIntStateOf(0)
    private var showUnread by mutableStateOf(AppConfig.showUnread)
    private var showLastUpdateTime by mutableStateOf(AppConfig.showLastUpdateTime)
    private var isGridLayout by mutableStateOf(AppConfig.bookshelfLayout != 0)
    private var gridColumns by mutableIntStateOf((AppConfig.bookshelfLayout + 2).coerceAtLeast(2))

    override val groupId: Long
        get() = selectedGroup?.groupId ?: BookGroup.IdAll

    override val books: List<Book>
        get() = booksMap.values.toList()

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        initComposeContent()
        initBookGroupData()
    }

    private fun initComposeContent() {
        binding.composeBookshelfContent.setContent {
            LegadoComposeTheme {
                BookshelfTabsComposeScreen(
                    selectedTabIndex = selectedTabIndex.safeTabIndex(composeGroups.size),
                    groups = composeGroups,
                    books = composeBooks,
                    showUnread = showUnread,
                    showLastUpdateTime = showLastUpdateTime,
                    isGrid = isGridLayout,
                    gridColumns = gridColumns,
                    scrollRequest = scrollRequest,
                    onRefresh = { selectedGroup?.let { group -> refreshGroupBooks(group) } },
                    onTabClick = ::onTabSelected,
                    onTabLongClick = { group ->
                        bookGroups.firstOrNull { it.groupId == group.groupId }?.let {
                            showDialogFragment(GroupEditDialog(it))
                        }
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
                    }
                )
            }
        }
    }

    private val selectedGroup: BookGroup?
        get() = bookGroups.getOrNull(selectedTabIndex)

    @Synchronized
    override fun upGroup(data: List<BookGroup>) {
        if (data.isEmpty()) {
            appDb.bookGroupDao.enableGroup(BookGroup.IdAll)
            return
        }
        if (data != bookGroups) {
            bookGroups.clear()
            bookGroups.addAll(data)
            composeGroups = data.map {
                BookshelfGroupUi(
                    groupId = it.groupId,
                    groupName = it.groupName,
                    cover = it.cover
                )
            }
            selectedTabIndex = AppConfig.saveTabPosition.safeTabIndex(bookGroups.size)
            refreshSelectedTabBooks()
        }
    }

    override fun upSort() {
        refreshSelectedTabBooks()
    }

    private fun onTabSelected(index: Int) {
        if (index == selectedTabIndex) {
            selectedGroup?.let { group ->
                toastOnUi("${group.groupName}(${composeBooks.size})")
            }
            return
        }
        selectedTabIndex = index
        AppConfig.saveTabPosition = index
        refreshSelectedTabBooks()
    }

    private fun refreshSelectedTabBooks() {
        selectedGroup?.let { refreshGroupBooks(it) }
    }

    private fun refreshGroupBooks(group: BookGroup) {
        updateSettings()
        booksFlowJob?.cancel()
        booksFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            appDb.bookDao.flowByGroup(group.groupId).map { list ->
                when (group.getRealBookSort()) {
                    1 -> list.sortedByDescending { it.latestChapterTime }
                    2 -> list.sortedWith { o1, o2 -> o1.name.cnCompare(o2.name) }
                    3 -> list.sortedBy { it.order }
                    4 -> list.sortedByDescending { max(it.latestChapterTime, it.durChapterTime) }
                    5 -> list.sortedWith { o1, o2 -> o1.author.cnCompare(o2.author) }
                    else -> list.sortedByDescending { it.durChapterTime }
                }
            }.flowWithLifecycleAndDatabaseChangeFirst(
                viewLifecycleOwner.lifecycle,
                Lifecycle.State.RESUMED,
                AppDatabase.BOOK_TABLE_NAME
            ).catch {
                AppLog.put("书架更新出错", it)
            }.conflate().flowOn(Dispatchers.Default).collect { list ->
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

    private fun Int.safeTabIndex(size: Int): Int {
        if (size <= 0) return 0
        return coerceIn(0, size - 1)
    }
}
