package io.legado.app.ui.config

import android.os.Build
import android.os.Bundle
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.EventBus
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.ActivityThemeComposeBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.databinding.DialogImageBlurringBinding
import io.legado.app.help.LauncherIconHelp
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.ThemeConfig
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.lib.prefs.ColorPreference
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.compose.theme.SettingsItemUi
import io.legado.app.ui.compose.theme.SettingsMaterialScreen
import io.legado.app.ui.compose.theme.SettingsSectionUi
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.widget.number.NumberPickerDialog
import io.legado.app.ui.widget.seekbar.SeekBarChangeListener
import io.legado.app.utils.ColorUtils
import io.legado.app.utils.MD5Utils
import io.legado.app.utils.externalFiles
import io.legado.app.utils.getCompatColor
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefInt
import io.legado.app.utils.getPrefString
import io.legado.app.utils.postEvent
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefInt
import io.legado.app.utils.putPrefString
import io.legado.app.utils.removePref
import io.legado.app.utils.startActivity
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import splitties.init.appCtx
import java.io.File
import java.io.FileOutputStream

@Suppress("SameParameterValue")
class ThemeComposeActivity : VMBaseActivity<ActivityThemeComposeBinding, ConfigViewModel>() {

    override val binding by viewBinding(ActivityThemeComposeBinding::inflate)
    override val viewModel by viewModels<ConfigViewModel>()

    private val requestCodeBgLight = 121
    private val requestCodeBgDark = 122
    private var sections by mutableStateOf<List<SettingsSectionUi>>(emptyList())

