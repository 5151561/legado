package io.legado.app.ui.main.explore

import android.os.Bundle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.outlined.ManageSearch
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.AppLog
import io.legado.app.data.AppDatabase
import io.legado.app.data.appDb
import io.legado.app.data.entities.BookSourcePart
import io.legado.app.data.entities.rule.ExploreKind
import io.legado.app.databinding.FragmentExploreBinding

import io.legado.app.help.source.clearExploreKindsCache
import io.legado.app.help.source.exploreKinds
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.book.explore.ExploreShowActivity
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.book.search.SearchScope
import io.legado.app.ui.book.source.edit.BookSourceEditActivity
import io.legado.app.ui.compose.theme.LegadoBottomBar
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.compose.theme.LegadoEmptyState
import io.legado.app.ui.compose.theme.LegadoMenuButton
import io.legado.app.ui.compose.theme.LegadoSearchAppBar
import io.legado.app.ui.compose.theme.LegadoSectionCard
import io.legado.app.ui.main.MainFragmentInterface
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.utils.flowWithLifecycleAndDatabaseChange
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * 发现界面
 */
class ExploreFragment() : VMBaseFragment<ExploreViewModel>(R.layout.fragment_explore),
    MainFragmentInterface {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")

    override val viewModel by viewModels<ExploreViewModel>()
    private val binding by viewBinding(FragmentExploreBinding::bind)

    private val sources = mutableStateListOf<BookSourcePart>()
    private val groups = mutableStateListOf<String>()
    private val expandedSourceUrls = mutableStateListOf<String>()
    private val loadingSourceUrls = mutableStateListOf<String>()
    private val exploreKindsMap = mutableStateMapOf<String, List<ExploreKind>>()
    private val errorMessages = mutableStateMapOf<String, String>()

    private var searchQuery by mutableStateOf("")
    private var exploreFlowJob: Job? = null

    override fun onFragmentCreated(view: android.view.View, savedInstanceState: Bundle?) {
        binding.composeExplore.setContent {
            LegadoComposeTheme {
                ExploreScreen(
                    query = searchQuery,
                    sources = sources,
                    groups = groups,
                    expandedSourceUrls = expandedSourceUrls.toSet(),
                    loadingSourceUrls = loadingSourceUrls.toSet(),
                    exploreKindsProvider = { exploreKindsMap[it].orEmpty() },
                    errorMessageProvider = { errorMessages[it] },
                    onQueryChange = {
                        searchQuery = it
                        upExploreData(it)
                    },
                    onGroupClick = { group ->
                        val query = "group:$group"
                        searchQuery = query
                        upExploreData(query)
                    },
                    onClearGroupFilter = {
                        searchQuery = ""
                        upExploreData()
                    },
                    onToggleExpand = ::toggleExpand,
                    onKindClick = ::openExploreKind,
                    onEditSource = ::editSource,
                    onTopSource = ::topSource,
                    onSearchBook = ::searchBook,
                    onLoginSource = ::loginSource,
                    onRefreshSource = ::refreshSource,
                    onDeleteSource = ::deleteSource,
                    onBackToTop = ::scrollToTop
                )
            }
        }
        initGroupData()
        upExploreData()
    }

    private fun initGroupData() {
        viewLifecycleOwner.lifecycleScope.launch {
            appDb.bookSourceDao.flowExploreGroups()
                .flowWithLifecycleAndDatabaseChange(
                    viewLifecycleOwner.lifecycle,
                    Lifecycle.State.RESUMED,
                    AppDatabase.BOOK_SOURCE_TABLE_NAME
                )
                .conflate()
                .distinctUntilChanged()
                .collect {
                    groups.clear()
                    groups.addAll(it)
                    delay(500)
                }
        }
    }

    private fun upExploreData(searchKey: String? = null) {
        exploreFlowJob?.cancel()
        exploreFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            when {
                searchKey.isNullOrBlank() -> appDb.bookSourceDao.flowExplore()
                searchKey.startsWith("group:") -> {
                    val key = searchKey.substringAfter("group:")
                    appDb.bookSourceDao.flowGroupExplore(key)
                }

                else -> appDb.bookSourceDao.flowExplore(searchKey)
            }.flowWithLifecycleAndDatabaseChange(
                viewLifecycleOwner.lifecycle,
                Lifecycle.State.RESUMED,
                AppDatabase.BOOK_SOURCE_TABLE_NAME
            ).catch {
                AppLog.put("发现界面更新数据出错", it)
            }.conflate().flowOn(IO).collect {
                sources.clear()
                sources.addAll(it)
                expandedSourceUrls.retainAll(it.map(BookSourcePart::bookSourceUrl).toSet())
                loadingSourceUrls.retainAll(it.map(BookSourcePart::bookSourceUrl).toSet())
                delay(500)
            }
        }
    }

    private fun toggleExpand(source: BookSourcePart) {
        val url = source.bookSourceUrl
        if (expandedSourceUrls.contains(url)) {
            expandedSourceUrls.remove(url)
            return
        }
        expandedSourceUrls.clear()
        expandedSourceUrls.add(url)
        if (!exploreKindsMap.containsKey(url) && !loadingSourceUrls.contains(url)) {
            loadExploreKinds(source)
        }
    }

    private fun loadExploreKinds(source: BookSourcePart) {
        val url = source.bookSourceUrl
        loadingSourceUrls.remove(url)
        loadingSourceUrls.add(url)
        errorMessages.remove(url)
        viewLifecycleOwner.lifecycleScope.launch {
            kotlin.runCatching {
                source.exploreKinds()
            }.onSuccess { kinds ->
                exploreKindsMap[url] = kinds
            }.onFailure {
                AppLog.put("加载发现分类失败", it)
                errorMessages[url] = it.localizedMessage ?: "加载失败"
                exploreKindsMap[url] = emptyList()
            }
            loadingSourceUrls.remove(url)
        }
    }

    private fun openExploreKind(source: BookSourcePart, kind: ExploreKind) {
        val exploreUrl = kind.url ?: return
        if (kind.title.startsWith("ERROR:")) {
            errorMessages[source.bookSourceUrl] = exploreUrl
            return
        }
        startActivity<ExploreShowActivity> {
            putExtra("exploreName", kind.title)
            putExtra("sourceUrl", source.bookSourceUrl)
            putExtra("exploreUrl", exploreUrl)
        }
    }

    private fun editSource(sourceUrl: String) {
        startActivity<BookSourceEditActivity> {
            putExtra("sourceUrl", sourceUrl)
        }
    }

    private fun topSource(source: BookSourcePart) {
        viewModel.topSource(source)
    }

    private fun deleteSource(source: BookSourcePart) {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n" + source.bookSourceName)
            noButton()
            yesButton {
                viewModel.deleteSource(source)
            }
        }
    }

    private fun searchBook(bookSource: BookSourcePart) {
        startActivity<SearchActivity> {
            putExtra("searchScope", SearchScope(bookSource).toString())
        }
    }

    private fun loginSource(source: BookSourcePart) {
        startActivity<SourceLoginActivity> {
            putExtra("type", "bookSource")
            putExtra("key", source.bookSourceUrl)
        }
    }

    private fun refreshSource(source: BookSourcePart) {
        viewLifecycleOwner.lifecycleScope.launch {
            kotlin.runCatching {
                source.clearExploreKindsCache()
                source.exploreKinds()
            }.onSuccess { kinds ->
                exploreKindsMap[source.bookSourceUrl] = kinds
                expandedSourceUrls.clear()
                expandedSourceUrls.add(source.bookSourceUrl)
            }.onFailure {
                AppLog.put("刷新发现分类失败", it)
                errorMessages[source.bookSourceUrl] = it.localizedMessage ?: "刷新失败"
            }
        }
    }

    private fun scrollToTop() {
        expandedSourceUrls.clear()
    }

    fun compressExplore() {
        if (expandedSourceUrls.isEmpty()) {
            scrollToTop()
        } else {
            expandedSourceUrls.clear()
        }
    }
}

