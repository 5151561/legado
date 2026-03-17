package io.legado.app.ui.book.explore

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.base.VMBaseComposeActivity
import io.legado.app.ui.book.info.BookInfoActivity
import io.legado.app.ui.compose.theme.LegadoEmptyState
import io.legado.app.ui.compose.theme.LegadoItemDivider
import io.legado.app.ui.compose.theme.LegadoSmallAppBar
import io.legado.app.ui.compose.theme.SearchBookItem
import io.legado.app.utils.startActivity

/**
 * 发现列表
 */
class ExploreShowActivity : VMBaseComposeActivity<ExploreShowViewModel>() {

    override val viewModel by viewModels<ExploreShowViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        viewModel.initData(intent)
    }

    @Composable
    override fun Content() {
        val title = remember { intent.getStringExtra("exploreName") ?: "" }
        val books by viewModel.booksData.observeAsState(emptyList())
        val error by viewModel.errorLiveData.observeAsState()
        val listState = rememberLazyListState()

        Scaffold(
            topBar = {
                LegadoSmallAppBar(
                    title = title,
                    onBackClick = { finish() }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (books.isEmpty()) {
                    if (error == null) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        LegadoEmptyState(
                            title = getString(R.string.error),
                            summary = error ?: "",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(books) { book ->
                            SearchBookItem(
                                book = book,
                                isInBookshelf = viewModel.isInBookShelf(book),
                                onClick = {
                                    startActivity<BookInfoActivity> {
                                        putExtra("name", book.name)
                                        putExtra("author", book.author)
                                        putExtra("bookUrl", book.bookUrl)
                                    }
                                }
                            )
                            LegadoItemDivider()
                        }

                        item {
                            LaunchedEffect(books.size) {
                                viewModel.explore()
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (error != null) {
                                    Text(
                                        text = error!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