    private val selectImage = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            when (it.requestCode) {
                requestCodeBgLight -> setBgFromUri(uri.toString(), PreferKey.bgImage) {
                    upTheme(false)
                }
                requestCodeBgDark -> setBgFromUri(uri.toString(), PreferKey.bgImageN) {
                    upTheme(true)
                }
            }
            refreshState()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.composeThemeSettingsContent.setContent {
            LegadoComposeTheme {
                SettingsMaterialScreen(
                    title = getString(R.string.theme_setting),
                    subtitle = "主题、图标与界面外观",
                    sections = sections,
                    onBackClick = ::finish,
                    actions = {
                        IconButton(onClick = {
                            AppConfig.isNightTheme = !AppConfig.isNightTheme
                            ThemeConfig.applyDayNight(this@ThemeComposeActivity)
                            refreshState()
                        }) {
                            Icon(Icons.Rounded.BrightnessMedium, contentDescription = null)
                        }
                    },
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
        val iconEntries = resources.getStringArray(R.array.icon_names)
        val iconValues = resources.getStringArray(R.array.icons)
        val launcherIconValue = getPrefString(PreferKey.launcherIcon, "ic_launcher")
        val launcherIconLabel = iconEntries.getOrElse(iconValues.indexOf(launcherIconValue).coerceAtLeast(0)) {
            iconEntries.firstOrNull().orEmpty()
        }
        sections = listOf(
            SettingsSectionUi(
                items = listOf(
                    SettingsItemUi(PreferKey.launcherIcon, Icons.Rounded.Palette, getString(R.string.change_icon), launcherIconLabel),
                    SettingsItemUi("welcomeStyle", Icons.Rounded.Palette, getString(R.string.welcome_style), getString(R.string.welcome_style_summary)),
                    SettingsItemUi(PreferKey.transparentStatusBar, Icons.Rounded.Palette, getString(R.string.immersion_status_bar), getString(R.string.status_bar_immersion), checked = getPrefBoolean(PreferKey.transparentStatusBar, true)),
                    SettingsItemUi(PreferKey.immNavigationBar, Icons.Rounded.Palette, getString(R.string.imm_navigation_bar), getString(R.string.imm_navigation_bar_s), checked = getPrefBoolean(PreferKey.immNavigationBar, true)),
                    SettingsItemUi(PreferKey.dynamicColor, Icons.Rounded.Palette, getString(R.string.dynamic_color), getString(R.string.dynamic_color_summary), checked = getPrefBoolean(PreferKey.dynamicColor, false), enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S),
                    SettingsItemUi(PreferKey.barElevation, Icons.Rounded.Palette, getString(R.string.bar_elevation), getString(R.string.bar_elevation_s, AppConfig.elevation.toString())),
                    SettingsItemUi(PreferKey.fontScale, Icons.Rounded.Palette, getString(R.string.font_scale), getFontScaleSummary()),
                    SettingsItemUi("coverConfig", Icons.Rounded.Palette, getString(R.string.cover_config), getString(R.string.cover_config_summary)),
                    SettingsItemUi("themeList", Icons.Rounded.Palette, getString(R.string.theme_list), getString(R.string.theme_list_summary))
                )
            ),
            SettingsSectionUi(
                label = getString(R.string.day),
                items = listOf(
                    colorItem(PreferKey.cPrimary, getString(R.string.primary), getPrefInt(PreferKey.cPrimary, getCompatColor(R.color.md_brown_500))),
                    colorItem(PreferKey.cAccent, getString(R.string.accent), getPrefInt(PreferKey.cAccent, getCompatColor(R.color.md_red_600))),
                    colorItem(PreferKey.cBackground, getString(R.string.background_color), getPrefInt(PreferKey.cBackground, getCompatColor(R.color.md_grey_100))),
                    colorItem(PreferKey.cBBackground, getString(R.string.navbar_color), getPrefInt(PreferKey.cBBackground, getCompatColor(R.color.md_grey_200))),
                    SettingsItemUi(PreferKey.bgImage, Icons.Rounded.Image, getString(R.string.background_image), getPrefString(PreferKey.bgImage).orEmpty().ifBlank { "未设置背景图" }),
                    SettingsItemUi("saveDayTheme", Icons.Rounded.ColorLens, getString(R.string.save_theme_config), getString(R.string.save_day_theme_summary))
                )
            ),
            SettingsSectionUi(
                label = getString(R.string.night),
                items = listOf(
                    colorItem(PreferKey.cNPrimary, getString(R.string.primary), getPrefInt(PreferKey.cNPrimary, getCompatColor(R.color.md_blue_grey_600))),
                    colorItem(PreferKey.cNAccent, getString(R.string.accent), getPrefInt(PreferKey.cNAccent, getCompatColor(R.color.md_deep_orange_800))),
                    colorItem(PreferKey.cNBackground, getString(R.string.background_color), getPrefInt(PreferKey.cNBackground, getCompatColor(R.color.md_grey_900))),
                    colorItem(PreferKey.cNBBackground, getString(R.string.navbar_color), getPrefInt(PreferKey.cNBBackground, getCompatColor(R.color.md_grey_850))),
                    SettingsItemUi(PreferKey.bgImageN, Icons.Rounded.Image, getString(R.string.background_image), getPrefString(PreferKey.bgImageN).orEmpty().ifBlank { "未设置背景图" }),
                    SettingsItemUi("saveNightTheme", Icons.Rounded.ColorLens, getString(R.string.save_theme_config), getString(R.string.save_night_theme_summary))
                )
            )
        )
    }

    private fun colorItem(key: String, title: String, color: Int): SettingsItemUi {
        return SettingsItemUi(
            key = key,
            icon = Icons.Rounded.ColorLens,
            title = title,
            summary = "#${Integer.toHexString(color).uppercase().takeLast(6)}"
        )
    }

    private fun handleItemClick(key: String) {
        when (key) {
            PreferKey.launcherIcon -> selectLauncherIcon()
            "welcomeStyle" -> startActivity<ConfigActivity> { putExtra("configTag", ConfigTag.WELCOME_CONFIG) }
            PreferKey.barElevation -> showBarElevationDialog()
            PreferKey.fontScale -> showFontScaleDialog()
            "coverConfig" -> startActivity<ConfigActivity> { putExtra("configTag", ConfigTag.COVER_CONFIG) }
            "themeList" -> ThemeListDialog().show(supportFragmentManager, "themeList")
            PreferKey.cPrimary,
            PreferKey.cAccent,
            PreferKey.cBackground,
            PreferKey.cBBackground,
            PreferKey.cNPrimary,
            PreferKey.cNAccent,
            PreferKey.cNBackground,
            PreferKey.cNBBackground -> showColorPicker(key)
            PreferKey.bgImage -> selectBgAction(false)
            PreferKey.bgImageN -> selectBgAction(true)
            "saveDayTheme",
            "saveNightTheme" -> alertSaveTheme(key)
        }
    }

    private fun handleSwitchChange(key: String, checked: Boolean) {
        putPrefBoolean(key, checked)
        when (key) {
            PreferKey.transparentStatusBar,
            PreferKey.immNavigationBar,
            PreferKey.dynamicColor -> recreateActivities()
        }
        refreshState()
    }

    private fun selectLauncherIcon() {
        val labels = resources.getStringArray(R.array.icon_names)
        val values = resources.getStringArray(R.array.icons)
        selector(labels.map { it as CharSequence }) { _, index ->
            putPrefString(PreferKey.launcherIcon, values.getOrNull(index) ?: "ic_launcher")
            LauncherIconHelp.changeIcon(getPrefString(PreferKey.launcherIcon))
            refreshState()
        }
    }

    private fun showBarElevationDialog() {
        NumberPickerDialog(this)
            .setTitle(getString(R.string.bar_elevation))
            .setMaxValue(32)
            .setMinValue(0)
            .setValue(AppConfig.elevation)
            .setCustomButton(R.string.btn_default_s) {
                AppConfig.elevation = io.legado.app.constant.AppConst.sysElevation
                recreateActivities()
            }
            .show {
                AppConfig.elevation = it
                recreateActivities()
            }
    }

    private fun showFontScaleDialog() {
        NumberPickerDialog(this)
            .setTitle(getString(R.string.font_scale))
            .setMaxValue(16)
            .setMinValue(8)
            .setValue(10)
            .setCustomButton(R.string.btn_default_s) {
                putPrefInt(PreferKey.fontScale, 0)
                recreateActivities()
            }
            .show {
                putPrefInt(PreferKey.fontScale, it)
                recreateActivities()
            }
    }

    private fun showColorPicker(key: String) {
        val current = getPrefInt(key, getCompatColor(R.color.md_brown_500))
        val dialog = ColorPreference.ColorPickerDialogCompat.newBuilder()
            .setDialogType(com.jaredrummler.android.colorpicker.ColorPickerDialog.TYPE_PRESETS)
            .setColor(current)
            .create()
        dialog.setColorPickerDialogListener(object :
            com.jaredrummler.android.colorpicker.ColorPickerDialogListener {
            override fun onColorSelected(dialogId: Int, color: Int) {
                when (key) {
                    PreferKey.cBackground -> if (!ColorUtils.isColorLight(color)) {
                        toastOnUi(R.string.day_background_too_dark)
                        return
                    }
                    PreferKey.cNBackground -> if (ColorUtils.isColorLight(color)) {
                        toastOnUi(R.string.night_background_too_light)
                        return
                    }
                }
                putPrefInt(key, color)
                when (key) {
                    PreferKey.cPrimary, PreferKey.cAccent, PreferKey.cBackground, PreferKey.cBBackground -> upTheme(false)
                    PreferKey.cNPrimary, PreferKey.cNAccent, PreferKey.cNBackground, PreferKey.cNBBackground -> upTheme(true)
                }
                refreshState()
            }
            override fun onDialogDismissed(dialogId: Int) = Unit
        })
        supportFragmentManager.beginTransaction().add(dialog, "color_$key").commitAllowingStateLoss()
    }

    private fun selectBgAction(isNight: Boolean) {
        val bgKey = if (isNight) PreferKey.bgImageN else PreferKey.bgImage
        val blurringKey = if (isNight) PreferKey.bgImageNBlurring else PreferKey.bgImageBlurring
        val actions = arrayListOf(
            getString(R.string.background_image_blurring),
            getString(R.string.select_image)
        )
        if (!getPrefString(bgKey).isNullOrEmpty()) {
            actions.add(getString(R.string.delete))
        }
        selector(actions.map { it as CharSequence }) { _, index ->
            when (index) {
                0 -> alertImageBlurring(blurringKey) { upTheme(isNight) }
                1 -> {
                    selectImage.launch {
                        requestCode = if (isNight) requestCodeBgDark else requestCodeBgLight
                        mode = HandleFileContract.IMAGE
                    }
                }
                2 -> {
                    removePref(bgKey)
                    upTheme(isNight)
                    refreshState()
                }
            }
        }
    }

    private fun alertImageBlurring(preferKey: String, success: () -> Unit) {
        alert(R.string.background_image_blurring) {
            val alertBinding = DialogImageBlurringBinding.inflate(layoutInflater).apply {
                getPrefInt(preferKey, 0).let {
                    seekBar.progress = it
                    textViewValue.text = it.toString()
                }
                seekBar.setOnSeekBarChangeListener(object : SeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        textViewValue.text = progress.toString()
                    }
                })
            }
            customView { alertBinding.root }
            okButton {
                putPrefInt(preferKey, alertBinding.seekBar.progress)
                success()
                refreshState()
            }
            cancelButton()
        }
    }