@Composable
private fun ExploreScreen(
    query: String,
    sources: List<BookSourcePart>,
    groups: List<String>,
    expandedSourceUrls: Set<String>,
    loadingSourceUrls: Set<String>,
    exploreKindsProvider: (String) -> List<ExploreKind>,
    errorMessageProvider: (String) -> String?,
    onQueryChange: (String) -> Unit,
    onGroupClick: (String) -> Unit,
    onClearGroupFilter: () -> Unit,
    onToggleExpand: (BookSourcePart) -> Unit,
    onKindClick: (BookSourcePart, ExploreKind) -> Unit,
    onEditSource: (String) -> Unit,
    onTopSource: (BookSourcePart) -> Unit,
    onSearchBook: (BookSourcePart) -> Unit,
    onLoginSource: (BookSourcePart) -> Unit,
    onRefreshSource: (BookSourcePart) -> Unit,
    onDeleteSource: (BookSourcePart) -> Unit,
    onBackToTop: () -> Unit
) {
    val listState = rememberLazyListState()
    val isGroupFilter = query.startsWith("group:")

    Scaffold(
        topBar = {
            LegadoSearchAppBar(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = "搜索发现源或输入 group:分组",
                actions = {
                    if (groups.isNotEmpty()) {
                        LegadoMenuButton(
                            icon = { Icon(Icons.Default.Groups, contentDescription = "分组筛选") }
                        ) { dismiss ->
                            if (isGroupFilter) {
                                DropdownMenuItem(
                                    text = { Text("清除分组筛选") },
                                    onClick = {
                                        onClearGroupFilter()
                                        dismiss()
                                    }
                                )
                                HorizontalDivider()
                            }
                            groups.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text(group) },
                                    onClick = {
                                        onGroupClick(group)
                                        dismiss()
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            LegadoBottomBar {
                Text(
                    text = if (isGroupFilter) {
                        "分组筛选中，共 ${sources.size} 个发现源"
                    } else {
                        "共 ${sources.size} 个发现源"
                    },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.material3.IconButton(
                    onClick = { onBackToTop() }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardDoubleArrowUp,
                        contentDescription = "收起并回到顶部"
                    )
                }
            }
        }
    ) { innerPadding ->
        if (sources.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                LegadoEmptyState(
                    title = if (query.isBlank()) "还没有可用发现源" else "没有匹配的发现源",
                    summary = if (query.isBlank()) "请先启用带发现入口的书源" else "换个关键词或分组再试一次"
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = sources,
                    key = { it.bookSourceUrl }
                ) { source ->
                    ExploreSourceCard(
                        source = source,
                        expanded = expandedSourceUrls.contains(source.bookSourceUrl),
                        loading = loadingSourceUrls.contains(source.bookSourceUrl),
                        kinds = exploreKindsProvider(source.bookSourceUrl),
                        errorMessage = errorMessageProvider(source.bookSourceUrl),
                        onToggleExpand = { onToggleExpand(source) },
                        onKindClick = { onKindClick(source, it) },
                        onEditSource = { onEditSource(source.bookSourceUrl) },
                        onTopSource = { onTopSource(source) },
                        onSearchBook = { onSearchBook(source) },
                        onLoginSource = { onLoginSource(source) },
                        onRefreshSource = { onRefreshSource(source) },
                        onDeleteSource = { onDeleteSource(source) }
                    )
                }
            }
        }
    }

    LaunchedEffect(expandedSourceUrls) {
        val expandedUrl = expandedSourceUrls.firstOrNull() ?: return@LaunchedEffect
        val index = sources.indexOfFirst { it.bookSourceUrl == expandedUrl }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ExploreSourceCard(
    source: BookSourcePart,
    expanded: Boolean,
    loading: Boolean,
    kinds: List<ExploreKind>,
    errorMessage: String?,
    onToggleExpand: () -> Unit,
    onKindClick: (ExploreKind) -> Unit,
    onEditSource: () -> Unit,
    onTopSource: () -> Unit,
    onSearchBook: () -> Unit,
    onLoginSource: () -> Unit,
    onRefreshSource: () -> Unit,
    onDeleteSource: () -> Unit
) {
    LegadoSectionCard(
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onToggleExpand,
                    onLongClick = {}
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.bookSourceName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = source.bookSourceGroup?.takeIf { it.isNotBlank() } ?: "未分组",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LegadoMenuButton(
                icon = { Icon(Icons.Default.MoreVert, contentDescription = "更多操作") }
            ) { dismiss ->
                DropdownMenuItem(
                    text = { Text("编辑") },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = {
                        onEditSource()
                        dismiss()
                    }
                )
                DropdownMenuItem(
                    text = { Text("置顶") },
                    leadingIcon = { Icon(Icons.Default.VerticalAlignTop, contentDescription = null) },
                    onClick = {
                        onTopSource()
                        dismiss()
                    }
                )
                DropdownMenuItem(
                    text = { Text("搜索") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    onClick = {
                        onSearchBook()
                        dismiss()
                    }
                )
                if (source.hasLoginUrl) {
                    DropdownMenuItem(
                        text = { Text("登录") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null) },
                        onClick = {
                            onLoginSource()
                            dismiss()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("刷新分类") },
                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                    onClick = {
                        onRefreshSource()
                        dismiss()
                    }
                )
                DropdownMenuItem(
                    text = { Text("删除") },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    onClick = {
                        onDeleteSource()
                        dismiss()
                    }
                )
            }
        }
        if (expanded) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            if (loading) {
                Text(
                    text = "正在加载分类…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (!errorMessage.isNullOrBlank()) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else if (kinds.isEmpty()) {
                Text(
                    text = "这个发现源没有可用分类",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    kinds.forEach { kind ->
                        ExploreKindChip(
                            kind = kind,
                            onClick = { onKindClick(kind) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExploreKindChip(
    kind: ExploreKind,
    onClick: () -> Unit
) {
    val isError = kind.title.startsWith("ERROR:")
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = kind.title,
                fontWeight = if (isError) FontWeight.SemiBold else FontWeight.Medium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = if (isError) Icons.AutoMirrored.Outlined.ManageSearch else Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    )
}
