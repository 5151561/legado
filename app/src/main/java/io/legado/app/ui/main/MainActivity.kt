package io.legado.app.ui.main

import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.postDelayed
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import io.legado.app.BuildConfig
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppConst.appInfo
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.ActivityMainBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.AppWebDav
import io.legado.app.help.book.BookHelp
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.LocalConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.storage.Backup
import io.legado.app.lib.dialogs.alert
import io.legado.app.service.BaseReadAloudService
import io.legado.app.ui.about.CrashLogsDialog
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.main.bookshelf.BaseBookshelfFragment
import io.legado.app.ui.main.bookshelf.style1.BookshelfFragment1
import io.legado.app.ui.main.bookshelf.style2.BookshelfFragment2
import io.legado.app.ui.main.explore.ExploreFragment
import io.legado.app.ui.main.my.MyFragment
import io.legado.app.ui.main.rss.RssFragment
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.utils.observeEvent
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * 主界面
 */
@Suppress("PrivatePropertyName")
class MainActivity : VMBaseActivity<ActivityMainBinding, MainViewModel>() {

    override val binding by viewBinding(ActivityMainBinding::inflate)
    override val viewModel by viewModels<MainViewModel>()

    private val idBookshelf1 = 11
    private val idBookshelf2 = 12
    private val idExplore = 1
    private val idRss = 2
    private val idMy = 3
    private val fragmentContainerId = View.generateViewId()
    private val fragmentMap = hashMapOf<Int, Fragment>()

    private var exitTime: Long = 0
    private var bookshelfReselected: Long = 0
    private var exploreReselected: Long = 0
    private var isFragmentContainerReady = false
    private val exitInterval = 2000L

