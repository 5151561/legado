package io.legado.app.ui.about

import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.data.appDb
import io.legado.app.data.entities.ReadRecordShow
import io.legado.app.databinding.ActivityReadRecordBinding
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.LocalConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.book.search.SearchActivity
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.compose.theme.LegadoItemDivider
import io.legado.app.ui.compose.theme.LegadoListRow
import io.legado.app.ui.compose.theme.LegadoMenuButton
import io.legado.app.ui.compose.theme.LegadoPageDefaults
import io.legado.app.ui.compose.theme.LegadoPageHeader
import io.legado.app.ui.compose.theme.LegadoSearchField
import io.legado.app.ui.compose.theme.LegadoSectionCard
import io.legado.app.ui.compose.theme.LegadoStatusChip
import io.legado.app.utils.startActivityForBook
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class ReadRecordActivity : BaseActivity<ActivityReadRecordBinding>() {

    override val binding by viewBinding(ActivityReadRecordBinding::inflate)

    private var records by mutableStateOf<List<ReadRecordShow>>(emptyList())
    private var allTime by mutableLongStateOf(0L)
    private var searchQuery by mutableStateOf("")
    private var showSearch by mutableStateOf(false)
    private var sortMode
        get() = LocalConfig.getInt("readRecordSort", 0)
        set(value) {
            LocalConfig.edit { putInt("readRecordSort", value) }
        }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.composeReadRecordContent.setContent {
            LegadoComposeTheme {
                LazyColumn(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = LegadoPageDefaults.HorizontalPadding,
                        end = LegadoPageDefaults.HorizontalPadding,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(LegadoPageDefaults.SectionSpacing)
                ) {
                    item {
                        LegadoPageHeader(
                            title = getString(R.string.read_record),
                            subtitle = "阅读历史与累计时长",
                            onBackClick = ::finish,
                            actions = {
                                IconButton(onClick = { showSearch = !showSearch }) {
                                    Icon(
                                        imageVector = if (showSearch) Icons.Filled.SearchOff else Icons.Filled.Search,
                                        contentDescription = "搜索"
                                    )
                                }
                                LegadoMenuButton(
                                    icon = { Icon(Icons.Rounded.MoreVert, contentDescription = null) }
                                ) { dismiss ->
                                    DropdownMenuItem(
                                        text = { Text("按书名排序") },
                                        onClick = {
                                            sortMode = 0
                                            refreshData()
                                            dismiss()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("按阅读时长排序") },
                                        onClick = {
                                            sortMode = 1
                                            refreshData()
                                            dismiss()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("按最近阅读排序") },
                                        onClick = {
                                            sortMode = 2
                                            refreshData()
                                            dismiss()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (AppConfig.enableReadRecord) "关闭阅读记录"
                                                else "开启阅读记录"
                                            )
                                        },
                                        onClick = {
                                            AppConfig.enableReadRecord = !AppConfig.enableReadRecord
                                            dismiss()
                                        }
                                    )
                                }
                            },
                            belowTitle = {
                                AnimatedVisibility(visible = showSearch || searchQuery.isNotBlank()) {
                                    LegadoSearchField(
                                        query = searchQuery,
                                        placeholder = getString(R.string.search),
                                        onQueryChange = {
                                            searchQuery = it
                                            refreshData(it)
                                        },
                                        modifier = androidx.compose.ui.Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                    )
                                }
                            }
                        )
                    }
                    item {
                        LegadoSectionCard {
                            LegadoListRow(
                                title = getString(R.string.all_read_time),
                                summary = formatDuring(allTime),
                                trailingContent = {
                                    IconButton(onClick = ::clearAllRecords) {
                                        Icon(Icons.Rounded.Delete, contentDescription = null)
                                    }
                                },
                                onClick = {}
                            )
                        }
                    }
                    item {
                        LegadoSectionCard(contentPadding = PaddingValues(vertical = 4.dp)) {
                            records.forEachIndexed { index, item ->
                                LegadoListRow(
                                    title = item.bookName,
                                    summary = buildString {
                                        append("阅读时长 ${formatDuring(item.readTime)}")
                                        if (item.lastRead > 0) {
                                            append(" · 最近 ")
                                            append(dateFormat.format(item.lastRead))
                                        }
                                    },
                                    trailingContent = {
                                        LegadoStatusChip(text = formatDuring(item.readTime))
                                        IconButton(onClick = { deleteRecord(item) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = null)
                                        }
                                    },
                                    onClick = { openRecord(item) }
                                )
                                if (index != records.lastIndex) {
                                    LegadoItemDivider(startIndent = 20)
                                }
                            }
                        }
                    }
                }
            }
        }
        refreshAllTime()
        refreshData()
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun refreshAllTime() {
        lifecycleScope.launch {
            allTime = withContext(IO) { appDb.readRecordDao.allTime }
        }
    }

    private fun refreshData(searchKey: String? = searchQuery) {
        lifecycleScope.launch {
            records = withContext(IO) {
                appDb.readRecordDao.search(searchKey ?: "").let { list ->
                    when (sortMode) {
                        1 -> list.sortedByDescending { it.readTime }
                        2 -> list.sortedByDescending { it.lastRead }
                        else -> list.sortedWith { o1, o2 ->
                            o1.bookName.compareTo(o2.bookName, ignoreCase = true)
                        }
                    }
                }
            }
        }
    }

    private fun clearAllRecords() {
        alert(R.string.delete, R.string.sure_del) {
            yesButton {
                appDb.readRecordDao.clear()
                refreshAllTime()
                refreshData()
            }
            noButton()
        }
    }

    private fun deleteRecord(item: ReadRecordShow) {
        alert(R.string.delete) {
            setMessage(getString(R.string.sure_del_any, item.bookName))
            yesButton {
                appDb.readRecordDao.deleteByName(item.bookName)
                refreshAllTime()
                refreshData()
            }
            noButton()
        }
    }

    private fun openRecord(item: ReadRecordShow) {
        lifecycleScope.launch {
            val book = withContext(IO) {
                appDb.bookDao.findByName(item.bookName).firstOrNull()
            }
            if (book == null) {
                SearchActivity.start(this@ReadRecordActivity, item.bookName)
            } else {
                startActivityForBook(book)
            }
        }
    }

    fun formatDuring(mss: Long): String {
        val days = mss / (1000 * 60 * 60 * 24)
        val hours = mss % (1000 * 60 * 60 * 24) / (1000 * 60 * 60)
        val minutes = mss % (1000 * 60 * 60) / (1000 * 60)
        val seconds = mss % (1000 * 60) / 1000
        val d = if (days > 0) "${days}天" else ""
        val h = if (hours > 0) "${hours}小时" else ""
        val m = if (minutes > 0) "${minutes}分钟" else ""
        val s = if (seconds > 0) "${seconds}秒" else ""
        var time = "$d$h$m$s"
        if (time.isBlank()) {
            time = "0秒"
        }
        return time
    }
}
