package io.legado.app.ui.book.bookmark

import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NoteAlt
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.unit.dp
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppLog
import io.legado.app.data.appDb
import io.legado.app.data.entities.Bookmark
import io.legado.app.databinding.ActivityAllBookmarkBinding
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.compose.theme.LegadoItemDivider
import io.legado.app.ui.compose.theme.LegadoListRow
import io.legado.app.ui.compose.theme.LegadoMenuButton
import io.legado.app.ui.compose.theme.LegadoPageDefaults
import io.legado.app.ui.compose.theme.LegadoPageHeader
import io.legado.app.ui.compose.theme.LegadoSectionCard
import io.legado.app.ui.compose.theme.LegadoSectionLabel
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class AllBookmarkActivity : VMBaseActivity<ActivityAllBookmarkBinding, AllBookmarkViewModel>() {

    override val viewModel by viewModels<AllBookmarkViewModel>()
    override val binding by viewBinding(ActivityAllBookmarkBinding::inflate)

    private var bookmarks by mutableStateOf<List<Bookmark>>(emptyList())

    private val exportDir = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            when (it.requestCode) {
                1 -> viewModel.exportBookmark(uri)
                2 -> viewModel.exportBookmarkMd(uri)
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.composeAllBookmarkContent.setContent {
            LegadoComposeTheme {
                val groups = bookmarks.groupBy { "${it.bookName}(${it.bookAuthor})" }
                LazyColumn(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = LegadoPageDefaults.HorizontalPadding,
                        top = 0.dp,
                        end = LegadoPageDefaults.HorizontalPadding,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(LegadoPageDefaults.SectionSpacing)
                ) {
                    item {
                        LegadoPageHeader(
                            title = getString(R.string.all_bookmark),
                            subtitle = "按书籍聚合查看所有书签",
                            onBackClick = ::finish,
                            actions = {
                                LegadoMenuButton(
                                    icon = { Icon(Icons.Rounded.MoreVert, contentDescription = null) }
                                ) { dismiss ->
                                    DropdownMenuItem(
                                        text = { Text(getString(R.string.export)) },
                                        onClick = {
                                            exportDir.launch { requestCode = 1 }
                                            dismiss()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.IosShare, contentDescription = null)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(getString(R.string.export_md)) },
                                        onClick = {
                                            exportDir.launch { requestCode = 2 }
                                            dismiss()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.NoteAlt, contentDescription = null)
                                        }
                                    )
                                }
                            }
                        )
                    }
                    groups.forEach { (book, items) ->
                        item { LegadoSectionLabel(book) }
                        item {
                            LegadoSectionCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                                items.forEachIndexed { index, item ->
                                    LegadoListRow(
                                        title = item.chapterName.ifBlank { item.bookName },
                                        summary = buildString {
                                            if (item.bookText.isNotBlank()) {
                                                append(item.bookText)
                                            }
                                            if (item.content.isNotBlank()) {
                                                if (isNotBlank()) append("\n")
                                                append(item.content)
                                            }
                                        }.ifBlank { "无摘录内容" },
                                        onClick = {
                                            showDialogFragment(BookmarkDialog(item, index))
                                        }
                                    )
                                    if (index != items.lastIndex) {
                                        LegadoItemDivider(startIndent = 20)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        observeData()
    }

    private fun observeData() {
        lifecycleScope.launch {
            appDb.bookmarkDao.flowAll().catch {
                AppLog.put("所有书签界面获取数据失败\n${it.localizedMessage}", it)
            }.flowOn(IO).collect {
                bookmarks = it
            }
        }
    }
}