    private var navigationItems by mutableStateOf(emptyList<MainNavigationItem>())
    private var selectedMenuId by mutableIntStateOf(R.id.menu_bookshelf)
    private var upBooksBadgeCount by mutableIntStateOf(0)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initComposeContent()
        refreshNavigationItems()
        applyDefaultHomePage()
        onBackPressedDispatcher.addCallback(this) {
            val firstMenuId = navigationItems.firstOrNull()?.menuId ?: R.id.menu_bookshelf
            if (selectedMenuId != firstMenuId) {
                selectMenu(firstMenuId)
                return@addCallback
            }
            (fragmentMap[currentBookshelfFragmentId()] as? BookshelfFragment2)?.let {
                if (it.back()) {
                    return@addCallback
                }
            }
            if (System.currentTimeMillis() - exitTime > exitInterval) {
                toastOnUi(R.string.double_click_exit)
                exitTime = System.currentTimeMillis()
            } else {
                if (BaseReadAloudService.pause) {
                    finish()
                } else {
                    moveTaskToBack(true)
                }
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        lifecycleScope.launch {
            if (!privacyPolicy()) return@launch
            upVersion()
            setLocalPassword()
            notifyAppCrash()
            backupSync()
            val isAutoRefreshedBook = savedInstanceState?.getBoolean("isAutoRefreshedBook") ?: false
            if (AppConfig.autoRefreshBook && !isAutoRefreshedBook) {
                binding.composeViewMain.postDelayed(1000L) {
                    viewModel.upAllBookToc()
                }
            }
            binding.composeViewMain.postDelayed(3000L) {
                viewModel.postLoad()
            }
        }
    }

    private fun initComposeContent() {
        binding.composeViewMain.setContent {
            LegadoComposeTheme {
                MainActivityScreen(
                    items = navigationItems,
                    selectedMenuId = selectedMenuId,
                    bookshelfBadgeCount = upBooksBadgeCount,
                    fragmentContainerId = fragmentContainerId,
                    onContainerReady = ::onFragmentContainerReady,
                    onItemClick = ::onNavigationItemClick
                )
            }
        }
    }

    private fun onFragmentContainerReady() {
        if (isFragmentContainerReady) return
        isFragmentContainerReady = true
        syncFragments()
    }

    private fun onNavigationItemClick(menuId: Int) {
        if (menuId == selectedMenuId) {
            onNavigationItemReselected(menuId)
        } else {
            selectMenu(menuId)
        }
    }

    private fun onNavigationItemReselected(menuId: Int) {
        when (menuId) {
            R.id.menu_bookshelf -> {
                if (System.currentTimeMillis() - bookshelfReselected > 300) {
                    bookshelfReselected = System.currentTimeMillis()
                } else {
                    (fragmentMap[currentBookshelfFragmentId()] as? BaseBookshelfFragment)?.gotoTop()
                }
            }

            R.id.menu_discovery -> {
                if (System.currentTimeMillis() - exploreReselected > 300) {
                    exploreReselected = System.currentTimeMillis()
                } else {
                    (fragmentMap[idExplore] as? ExploreFragment)?.compressExplore()
                }
            }
        }
    }

    private fun selectMenu(menuId: Int) {
        selectedMenuId = menuId
        syncFragments()
    }

    private fun refreshNavigationItems(selectLast: Boolean = false) {
        val newItems = buildNavigationItems()
        navigationItems = newItems
        val selectedExists = newItems.any { it.menuId == selectedMenuId }
        selectedMenuId = when {
            selectLast -> newItems.lastOrNull()?.menuId ?: R.id.menu_bookshelf
            selectedExists -> selectedMenuId
            else -> newItems.firstOrNull()?.menuId ?: R.id.menu_bookshelf
        }
        if (!AppConfig.showWaitUpCount) {
            upBooksBadgeCount = 0
        }
        syncFragments()
    }

    private fun applyDefaultHomePage() {
        val targetMenuId = when (AppConfig.defaultHomePage) {
            "explore" -> R.id.menu_discovery
            "rss" -> R.id.menu_rss
            "my" -> R.id.menu_my_config
            else -> R.id.menu_bookshelf
        }
        if (navigationItems.any { it.menuId == targetMenuId }) {
            selectedMenuId = targetMenuId
        }
        syncFragments()
    }

    private fun buildNavigationItems(): List<MainNavigationItem> {
        val items = mutableListOf(
            MainNavigationItem(
                menuId = R.id.menu_bookshelf,
                fragmentId = currentBookshelfFragmentId(),
                selectedIconRes = R.drawable.ic_bottom_books_s,
                unselectedIconRes = R.drawable.ic_bottom_books_e,
                labelRes = R.string.bookshelf
            )
        )
        if (AppConfig.showDiscovery) {
            items += MainNavigationItem(
                menuId = R.id.menu_discovery,
                fragmentId = idExplore,
                selectedIconRes = R.drawable.ic_bottom_explore_s,
                unselectedIconRes = R.drawable.ic_bottom_explore_e,
                labelRes = R.string.discovery
            )
        }
        if (AppConfig.showRSS) {
            items += MainNavigationItem(
                menuId = R.id.menu_rss,
                fragmentId = idRss,
                selectedIconRes = R.drawable.ic_bottom_rss_feed_s,
                unselectedIconRes = R.drawable.ic_bottom_rss_feed_e,
                labelRes = R.string.rss
            )
        }
        items += MainNavigationItem(
            menuId = R.id.menu_my_config,
            fragmentId = idMy,
            selectedIconRes = R.drawable.ic_bottom_person_s,
            unselectedIconRes = R.drawable.ic_bottom_person_e,
            labelRes = R.string.my
        )
        return items
    }

    private fun currentBookshelfFragmentId(): Int {
        return if (AppConfig.bookGroupStyle == 1) idBookshelf2 else idBookshelf1
    }

    private fun syncFragments() {
        if (!isFragmentContainerReady || navigationItems.isEmpty()) return
        if (supportFragmentManager.isStateSaved) {
            binding.composeViewMain.post { if (!isDestroyed) syncFragments() }
            return
        }
        val currentItem = navigationItems.find { it.menuId == selectedMenuId } ?: navigationItems.first()
        val expectedFragmentIds = navigationItems.map { it.fragmentId }.toSet()
        val transaction = supportFragmentManager.beginTransaction().setReorderingAllowed(true)

        supportFragmentManager.fragments.forEach { fragment ->
            val fragmentId = fragment.tag?.removePrefix(MAIN_FRAGMENT_TAG_PREFIX)?.toIntOrNull()
            if (fragmentId != null && fragmentId !in expectedFragmentIds) {
                transaction.remove(fragment)
                fragmentMap.remove(fragmentId)
            }
        }

        navigationItems.forEachIndexed { index, item ->
            val fragment = obtainFragment(item.fragmentId, index)
            fragmentMap[item.fragmentId] = fragment
            if (fragment.isAdded) {
                if (item.menuId == currentItem.menuId) {
                    transaction.show(fragment)
                } else {
                    transaction.hide(fragment)
                }
            } else {
                transaction.add(fragmentContainerId, fragment, fragmentTag(item.fragmentId))
                if (item.menuId != currentItem.menuId) {
                    transaction.hide(fragment)
                }
            }
        }

        transaction.commitNowAllowingStateLoss()
    }

    private fun obtainFragment(fragmentId: Int, position: Int): Fragment {
        fragmentMap[fragmentId]?.let { return it }
        supportFragmentManager.findFragmentByTag(fragmentTag(fragmentId))?.let {
            fragmentMap[fragmentId] = it
            return it
        }
        return when (fragmentId) {
            idBookshelf1 -> BookshelfFragment1(position)
            idBookshelf2 -> BookshelfFragment2(position)
            idExplore -> ExploreFragment(position)
            idRss -> RssFragment(position)
            else -> MyFragment(position)
        }
    }

    private fun fragmentTag(fragmentId: Int): String {
        return "$MAIN_FRAGMENT_TAG_PREFIX$fragmentId"
    }

    private suspend fun privacyPolicy(): Boolean = suspendCancellableCoroutine sc@{ block ->
        if (LocalConfig.privacyPolicyOk) {
            block.resume(true)
            return@sc
        }
        val privacyPolicy = String(assets.open("privacyPolicy.md").readBytes())
        alert(getString(R.string.privacy_policy), privacyPolicy) {
            positiveButton(R.string.agree) {
                LocalConfig.privacyPolicyOk = true
                block.resume(true)
            }
            negativeButton(R.string.refuse) {
                finish()
                block.resume(false)
            }
        }
    }

    private suspend fun upVersion() = suspendCancellableCoroutine sc@{ block ->
        if (LocalConfig.versionCode == appInfo.versionCode) {
            block.resume(null)
            return@sc
        }
        LocalConfig.versionCode = appInfo.versionCode
        if (LocalConfig.isFirstOpenApp) {
            val help = String(assets.open("web/help/md/appHelp.md").readBytes())
            val dialog = TextDialog(getString(R.string.help), help, TextDialog.Mode.MD)
            dialog.setOnDismissListener {
                block.resume(null)
            }
            showDialogFragment(dialog)
        } else if (!BuildConfig.DEBUG) {
            val log = String(assets.open("updateLog.md").readBytes())
            val dialog = TextDialog(getString(R.string.update_log), log, TextDialog.Mode.MD)
            dialog.setOnDismissListener {
                block.resume(null)
            }
            showDialogFragment(dialog)
        } else {
            block.resume(null)
        }
    }

    private suspend fun setLocalPassword() = suspendCancellableCoroutine sc@{ block ->
        if (LocalConfig.password != null) {
            block.resume(null)
            return@sc
        }
        alert(R.string.set_local_password, R.string.set_local_password_summary) {
            val editTextBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "password"
            }
            customView {
                editTextBinding.root
            }
            onDismiss {
                block.resume(null)
            }
            okButton {
                LocalConfig.password = editTextBinding.editView.text.toString()
            }
            cancelButton {
                LocalConfig.password = ""
            }
        }
    }

