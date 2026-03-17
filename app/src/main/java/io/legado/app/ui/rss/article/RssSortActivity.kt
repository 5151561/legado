@file:Suppress("DEPRECATION")

package io.legado.app.ui.rss.article

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.legado.app.R
import io.legado.app.base.VMBaseComposeActivity
import io.legado.app.data.appDb
import io.legado.app.help.source.sortUrls
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.compose.theme.*
import io.legado.app.ui.login.SourceLoginActivity
import io.legado.app.ui.rss.read.ReadRssActivity
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.ui.widget.dialog.VariableDialog
import io.legado.app.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RssSortActivity : VMBaseComposeActivity<RssSortViewModel>(),
    VariableDialog.Callback {

    override val viewModel by viewModels<RssSortViewModel>()
    private val sortList = mutableStateListOf<Pair<String, String>>()
    
    private val editSourceResult = registerForActivityResult(
        StartActivityContract(RssSourceEditActivity::class.java)
    ) {
        if (it.resultCode == RESULT_OK) {
            viewModel.initData(intent) {
                upFragments()
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.initData(intent) {
            upFragments()
        }
    }

    private fun upFragments() {
        lifecycleScope.launch {
            viewModel.rssSource?.sortUrls()?.let {
                sortList.clear()
                sortList.addAll(it)
            }
        }
    }

    @Composable
    override fun Content() {
        val title by viewModel.titleLiveData.observeAsState(stringResource(R.string.rss))
        
        Scaffold(
            topBar = {
                LegadoSmallAppBar(
                    title = title,
                    onBackClick = { finish() },
                    actions = {
                        LegadoMenuButton(
                            icon = { Icon(Icons.Default.MoreVert, contentDescription = "更多") }
                        ) { dismiss ->
                            if (!viewModel.rssSource?.loginUrl.isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.login)) },
                                    onClick = {
                                        startActivity<SourceLoginActivity> {
                                            putExtra("type", "rssSource")
                                            putExtra("key", viewModel.rssSource?.sourceUrl)
                                        }
                                        dismiss()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.refresh_sort)) },
                                onClick = { viewModel.clearSortCache { upFragments() }; dismiss() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.set_source_variable)) },
                                onClick = { setSourceVariable(); dismiss() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit_source)) },
                                onClick = {
                                    viewModel.rssSource?.sourceUrl?.let {
                                        editSourceResult.launch { putExtra("sourceUrl", it) }
                                    }
                                    dismiss()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.clear)) },
                                onClick = { viewModel.clearArticles(); dismiss() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.read_record)) },
                                onClick = { showDialogFragment<ReadRecordDialog>(); dismiss() }
                            )
                        }
                    }
                )
            }
        ) { padding ->
            if (sortList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    val pagerState = rememberPagerState { sortList.size }
                    
                    if (sortList.size > 1) {
                        ScrollableTabRow(
                            selectedTabIndex = pagerState.currentPage,
                            edgePadding = 0.dp,
                            containerColor = MaterialTheme.colorScheme.surface,
                            divider = {}
                        ) {
                            sortList.forEachIndexed { index, pair ->
                                val coroutineScope = rememberCoroutineScope()
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                                    text = { Text(pair.first) }
                                )
                            }
                        }
                    }
                    
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f)
                    ) { pageIdx ->
                        val sort = sortList[pageIdx]
                        RssArticlesPage(sort.first, sort.second)
                    }
                }
            }
        }
    }

    @Composable
    private fun RssArticlesPage(sortName: String, sortUrl: String) {
        val pageViewModel = viewModel<RssArticlesViewModel>(key = sortUrl)
        val articles by appDb.rssArticleDao.flowByOriginSort(viewModel.url ?: "", sortName).collectAsState(emptyList())
        val hasMore by pageViewModel.loadFinallyLiveData.observeAsState(true)
        val error by pageViewModel.loadErrorLiveData.observeAsState()
        
        LaunchedEffect(sortUrl) {
            pageViewModel.sortName = sortName
            pageViewModel.sortUrl = sortUrl
            viewModel.rssSource?.let { pageViewModel.loadArticles(it) }
        }
        
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(articles) { article ->
                RssArticleItem(
                    article = article,
                    onClick = {
                        viewModel.read(article)
                        startActivity<ReadRssActivity> {
                            putExtra("title", article.title)
                            putExtra("origin", article.origin)
                            putExtra("link", article.link)
                        }
                    }
                )
                LegadoItemDivider()
            }
            
            item {
                if (hasMore) {
                    LaunchedEffect(articles.size) {
                        viewModel.rssSource?.let { pageViewModel.loadMore(it) }
                    }
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (articles.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.no_find), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    private fun setSourceVariable() {
        lifecycleScope.launch {
            val source = viewModel.rssSource ?: return@launch
            val comment = source.getDisplayVariableComment("源变量可在js中通过source.getVariable()获取")
            val variable = withContext(Dispatchers.IO) { source.getVariable() }
            showDialogFragment(
                VariableDialog(
                    getString(R.string.set_source_variable),
                    source.getKey(),
                    variable,
                    comment
                )
            )
        }
    }

    override fun setVariable(key: String, variable: String?) {
        viewModel.rssSource?.setVariable(variable)
    }

}
