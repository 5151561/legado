package io.legado.app.ui.rss.subscription

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.widget.AppCompatSpinner
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.BaseComposeActivity
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.RuleSub
import io.legado.app.databinding.DialogRuleSubEditBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.association.ImportBookSourceDialog
import io.legado.app.ui.association.ImportReplaceRuleDialog
import io.legado.app.ui.association.ImportRssSourceDialog
import io.legado.app.ui.compose.theme.LegadoEmptyState
import io.legado.app.ui.compose.theme.LegadoMenuButton
import io.legado.app.ui.compose.theme.LegadoSmallAppBar
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 规则订阅界面
 */
class RuleSubActivity : BaseComposeActivity(), RuleSubAdapter.Callback {

    private val ruleSubs = mutableStateListOf<RuleSub>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initData()
    }

    private fun initData() {
        lifecycleScope.launch {
            appDb.ruleSubDao.flowAll().catch {
                AppLog.put("规则订阅界面获取数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).conflate().collect {
                ruleSubs.clear()
                ruleSubs.addAll(it)
            }
        }
    }

    @Composable
    override fun Content() {
        Scaffold(
            topBar = {
                LegadoSmallAppBar(
                    title = stringResource(id = R.string.rule_subscription),
                    onBackClick = { finish() },
                    actions = {
                        IconButton(onClick = {
                            val order = appDb.ruleSubDao.maxOrder + 1
                            editSubscription(RuleSub(customOrder = order))
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "添加")
                        }
                    }
                )
            }
        ) { padding ->
            if (ruleSubs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    LegadoEmptyState(
                        title = stringResource(R.string.empty),
                        summary = "点击右上角添加规则一键导入链接"
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(ruleSubs, key = { it.id }) { ruleSub ->
                        RuleSubItem(
                            ruleSub = ruleSub,
                            onOpen = { openSubscription(ruleSub) },
                            onEdit = { editSubscription(ruleSub) },
                            onDelete = { delSubscription(ruleSub) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun RuleSubItem(
        ruleSub: RuleSub,
        onOpen: () -> Unit,
        onEdit: () -> Unit,
        onDelete: () -> Unit
    ) {
        val typeArray = stringArrayResource(id = R.array.rule_type)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = typeArray.getOrElse(ruleSub.type) { "未知" },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ruleSub.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ruleSub.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = onOpen) {
                Icon(Icons.Default.Edit, contentDescription = "导入", tint = MaterialTheme.colorScheme.primary)
            }
            
            LegadoMenuButton(
                icon = { Icon(Icons.Default.MoreVert, contentDescription = "更多") }
            ) { dismiss ->
                DropdownMenuItem(
                    text = { Text("编辑") },
                    onClick = { onEdit(); dismiss() }
                )
                DropdownMenuItem(
                    text = { Text("删除") },
                    onClick = { onDelete(); dismiss() }
                )
            }
        }
    }

    override fun openSubscription(ruleSub: RuleSub) {
        when (ruleSub.type) {
            0 -> showDialogFragment(ImportBookSourceDialog(ruleSub.url))
            1 -> showDialogFragment(ImportRssSourceDialog(ruleSub.url))
            2 -> showDialogFragment(ImportReplaceRuleDialog(ruleSub.url))
        }
    }

    override fun editSubscription(ruleSub: RuleSub) {
        alert(R.string.rule_subscription) {
            val alertBinding = DialogRuleSubEditBinding.inflate(layoutInflater).apply {
                val typeArray = resources.getStringArray(R.array.rule_type)
                spType.adapter = ArrayAdapter(this@RuleSubActivity, android.R.layout.simple_spinner_dropdown_item, typeArray)
                if (ruleSub.type !in typeArray.indices) {
                    ruleSub.type = 0
                }
                spType.setSelection(ruleSub.type)
                etName.setText(ruleSub.name)
                etUrl.setText(ruleSub.url)
            }
            customView { alertBinding.root }
            okButton {
                lifecycleScope.launch {
                    ruleSub.type = alertBinding.spType.selectedItemPosition
                    ruleSub.name = alertBinding.etName.text?.toString() ?: ""
                    ruleSub.url = alertBinding.etUrl.text?.toString() ?: ""
                    val rs = withContext(IO) {
                        appDb.ruleSubDao.findByUrl(ruleSub.url)
                    }
                    if (rs != null && rs.id != ruleSub.id) {
                        toastOnUi("${getString(R.string.url_already)}(${rs.name})")
                        return@launch
                    }
                    withContext(IO) {
                        appDb.ruleSubDao.insert(ruleSub)
                    }
                }
            }
            cancelButton()
        }
    }

    override fun delSubscription(ruleSub: RuleSub) {
        lifecycleScope.launch(IO) {
            appDb.ruleSubDao.delete(ruleSub)
        }
    }

    override fun updateSourceSub(vararg ruleSub: RuleSub) {
        lifecycleScope.launch(IO) {
            appDb.ruleSubDao.update(*ruleSub)
        }
    }

    override fun upOrder() {
        lifecycleScope.launch(IO) {
            val sourceSubs = appDb.ruleSubDao.all
            for ((index: Int, ruleSub: RuleSub) in sourceSubs.withIndex()) {
                ruleSub.customOrder = index + 1
            }
            appDb.ruleSubDao.update(*sourceSubs.toTypedArray())
        }
    }
}