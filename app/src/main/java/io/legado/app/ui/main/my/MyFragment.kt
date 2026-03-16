package io.legado.app.ui.main.my

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.legado.app.R
import io.legado.app.base.BaseFragment
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.FragmentMyConfigBinding
import io.legado.app.help.config.ThemeConfig
import io.legado.app.lib.dialogs.selector
import io.legado.app.ui.about.AboutActivity
import io.legado.app.ui.about.ReadRecordActivity
import io.legado.app.ui.book.bookmark.AllBookmarkActivity
import io.legado.app.ui.book.source.manage.BookSourceActivity
import io.legado.app.ui.book.toc.rule.TxtTocRuleActivity
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.config.ConfigActivity
import io.legado.app.ui.config.ConfigTag
import io.legado.app.ui.dict.rule.DictRuleActivity
import io.legado.app.ui.file.FileManageActivity
import io.legado.app.ui.main.MainFragmentInterface
import io.legado.app.ui.replace.ReplaceRuleActivity
import io.legado.app.service.WebService
import io.legado.app.utils.getPrefString
import io.legado.app.utils.observeEventSticky
import io.legado.app.utils.openUrl
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefString
import io.legado.app.utils.sendToClip
import io.legado.app.utils.showHelp
import io.legado.app.utils.startActivity
import io.legado.app.utils.viewbindingdelegate.viewBinding

class MyFragment() : BaseFragment(R.layout.fragment_my_config), MainFragmentInterface {

    constructor(position: Int) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        arguments = bundle
    }

    override val position: Int? get() = arguments?.getInt("position")

    private val binding by viewBinding(FragmentMyConfigBinding::bind)
    private var themeModeLabel by mutableStateOf("")
    private var webServiceEnabled by mutableStateOf(false)
    private var webServiceSummary by mutableStateOf("")

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        refreshComposeState()
        binding.composeMyContent.setContent {
            LegadoComposeTheme {
                MyMaterialScreen(
                    themeModeLabel = themeModeLabel,
                    webServiceEnabled = webServiceEnabled,
                    webServiceSummary = webServiceSummary,
                    onHelpClick = { showHelp("appHelp") },
                    onThemeModeClick = ::showThemeModeSelector,
                    onWebServiceToggle = ::toggleWebService,
                    onWebServiceLongClick = ::showWebServiceActions,
                    onItemClick = ::handleItemClick
                )
            }
        }
        observeEventSticky<String>(EventBus.WEB_SERVICE) {
            refreshComposeState()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshComposeState()
    }

    private fun refreshComposeState() {
        val labels = resources.getStringArray(R.array.theme_mode)
        val values = resources.getStringArray(io.legado.app.R.array.theme_mode_v)
        val selectedValue = requireContext().getPrefString(PreferKey.themeMode, "0") ?: "0"
        themeModeLabel = labels.getOrElse(values.indexOf(selectedValue).coerceAtLeast(0)) {
            labels.firstOrNull().orEmpty()
        }
        webServiceEnabled = WebService.isRun
        webServiceSummary = if (WebService.isRun) {
            WebService.hostAddress
        } else {
            getString(R.string.web_service_desc)
        }
    }

    private fun showThemeModeSelector() {
        val labels = ArrayList(resources.getStringArray(R.array.theme_mode).toList())
        val values = resources.getStringArray(io.legado.app.R.array.theme_mode_v)
        context?.selector(labels) { _, index ->
            requireContext().putPrefString(PreferKey.themeMode, values.getOrNull(index) ?: "0")
            ThemeConfig.applyDayNight(requireContext())
            refreshComposeState()
        }
    }

    private fun toggleWebService(enabled: Boolean) {
        requireContext().putPrefBoolean(PreferKey.webService, enabled)
        if (enabled) {
            WebService.start(requireContext())
        } else {
            WebService.stop(requireContext())
        }
        refreshComposeState()
    }

    private fun showWebServiceActions() {
        if (!WebService.isRun) return
        context?.selector(arrayListOf("复制地址", "浏览器打开")) { _, index ->
            when (index) {
                0 -> context?.sendToClip(webServiceSummary)
                1 -> context?.openUrl(webServiceSummary)
            }
        }
    }

    private fun handleItemClick(key: String) {
        when (key) {
            "bookSourceManage" -> startActivity<BookSourceActivity>()
            "replaceManage" -> startActivity<ReplaceRuleActivity>()
            "dictRuleManage" -> startActivity<DictRuleActivity>()
            "txtTocRuleManage" -> startActivity<TxtTocRuleActivity>()
            "themeMode" -> showThemeModeSelector()
            "webService" -> toggleWebService(!webServiceEnabled)
            "bookmark" -> startActivity<AllBookmarkActivity>()
            "setting" -> startActivity<ConfigActivity> {
                putExtra("configTag", ConfigTag.OTHER_CONFIG)
            }

            "web_dav_setting" -> startActivity<ConfigActivity> {
                putExtra("configTag", ConfigTag.BACKUP_CONFIG)
            }

            "theme_setting" -> startActivity<ConfigActivity> {
                putExtra("configTag", ConfigTag.THEME_CONFIG)
            }

            "fileManage" -> startActivity<FileManageActivity>()
            "readRecord" -> startActivity<ReadRecordActivity>()
            "about" -> startActivity<AboutActivity>()
            "exit" -> activity?.finish()
        }
    }
}
