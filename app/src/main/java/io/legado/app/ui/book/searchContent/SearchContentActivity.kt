package io.legado.app.ui.book.searchContent

import android.content.Intent
import android.os.Bundle
import android.text.Spanned
import android.widget.TextView
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppLog
import io.legado.app.constant.EventBus
import io.legado.app.data.appDb
import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.databinding.ActivitySearchContentBinding
import io.legado.app.help.IntentData
import io.legado.app.help.book.BookHelp
import io.legado.app.help.book.isLocal
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.compose.theme.LegadoEmptyState
import io.legado.app.ui.compose.theme.LegadoPageDefaults
import io.legado.app.ui.compose.theme.LegadoSectionCard
import io.legado.app.ui.compose.theme.LegadoSmallAppBar
import io.legado.app.ui.compose.theme.LegadoTheme
import io.legado.app.utils.observeEvent
import io.legado.app.utils.postEvent
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchContentActivity :
    VMBaseActivity<ActivitySearchContentBinding, SearchContentViewModel>() {

    override val binding by viewBinding(ActivitySearchContentBinding::inflate)
    override val viewModel by viewModels<SearchContentViewModel>()

    private val searchResults = mutableStateListOf<SearchResult>()
    private var statusText by mutableStateOf("")
    private var queryText by mutableStateOf("")
    private var isSearching by mutableStateOf(false)
    private var replaceEnabled by mutableStateOf(false)
    private var initialScrollIndex by mutableIntStateOf(0)

    private var durChapterIndex = 0
    private var searchJob: Job? = null
    private var initJob: Job? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.composeSearchContent.setContent {
            LegadoComposeTheme {
                SearchContentScreen(
                    query = queryText,
                    statusText = statusText,
                    results = searchResults,
                    isSearching = isSearching,
                    replaceEnabled = replaceEnabled,
                    initialScrollIndex = initialScrollIndex,
                    onBackClick = ::finish,
                    onQueryChange = { queryText = it },
                    onSubmitSearch = { startContentSearch(it.trim()) },
                    onToggleReplace = {
                        replaceEnabled = it
                        viewModel.replaceEnabled = it
                    },
                    onStopSearch = { searchJob?.cancel() },
                    onResultClick = ::openSearchResult
                )
            }
        }

        val cachedResults = IntentData.get<List<SearchResult>>("searchResultList")
        initialScrollIndex = intent.getIntExtra("searchResultIndex", 0)
        val shouldAutoSearch = cachedResults == null
        val bookUrl = intent.getStringExtra("bookUrl") ?: return
        viewModel.initBook(bookUrl) {
            initSearchResultList(cachedResults)
            initBook(shouldAutoSearch)
        }
    }

    private fun initSearchResultList(list: List<SearchResult>?) {
        list ?: return
        searchResults.clear()
        searchResults.addAll(list)
        viewModel.searchResultList.clear()
        viewModel.searchResultList.addAll(list)
        viewModel.searchResultCounts = list.size
        statusText = getString(R.string.search_content_size) + ": ${viewModel.searchResultCounts}"
    }

    private fun initBook(submit: Boolean = true) {
        statusText = getString(R.string.search_content_size) + ": ${viewModel.searchResultCounts}"
        viewModel.book?.let {
            initCacheFileNames(it)
            durChapterIndex = it.durChapterIndex
            intent.getStringExtra("searchWord")?.let { searchWord ->
                queryText = searchWord
                if (submit) {
                    startContentSearch(searchWord)
                }
            }
        }
    }

    private fun initCacheFileNames(book: Book) {
        initJob = lifecycleScope.launch {
            withContext(IO) {
                viewModel.cacheChapterNames.addAll(BookHelp.getChapterFiles(book))
            }
        }
    }

    override fun observeLiveBus() {
        observeEvent<Pair<Book, BookChapter>>(EventBus.SAVE_CONTENT) { (book, chapter) ->
            viewModel.book?.bookUrl?.let { bookUrl ->
                if (book.bookUrl == bookUrl) {
                    viewModel.cacheChapterNames.add(chapter.getFileName())
                }
            }
        }
    }

    private fun startContentSearch(query: String) {
        if (query.isBlank()) return
        searchJob?.cancel()
        searchResults.clear()
        viewModel.searchResultList.clear()
        viewModel.searchResultCounts = 0
        viewModel.lastQuery = query
        queryText = query
        statusText = getString(R.string.search_content_size) + ": 0"
        isSearching = true
        initialScrollIndex = 0
        searchJob = lifecycleScope.launch(IO) {
            initJob?.join()
            kotlin.runCatching {
                appDb.bookChapterDao.getChapterList(viewModel.bookUrl).forEach { bookChapter ->
                    ensureActive()
                    val chapterResults = if (
                        isLocalBook || viewModel.cacheChapterNames.contains(bookChapter.getFileName())
                    ) {
                        viewModel.searchChapter(query, bookChapter)
                    } else {
                        return@forEach
                    }
                    ensureActive()
                    if (chapterResults.isNotEmpty()) {
                        viewModel.searchResultList.addAll(chapterResults)
                        withContext(kotlinx.coroutines.Dispatchers.Main) {
                            searchResults.addAll(chapterResults)
                            statusText = getString(R.string.search_content_size) + ": ${viewModel.searchResultCounts}"
                        }
                    }
                }
                if (viewModel.searchResultCounts == 0) {
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        searchResults.add(
                            SearchResult(resultText = getString(R.string.search_content_empty))
                        )
                    }
                }
            }.onFailure {
                AppLog.put("全文搜索出错\n${it.localizedMessage}", it)
            }
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                statusText = getString(R.string.search_content_size) + ": ${viewModel.searchResultCounts}"
                isSearching = false
            }
        }
    }

    private val isLocalBook: Boolean
        get() = viewModel.book?.isLocal == true

    private fun openSearchResult(searchResult: SearchResult, index: Int) {
        searchJob?.cancel()
        postEvent(EventBus.SEARCH_RESULT, viewModel.searchResultList as List<SearchResult>)
        val searchData = Intent()
        val key = System.currentTimeMillis()
        IntentData.put("searchResult$key", searchResult)
        IntentData.put("searchResultList$key", viewModel.searchResultList)
        searchData.putExtra("key", key)
        searchData.putExtra("index", index)
        setResult(RESULT_OK, searchData)
        finish()
    }
}

