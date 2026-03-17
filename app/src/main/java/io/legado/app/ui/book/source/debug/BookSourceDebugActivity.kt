package io.legado.app.ui.book.source.debug

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivitySourceDebugBinding
import io.legado.app.help.source.clearExploreKindsCache
import io.legado.app.help.source.exploreKinds
import io.legado.app.lib.dialogs.selector
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.compose.theme.LegadoMenuButton
import io.legado.app.ui.compose.theme.LegadoSectionCard
import io.legado.app.ui.compose.theme.LegadoSmallAppBar
import io.legado.app.ui.qrcode.QrCodeResult
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.launch
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.showHelp
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch

class BookSourceDebugActivity : VMBaseActivity<ActivitySourceDebugBinding, BookSourceDebugModel>() {

    override val binding by viewBinding(ActivitySourceDebugBinding::inflate)
    override val viewModel by viewModels<BookSourceDebugModel>()

    private val debugLogs = mutableStateListOf<String>()
    private var searchQuery by mutableStateOf("")
    private var isSearching by mutableStateOf(false)
    private var isHelpExpanded by mutableStateOf(true)
    private var exploreKindsText by mutableStateOf("系统::http://xxx")
    private var myKeywordText by mutableStateOf("我的")
    
    private val exploreKindTitles = mutableStateListOf<String>()
    private val exploreKindsUrls = mutableStateListOf<String>()

