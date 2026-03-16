package io.legado.app.ui.config

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.jeremyliao.liveeventbus.LiveEventBus
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.ActivityOtherComposeBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.help.AppFreezeMonitor
import io.legado.app.help.DispatchersMonitor
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.LocalConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.model.CheckSource
import io.legado.app.model.ImageProvider
import io.legado.app.receiver.SharedReceiverActivity
import io.legado.app.service.WebService
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.compose.theme.SettingsItemUi
import io.legado.app.ui.compose.theme.SettingsMaterialScreen
import io.legado.app.ui.compose.theme.SettingsSectionUi
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.widget.number.NumberPickerDialog
import io.legado.app.utils.LogUtils
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefString
import io.legado.app.utils.postEvent
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefString
import io.legado.app.utils.removePref
import io.legado.app.utils.restart
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.viewbindingdelegate.viewBinding
import splitties.init.appCtx

class OtherComposeActivity : VMBaseActivity<ActivityOtherComposeBinding, ConfigViewModel>() {

    override val binding by viewBinding(ActivityOtherComposeBinding::inflate)
    override val viewModel by viewModels<ConfigViewModel>()

    private val packageManagerRef = appCtx.packageManager
    private val componentName = ComponentName(appCtx, SharedReceiverActivity::class.java.name)
    private var sections by mutableStateOf<List<SettingsSectionUi>>(emptyList())

