package io.legado.app.ui.rss.favorites

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.data.appDb
import io.legado.app.data.entities.RssStar
import io.legado.app.databinding.ActivityRssFavoritesBinding
import io.legado.app.databinding.ItemRssArticleBinding
import io.legado.app.help.glide.ImageLoader
import io.legado.app.help.glide.OkHttpModelLoader
import io.legado.app.lib.dialogs.alert
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.compose.theme.LegadoEmptyState
import io.legado.app.ui.compose.theme.LegadoMenuButton
import io.legado.app.ui.compose.theme.LegadoSmallAppBar
import io.legado.app.ui.rss.read.ReadRssActivity
import io.legado.app.utils.gone
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding
import io.legado.app.utils.visible
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

class RssFavoritesActivity : VMBaseActivity<ActivityRssFavoritesBinding, RssFavoritesViewModel>() {

    override val binding by viewBinding(ActivityRssFavoritesBinding::inflate)
    override val viewModel by viewModels<RssFavoritesViewModel>()

    private val groups = mutableStateListOf<String>()
    
    // Store loaded lists of items by group name
    private val groupItems = mutableStateMapOf<String, List<RssStar>>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.composeRssFavorites.setContent {
            LegadoComposeTheme {
                RssFavoritesScreen(
                    groups = groups,
                    groupItemsProvider = { group -> groupItems[group] ?: emptyList() },
                    onBackClick = { finish() },
                    onDeleteGroup = ::deleteGroup,
                    onDeleteAll = ::deleteAll,
                    onReadRss = { rssStar -> readRss(rssStar) },
                    onDeleteStar = { rssStar -> delStar(rssStar) }
                )
            }
        }

        lifecycleScope.launch {
            appDb.rssStarDao.flowGroups()
                .catch { }
                .distinctUntilChanged()
                .flowOn(IO)
                .collect { groupList ->
                    groups.clear()
                    groups.addAll(groupList)
                    // load content for all groups
                    groupList.forEach { group ->
                        loadGroupItems(group)
                    }
                }
        }
    }

    private fun loadGroupItems(group: String) {
        lifecycleScope.launch {
            appDb.rssStarDao.flowByGroup(group)
                .catch { }
                .flowOn(IO)
                .collect { items ->
                    groupItems[group] = items
                }
        }
    }

    private fun deleteGroup(group: String) {
        alert(R.string.draw) {
            setMessage(
                getString(R.string.sure_del) + "\n<" + group + ">" + getString(R.string.group)
            )
            noButton()
            yesButton {
                appDb.rssStarDao.deleteByGroup(group)
            }
        }
    }

    private fun deleteAll() {
        alert(R.string.draw) {
            setMessage(
                getString(R.string.sure_del) + "\n<" + getString(R.string.all) + ">"
                        + getString(R.string.favorite)
            )
            noButton()
            yesButton {
                appDb.rssStarDao.deleteAll()
            }
        }
    }

    private fun readRss(rssStar: RssStar) {
        startActivity<ReadRssActivity> {
            putExtra("title", rssStar.title)
            putExtra("origin", rssStar.origin)
            putExtra("link", rssStar.link)
        }
    }

    private fun delStar(rssStar: RssStar) {
        alert(R.string.draw) {
            setMessage(getString(R.string.sure_del) + "\n<" + rssStar.title + ">")
            noButton()
            yesButton {
                appDb.rssStarDao.delete(rssStar.origin, rssStar.link)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RssFavoritesScreen(
    groups: List<String>,
    groupItemsProvider: (String) -> List<RssStar>,
    onBackClick: () -> Unit,
    onDeleteGroup: (String) -> Unit,
    onDeleteAll: () -> Unit,
    onReadRss: (RssStar) -> Unit,
    onDeleteStar: (RssStar) -> Unit
) {
    if (groups.isEmpty()) {
        Scaffold(
            topBar = {
                LegadoSmallAppBar(
                    title = "收藏夹",
                    onBackClick = onBackClick
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                LegadoEmptyState(title = "没有任何收藏", summary = "在发现源的文章阅读界面点击收藏")
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { groups.size })
    val currentGroup = groups.getOrNull(pagerState.currentPage)

    Scaffold(
        topBar = {
            LegadoSmallAppBar(
                title = "收藏夹",
                onBackClick = onBackClick,
                actions = {
                    LegadoMenuButton(
                        icon = { Icon(Icons.Default.MoreVert, contentDescription = "更多") }
                    ) { dismiss ->
                        DropdownMenuItem(
                            text = { Text("删除当前分组") },
                            onClick = {
                                if (currentGroup != null) onDeleteGroup(currentGroup)
                                dismiss()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("清空所有收藏") },
                            onClick = {
                                onDeleteAll()
                                dismiss()
                            }
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
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 0.dp
            ) {
                groups.forEachIndexed { index, group ->
                    val coroutineScope = rememberCoroutineScope()
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        text = {
                            Text(
                                text = group,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { page ->
                val group = groups[page]
                val items = groupItemsProvider(group)

                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "分组内没有收藏文章",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(items) { item ->
                            RssStarItemCard(
                                item = item,
                                onClick = { onReadRss(item) },
                                onLongClick = { onDeleteStar(item) }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RssStarItemCard(
    item: RssStar,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // Capture colors in Composable context
    val titleColor = MaterialTheme.colorScheme.onSurface 
    val dateColor = MaterialTheme.colorScheme.onSurfaceVariant
    val titleColorArgb = titleColor.toArgb()
    val dateColorArgb = dateColor.toArgb()

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        factory = { context ->
            val inflater = LayoutInflater.from(context)
            val binding = ItemRssArticleBinding.inflate(inflater, null, false)
            binding.root.tag = binding
            binding.root
        },
        update = { view ->
            val binding = view.tag as ItemRssArticleBinding
            binding.run {
                tvTitle.text = item.title
                tvPubDate.text = item.pubDate
                
                tvTitle.setTextColor(titleColorArgb)
                tvPubDate.setTextColor(dateColorArgb)

                if (item.image.isNullOrBlank()) {
                    imageView.gone()
                } else {
                    val options = RequestOptions().set(OkHttpModelLoader.sourceOriginOption, item.origin)
                    ImageLoader.load(view.context, item.image)
                        .apply(options)
                        .addListener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                imageView.gone()
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>?,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                imageView.visible()
                                return false
                            }
                        })
                        .into(imageView)
                }
            }
        }
    )
}
