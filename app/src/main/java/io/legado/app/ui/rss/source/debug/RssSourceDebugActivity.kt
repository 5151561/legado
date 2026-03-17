package io.legado.app.ui.rss.source.debug

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.databinding.ActivitySourceDebugBinding
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.compose.theme.LegadoMenuButton
import io.legado.app.ui.compose.theme.LegadoSmallAppBar
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch

class RssSourceDebugActivity : VMBaseActivity<ActivitySourceDebugBinding, RssSourceDebugModel>() {

    override val binding by viewBinding(ActivitySourceDebugBinding::inflate)
    override val viewModel by viewModels<RssSourceDebugModel>()

    private val debugLogs = mutableStateListOf<String>()
    private var isSearching by mutableStateOf(false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.composeSourceDebug.setContent {
            LegadoComposeTheme {
                RssSourceDebugScreen(
                    logs = debugLogs,
                    isSearching = isSearching,
                    onBackClick = { finish() },
                    onActionMenuClick = ::handleActionMenuClick
                )
            }
        }

        viewModel.observe { state, msg ->
            lifecycleScope.launch {
                debugLogs.add(msg)
                if (state == -1 || state == 1000) {
                    isSearching = false
                }
            }
        }
        viewModel.initData(intent.getStringExtra("key")) {
            startDebug()
        }
    }

    private fun handleActionMenuClick(action: String) {
        when(action) {
            "list_src" -> showDialogFragment(TextDialog("Html", viewModel.listSrc))
            "content_src" -> showDialogFragment(TextDialog("Html", viewModel.contentSrc))
        }
    }

    private fun startDebug() {
        debugLogs.clear()
        viewModel.rssSource?.let {
            isSearching = true
            viewModel.startDebug(it)
        } ?: run {
            toastOnUi(R.string.error_no_source)
            isSearching = false
        }
    }
}

@Composable
private fun RssSourceDebugScreen(
    logs: List<String>,
    isSearching: Boolean,
    onBackClick: () -> Unit,
    onActionMenuClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            LegadoSmallAppBar(
                title = "调试源",
                onBackClick = onBackClick,
                actions = {
                    LegadoMenuButton(
                        icon = { Icon(Icons.Default.MoreVert, contentDescription = "更多") }
                    ) { dismiss ->
                        DropdownMenuItem(
                            text = { Text("列表源码") },
                            onClick = { onActionMenuClick("list_src"); dismiss() }
                        )
                        DropdownMenuItem(
                            text = { Text("正文源码") },
                            onClick = { onActionMenuClick("content_src"); dismiss() }
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