    private val localBookTreeSelect = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { treeUri ->
            AppConfig.defaultBookTreeUri = treeUri.toString()
            refreshState()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.composeOtherSettingsContent.setContent {
            LegadoComposeTheme {
                SettingsMaterialScreen(
                    title = getString(R.string.other_setting),
                    subtitle = "应用行为、性能与导入导出偏好",
                    sections = sections,
                    onBackClick = ::finish,
                    onItemClick = ::handleItemClick,
                    onSwitchChange = ::handleSwitchChange
                )
            }
        }
        refreshState()
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun refreshState() {
        val languageEntries = resources.getStringArray(R.array.language)
        val languageValues = resources.getStringArray(R.array.language_value)
        val currentLanguage = getPrefString(PreferKey.language, "auto")
        val languageLabel = languageEntries.getOrElse(languageValues.indexOf(currentLanguage).coerceAtLeast(0)) {
            languageEntries.firstOrNull().orEmpty()
        }
        val homeEntries = resources.getStringArray(R.array.default_home_page)
        val homeValues = resources.getStringArray(R.array.default_home_page_value)
        val currentHome = getPrefString(PreferKey.defaultHomePage, "bookshelf")
        val homeLabel = homeEntries.getOrElse(homeValues.indexOf(currentHome).coerceAtLeast(0)) {
            homeEntries.firstOrNull().orEmpty()
        }
        val variantEntries = resources.getStringArray(R.array.default_app_variant)
        val variantValues = resources.getStringArray(R.array.default_app_variant_value)
        val currentVariant = getPrefString(PreferKey.updateToVariant, "default_version")
        val variantLabel = variantEntries.getOrElse(variantValues.indexOf(currentVariant).coerceAtLeast(0)) {
            variantEntries.firstOrNull().orEmpty()
        }

        sections = listOf(
            SettingsSectionUi(
                items = listOf(
                    SettingsItemUi(PreferKey.language, Icons.Rounded.Settings, getString(R.string.language), languageLabel)
                )
            ),
            SettingsSectionUi(
                label = getString(R.string.main_activity),
                items = listOf(
                    SettingsItemUi(PreferKey.autoRefresh, Icons.Rounded.Settings, getString(R.string.pt_auto_refresh), getString(R.string.ps_auto_refresh), checked = getPrefBoolean(PreferKey.autoRefresh, false)),
                    SettingsItemUi(PreferKey.defaultToRead, Icons.Rounded.Settings, getString(R.string.pt_default_read), getString(R.string.ps_default_read), checked = getPrefBoolean(PreferKey.defaultToRead, false)),
                    SettingsItemUi(PreferKey.showDiscovery, Icons.Rounded.Settings, getString(R.string.show_discovery), checked = getPrefBoolean(PreferKey.showDiscovery, true)),
                    SettingsItemUi(PreferKey.showRss, Icons.Rounded.Settings, getString(R.string.show_rss), checked = getPrefBoolean(PreferKey.showRss, true)),
                    SettingsItemUi(PreferKey.defaultHomePage, Icons.Rounded.Settings, getString(R.string.default_home_page), homeLabel)
                )
            ),
            SettingsSectionUi(
                label = getString(R.string.other_setting),
                items = listOf(
                    SettingsItemUi("localPassword", Icons.Rounded.Settings, getString(R.string.set_local_password), getString(R.string.set_local_password_summary)),
                    SettingsItemUi(PreferKey.userAgent, Icons.Rounded.Settings, getString(R.string.user_agent), AppConfig.userAgent),
                    SettingsItemUi(PreferKey.webServiceWakeLock, Icons.Rounded.Settings, getString(R.string.web_service_wake_lock), getString(R.string.web_service_wake_lock_summary), checked = getPrefBoolean(PreferKey.webServiceWakeLock, false)),
                    SettingsItemUi(PreferKey.defaultBookTreeUri, Icons.Rounded.Settings, getString(R.string.book_tree_uri_t), AppConfig.defaultBookTreeUri ?: getString(R.string.book_tree_uri_s)),
                    SettingsItemUi(PreferKey.sourceEditMaxLine, Icons.Rounded.Settings, getString(R.string.source_edit_text_max_line), getString(R.string.source_edit_max_line_summary, AppConfig.sourceEditMaxLine.toString())),
                    SettingsItemUi(PreferKey.checkSource, Icons.Rounded.Settings, getString(R.string.check_source_config), CheckSource.summary),
                    SettingsItemUi(PreferKey.uploadRule, Icons.Rounded.Settings, getString(R.string.direct_link_upload_rule), getString(R.string.direct_link_upload_rule_summary)),
                    SettingsItemUi(PreferKey.cronet, Icons.Rounded.Settings, "Cronet", getString(R.string.pref_cronet_summary), checked = getPrefBoolean(PreferKey.cronet, false)),
                    SettingsItemUi(PreferKey.antiAlias, Icons.Rounded.Settings, getString(R.string.anti_alias), getString(R.string.pref_anti_alias_summary), checked = getPrefBoolean(PreferKey.antiAlias, false)),
                    SettingsItemUi(PreferKey.bitmapCacheSize, Icons.Rounded.Settings, getString(R.string.bitmap_cache_size), getString(R.string.bitmap_cache_size_summary, AppConfig.bitmapCacheSize.toString())),
                    SettingsItemUi(PreferKey.imageRetainNum, Icons.Rounded.Settings, getString(R.string.image_retain_number), getString(R.string.image_retain_number_summary, AppConfig.imageRetainNum.toString())),
                    SettingsItemUi(PreferKey.preDownloadNum, Icons.Rounded.Settings, getString(R.string.pre_download), getString(R.string.pre_download_s, AppConfig.preDownloadNum.toString())),
                    SettingsItemUi(PreferKey.replaceEnableDefault, Icons.Rounded.Settings, getString(R.string.replace_enable_default_t), getString(R.string.replace_enable_default_s), checked = getPrefBoolean(PreferKey.replaceEnableDefault, true)),
                    SettingsItemUi("mediaButtonOnExit", Icons.Rounded.Settings, getString(R.string.media_button_on_exit_title), getString(R.string.media_button_on_exit_summary), checked = getPrefBoolean("mediaButtonOnExit", true)),
                    SettingsItemUi(PreferKey.readAloudByMediaButton, Icons.Rounded.Settings, getString(R.string.read_aloud_by_media_button_title), getString(R.string.read_aloud_by_media_button_summary), checked = getPrefBoolean(PreferKey.readAloudByMediaButton, false)),
                    SettingsItemUi(PreferKey.ignoreAudioFocus, Icons.Rounded.Settings, getString(R.string.ignore_audio_focus_title), getString(R.string.ignore_audio_focus_summary), checked = getPrefBoolean(PreferKey.ignoreAudioFocus, false)),
                    SettingsItemUi(PreferKey.autoClearExpired, Icons.Rounded.Settings, getString(R.string.auto_clear_expired), getString(R.string.auto_clear_expired_summary), checked = getPrefBoolean(PreferKey.autoClearExpired, true)),
                    SettingsItemUi(PreferKey.showAddToShelfAlert, Icons.Rounded.Settings, getString(R.string.show_add_to_shelf_alert_title), getString(R.string.show_add_to_shelf_alert_summary), checked = getPrefBoolean(PreferKey.showAddToShelfAlert, true)),
                    SettingsItemUi(PreferKey.updateToVariant, Icons.Rounded.Settings, getString(R.string.update_to_variant_title), variantLabel),
                    SettingsItemUi(PreferKey.showMangaUi, Icons.Rounded.Settings, getString(R.string.show_manga_ui), checked = getPrefBoolean(PreferKey.showMangaUi, true)),
                    SettingsItemUi(PreferKey.webPort, Icons.Rounded.Settings, getString(R.string.web_port_title), getString(R.string.web_port_summary, AppConfig.webPort.toString())),
                    SettingsItemUi(PreferKey.cleanCache, Icons.Rounded.Settings, getString(R.string.clear_cache), getString(R.string.clear_cache_summary)),
                    SettingsItemUi(PreferKey.clearWebViewData, Icons.Rounded.Settings, getString(R.string.clear_webview_data), getString(R.string.clear_webview_data_summary)),
                    SettingsItemUi(PreferKey.shrinkDatabase, Icons.Rounded.Settings, getString(R.string.shrink_database), getString(R.string.shrink_database_summary)),
                    SettingsItemUi(PreferKey.threadCount, Icons.Rounded.Settings, getString(R.string.threads_num_title), getString(R.string.threads_num, AppConfig.threadCount.toString())),
                    SettingsItemUi(PreferKey.processText, Icons.Rounded.Settings, getString(R.string.add_to_text_context_menu_t), getString(R.string.add_to_text_context_menu_s), checked = isProcessTextEnabled()),
                    SettingsItemUi(PreferKey.recordLog, Icons.Rounded.Settings, getString(R.string.record_log), getString(R.string.record_debug_log), checked = getPrefBoolean(PreferKey.recordLog, false)),
                    SettingsItemUi(PreferKey.recordHeapDump, Icons.Rounded.Settings, getString(R.string.record_heap_dump_t), getString(R.string.record_heap_dump_s), checked = getPrefBoolean(PreferKey.recordHeapDump, false))
                )
            )
        )
    }

    private fun handleItemClick(key: String) {
        when (key) {
            PreferKey.language -> chooseListPreference(PreferKey.language, R.array.language, R.array.language_value)
            PreferKey.defaultHomePage -> chooseListPreference(PreferKey.defaultHomePage, R.array.default_home_page, R.array.default_home_page_value)
            "localPassword" -> alertLocalPassword()
            PreferKey.userAgent -> showUserAgentDialog()
            PreferKey.defaultBookTreeUri -> localBookTreeSelect.launch {
                title = getString(R.string.select_book_folder)
                mode = HandleFileContract.DIR_SYS
            }
            PreferKey.sourceEditMaxLine -> showNumberDialog(getString(R.string.source_edit_text_max_line), 10, Int.MAX_VALUE, AppConfig.sourceEditMaxLine) { AppConfig.sourceEditMaxLine = it }
            PreferKey.checkSource -> showDialogFragment<CheckSourceConfig>()
            PreferKey.uploadRule -> showDialogFragment<DirectLinkUploadConfig>()
            PreferKey.bitmapCacheSize -> showNumberDialog(getString(R.string.bitmap_cache_size), 1, 1024, AppConfig.bitmapCacheSize) {
                AppConfig.bitmapCacheSize = it
                ImageProvider.bitmapLruCache.resize(ImageProvider.cacheSize)
            }
            PreferKey.imageRetainNum -> showNumberDialog(getString(R.string.image_retain_number), 0, 999, AppConfig.imageRetainNum) { AppConfig.imageRetainNum = it }
            PreferKey.preDownloadNum -> showNumberDialog(getString(R.string.pre_download), 0, 9999, AppConfig.preDownloadNum) { AppConfig.preDownloadNum = it }
            PreferKey.updateToVariant -> chooseListPreference(PreferKey.updateToVariant, R.array.default_app_variant, R.array.default_app_variant_value)
            PreferKey.webPort -> showNumberDialog(getString(R.string.web_port_title), 1024, 60000, AppConfig.webPort) { AppConfig.webPort = it }
            PreferKey.cleanCache -> clearCache()
            PreferKey.clearWebViewData -> clearWebViewData()
            PreferKey.shrinkDatabase -> shrinkDatabase()
            PreferKey.threadCount -> showNumberDialog(getString(R.string.threads_num_title), 1, 999, AppConfig.threadCount) { AppConfig.threadCount = it }
        }
        refreshState()
    }

    private fun handleSwitchChange(key: String, checked: Boolean) {
        if (key == PreferKey.processText) {
            setProcessTextEnable(checked)
        } else {
            putPrefBoolean(key, checked)
        }
        when (key) {
            PreferKey.showDiscovery, PreferKey.showRss -> postEvent(EventBus.NOTIFY_MAIN, true)
            PreferKey.language -> appCtx.restart()
            PreferKey.threadCount -> postEvent(PreferKey.threadCount, "")
            PreferKey.webPort -> if (WebService.isRun) {
                WebService.stop(this)
                WebService.start(this)
            }
            PreferKey.recordLog -> {
                AppConfig.recordLog = checked
                LogUtils.upLevel()
                LogUtils.logDeviceInfo()
                LiveEventBus.config().enableLogger(AppConfig.recordLog)
                AppFreezeMonitor.init(appCtx)
                DispatchersMonitor.init()
            }
        }
        refreshState()
    }

    private fun chooseListPreference(key: String, entriesRes: Int, valuesRes: Int) {
        val labels = resources.getStringArray(entriesRes)
        val values = resources.getStringArray(valuesRes)
        selector(labels.map { it as CharSequence }) { _, index ->
            val selected = values.getOrNull(index) ?: return@selector
            putPrefString(key, selected)
            if (key == PreferKey.language) {
                appCtx.restart()
            }
            refreshState()
        }
    }

    private fun showNumberDialog(title: String, min: Int, max: Int, value: Int, onSave: (Int) -> Unit) {
        NumberPickerDialog(this)
            .setTitle(title)
            .setMinValue(min)
            .setMaxValue(max)
            .setValue(value)
            .show {
                onSave(it)
                refreshState()
            }
    }

    private fun showUserAgentDialog() {
        alert(getString(R.string.user_agent)) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = getString(R.string.user_agent)
                editView.setText(AppConfig.userAgent)
            }
            customView { alertBinding.root }
            okButton {
                val userAgent = alertBinding.editView.text?.toString()
                if (userAgent.isNullOrBlank()) {
                    removePref(PreferKey.userAgent)
                } else {
                    putPrefString(PreferKey.userAgent, userAgent)
                }
                refreshState()
            }
            cancelButton()
        }
    }

    private fun clearCache() {
        alert(titleResource = R.string.clear_cache, messageResource = R.string.sure_del) {
            okButton { viewModel.clearCache() }
            noButton()
        }
    }

    private fun shrinkDatabase() {
        alert(R.string.sure, R.string.shrink_database) {
            okButton { viewModel.shrinkDatabase() }
            noButton()
        }
    }

    private fun clearWebViewData() {
        alert(R.string.clear_webview_data, R.string.sure_del) {
            okButton { viewModel.clearWebViewData() }
            noButton()
        }
    }

    private fun isProcessTextEnabled(): Boolean {
        return packageManagerRef.getComponentEnabledSetting(componentName) !=
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    private fun setProcessTextEnable(enable: Boolean) {
        packageManagerRef.setComponentEnabledSetting(
            componentName,
            if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun alertLocalPassword() {
        alert(R.string.set_local_password, R.string.set_local_password_summary) {
            val editTextBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "password"
            }
            customView { editTextBinding.root }
            okButton {
                LocalConfig.password = editTextBinding.editView.text.toString()
            }
            cancelButton()
        }
    }
}
