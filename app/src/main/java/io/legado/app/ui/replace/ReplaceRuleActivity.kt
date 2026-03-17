package io.legado.app.ui.replace

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.databinding.ActivityReplaceRuleBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.DirectLinkUpload
import io.legado.app.help.book.ContentProcessor
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.association.ImportReplaceRuleDialog
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.compose.theme.LegadoMenuButton
import io.legado.app.ui.compose.theme.LegadoPageDefaults
import io.legado.app.ui.compose.theme.RuleManageItemUi
import io.legado.app.ui.compose.theme.RuleManageMaterialScreen
import io.legado.app.ui.compose.theme.RuleManageSheetAction
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.ui.replace.edit.ReplaceEditActivity
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class ReplaceRuleActivity : VMBaseActivity<ActivityReplaceRuleBinding, ReplaceRuleViewModel>() {

    override val binding by viewBinding(ActivityReplaceRuleBinding::inflate)
    override val viewModel by viewModels<ReplaceRuleViewModel>()

    private val importRecordKey = "replaceRuleRecordKey"
    private var groups by mutableStateOf<List<String>>(emptyList())
    private var replaceRules by mutableStateOf<List<ReplaceRule>>(emptyList())
    private var selectedIds by mutableStateOf<Set<Long>>(emptySet())
    private var searchQuery by mutableStateOf("")
    private var replaceRuleFlowJob: Job? = null
    private var dataInit = false

    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it ?: return@registerForActivityResult
        showDialogFragment(ImportReplaceRuleDialog(it))
    }
    private val editActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                setResult(RESULT_OK)
            }
        }
    private val importDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            showDialogFragment(ImportReplaceRuleDialog(uri.toString()))
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
        binding.composeReplaceRuleContent.setContent {
            LegadoComposeTheme {
                RuleManageMaterialScreen(
                    title = getString(R.string.replace_purify),
                    subtitle = "管理替换净化规则与分组过滤",
                    items = replaceRules.map {
                        RuleManageItemUi(
                            key = it.id.toString(),
                            title = it.name,
                            summary = buildString {
                                append(if (it.isRegex) "正则" else "文本")
                                if (it.pattern.isNotBlank()) {
                                    append(" · ")
                                    append(it.pattern)
                                }
                                if (it.replacement.isNotBlank()) {
                                    append(" → ")
                                    append(it.replacement)
                                }
                            },
                            enabled = it.isEnabled,
                            selected = selectedIds.contains(it.id),
                            badge = it.group
                        )
                    },
                    selectedCount = selectedIds.size,
                    onBackClick = ::finish,
                    onSelectAll = ::selectAll,
                    onInvertSelection = ::invertSelection,
                    onDeleteSelection = ::deleteSelection,
                    onItemClick = { item -> toggleSelection(item.key.toLong()) },
                    onToggleSelect = { item, checked ->
                        updateSelection(item.key.toLong(), checked)
                    },
                    onToggleEnabled = { item, checked ->
                        replaceRules.firstOrNull { it.id.toString() == item.key }?.let {
                            setResult(RESULT_OK)
                            it.isEnabled = checked
                            viewModel.update(it)
                        }
                    },
                    onEditItem = { item ->
                        setResult(RESULT_OK)
                        editActivity.launch(
                            ReplaceEditActivity.startIntent(this, item.key.toLong())
                        )
                    },
                    batchActions = listOf(
                        RuleManageSheetAction("enable", "批量启用", Icons.Rounded.KeyboardDoubleArrowUp),
                        RuleManageSheetAction("disable", "批量禁用", Icons.Rounded.KeyboardDoubleArrowDown),
                        RuleManageSheetAction("top", "批量置顶", Icons.Rounded.KeyboardDoubleArrowUp),
                        RuleManageSheetAction("bottom", "批量置底", Icons.Rounded.KeyboardDoubleArrowDown),
                        RuleManageSheetAction("export", "导出选中", Icons.Rounded.Publish)
                    ),
                    onBatchAction = ::handleBatchAction,
                    useSearchAppBar = true,
                    searchQuery = searchQuery,
                    onSearchQueryChange = {
                        searchQuery = it
                        observeReplaceRuleData(it)
                    },
                    searchPlaceholder = getString(R.string.replace_purify_search),
                    topBarContent = {
                        IconButton(onClick = {
                            editActivity.launch(ReplaceEditActivity.startIntent(this@ReplaceRuleActivity))
                        }) {
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
                                text = { Text("分组管理") },
                                onClick = {
                                    showDialogFragment<GroupManageDialog>()
                                    dismiss()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("无分组") },
                                onClick = {
                                    searchQuery = getString(R.string.no_group)
                                    observeReplaceRuleData(searchQuery)
                                    dismiss()
                                }
                            )
                            groups.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text(group) },
                                    onClick = {
                                        searchQuery = "group:$group"
                                        observeReplaceRuleData(searchQuery)
                                        dismiss()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("帮助") },
                                onClick = {
                                    showHelp("replaceRuleHelp")
                                    dismiss()
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null)
                                }
                            )
                        }
                    },
                    itemMenuContent = { item, dismiss ->
                        DropdownMenuItem(
                            text = { Text("置顶") },
                            onClick = {
                                replaceRules.firstOrNull { it.id.toString() == item.key }?.let {
                                    setResult(RESULT_OK)
                                    viewModel.toTop(it)
                                }
                                dismiss()
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.KeyboardDoubleArrowUp, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("置底") },
                            onClick = {
                                replaceRules.firstOrNull { it.id.toString() == item.key }?.let {
                                    setResult(RESULT_OK)
                                    viewModel.toBottom(it)
                                }
                                dismiss()
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.KeyboardDoubleArrowDown, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = {
                                replaceRules.firstOrNull { it.id.toString() == item.key }?.let(::deleteRule)
                                dismiss()
                            }
                        )
                    }
                )
            }
        }
        observeReplaceRuleData()
        observeGroupData()
    }

    private val selectedRules: List<ReplaceRule>
        get() = replaceRules.filter { selectedIds.contains(it.id) }

    private fun observeReplaceRuleData(searchKey: String? = null) {
        dataInit = false
        replaceRuleFlowJob?.cancel()
        replaceRuleFlowJob = lifecycleScope.launch {
            when {
                searchKey.isNullOrEmpty() -> appDb.replaceRuleDao.flowAll()
                searchKey == getString(R.string.no_group) -> appDb.replaceRuleDao.flowNoGroup()
                searchKey.startsWith("group:") -> {
                    appDb.replaceRuleDao.flowGroupSearch("%${searchKey.substringAfter("group:")}%")
                }
                else -> appDb.replaceRuleDao.flowSearch("%$searchKey%")
            }.catch {
                AppLog.put("替换规则管理界面更新数据出错", it)
            }.flowOn(IO).conflate().collect {
                if (dataInit) {
                    setResult(Activity.RESULT_OK)
                }
                replaceRules = it
                selectedIds = selectedIds.intersect(it.mapTo(linkedSetOf()) { rule -> rule.id })
                dataInit = true
                delay(100)
            }
        }
    }

    private fun observeGroupData() {
        lifecycleScope.launch {
            appDb.replaceRuleDao.flowGroups().collect {
                groups = it
            }
        }
    }

    private fun updateSelection(id: Long, checked: Boolean) {
        selectedIds = selectedIds.toMutableSet().apply {
            if (checked) add(id) else remove(id)
        }
    }

    private fun toggleSelection(id: Long) {
        selectedIds = selectedIds.toMutableSet().apply {
            if (!add(id)) remove(id)
        }
    }

    private fun selectAll() {
        selectedIds = replaceRules.mapTo(linkedSetOf()) { it.id }
    }

    private fun invertSelection() {
        selectedIds = replaceRules.mapNotNullTo(linkedSetOf()) {
            if (selectedIds.contains(it.id)) null else it.id
        }
    }

    private fun deleteSelection() {
        if (selectedRules.isEmpty()) return
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
            yesButton {
                setResult(RESULT_OK)
                viewModel.delSelection(selectedRules)
            }
            noButton()
        }
    }

    private fun deleteRule(rule: ReplaceRule) {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n" + rule.name)
            noButton()
            yesButton {
                setResult(RESULT_OK)
                viewModel.delete(rule)
            }
        }
    }

    private fun handleBatchAction(action: String) {
        when (action) {
            "enable" -> viewModel.enableSelection(selectedRules)
            "disable" -> viewModel.disableSelection(selectedRules)
            "top" -> {
                setResult(RESULT_OK)
                viewModel.topSelect(selectedRules)
            }
            "bottom" -> {
                setResult(RESULT_OK)
                viewModel.bottomSelect(selectedRules)
            }
            "export" -> exportSelection()
        }
    }

    private fun exportSelection() {
        if (selectedRules.isEmpty()) return
        exportResult.launch {
            mode = HandleFileContract.EXPORT
            fileData = HandleFileContract.FileData(
                "exportReplaceRule.json",
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
                    showDialogFragment(ImportReplaceRuleDialog(it))
                }
            }
            cancelButton()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Coroutine.async { ContentProcessor.upReplaceRules() }
    }
}