    private fun notifyAppCrash() {
        if (!LocalConfig.appCrash || BuildConfig.DEBUG) {
            return
        }
        LocalConfig.appCrash = false
        alert(getString(R.string.draw), "检测到阅读发生了崩溃，是否打开崩溃日志以便报告问题？") {
            yesButton {
                showDialogFragment<CrashLogsDialog>()
            }
            noButton()
        }
    }

    private fun backupSync() {
        if (!AppConfig.autoCheckNewBackup) {
            return
        }
        lifecycleScope.launch {
            val lastBackupFile =
                withContext(IO) { AppWebDav.lastBackUp().getOrNull() } ?: return@launch
            if (lastBackupFile.lastModify - LocalConfig.lastBackup > DateUtils.MINUTE_IN_MILLIS) {
                LocalConfig.lastBackup = lastBackupFile.lastModify
                alert(R.string.restore, R.string.webdav_after_local_restore_confirm) {
                    cancelButton()
                    okButton {
                        viewModel.restoreWebDav(lastBackupFile.displayName)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (AppConfig.autoRefreshBook) {
            outState.putBoolean("isAutoRefreshedBook", true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Coroutine.async {
            BookHelp.clearInvalidCache()
        }
        if (!BuildConfig.DEBUG) {
            Backup.autoBack(this)
        }
    }

    override fun recreate() {
        (fragmentMap[currentBookshelfFragmentId()] as? BaseBookshelfFragment)?.upSort()
        super.recreate()
    }

    override fun observeLiveBus() {
        viewModel.onUpBooksLiveData.observe(this) {
            upBooksBadgeCount = if (AppConfig.showWaitUpCount && it > 0) it else 0
        }
        observeEvent<String>(EventBus.RECREATE) {
            recreate()
        }
        observeEvent<Boolean>(EventBus.NOTIFY_MAIN) {
            refreshNavigationItems(selectLast = it)
        }
        observeEvent<String>(PreferKey.threadCount) {
            viewModel.upPool()
        }
    }

    companion object {
        private const val MAIN_FRAGMENT_TAG_PREFIX = "main_fragment_"
    }
}