@Composable
private fun SearchContentScreen(
    query: String,
    statusText: String,
    results: List<SearchResult>,
    isSearching: Boolean,
    replaceEnabled: Boolean,
    initialScrollIndex: Int,
    onBackClick: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onToggleReplace: (Boolean) -> Unit,
    onStopSearch: () -> Unit,
    onResultClick: (SearchResult, Int) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(initialScrollIndex, results.size) {
        if (results.isNotEmpty() && initialScrollIndex in results.indices) {
            listState.scrollToItem(initialScrollIndex)
        }
    }

    Scaffold(
        topBar = {
            LegadoSmallAppBar(
                title = "全文搜索",
                onBackClick = onBackClick,
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoFixHigh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Switch(
                            checked = replaceEnabled,
                            onCheckedChange = onToggleReplace
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = {
                            if (results.isNotEmpty()) {
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    listState.animateScrollToItem(0)
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = null)
                    }
                    IconButton(
                        onClick = {
                            if (results.isNotEmpty()) {
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    listState.animateScrollToItem(results.lastIndex)
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
                    }
                }
            }
        },
        floatingActionButton = {
            if (isSearching) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = LegadoTheme.extendedColors.warning
                ) {
                    IconButton(onClick = onStopSearch) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = LegadoPageDefaults.HorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            LegadoSectionCard {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    placeholder = { Text("搜索正文内容") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "提交搜索后会按章节扫描并持续追加结果",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    onClick = { onSubmitSearch(query) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "开始搜索",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (results.isEmpty() && !isSearching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LegadoEmptyState(
                        title = "还没有搜索结果",
                        summary = "输入关键词后开始按章节搜索正文内容"
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(results) { index, item ->
                        SearchResultCard(
                            result = item,
                            onClick = { onResultClick(item, index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    result: SearchResult,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val textColorHex = colorLongToHex(MaterialTheme.colorScheme.onSurface)
    val accentColorHex = colorLongToHex(MaterialTheme.colorScheme.primary)
    val html = remember(result, textColorHex, accentColorHex) {
        result.getHtmlCompat(
            textColor = textColorHex,
            accentColor = accentColorHex
        )
    }
    LegadoSectionCard(
        contentPadding = PaddingValues(16.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { TextView(it) },
            update = { textView ->
                textView.text = html
                textView.textSize = 15f
                textView.linksClickable = true
                textView.setTextIsSelectable(true)
                textView.setOnClickListener {
                    if (result.query.isNotBlank()) {
                        onClick()
                    }
                }
            }
        )
    }
}

private fun colorLongToHex(color: androidx.compose.ui.graphics.Color): String {
    return color.toArgb().toUInt().toString(16).takeLast(6)
}
