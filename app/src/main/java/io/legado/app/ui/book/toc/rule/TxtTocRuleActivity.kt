package io.legado.app.ui.book.toc.rule

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
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
import io.legado.app.data.entities.TxtTocRule
import io.legado.app.databinding.ActivityTxtTocRuleBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.DirectLinkUpload
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.association.ImportTxtTocRuleDialog
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
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class TxtTocRuleActivity : VMBaseActivity<ActivityTxtTocRuleBinding, TxtTocRuleViewModel>(),
    TxtTocRuleEditDialog.Callback {

    override val viewModel by viewModels<TxtTocRuleViewModel>()
    override val binding by viewBinding(ActivityTxtTocRuleBinding::inflate)

    private val importTocRuleKey = "tocRuleUrl"
    private var tocRules by mutableStateOf<List<TxtTocRule>>(emptyList())
    private var selectedIds by mutableStateOf<Set<Long>>(emptySet())
    private var searchQuery by mutableStateOf("")

    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it ?: return@registerForActivityResult
        showDialogFragment(ImportTxtTocRuleDialog(it))
    }
    private val importDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            showDialogFragment(ImportTxtTocRuleDialog(uri.toString()))
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
        binding.composeTxtTocRuleContent.setContent {
            LegadoComposeTheme {
                val filteredRules = tocRules.filter {
                    searchQuery.isBlank() ||
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.example.orEmpty().contains(searchQuery, ignoreCase = true)
                }
                RuleManageMaterialScreen(
                    title = getString(R.string.txt_toc_rule),
                    subtitle = "集中管理 TXT 目录匹配规则",
                    items = filteredRules.map {
                        RuleManageItemUi(
                            key = it.id.toString(),
                            title = it.name,
                            summary = it.example.orEmpty().ifBlank { "未填写示例内容" },
                            enabled = it.enable,
                            selected = selectedIds.contains(it.id)
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
                        tocRules.firstOrNull { it.id.toString() == item.key }?.let {
                            it.enable = checked
                            viewModel.update(it)
                        }
                    },
                    onEditItem = { item ->
                        showDialogFragment(TxtTocRuleEditDialog(item.key.toLong()))
                    },
                    batchActions = listOf(
                        RuleManageSheetAction("enable", "批量启用", Icons.Rounded.KeyboardDoubleArrowUp),
                        RuleManageSheetAction("disable", "批量禁用", Icons.Rounded.KeyboardDoubleArrowDown),
                        RuleManageSheetAction("export", "导出选中", Icons.Rounded.Publish)
                    ),
                    onBatchAction = ::handleBatchAction,
                    useSmallAppBar = true,
                    topBarContent = {
                        IconButton(onClick = { showDialogFragment(TxtTocRuleEditDialog()) }) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                        }
                        LegadoMenuButton(
                            icon = { Icon(Icons.Rounded.MoreVert, contentDescription = null) }
                        ) { dismiss ->
                            DropdownMenuItem(
                                text = { androidx.compose.material3.Text("本地导入") },
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
                                text = { androidx.compose.material3.Text("在线导入") },
                                onClick = {
                                    showImportDialog()
                                    dismiss()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Download, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { androidx.compose.material3.Text("扫码导入") },
                                onClick = {
                                    qrCodeResult.launch()
                                    dismiss()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { androidx.compose.material3.Text("导入默认规则") },
                                onClick = {
                                    viewModel.importDefault()
                                    dismiss()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Publish, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { androidx.compose.material3.Text("帮助") },
                                onClick = {
                                    showHelp("txtTocRuleHelp")
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
                            text = { androidx.compose.material3.Text("置顶") },
                            onClick = {
                                tocRules.firstOrNull { it.id.toString() == item.key }?.let {
                                    viewModel.toTop(it)
                                }
                                dismiss()
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.KeyboardDoubleArrowUp, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { androidx.compose.material3.Text("置底") },
                            onClick = {
                                tocRules.firstOrNull { it.id.toString() == item.key }?.let {
                                    viewModel.toBottom(it)
                                }
                                dismiss()
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.KeyboardDoubleArrowDown, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { androidx.compose.material3.Text("删除") },
                            onClick = {
                                tocRules.firstOrNull { it.id.toString() == item.key }?.let(::deleteRule)
                                dismiss()
                            }
                        )
                    }
                )
            }
        }
        observeData()
    }

    private val selectedRules: List<TxtTocRule>
        get() = tocRules.filter { selectedIds.contains(it.id) }

    private fun observeData() {
        lifecycleScope.launch {
            appDb.txtTocRuleDao.observeAll().catch {
                AppLog.put("TXT目录规则界面获取数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).conflate().collect { rules ->
                tocRules = rules
                selectedIds = selectedIds.intersect(rules.mapTo(linkedSetOf()) { it.id })
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
        selectedIds = tocRules.mapTo(linkedSetOf()) { it.id }
    }

    private fun invertSelection() {
        selectedIds = tocRules.mapNotNullTo(linkedSetOf()) {
            if (selectedIds.contains(it.id)) null else it.id
        }
    }

    private fun deleteSelection() {
        if (selectedRules.isEmpty()) return
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
            yesButton { viewModel.del(*selectedRules.toTypedArray()) }
            noButton()
        }
    }

    private fun deleteRule(rule: TxtTocRule) {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n" + rule.name)
            noButton()
            yesButton { viewModel.del(rule) }
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
                "exportTxtTocRule.json",
                GSON.toJson(selectedRules).toByteArray(),
                "application/json"
            )
        }
    }

    override fun saveTxtTocRule(txtTocRule: TxtTocRule) {
        viewModel.save(txtTocRule)
    }

    @SuppressLint("InflateParams")
    private fun showImportDialog() {
        val aCache = ACache.get(cacheDir = false)
        val defaultUrl = "https://gitee.com/fisher52/YueDuJson/raw/master/myTxtChapterRule.json"
        val cacheUrls: MutableList<String> = aCache
            .getAsString(importTocRuleKey)
            ?.splitNotBlank(",")
            ?.toMutableList()
            ?: mutableListOf()
        if (!cacheUrls.contains(defaultUrl)) {
            cacheUrls.add(0, defaultUrl)
        }
        alert(titleResource = R.string.import_on_line) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url"
                editView.setFilterValues(cacheUrls)
                editView.delCallBack = {
                    cacheUrls.remove(it)
                    aCache.put(importTocRuleKey, cacheUrls.joinToString(","))
                }
            }
            customView { alertBinding.root }
            okButton {
                val text = alertBinding.editView.text?.toString()
                text?.let {
                    if (it.isAbsUrl() && !cacheUrls.contains(it)) {
                        cacheUrls.add(0, it)
                        aCache.put(importTocRuleKey, cacheUrls.joinToString(","))
                    }
                    showDialogFragment(ImportTxtTocRuleDialog(it))
                }
            }
            cancelButton()
        }
    }
}
