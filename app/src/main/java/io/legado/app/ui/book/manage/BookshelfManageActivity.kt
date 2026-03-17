package io.legado.app.ui.book.manage

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.BookSource
import io.legado.app.databinding.ActivityArrangeBookBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.DirectLinkUpload
import io.legado.app.help.book.contains
import io.legado.app.help.book.isLocal
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.LocalConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.book.group.GroupManageDialog
import io.legado.app.ui.book.group.GroupSelectDialog
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.compose.theme.LegadoBottomBar
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.compose.theme.LegadoMenuButton
import io.legado.app.ui.compose.theme.LegadoSearchAppBar
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.theme.applyLegadoPageListStyle

import io.legado.app.ui.widget.dialog.WaitDialog
import io.legado.app.ui.widget.recycler.DragSelectTouchHelper
import io.legado.app.ui.widget.recycler.ItemTouchCallback
import io.legado.app.ui.widget.recycler.VerticalDivider
import io.legado.app.ui.widget.recycler.scroller.FastScrollRecyclerView
import io.legado.app.utils.cnCompare
import io.legado.app.utils.dpToPx
import io.legado.app.utils.isAbsUrl
import io.legado.app.utils.sendToClip
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

/**
 * 书架管理
 */
class BookshelfManageActivity :
    VMBaseActivity<ActivityArrangeBookBinding, BookshelfManageViewModel>(),
    BookAdapter.CallBack,
    SourcePickerDialog.Callback,
    GroupSelectDialog.CallBack {

    override val binding by viewBinding(ActivityArrangeBookBinding::inflate)
    override val viewModel by viewModels<BookshelfManageViewModel>()
    override val groupList: ArrayList<BookGroup> = arrayListOf()
    private val groupRequestCode = 22
    private val addToGroupRequestCode = 34
    private val adapter by lazy { BookAdapter(this, this) }
    private val itemTouchCallback by lazy { ItemTouchCallback(adapter) }
    private var booksFlowJob: Job? = null

    private var searchQuery by mutableStateOf("")
    private var books: List<Book>? = null
    private val waitDialog by lazy { WaitDialog(this) }
    private var selectCount by mutableIntStateOf(0)
    
    private val exportDir = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            alert(R.string.export_success) {
                if (uri.toString().isAbsUrl()) {
                    setMessage(DirectLinkUpload.getSummary())
                }
                val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                    editView.hint = getString(R.string.path)
                    editView.setText(uri.toString())
                }
                customView { alertBinding.root }
                okButton {
                    sendToClip(uri.toString())
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.groupId = intent.getLongExtra("groupId", -1)

        binding.composeArrangeBook.setContent {
            LegadoComposeTheme {
                var isOpenBookInfo by remember { mutableStateOf(AppConfig.openBookInfoByClickTitle) }

                BookshelfManageScreen(
                    query = searchQuery,
                    groupName = viewModel.groupName ?: "",
                    selectCount = selectCount,
                    totalCount = adapter.itemCount,
                    groups = groupList,
                    isOpenBookInfo = isOpenBookInfo,
                    onOpenBookInfoChange = { 
                        isOpenBookInfo = it 
                        AppConfig.openBookInfoByClickTitle = it
                        adapter.notifyItemRangeChanged(0, adapter.itemCount)
                    },
                    onQueryChange = { 
                        searchQuery = it 
                        upBookData()
                    },
                    onBackClick = { finish() },
                    onActionMenuClick = ::handleMenuAction,
                    onSelectAll = { selectAll(!adapter.selection.containsAll(adapter.getItems())) },
                    onRevertSelection = ::revertSelection,
                    onMainAction = ::onClickSelectBarMainAction,
                    onBottomMenuClick = ::handleBottomMenuAction,
                    recyclerContent = { modifier ->
                        AndroidView(
                            modifier = modifier,
                            factory = { context ->
                                val inflater = LayoutInflater.from(context)
                                val recyclerView = inflater.inflate(R.layout.view_fast_scroll_recycler, null, false) as FastScrollRecyclerView
                                initRecyclerView(recyclerView)
                                recyclerView
                            }
                        )
                    }
                )
            }
        }

        lifecycleScope.launch {
            viewModel.groupName = withContext(IO) {
                appDb.bookGroupDao.getByID(viewModel.groupId)?.groupName
                    ?: getString(R.string.no_group)
            }
        }
        
        initOtherView()
        initGroupData()
        upBookDataByGroupId()
    }

    override fun observeLiveBus() {
        viewModel.batchChangeSourceState.observe(this) {
            if (it) {
                waitDialog.setText(R.string.change_source_batch)
                waitDialog.show()
            } else {
                waitDialog.dismiss()
            }
        }
        viewModel.batchChangeSourceProcessLiveData.observe(this) {
            waitDialog.setText(it)
        }
    }

    private fun handleMenuAction(action: String, group: BookGroup?) {
        when(action) {
            "group_manage" -> showDialogFragment<GroupManageDialog>()
            "export_all" -> viewModel.saveAllUseBookSourceToFile { file ->
                exportDir.launch {
                    mode = HandleFileContract.EXPORT
                    fileData = HandleFileContract.FileData(
                        "bookSource.json",
                        file,
                        "application/json"
                    )
                }
            }
            "group_select" -> if (group != null) {
                viewModel.groupName = group.groupName
                viewModel.groupId = group.groupId
                upBookDataByGroupId()
            }
        }
    }

    private fun handleBottomMenuAction(action: String) {
        when (action) {
            "del_selection" -> alertDelSelection()
            "update_enable" -> viewModel.upCanUpdate(adapter.selection, true)
            "update_disable" -> viewModel.upCanUpdate(adapter.selection, false)
            "add_to_group" -> selectGroup(addToGroupRequestCode, 0)
            "change_source" -> showDialogFragment<SourcePickerDialog>()
            "clear_cache" -> viewModel.clearCache(adapter.selection)
            "check_selected_interval" -> adapter.checkSelectedInterval()
        }
    }

    private fun selectAll(selectAll: Boolean) {
        adapter.selectAll(selectAll)
    }

    private fun revertSelection() {
        adapter.revertSelection()
    }

    private fun onClickSelectBarMainAction() {
        selectGroup(groupRequestCode, 0)
    }

    private fun initRecyclerView(recyclerView: FastScrollRecyclerView) {
        recyclerView.applyLegadoPageListStyle()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(VerticalDivider(this))
        recyclerView.adapter = adapter
        itemTouchCallback.isCanDrag = AppConfig.bookshelfSort == 3
        val dragSelectTouchHelper = DragSelectTouchHelper(adapter.dragSelectCallback).setSlideArea(16, 50)
        dragSelectTouchHelper.attachToRecyclerView(recyclerView)
        dragSelectTouchHelper.activeSlideSelect()
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(recyclerView)
    }

    private fun initOtherView() {
        waitDialog.setOnCancelListener {
            viewModel.batchChangeSourceCoroutine?.cancel()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initGroupData() {
        lifecycleScope.launch {
            appDb.bookGroupDao.flowAll().catch {
                AppLog.put("书架管理界面获取分组数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).conflate().collect {
                groupList.clear()
                groupList.addAll(it)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun upBookDataByGroupId() {
        booksFlowJob?.cancel()
        booksFlowJob = lifecycleScope.launch {
            val bookSort = AppConfig.getBookSortByGroupId(viewModel.groupId)
            appDb.bookDao.flowByGroup(viewModel.groupId).map { list ->
                when (bookSort) {
                    1 -> list.sortedByDescending { it.latestChapterTime }
                    2 -> list.sortedWith { o1, o2 -> o1.name.cnCompare(o2.name) }
                    3 -> list.sortedBy { it.order }
                    4 -> list.sortedByDescending { max(it.latestChapterTime, it.durChapterTime) }
                    else -> list.sortedByDescending { it.durChapterTime }
                }
            }.catch {
                AppLog.put("书架管理界面获取书籍列表失败\n${it.localizedMessage}", it)
            }.flowOn(IO)
            .conflate().collect {
                books = it
                upBookData()
                itemTouchCallback.isCanDrag = bookSort == 3
            }
        }
    }

    private fun upBookData() {
        books?.let { books ->
            if (searchQuery.isEmpty()) {
                adapter.setItems(books)
            } else {
                books.filter {
                    it.contains(searchQuery)
                }.let {
                    adapter.setItems(it)
                }
            }
        }
    }

    private fun alertDelSelection() {
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
            val checkBox = CheckBox(this@BookshelfManageActivity).apply {
                setText(R.string.delete_book_file)
                isChecked = LocalConfig.deleteBookOriginal
            }
            val view = LinearLayout(this@BookshelfManageActivity).apply {
                setPadding(16.dpToPx(), 0, 16.dpToPx(), 0)
                addView(checkBox)
            }
            customView { view }
            okButton {
                LocalConfig.deleteBookOriginal = checkBox.isChecked
                viewModel.deleteBook(adapter.selection, checkBox.isChecked)
            }
            noButton()
        }
    }

    override fun selectGroup(requestCode: Int, groupId: Long) {
        showDialogFragment(
            GroupSelectDialog(groupId, requestCode)
        )
    }

    override fun upGroup(requestCode: Int, groupId: Long) {
        when (requestCode) {
            groupRequestCode -> adapter.selection.let { books ->
                val array = Array(books.size) {
                    books[it].copy(group = groupId)
                }
                viewModel.updateBook(*array)
            }

            adapter.groupRequestCode -> {
                adapter.actionItem?.let {
                    viewModel.updateBook(it.copy(group = groupId))
                }
            }

            addToGroupRequestCode -> adapter.selection.let { books ->
                val array = Array(books.size) { index ->
                    val book = books[index]
                    book.copy(group = book.group or groupId)
                }
                viewModel.updateBook(*array)
            }
        }
    }

    override fun upSelectCount() {
        selectCount = adapter.selection.size
    }

    override fun updateBook(vararg book: Book) {
        viewModel.updateBook(*book)
    }

    override fun deleteBook(book: Book) {
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
            var checkBox: CheckBox? = null
            if (book.isLocal) {
                checkBox = CheckBox(this@BookshelfManageActivity).apply {
                    setText(R.string.delete_book_file)
                    isChecked = LocalConfig.deleteBookOriginal
                }
                val view = LinearLayout(this@BookshelfManageActivity).apply {
                    setPadding(16.dpToPx(), 0, 16.dpToPx(), 0)
                    addView(checkBox)
                }
                customView { view }
            }
            okButton {
                if (checkBox != null) {
                    LocalConfig.deleteBookOriginal = checkBox.isChecked
                }
                viewModel.deleteBook(listOf(book), LocalConfig.deleteBookOriginal)
            }
        }
    }

    override fun openBook(book: Book) {
        startActivity<BookInfoActivity> {
            putExtra("name", book.name)
            putExtra("author", book.author)
        }
    }

    override fun sourceOnClick(source: BookSource) {
        viewModel.changeSource(adapter.selection, source)
        viewModel.batchChangeSourceState.value = true
    }
}

@Composable
private fun BookshelfManageScreen(
    query: String,
    groupName: String,
    selectCount: Int,
    totalCount: Int,
    groups: List<BookGroup>,
    isOpenBookInfo: Boolean,
    onOpenBookInfoChange: (Boolean) -> Unit,
    onQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onActionMenuClick: (String, BookGroup?) -> Unit,
    onSelectAll: () -> Unit,
    onRevertSelection: () -> Unit,
    onMainAction: () -> Unit,
    onBottomMenuClick: (String) -> Unit,
    recyclerContent: @Composable (Modifier) -> Unit
) {
    Scaffold(
        topBar = {
            LegadoSearchAppBar(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = "筛选 • $groupName",
                onBackClick = onBackClick,
                actions = {
                    LegadoMenuButton(
                        icon = { Icon(Icons.Default.MoreVert, contentDescription = "更 多") }
                    ) { dismiss ->
                        DropdownMenuItem(
                            text = { Text("分组管理") },
                            onClick = { onActionMenuClick("group_manage", null); dismiss() }
                        )
                        DropdownMenuItem(
                            text = { Text("单击打开书籍信息") },
                            trailingIcon = { 
                                Checkbox(
                                    checked = isOpenBookInfo, 
                                    onCheckedChange = null
                                ) 
                            },
                            onClick = { 
                                onOpenBookInfoChange(!isOpenBookInfo)
                                dismiss() 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("书源导出含书籍验证") },
                            onClick = { onActionMenuClick("export_all", null); dismiss() }
                        )
                        HorizontalDivider()
                        groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.groupName) },
                                onClick = { onActionMenuClick("group_select", group); dismiss() }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            LegadoBottomBar {
                TextButton(onClick = onSelectAll) {
                    Text("全选")
                }
                TextButton(onClick = onRevertSelection) {
                    Text("反选")
                }
                TextButton(onClick = onMainAction) {
                    Text("移至分组", color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    text = "$selectCount/$totalCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                LegadoMenuButton(
                    icon = { Icon(Icons.Default.MoreVert, contentDescription = "更多") }
                ) { dismiss ->
                    DropdownMenuItem(
                        text = { Text("删除选中内容") },
                        onClick = { onBottomMenuClick("del_selection"); dismiss() }
                    )
                    DropdownMenuItem(
                        text = { Text("允许选中书籍更新") },
                        onClick = { onBottomMenuClick("update_enable"); dismiss() }
                    )
                    DropdownMenuItem(
                        text = { Text("禁止选中书籍更新") },
                        onClick = { onBottomMenuClick("update_disable"); dismiss() }
                    )
                    DropdownMenuItem(
                        text = { Text("将选中书籍加到分组") },
                        onClick = { onBottomMenuClick("add_to_group"); dismiss() }
                    )
                    DropdownMenuItem(
                        text = { Text("批量换源") },
                        onClick = { onBottomMenuClick("change_source"); dismiss() }
                    )
                    DropdownMenuItem(
                        text = { Text("清理选中书籍缓存") },
                        onClick = { onBottomMenuClick("clear_cache"); dismiss() }
                    )
                    DropdownMenuItem(
                        text = { Text("间隔选中状态") },
                        onClick = { onBottomMenuClick("check_selected_interval"); dismiss() }
                    )
                }
            }
        }
    ) { innerPadding ->
        recyclerContent(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }
}
