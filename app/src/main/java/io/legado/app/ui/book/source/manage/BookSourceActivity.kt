package io.legado.app.ui.book.source.manage

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppLog
import io.legado.app.constant.EventBus
import io.legado.app.data.AppDatabase
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.databinding.ActivityBookSourceBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.DirectLinkUpload
import io.legado.app.help.config.LocalConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.model.CheckSource
import io.legado.app.model.Debug
import io.legado.app.ui.association.ImportBookSourceDialog
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.book.search.SearchScope
import io.legado.app.ui.book.source.debug.BookSourceDebugActivity
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.config.CheckSourceConfig
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.utils.ACache
import io.legado.app.utils.makeLegadoSnackbar
import io.legado.app.utils.NetworkUtils
import io.legado.app.utils.cnCompare
import io.legado.app.utils.dpToPx
import io.legado.app.utils.flowWithLifecycleAndDatabaseChange
import io.legado.app.utils.flowWithLifecycleAndDatabaseChangeFirst
import io.legado.app.utils.isAbsUrl
import io.legado.app.utils.observeEvent
import io.legado.app.utils.sendToClip
import io.legado.app.utils.share
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.showHelp
import io.legado.app.utils.splitNotBlank
import io.legado.app.utils.startActivity
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.transaction
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BookSourceActivity : VMBaseActivity<ActivityBookSourceBinding, BookSourceViewModel>() {

    override val binding by viewBinding(ActivityBookSourceBinding::inflate)
    override val viewModel by viewModels<BookSourceViewModel>()

    private val importRecordKey = "bookSourceRecordKey"
    private var sourceFlowJob: Job? = null
    private var checkMessageRefreshJob: Job? = null
    private val groups = linkedSetOf<String>()
    private var snackBar: Snackbar? = null
    private var groupSourcesByDomain = false
    private val hostMap = hashMapOf<String, String>()

    private var searchQuery by mutableStateOf("")
    private var sources by mutableStateOf<List<BookSourcePart>>(emptyList())
    private var selectedUrls by mutableStateOf<Set<String>>(emptySet())
    private var debugMessageVersion by mutableIntStateOf(0)
    private var sort by mutableStateOf(BookSourceSort.Default)
    private var sortAscending by mutableStateOf(true)

    private val qrResult = registerForActivityResult(QrCodeResult()) {
        it ?: return@registerForActivityResult
        showDialogFragment(ImportBookSourceDialog(it))
    }
    private val importDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            showDialogFragment(ImportBookSourceDialog(uri.toString()))
        }
    }
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
        binding.composeBookSourceContent.setContent {
            LegadoComposeTheme {
                BookSourceScreen(
                    sources = sources,
                    selectedUrls = selectedUrls,
                    searchQuery = searchQuery,
                    groups = groups.toList(),
                    sort = sort,
                    sortAscending = sortAscending,
                    groupSourcesByDomain = groupSourcesByDomain,
                    debugMessageVersion = debugMessageVersion,
                    onSearchQueryChange = ::onSearchQueryChange,
                    onBackClick = { finish() },
                    onSortChange = ::onSortChange,
                    onToggleSortAscending = ::toggleSortAscending,
                    onTopAction = ::onTopAction,
                    onGroupFilterClick = { updateSearch("group:$it") },
                    onToggleSelect = ::toggleSelection,
                    onToggleEnable = { source, enabled -> viewModel.enable(enabled, listOf(source)) },
                    onEditClick = ::edit,
                    onItemAction = ::onItemAction,
                    onSelectAll = ::toggleSelectAll,
                    onInvertSelection = ::revertSelection,
                    onDeleteSelected = ::deleteSelected,
                    onBatchAction = ::onBatchAction,
                    sourceHostProvider = { getSourceHost(it.bookSourceUrl) },
                    debugMessageProvider = { Debug.debugMessageMap[it.bookSourceUrl].orEmpty() }
                )
            }
        }
        upBookSource()
        initLiveDataGroup()
        resumeCheckSource()
        if (!LocalConfig.bookSourcesHelpVersionIsLast) {
            showHelp("SourceMBookHelp")
        }
    }

    private fun onSearchQueryChange(query: String) {
        searchQuery = query
        upBookSource(query)
    }

    private fun onSortChange(nextSort: BookSourceSort) {
        sort = nextSort
        upBookSource(searchQuery)
    }

    private fun toggleSortAscending() {
        sortAscending = !sortAscending
        upBookSource(searchQuery)
    }

    private fun onTopAction(action: BookSourceTopAction) {
        when (action) {
            BookSourceTopAction.Add -> startActivity<BookSourceEditActivity>()
            BookSourceTopAction.ImportLocal -> importDoc.launch {
                mode = HandleFileContract.FILE
                allowExtensions = arrayOf("txt", "json")
            }

            BookSourceTopAction.ImportOnline -> showImportDialog()
            BookSourceTopAction.ImportQr -> qrResult.launch(null)
            BookSourceTopAction.GroupManage -> showDialogFragment<GroupManageDialog>()
            BookSourceTopAction.ToggleDomainGroup -> {
                groupSourcesByDomain = !groupSourcesByDomain
                upBookSource(searchQuery)
            }

            BookSourceTopAction.Help -> showHelp("SourceMBookHelp")
            BookSourceTopAction.FilterEnabled -> updateSearch(getString(R.string.enabled))
            BookSourceTopAction.FilterDisabled -> updateSearch(getString(R.string.disabled))
            BookSourceTopAction.FilterNeedLogin -> updateSearch(getString(R.string.need_login))
            BookSourceTopAction.FilterNoGroup -> updateSearch(getString(R.string.no_group))
            BookSourceTopAction.FilterEnabledExplore -> updateSearch(getString(R.string.enabled_explore))
            BookSourceTopAction.FilterDisabledExplore -> updateSearch(getString(R.string.disabled_explore))
        }
    }

    private fun onItemAction(source: BookSourcePart, action: BookSourceItemAction) {
        when (action) {
            BookSourceItemAction.Delete -> del(source)
            BookSourceItemAction.ToggleExplore -> viewModel.enableExplore(!source.enabledExplore, listOf(source))
            BookSourceItemAction.ToTop -> toTop(source)
            BookSourceItemAction.ToBottom -> toBottom(source)
            BookSourceItemAction.Search -> searchBook(source)
            BookSourceItemAction.Debug -> debug(source)
            BookSourceItemAction.Login -> startActivity<SourceLoginActivity> {
                putExtra("type", "bookSource")
                putExtra("key", source.bookSourceUrl)
            }
        }
    }

    private fun onBatchAction(action: BookSourceBatchAction) {
        when (action) {
            BookSourceBatchAction.EnableSelected -> viewModel.enableSelection(selection)
            BookSourceBatchAction.DisableSelected -> viewModel.disableSelection(selection)
            BookSourceBatchAction.EnableExploreSelected -> viewModel.enableSelectExplore(selection)
            BookSourceBatchAction.DisableExploreSelected -> viewModel.disableSelectExplore(selection)
            BookSourceBatchAction.AddGroup -> selectionAddToGroups()
            BookSourceBatchAction.RemoveGroup -> selectionRemoveFromGroups()
            BookSourceBatchAction.TopSelected -> viewModel.topSource(*selection.toTypedArray())
            BookSourceBatchAction.BottomSelected -> viewModel.bottomSource(*selection.toTypedArray())
            BookSourceBatchAction.ExportSelected -> exportSelection()
            BookSourceBatchAction.ShareSelected -> shareSelection()
            BookSourceBatchAction.CheckSelected -> checkSource()
            BookSourceBatchAction.CheckSelectedInterval -> checkSelectedInterval()
        }
    }

    private val selection: List<BookSourcePart>
        get() = sources.filter { selectedUrls.contains(it.bookSourceUrl) }

    private fun updateSearch(query: String) {
        searchQuery = query
        upBookSource(query)
    }

    private fun toggleSelection(source: BookSourcePart) {
        selectedUrls = selectedUrls.toMutableSet().apply {
            if (!add(source.bookSourceUrl)) {
                remove(source.bookSourceUrl)
            }
        }
    }

    private fun toggleSelectAll() {
        selectedUrls = if (sources.isNotEmpty() && selectedUrls.size == sources.size) {
            emptySet()
        } else {
            sources.mapTo(linkedSetOf()) { it.bookSourceUrl }
        }
    }

    private fun revertSelection() {
        selectedUrls = sources.mapNotNullTo(linkedSetOf()) {
            if (selectedUrls.contains(it.bookSourceUrl)) null else it.bookSourceUrl
        }
    }

    private fun deleteSelected() {
        if (selection.isEmpty()) return
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
            yesButton { viewModel.del(selection) }
            noButton()
        }
    }

    private fun checkSelectedInterval() {
        if (selection.isEmpty()) return
        val selectedPosition = sources.mapIndexedNotNull { index, item ->
            if (selectedUrls.contains(item.bookSourceUrl)) index else null
        }
        if (selectedPosition.isEmpty()) return
        val minPosition = selectedPosition.min()
        val maxPosition = selectedPosition.max()
        selectedUrls = sources.subList(minPosition, maxPosition + 1)
            .mapTo(linkedSetOf()) { it.bookSourceUrl }
    }

    private fun exportSelection() {
        viewModel.saveToFile(
            displayedSources = sources,
            selectedSources = selection,
            searchKey = searchQuery,
            sortAscending = sortAscending,
            sort = sort
        ) { file ->
            exportDir.launch {
                mode = HandleFileContract.EXPORT
                fileData = HandleFileContract.FileData(
                    "bookSource.json",
                    file,
                    "application/json"
                )
            }
        }
    }

    private fun shareSelection() {
        viewModel.saveToFile(
            displayedSources = sources,
            selectedSources = selection,
            searchKey = searchQuery,
            sortAscending = sortAscending,
            sort = sort
        ) { file ->
            share(file)
        }
    }

    private fun upBookSource(searchKey: String? = null) {
        sourceFlowJob?.cancel()
        sourceFlowJob = lifecycleScope.launch {
            when {
                searchKey.isNullOrEmpty() -> appDb.bookSourceDao.flowAll()
                searchKey == getString(R.string.enabled) -> appDb.bookSourceDao.flowEnabled()
                searchKey == getString(R.string.disabled) -> appDb.bookSourceDao.flowDisabled()
                searchKey == getString(R.string.need_login) -> appDb.bookSourceDao.flowLogin()
                searchKey == getString(R.string.no_group) -> appDb.bookSourceDao.flowNoGroup()
                searchKey == getString(R.string.enabled_explore) -> appDb.bookSourceDao.flowEnabledExplore()
                searchKey == getString(R.string.disabled_explore) -> appDb.bookSourceDao.flowDisabledExplore()
                searchKey.startsWith("group:") -> appDb.bookSourceDao.flowGroupSearch(searchKey.substringAfter("group:"))
                else -> appDb.bookSourceDao.flowSearch(searchKey)
            }.map { data ->
                hostMap.clear()
                if (groupSourcesByDomain) {
                    data.sortedWith(
                        compareBy<BookSourcePart> { getSourceHost(it.bookSourceUrl) == "#" }
                            .thenBy { getSourceHost(it.bookSourceUrl) }
                            .thenByDescending { it.lastUpdateTime }
                    )
                } else if (sortAscending) {
                    when (sort) {
                        BookSourceSort.Weight -> data.sortedBy { it.weight }
                        BookSourceSort.Name -> data.sortedWith { o1, o2 ->
                            o1.bookSourceName.cnCompare(o2.bookSourceName)
                        }

                        BookSourceSort.Url -> data.sortedBy { it.bookSourceUrl }
                        BookSourceSort.Update -> data.sortedByDescending { it.lastUpdateTime }
                        BookSourceSort.Respond -> data.sortedBy { it.respondTime }
                        BookSourceSort.Enable -> data.sortedWith { o1, o2 ->
                            var sortValue = -o1.enabled.compareTo(o2.enabled)
                            if (sortValue == 0) {
                                sortValue = o1.bookSourceName.cnCompare(o2.bookSourceName)
                            }
                            sortValue
                        }

                        else -> data
                    }
                } else {
                    when (sort) {
                        BookSourceSort.Weight -> data.sortedByDescending { it.weight }
                        BookSourceSort.Name -> data.sortedWith { o1, o2 ->
                            o2.bookSourceName.cnCompare(o1.bookSourceName)
                        }

                        BookSourceSort.Url -> data.sortedByDescending { it.bookSourceUrl }
                        BookSourceSort.Update -> data.sortedBy { it.lastUpdateTime }
                        BookSourceSort.Respond -> data.sortedByDescending { it.respondTime }
                        BookSourceSort.Enable -> data.sortedWith { o1, o2 ->
                            var sortValue = o1.enabled.compareTo(o2.enabled)
                            if (sortValue == 0) {
                                sortValue = o1.bookSourceName.cnCompare(o2.bookSourceName)
                            }
                            sortValue
                        }

                        else -> data.reversed()
                    }
                }
            }.flowWithLifecycleAndDatabaseChange(
                lifecycle,
                table = AppDatabase.BOOK_SOURCE_TABLE_NAME
            ).catch {
                AppLog.put("书源界面更新书源出错", it)
            }.flowOn(IO).conflate().collect { data ->
                sources = data
                selectedUrls = selectedUrls.filterTo(linkedSetOf()) { selectedUrl ->
                    data.any { it.bookSourceUrl == selectedUrl }
                }
                delay(200)
            }
        }
    }

    private fun initLiveDataGroup() {
        lifecycleScope.launch {
            appDb.bookSourceDao.flowGroups()
                .flowWithLifecycleAndDatabaseChange(
                    lifecycle,
                    table = AppDatabase.BOOK_SOURCE_TABLE_NAME
                )
                .flowWithLifecycleAndDatabaseChangeFirst(
                    lifecycle,
                    table = AppDatabase.BOOK_SOURCE_TABLE_NAME
                )
                .conflate()
                .distinctUntilChanged()
                .collect {
                    groups.clear()
                    groups.addAll(it)
                    delay(200)
                }
        }
    }

    @SuppressLint("InflateParams")
    private fun checkSource() {
        if (selection.isEmpty()) return
        val dialog = alert(titleResource = R.string.search_book_key) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "search word"
                editView.setText(CheckSource.keyword)
            }
            customView { alertBinding.root }
            okButton {
                keepScreenOn(true)
                alertBinding.editView.text?.toString()?.let {
                    if (it.isNotEmpty()) {
                        CheckSource.keyword = it
                    }
                }
                val selectItems = selection
                CheckSource.start(this@BookSourceActivity, selectItems)
                val firstItem = sources.indexOf(selectItems.firstOrNull())
                val lastItem = sources.indexOf(selectItems.lastOrNull())
                Debug.isChecking = firstItem >= 0 && lastItem >= 0
                startCheckMessageRefreshJob(firstItem, lastItem)
            }
            neutralButton(R.string.check_source_config)
            cancelButton()
        }
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setOnClickListener {
            showDialogFragment<CheckSourceConfig>()
        }
    }

    private fun resumeCheckSource() {
        if (!Debug.isChecking) return
        keepScreenOn(true)
        CheckSource.resume(this)
        startCheckMessageRefreshJob(0, 0)
    }

    @SuppressLint("InflateParams")
    private fun selectionAddToGroups() {
        alert(titleResource = R.string.add_group) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.setHint(R.string.group_name)
                editView.setFilterValues(groups.toList())
                editView.dropDownHeight = 180.dpToPx()
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    if (it.isNotEmpty()) {
                        viewModel.selectionAddToGroups(selection, it)
                    }
                }
            }
            cancelButton()
        }
    }

    @SuppressLint("InflateParams")
    private fun selectionRemoveFromGroups() {
        alert(titleResource = R.string.remove_group) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.setHint(R.string.group_name)
                editView.setFilterValues(groups.toList())
                editView.dropDownHeight = 180.dpToPx()
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    if (it.isNotEmpty()) {
                        viewModel.selectionRemoveFromGroups(selection, it)
                    }
                }
            }
            cancelButton()
        }
    }

    @SuppressLint("InflateParams")
    private fun showImportDialog() {
        val aCache = ACache.get(cacheDir = false)
        val cacheUrls: MutableList<String> = aCache
            .getAsString(importRecordKey)
            ?.splitNotBlank(",")
            ?.toMutableList() ?: mutableListOf()
        alert(titleResource = R.string.import_on_line) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url"
                editView.setFilterValues(cacheUrls)
                editView.delCallBack = {
                    cacheUrls.remove(it)
                    aCache.put(importRecordKey, cacheUrls.joinToString(","))
                }
            }
            customView { alertBinding.root }
            okButton {
                val text = alertBinding.editView.text?.toString()
                text?.let {
                    if (it.isAbsUrl() && !cacheUrls.contains(it)) {
                        cacheUrls.add(0, it)
                        aCache.put(importRecordKey, cacheUrls.joinToString(","))
                    }
                    showDialogFragment(ImportBookSourceDialog(it))
                }
            }
            cancelButton()
        }
    }

    override fun observeLiveBus() {
        observeEvent<String>(EventBus.CHECK_SOURCE) { msg ->
            snackBar?.setText(msg) ?: run {
                snackBar = binding.root
                    .makeLegadoSnackbar(msg, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.cancel) {
                        CheckSource.stop(this)
                        Debug.finishChecking()
                    }.apply { show() }
            }
            debugMessageVersion++
        }
        observeEvent<Int>(EventBus.CHECK_SOURCE_DONE) {
            keepScreenOn(false)
            snackBar?.dismiss()
            snackBar = null
            debugMessageVersion++
            groups.forEach { group ->
                if (group.contains("失效") && searchQuery.isEmpty()) {
                    updateSearch("失效")
                    toastOnUi("发现有失效书源，已为您自动筛选！")
                }
            }
        }
    }

    private fun startCheckMessageRefreshJob(firstItem: Int, lastItem: Int) {
        checkMessageRefreshJob?.cancel()
        checkMessageRefreshJob = lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                while (isActive) {
                    debugMessageVersion++
                    if (!Debug.isChecking) {
                        checkMessageRefreshJob?.cancel()
                    }
                    delay(if (lastItem == 0 && firstItem == 0) 300L else 300L)
                }
            }
        }
    }

    private fun keepScreenOn(on: Boolean) {
        val isScreenOn =
            (window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        if (on == isScreenOn) return
        if (on) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun getSourceHost(origin: String): String {
        return hostMap.getOrPut(origin) {
            NetworkUtils.getSubDomainOrNull(origin) ?: "#"
        }
    }

    private fun del(bookSource: BookSourcePart) {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n" + bookSource.bookSourceName)
            noButton()
            yesButton {
                viewModel.del(listOf(bookSource))
            }
        }
    }

    private fun edit(bookSource: BookSourcePart) {
        startActivity<BookSourceEditActivity> {
            putExtra("sourceUrl", bookSource.bookSourceUrl)
        }
    }

    private fun toTop(bookSource: BookSourcePart) {
        if (sortAscending) {
            viewModel.topSource(bookSource)
        } else {
            viewModel.bottomSource(bookSource)
        }
    }

    private fun toBottom(bookSource: BookSourcePart) {
        if (sortAscending) {
            viewModel.bottomSource(bookSource)
        } else {
            viewModel.topSource(bookSource)
        }
    }

    private fun searchBook(bookSource: BookSourcePart) {
        startActivity<SearchActivity> {
            putExtra("searchScope", SearchScope(bookSource).toString())
        }
    }

    private fun debug(bookSource: BookSourcePart) {
        startActivity<BookSourceDebugActivity> {
            putExtra("key", bookSource.bookSourceUrl)
        }
    }

    override fun finish() {
        if (searchQuery.isEmpty()) {
            super.finish()
        } else {
            updateSearch("")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!Debug.isChecking) {
            Debug.debugMessageMap.clear()
        }
    }
}