    private fun alertSaveTheme(key: String) {
        alert(R.string.theme_name) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "name"
            }
            customView { alertBinding.root }
            okButton {
                val themeName = alertBinding.editView.text?.toString().orEmpty()
                if (themeName.isBlank()) return@okButton
                when (key) {
                    "saveDayTheme" -> ThemeConfig.saveDayTheme(this@ThemeComposeActivity, themeName)
                    "saveNightTheme" -> ThemeConfig.saveNightTheme(this@ThemeComposeActivity, themeName)
                }
            }
            cancelButton()
        }
    }

    private fun setBgFromUri(uri: String, bgKey: String, success: () -> Unit) {
        val saveDir = File(externalFiles, "bgImage").apply { mkdirs() }
        val saveFile = File(saveDir, "${MD5Utils.md5Encode16(uri)}.jpg")
        runCatching {
            contentResolver.openInputStream(android.net.Uri.parse(uri))?.use { input ->
                FileOutputStream(saveFile).use { output ->
                    input.copyTo(output)
                }
            }
            putPrefString(bgKey, saveFile.absolutePath)
            success()
        }.onFailure {
            toastOnUi(it.localizedMessage)
        }
    }

    private fun upTheme(isNightTheme: Boolean) {
        if (AppConfig.isNightTheme == isNightTheme) {
            ThemeConfig.applyTheme(this)
            recreateActivities()
        }
    }

    private fun recreateActivities() {
        ThemeConfig.applyTheme(this)
        postEvent(EventBus.RECREATE, "")
    }

    private fun getFontScaleSummary(): String {
        val value = getPrefInt(PreferKey.fontScale, 0)
        return if (value == 0) {
            getString(R.string.btn_default_s)
        } else {
            getString(R.string.font_scale_summary, value / 10f)
        }
    }
}
