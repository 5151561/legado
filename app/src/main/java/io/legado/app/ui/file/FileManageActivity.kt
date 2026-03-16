package io.legado.app.ui.file

import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst
import io.legado.app.databinding.ActivityFileManageBinding
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.compose.theme.LegadoEmptyState
import io.legado.app.ui.compose.theme.LegadoItemDivider
import io.legado.app.ui.compose.theme.LegadoLeadingIcon
import io.legado.app.ui.compose.theme.LegadoListRow
import io.legado.app.ui.compose.theme.LegadoPageDefaults
import io.legado.app.ui.compose.theme.LegadoPageHeader
import io.legado.app.ui.compose.theme.LegadoSearchField
import io.legado.app.ui.compose.theme.LegadoSectionCard
import io.legado.app.utils.openFileUri
import io.legado.app.utils.viewbindingdelegate.viewBinding
import java.io.File

class FileManageActivity : VMBaseActivity<ActivityFileManageBinding, FileManageViewModel>() {

    override val binding by viewBinding(ActivityFileManageBinding::inflate)
    override val viewModel by viewModels<FileManageViewModel>()

    private val dirParent = ".."
    private var currentFiles by mutableStateOf<List<File>>(emptyList())
    private var searchQuery by mutableStateOf("")

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.composeFileManageContent.setContent {
            LegadoComposeTheme {
                val filteredFiles = if (searchQuery.isBlank()) {
                    currentFiles
                } else {
                    currentFiles.filter {
                        it.name == dirParent || it.name.contains(searchQuery, ignoreCase = true)
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = LegadoPageDefaults.HorizontalPadding,
                        end = LegadoPageDefaults.HorizontalPadding,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(LegadoPageDefaults.SectionSpacing)
                ) {
                    item {
                        LegadoPageHeader(
                            title = getString(R.string.file_manage),
                            subtitle = "查看应用目录与导入导出文件",
                            onBackClick = ::finish,
                            belowTitle = {
                                LegadoSearchField(
                                    query = searchQuery,
                                    placeholder = getString(R.string.screen) + " • " + getString(R.string.file_manage),
                                    onQueryChange = { searchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                )
                            }
                        )
                    }
                    item {
                        PathBreadcrumb(
                            paths = viewModel.subDocs.toList(),
                            onRootClick = {
                                viewModel.subDocs.clear()
                                viewModel.upFiles(viewModel.rootDoc)
                            },
                            onPathClick = { index ->
                                viewModel.subDocs = viewModel.subDocs.subList(0, index + 1)
                                viewModel.upFiles(viewModel.subDocs.lastOrNull())
                            }
                        )
                    }
                    if (filteredFiles.isEmpty()) {
                        item {
                            LegadoEmptyState(
                                title = getString(R.string.empty),
                                summary = "当前目录没有可显示的文件"
                            )
                        }
                    } else {
                        item {
                            LegadoSectionCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                                filteredFiles.forEachIndexed { index, item ->
                                    LegadoListRow(
                                        title = when {
                                            item == viewModel.lastDir -> dirParent
                                            else -> item.name
                                        },
                                        summary = when {
                                            item == viewModel.lastDir -> "返回上一级目录"
                                            item.isDirectory -> "文件夹"
                                            else -> "${item.extension.ifBlank { "文件" }} · ${item.length()} B"
                                        },
                                        leadingContent = {
                                            LegadoLeadingIcon(
                                                icon = when {
                                                    item == viewModel.lastDir -> Icons.Rounded.KeyboardArrowUp
                                                    item.isDirectory -> Icons.Rounded.Folder
                                                    else -> Icons.Rounded.Description
                                                }
                                            )
                                        },
                                        trailingContent = {
                                            if (item != viewModel.lastDir) {
                                                IconButton(onClick = { viewModel.delFile(item) }) {
                                                    Icon(Icons.Rounded.Delete, contentDescription = null)
                                                }
                                            }
                                        },
                                        onClick = { handleFileClick(item) }
                                    )
                                    if (index != filteredFiles.lastIndex) {
                                        LegadoItemDivider()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        onBackPressedDispatcher.addCallback(this) {
            if (viewModel.lastDir != viewModel.rootDoc) {
                gotoLastDir()
                return@addCallback
            }
            finish()
        }
        viewModel.upFiles(viewModel.rootDoc)
    }

    override fun observeLiveBus() {
        viewModel.filesLiveData.observe(this) {
            currentFiles = it
            searchQuery = ""
        }
    }

    private fun handleFileClick(item: File) {
        when {
            item == viewModel.lastDir -> gotoLastDir()
            item.isDirectory -> {
                viewModel.subDocs.add(item)
                viewModel.upFiles(item)
            }
            else -> {
                openFileUri(
                    FileProvider.getUriForFile(
                        this,
                        AppConst.authority,
                        item
                    )
                )
            }
        }
    }

    private fun gotoLastDir() {
        viewModel.subDocs.removeLastOrNull()
        viewModel.upFiles(viewModel.lastDir)
    }
}

@Composable
private fun PathBreadcrumb(
    paths: List<File>,
    onRootClick: () -> Unit,
    onPathClick: (Int) -> Unit
) {
    val scrollState = rememberScrollState()
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BreadcrumbChip(label = "root", onClick = onRootClick)
            paths.forEachIndexed { index, file ->
                BreadcrumbChip(label = file.name, onClick = { onPathClick(index) })
            }
        }
    }
}

@Composable
private fun BreadcrumbChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge
        )
    }
}
