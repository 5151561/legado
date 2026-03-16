package io.legado.app.ui.main.rss

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.VMBaseFragment
import io.legado.app.constant.AppLog
import io.legado.app.data.AppDatabase
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssSource
import io.legado.app.databinding.FragmentRssBinding
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.main.MainFragmentInterface
import io.legado.app.ui.rss.article.RssSortActivity
import io.legado.app.ui.rss.favorites.RssFavoritesActivity
import io.legado.app.ui.rss.read.ReadRssActivity
import io.legado.app.ui.rss.source.edit.RssSourceEditActivity
import io.legado.app.ui.rss.source.manage.RssSourceActivity
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.utils.flowWithLifecycleAndDatabaseChange
import io.legado.app.utils.openUrl
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * 订阅界面
 */
class RssFragment() : VMBaseFragment<RssViewModel>(R.layout.fragment_rss),
    MainFragmentInterface {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")

    private val binding by viewBinding(FragmentRssBinding::bind)
    override val viewModel by viewModels<RssViewModel>()

    private var rssSources by mutableStateOf<List<RssSource>>(emptyList())
    private var rssFlowJob: Job? = null

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.composeView.setContent {
            LegadoComposeTheme {
                RssComposeScreen(
                    onSearchQueryChange = { upRssFlowJob(it) },
                    onRssClick = { openRss(it) },
                    onAction = ::onItemAction,
                    onTopAction = ::onTopAction,
                    rssSources = rssSources
                )
            }
        }
        upRssFlowJob()
    }

    private fun onTopAction(action: RssTopAction) {
        when (action) {
            RssTopAction.Star -> startActivity<RssFavoritesActivity>()
            RssTopAction.Config -> startActivity<RssSourceActivity>()
        }
    }

    private fun onItemAction(rssSource: RssSource, action: RssAction) {
        when (action) {
            RssAction.ToTop -> viewModel.topSource(rssSource)
            RssAction.Edit -> edit(rssSource)
            RssAction.Disable -> viewModel.disable(rssSource)
            RssAction.Delete -> del(rssSource)
        }
    }

    private fun upRssFlowJob(searchKey: String? = null) {
        rssFlowJob?.cancel()
        rssFlowJob = viewLifecycleOwner.lifecycleScope.launch {
            when {
                searchKey.isNullOrEmpty() -> appDb.rssSourceDao.flowEnabled()
                searchKey.startsWith("group:") -> {
                    val key = searchKey.substringAfter("group:")
                    appDb.rssSourceDao.flowEnabledByGroup(key)
                }
                else -> appDb.rssSourceDao.flowEnabled(searchKey)
            }.flowWithLifecycleAndDatabaseChange(
                viewLifecycleOwner.lifecycle,
                Lifecycle.State.RESUMED,
                AppDatabase.RSS_SOURCE_TABLE_NAME
            ).catch {
                AppLog.put("订阅界面更新数据出错", it)
            }.flowOn(IO).collect {
                rssSources = it
            }
        }
    }

    private fun openRss(rssSource: RssSource) {
        if (rssSource.singleUrl) {
            viewModel.getSingleUrl(rssSource) { url ->
                if (url.startsWith("http", true)) {
                    startActivity<ReadRssActivity> {
                        putExtra("title", rssSource.sourceName)
                        putExtra("origin", url)
                    }
                } else {
                    context?.openUrl(url)
                }
            }
        } else {
            startActivity<RssSortActivity> {
                putExtra("url", rssSource.sourceUrl)
            }
        }
    }

    private fun edit(rssSource: RssSource) {
        startActivity<RssSourceEditActivity> {
            putExtra("sourceUrl", rssSource.sourceUrl)
        }
    }

    private fun del(rssSource: RssSource) {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n" + rssSource.sourceName)
            noButton()
            yesButton {
                viewModel.del(rssSource)
            }
        }
    }
}
