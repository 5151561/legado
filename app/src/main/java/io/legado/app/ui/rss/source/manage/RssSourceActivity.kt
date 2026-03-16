package io.legado.app.ui.rss.source.manage

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssSource
import io.legado.app.databinding.ActivityRssSourceBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.DirectLinkUpload
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.association.ImportRssSourceDialog
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.utils.ACache
import io.legado.app.utils.dpToPx
import io.legado.app.utils.isAbsUrl
import io.legado.app.utils.launch
import io.legado.app.utils.sendToClip
import io.legado.app.utils.share
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.showHelp
import io.legado.app.utils.splitNotBlank
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * 订阅源管理
 */
class RssSourceActivity : VMBaseActivity<ActivityRssSourceBinding, RssSourceViewModel>() {

    override val binding by viewBinding(ActivityRssSourceBinding::inflate)
    override val viewModel by viewModels<RssSourceViewModel>()
    private val importRecordKey = "rssSourceRecordKey"

    private var searchQuery by mutableStateOf("")
    private var sources by mutableStateOf<List<RssSource>>(emptyList())
    private var selectedUrls by mutableStateOf<Set<String>>(emptySet())
    private var groups by mutableStateOf<List<String>>(emptyList())

    private var sourceFlowJob: Job? = null

    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it ?: return@registerForActivityResult
        showDialogFragment(ImportRssSourceDialog(it))
    }
    private val importDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            showDialogFragment(ImportRssSourceDialog(uri.toString()))
        }
    }
    private val exportResult = registerForActivityResult(HandleFileContract()) {
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
        binding.composeRssSourceContent.setContent {
            LegadoComposeTheme {
                RssSourceScreen(
                    sources = sources,
                    selectedUrls = selectedUrls,
                    searchQuery = searchQuery,
                    groups = groups,
                    onSearchQueryChange = ::onSearchQueryChange,
                    onBackClick = { finish() },
                    onTopAction = ::onTopAction,
                    onGroupFilterClick = { updateSearch("group:$it") },
                    onToggleSelect = ::toggleSelection,
                    onToggleEnable = { source, enabled ->
                        val newSource = source.copy(enabled = enabled)
                        viewModel.update(newSource)
                    },
                    onEditClick = ::edit,
                    onItemAction = ::onItemAction,
                    onSelectAll = ::toggleSelectAll,
                    onInvertSelection = ::revertSelection,
                    onDeleteSelected = ::deleteSelected,
                    onBatchAction = ::onBatchAction
                )
            }
        }
        upSourceFlow()
        initGroupFlow()
    }

    private fun onSearchQueryChange(query: String) {
        searchQuery = query
        upSourceFlow(query)
    }

    private fun updateSearch(query: String) {
        searchQuery = query
        upSourceFlow(query)
    }

    private fun onTopAction(action: RssSourceTopAction) {
        when (action) {
            RssSourceTopAction.Add -> startActivity<RssSourceEditActivity>()
            RssSourceTopAction.ImportLocal -> importDoc.launch {
                mode = HandleFileContract.FILE
                allowExtensions = arrayOf("txt", "json")
            }
            RssSourceTopAction.ImportOnline -> showImportDialog()
            RssSourceTopAction.ImportQr -> qrCodeResult.launch()
            RssSourceTopAction.GroupManage -> showDialogFragment<GroupManageDialog>()
            RssSourceTopAction.ImportDefault -> viewModel.importDefault()
            RssSourceTopAction.Help -> showHelp("SourceMRssHelp")
            RssSourceTopAction.FilterEnabled -> updateSearch(getString(R.string.enabled))
            RssSourceTopAction.FilterDisabled -> updateSearch(getString(R.string.disabled))
            RssSourceTopAction.FilterNeedLogin -> updateSearch(getString(R.string.need_login))
            RssSourceTopAction.FilterNoGroup -> updateSearch(getString(R.string.no_group))
        }
    }

    private fun onItemAction(source: RssSource, action: RssSourceItemAction) {
        when (action) {
            RssSourceItemAction.Delete -> del(source)
            RssSourceItemAction.ToTop -> viewModel.topSource(source)
            RssSourceItemAction.ToBottom -> viewModel.bottomSource(source)
        }
    }

    private fun onBatchAction(action: RssSourceBatchAction) {
        when (action) {
            RssSourceBatchAction.EnableSelected -> viewModel.enableSelection(selection)
            RssSourceBatchAction.DisableSelected -> viewModel.disableSelection(selection)
            RssSourceBatchAction.AddGroup -> selectionAddToGroups()
            RssSourceBatchAction.RemoveGroup -> selectionRemoveFromGroups()
            RssSourceBatchAction.TopSelected -> viewModel.topSource(*selection.toTypedArray())
            RssSourceBatchAction.BottomSelected -> viewModel.bottomSource(*selection.toTypedArray())
            RssSourceBatchAction.ExportSelected -> exportSelection()
            RssSourceBatchAction.ShareSelected -> shareSelection()
            RssSourceBatchAction.CheckSelectedInterval -> checkSelectedInterval()
        }
    }

    private val selection: List<RssSource>
        get() = sources.filter { selectedUrls.contains(it.sourceUrl) }

    private fun toggleSelection(source: RssSource) {
        selectedUrls = selectedUrls.toMutableSet().apply {
            if (!add(source.sourceUrl)) {
                remove(source.sourceUrl)
            }
        }
    }

    private fun toggleSelectAll() {
        selectedUrls = if (sources.isNotEmpty() && selectedUrls.size == sources.size) {
            emptySet()
        } else {
            sources.mapTo(mutableSetOf()) { it.sourceUrl }
        }
    }

    private fun revertSelection() {
        selectedUrls = sources.mapNotNullTo(mutableSetOf()) {
            if (selectedUrls.contains(it.sourceUrl)) null else it.sourceUrl
        }
    }

    private fun deleteSelected() {
        if (selection.isEmpty()) return
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
            yesButton { viewModel.del(*selection.toTypedArray()) }
            noButton()
        }
    }

    private fun checkSelectedInterval() {
        if (selection.isEmpty()) return
        val selectedPosition = sources.mapIndexedNotNull { index, item ->
            if (selectedUrls.contains(item.sourceUrl)) index else null
        }
        if (selectedPosition.isEmpty()) return
        val minPosition = selectedPosition.min()
        val maxPosition = selectedPosition.max()
        selectedUrls = sources.subList(minPosition, maxPosition + 1)
            .mapTo(mutableSetOf()) { it.sourceUrl }
    }

    private fun exportSelection() {
        viewModel.saveToFile(selection) { file ->
            exportResult.launch {
                mode = HandleFileContract.EXPORT
                fileData = HandleFileContract.FileData(
                    "exportRssSource.json", file, "application/json"
                )
            }
        }
    }

    private fun shareSelection() {
        viewModel.saveToFile(selection) {
            share(it)
        }
    }

    private fun initGroupFlow() {
        lifecycleScope.launch {
            appDb.rssSourceDao.flowGroups().conflate().collect {
                groups = it
            }
        }
    }

    private fun upSourceFlow(searchKey: String? = searchQuery) {
        sourceFlowJob?.cancel()
        sourceFlowJob = lifecycleScope.launch {
            when {
                searchKey.isNullOrBlank() -> {
                    appDb.rssSourceDao.flowAll()
                }
                searchKey == getString(R.string.enabled) -> {
                    appDb.rssSourceDao.flowEnabled()
                }
                searchKey == getString(R.string.disabled) -> {
                    appDb.rssSourceDao.flowDisabled()
                }
                searchKey == getString(R.string.need_login) -> {
                    appDb.rssSourceDao.flowLogin()
                }
                searchKey == getString(R.string.no_group) -> {
                    appDb.rssSourceDao.flowNoGroup()
                }
                searchKey.startsWith("group:") -> {
                    val key = searchKey.substringAfter("group:")
                    appDb.rssSourceDao.flowGroupSearch(key)
                }
                else -> {
                    appDb.rssSourceDao.flowSearch(searchKey)
                }
            }.catch {
                AppLog.put("订阅源管理界面更新数据出错", it)
            }.flowOn(IO).conflate().collect {
                sources = it
                selectedUrls = selectedUrls.filterTo(mutableSetOf()) { selectedUrl ->
                    it.any { source -> source.sourceUrl == selectedUrl }
                }
                delay(100)
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun selectionAddToGroups() {
        alert(titleResource = R.string.add_group) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.setHint(R.string.group_name)
                editView.setFilterValues(groups)
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
                editView.setFilterValues(groups)
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
                    showDialogFragment(ImportRssSourceDialog(it))
                }
            }
            cancelButton()
        }
    }

    private fun del(source: RssSource) {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n" + source.sourceName)
            noButton()
            yesButton {
                viewModel.del(source)
            }
        }
    }

    private fun edit(source: RssSource) {
        startActivity<RssSourceEditActivity> {
            putExtra("sourceUrl", source.sourceUrl)
        }
    }

    override fun finish() {
        if (searchQuery.isEmpty()) {
            super.finish()
        } else {
            updateSearch("")
        }
    }
}
