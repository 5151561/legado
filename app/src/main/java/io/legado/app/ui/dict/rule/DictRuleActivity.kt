package io.legado.app.ui.dict.rule

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowDown
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.DictRule
import io.legado.app.databinding.ActivityDictRuleBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.DirectLinkUpload
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.association.ImportDictRuleDialog
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.compose.theme.LegadoMenuButton
import io.legado.app.ui.compose.theme.RuleManageItemUi
import io.legado.app.ui.compose.theme.RuleManageMaterialScreen
import io.legado.app.ui.compose.theme.RuleManageSheetAction
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.utils.ACache
import io.legado.app.utils.GSON
import io.legado.app.utils.isAbsUrl
import io.legado.app.utils.launch
import io.legado.app.utils.sendToClip
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.showHelp
import io.legado.app.utils.splitNotBlank
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class DictRuleActivity : VMBaseActivity<ActivityDictRuleBinding, DictRuleViewModel>() {

    override val viewModel by viewModels<DictRuleViewModel>()
    override val binding by viewBinding(ActivityDictRuleBinding::inflate)

    private val importRecordKey = "dictRuleUrls"
    private var dictRules by mutableStateOf<List<DictRule>>(emptyList())
    private var selectedNames by mutableStateOf<Set<String>>(emptySet())
    private var searchQuery by mutableStateOf("")
    private var showSearch by mutableStateOf(false)

    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it ?: return@registerForActivityResult
        showDialogFragment(ImportDictRuleDialog(it))
    }
    private val importDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            showDialogFragment(ImportDictRuleDialog(uri.toString()))
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
        binding.composeDictRuleContent.setContent {
            LegadoComposeTheme {
                val filteredRules = dictRules.filter {
                    searchQuery.isBlank() ||
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.urlRule.contains(searchQuery, ignoreCase = true)
                }
                RuleManageMaterialScreen(
                    title = getString(R.string.dict_rule),
                    subtitle = "管理词典规则与启用状态",
                    items = filteredRules.map {
                        RuleManageItemUi(
                            key = it.name,
                            title = it.name,
                            summary = it.urlRule.ifBlank { "未配置检索地址" },
                            enabled = it.enabled,
                            selected = selectedNames.contains(it.name)
                        )
                    },
                    selectedCount = selectedNames.size,
                    onBackClick = ::finish,
                    onSelectAll = ::selectAll,
                    onInvertSelection = ::invertSelection,
                    onDeleteSelection = ::deleteSelection,
                    onItemClick = { item -> toggleSelection(item.key) },
                    onToggleSelect = { item, checked -> updateSelection(item.key, checked) },
                    onToggleEnabled = { item, checked ->
                        dictRules.firstOrNull { it.name == item.key }?.let {
                            it.enabled = checked
                            viewModel.update(it)
                        }
                    },
                    onEditItem = { item ->
                        showDialogFragment(DictRuleEditDialog(item.key))
                    },
                    batchActions = listOf(
                        RuleManageSheetAction("enable", "批量启用", Icons.Rounded.KeyboardDoubleArrowUp),
                        RuleManageSheetAction("disable", "批量禁用", Icons.Rounded.KeyboardDoubleArrowDown),
                        RuleManageSheetAction("export", "导出选中", Icons.Rounded.Publish)
                    ),
                    onBatchAction = ::handleBatchAction,
                    topBarContent = {
                        IconButton(onClick = { showSearch = !showSearch }) {
                            Icon(
                                imageVector = if (showSearch) Icons.Default.SearchOff else Icons.Default.Search,
                                contentDescription = "搜索"
                            )
                        }
                        IconButton(onClick = { showDialogFragment<DictRuleEditDialog>() }) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                        }
                        LegadoMenuButton(
                            icon = { Icon(Icons.Rounded.MoreVert, contentDescription = null) }
                        ) { dismiss ->
                            DropdownMenuItem(
                                text = { Text("本地导入") },
                                onClick = {
                                    importDoc.launch {
                                        mode = HandleFileContract.FILE
                                        allowExtensions = arrayOf("txt", "json")
                                    }
                                    dismiss()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.FileUpload, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("在线导入") },
                                onClick = {
                                    showImportDialog()
                                    dismiss()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Download, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("扫码导入") },
                                onClick = {
                                    qrCodeResult.launch()
                                    dismiss()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("导入默认规则") },
                                onClick = {
                                    viewModel.importDefault()
                                    dismiss()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Publish, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("帮助") },
                                onClick = {
                                    showHelp("dictRuleHelp")
                                    dismiss()
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null)
                                }
                            )
                        }
                    },
                    headerBottomContent = {
                        AnimatedVisibility(visible = showSearch || searchQuery.isNotBlank()) {
                            io.legado.app.ui.compose.theme.LegadoSearchField(
                                query = searchQuery,
                                placeholder = "搜索字典规则",
                                onQueryChange = { searchQuery = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                            )
                        }
                    },
                    itemMenuContent = { item, dismiss ->
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = {
                                dictRules.firstOrNull { it.name == item.key }?.let(::deleteRule)
                                dismiss()
                            }
                        )
                    }
                )
            }
        }
        observeData()
    }

    private val selectedRules: List<DictRule>
        get() = dictRules.filter { selectedNames.contains(it.name) }

    private fun observeData() {
        lifecycleScope.launch {
            appDb.dictRuleDao.flowAll().catch {
                AppLog.put("字典规则获取数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).collect {
                dictRules = it
                selectedNames = selectedNames.intersect(it.mapTo(linkedSetOf()) { rule -> rule.name })
            }
        }
    }

    private fun updateSelection(name: String, checked: Boolean) {
        selectedNames = selectedNames.toMutableSet().apply {
            if (checked) add(name) else remove(name)
        }
    }

    private fun toggleSelection(name: String) {
        selectedNames = selectedNames.toMutableSet().apply {
            if (!add(name)) remove(name)
        }
    }

    private fun selectAll() {
        selectedNames = dictRules.mapTo(linkedSetOf()) { it.name }
    }

    private fun invertSelection() {
        selectedNames = dictRules.mapNotNullTo(linkedSetOf()) {
            if (selectedNames.contains(it.name)) null else it.name
        }
    }

    private fun deleteSelection() {
        if (selectedRules.isEmpty()) return
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
            yesButton { viewModel.delete(*selectedRules.toTypedArray()) }
            noButton()
        }
    }

    private fun deleteRule(rule: DictRule) {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n" + rule.name)
            noButton()
            yesButton { viewModel.delete(rule) }
        }
    }

    private fun handleBatchAction(action: String) {
        when (action) {
            "enable" -> viewModel.enableSelection(*selectedRules.toTypedArray())
            "disable" -> viewModel.disableSelection(*selectedRules.toTypedArray())
            "export" -> exportSelection()
        }
    }

    private fun exportSelection() {
        if (selectedRules.isEmpty()) return
        exportResult.launch {
            mode = HandleFileContract.EXPORT
            fileData = HandleFileContract.FileData(
                "exportDictRule.json",
                GSON.toJson(selectedRules).toByteArray(),
                "application/json"
            )
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
                    showDialogFragment(ImportDictRuleDialog(it))
                }
            }
            cancelButton()
        }
    }
}