    private val qrCodeResult = registerForActivityResult(QrCodeResult()) {
        it?.let {
            searchQuery = it
            startSearch(it)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.composeSourceDebug.setContent {
            LegadoComposeTheme {
                BookSourceDebugScreen(
                    query = searchQuery,
                    logs = debugLogs,
                    isSearching = isSearching,
                    isHelpExpanded = isHelpExpanded,
                    myKeyword = myKeywordText,
                    exploreKindsText = exploreKindsText,
                    onQueryChange = { searchQuery = it },
                    onSubmitSearch = { startSearch(it) },
                    onFocusChange = { hasFocus -> 
                        isHelpExpanded = hasFocus
                    },
                    onHelpItemClick = { prefix, fillOnly ->
                        if (fillOnly) {
                            setSearchQuery(prefix, false)
                        } else {
                            if (searchQuery.isBlank() || searchQuery.length <= 2) {
                                setSearchQuery(prefix, false)
                            } else {
                                if (!searchQuery.startsWith(prefix)) {
                                    setSearchQuery("$prefix$searchQuery", true)
                                } else {
                                    setSearchQuery(searchQuery, true)
                                }
                            }
                        }
                    },
                    onExploreLongClick = {
                        if (exploreKindTitles.isNotEmpty()) {
                            selector("选择发现", exploreKindTitles) { _, index ->
                                val title = exploreKindTitles[index]
                                val url = exploreKindsUrls[index]
                                exploreKindsText = "$title::$url"
                                setSearchQuery(exploreKindsText, true)
                            }
                        }
                    },
                    onBackClick = { finish() },
                    onActionMenuClick = ::handleActionMenuClick
                )
            }
        }

        viewModel.init(intent.getStringExtra("key")) {
            initHelpData()
        }
        viewModel.observe { state, msg ->
            lifecycleScope.launch {
                debugLogs.add(msg)
                if (state == -1 || state == 1000) {
                    isSearching = false
                }
            }
        }
    }

    private fun handleActionMenuClick(action: String) {
        when(action) {
            "scan" -> qrCodeResult.launch()
            "search_src" -> showDialogFragment(TextDialog("html", viewModel.searchSrc))
            "book_src" -> showDialogFragment(TextDialog("html", viewModel.bookSrc))
            "toc_src" -> showDialogFragment(TextDialog("html", viewModel.tocSrc))
            "content_src" -> showDialogFragment(TextDialog("html", viewModel.contentSrc))
            "refresh_explore" -> {
                lifecycleScope.launch {
                    viewModel.bookSource?.clearExploreKindsCache()
                    debugLogs.clear()
                    isHelpExpanded = true
                    initExploreKinds()
                }
            }
            "help" -> showHelp("debugHelp")
        }
    }

    private fun setSearchQuery(query: String, submit: Boolean) {
        searchQuery = query
        if (submit) {
            startSearch(query)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initHelpData() {
        viewModel.bookSource?.ruleSearch?.checkKeyWord?.let {
            if (it.isNotBlank()) {
                myKeywordText = it
            }
        }
        initExploreKinds()
    }

    @SuppressLint("SetTextI18n")
    private fun initExploreKinds() {
        lifecycleScope.launch {
            try {
                exploreKindTitles.clear()
                exploreKindsUrls.clear()
                val exploreKinds = viewModel.bookSource?.exploreKinds()?.filter {
                    !it.url.isNullOrBlank()
                }
                exploreKinds?.firstOrNull()?.let {
                    exploreKindsText = "${it.title}::${it.url}"
                    if (it.title.startsWith("ERROR:")) {
                        debugLogs.add("获取发现出错\n${it.url}")
                        isHelpExpanded = false
                        return@launch
                    }
                }
                exploreKinds?.forEach { 
                    exploreKindTitles.add(it.title ?: "")
                    exploreKindsUrls.add(it.url ?: "")
                }
            } catch (e: NullPointerException) {
                debugLogs.add("获取发现出错 JSON 数据错误\n$e")
                isHelpExpanded = false
            }
        }
    }

    private fun startSearch(key: String) {
        if (key.isBlank()) return
        isHelpExpanded = false
        debugLogs.clear()
        viewModel.startDebug(key, {
            isSearching = true
        }, {
            toastOnUi("未获取到书源")
            isSearching = false
        })
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun BookSourceDebugScreen(
    query: String,
    logs: List<String>,
    isSearching: Boolean,
    isHelpExpanded: Boolean,
    myKeyword: String,
    exploreKindsText: String,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onHelpItemClick: (String, Boolean) -> Unit,
    onExploreLongClick: () -> Unit,
    onBackClick: () -> Unit,
    onActionMenuClick: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            LegadoSmallAppBar(
                title = "调试书源",
                onBackClick = onBackClick,
                actions = {
                    LegadoMenuButton(
                        icon = { Icon(Icons.Default.MoreVert, contentDescription = "更多") }
                    ) { dismiss ->
                        DropdownMenuItem(
                            text = { Text("扫源码") },
                            onClick = { onActionMenuClick("scan"); dismiss() }
                        )
                        DropdownMenuItem(
                            text = { Text("搜索源码") },
                            onClick = { onActionMenuClick("search_src"); dismiss() }
                        )
                        DropdownMenuItem(
                            text = { Text("详情源码") },
                            onClick = { onActionMenuClick("book_src"); dismiss() }
                        )
                        DropdownMenuItem(
                            text = { Text("目录源码") },
                            onClick = { onActionMenuClick("toc_src"); dismiss() }
                        )
                        DropdownMenuItem(
                            text = { Text("正文源码") },
                            onClick = { onActionMenuClick("content_src"); dismiss() }
                        )
                        DropdownMenuItem(
                            text = { Text("刷新发现") },
                            onClick = { onActionMenuClick("refresh_explore"); dismiss() }
                        )
                        DropdownMenuItem(
                            text = { Text("帮助") },
                            onClick = { onActionMenuClick("help"); dismiss() }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LegadoSectionCard(
                modifier = Modifier.padding(16.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { onFocusChange(it.isFocused) },
                    placeholder = { Text("输入调试书名/发现/目录") },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { 
                            focusManager.clearFocus()
                            onSubmitSearch(query) 
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = { 
                            focusManager.clearFocus()
                            onSubmitSearch(query) 
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                    }
                )

                AnimatedVisibility(visible = isHelpExpanded) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        Text(
                            text = "调试搜索>>输入关键字，如：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = { onHelpItemClick(myKeyword, true) },
                                label = { Text(myKeyword) }
                            )
                            AssistChip(
                                onClick = { onHelpItemClick("系统", true) },
                                label = { Text("系统") }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "调试发现>>输入发现URL，如：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.combinedClickable(
                                onClick = { 
                                    if (!exploreKindsText.startsWith("ERROR:")) {
                                        onHelpItemClick(exploreKindsText, true)
                                    }
                                },
                                onLongClick = onExploreLongClick
                            )
                        ) {
                            Text(
                                text = exploreKindsText,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "调试详情页>>输入详情页URL，如：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AssistChip(
                            onClick = { onHelpItemClick("https://m.qidian.com/book/1015609210", true) },
                            label = { Text("https://m.qidian.com/book/1015609210") }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "调试目录页>>输入目录页URL，如：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AssistChip(
                            onClick = { onHelpItemClick("++", false) },
                            label = { Text("++https://www.zhaishuyuan.com/read/30394") }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "调试正文页>>输入正文页URL，如：",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AssistChip(
                            onClick = { onHelpItemClick("--", false) },
                            label = { Text("--https://www.zhaishuyuan.com/chapter/30394/20940996") }
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isSearching) {
                Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(logs) { log ->
                    SelectionContainer {
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
