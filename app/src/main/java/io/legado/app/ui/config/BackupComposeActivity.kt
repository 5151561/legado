package io.legado.app.ui.config

import android.content.Context
import android.os.Bundle
import android.text.InputType
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Notes
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.VMBaseActivity
import io.legado.app.constant.AppLog
import io.legado.app.constant.PreferKey
import io.legado.app.databinding.ActivityBackupComposeBinding
import io.legado.app.databinding.DialogEditTextBinding
import io.legado.app.exception.NoStackTraceException
import io.legado.app.help.AppWebDav
import io.legado.app.help.config.AppConfig
import io.legado.app.help.config.LocalConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.storage.Backup
import io.legado.app.help.storage.BackupConfig
import io.legado.app.help.storage.ImportOldData
import io.legado.app.help.storage.Restore
import io.legado.app.lib.dialogs.alert
import io.legado.app.lib.dialogs.selector
import io.legado.app.ui.about.AppLogDialog
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.compose.theme.SettingsItemUi
import io.legado.app.ui.compose.theme.SettingsMaterialScreen
import io.legado.app.ui.compose.theme.SettingsSectionUi
import io.legado.app.ui.file.HandleFileContract
import io.legado.app.ui.widget.dialog.WaitDialog
import io.legado.app.utils.FileDoc
import io.legado.app.utils.checkWrite
import io.legado.app.utils.getPrefBoolean
import io.legado.app.utils.getPrefString
import io.legado.app.utils.isContentScheme
import io.legado.app.utils.launch
import io.legado.app.utils.putPrefBoolean
import io.legado.app.utils.putPrefString
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.showHelp
import io.legado.app.utils.toEditable
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.init.appCtx

class BackupComposeActivity :
    VMBaseActivity<ActivityBackupComposeBinding, ConfigViewModel>() {

    override val binding by viewBinding(ActivityBackupComposeBinding::inflate)
    override val viewModel by viewModels<ConfigViewModel>()

    private val waitDialog by lazy { WaitDialog(this) }
    private var backupJob: Job? = null
    private var restoreJob: Job? = null
    private var sections by mutableStateOf<List<SettingsSectionUi>>(emptyList())

    private val selectBackupPath = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            AppConfig.backupPath = if (uri.isContentScheme()) uri.toString() else uri.path
            refreshState()
        }
    }
    private val backupDir = registerForActivityResult(HandleFileContract()) { result ->
        result.uri?.let { uri ->
            val backupPath = if (uri.isContentScheme()) uri.toString() else uri.path.orEmpty()
            AppConfig.backupPath = backupPath
            backup(backupPath)
            refreshState()
        }
    }
    private val restoreDoc = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            waitDialog.setText("恢复中…")
            waitDialog.show()
            val task = Coroutine.async {
                Restore.restore(appCtx, uri)
            }.onFinally {
                waitDialog.dismiss()
            }
            waitDialog.setOnCancelListener {
                task.cancel()
            }
        }
    }
    private val restoreOld = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            ImportOldData.importUri(appCtx, uri)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.composeBackupSettingsContent.setContent {
            LegadoComposeTheme {
                SettingsMaterialScreen(
                    title = getString(R.string.backup_restore),
                    subtitle = "WebDav 同步、本地备份与恢复",
                    sections = sections,
                    onBackClick = ::finish,
                    actions = {
                        IconButton(onClick = { showHelp("webDavHelp") }) {
                            Icon(Icons.Rounded.HelpOutline, contentDescription = null)
                        }
                        IconButton(onClick = { showDialogFragment<AppLogDialog>() }) {
                            Icon(Icons.Rounded.Notes, contentDescription = null)
                        }
                    },
                    onItemClick = ::handleItemClick,
                    onSwitchChange = ::handleSwitchChange
                )
            }
        }
        refreshState()
        if (!LocalConfig.backupHelpVersionIsLast) {
            showHelp("webDavHelp")
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun refreshState() {
        sections = listOf(
            SettingsSectionUi(
                label = getString(R.string.web_dav_set),
                items = listOf(
                    SettingsItemUi(PreferKey.webDavUrl, Icons.Rounded.Notes, getString(R.string.web_dav_url), getPrefString(PreferKey.webDavUrl).ifNullOrBlank(getString(R.string.web_dav_url_s))),
                    SettingsItemUi(PreferKey.webDavAccount, Icons.Rounded.Notes, getString(R.string.web_dav_account), getPrefString(PreferKey.webDavAccount).ifNullOrBlank(getString(R.string.web_dav_account_s))),
                    SettingsItemUi(PreferKey.webDavPassword, Icons.Rounded.Notes, getString(R.string.web_dav_pw), getPasswordSummary()),
                    SettingsItemUi(PreferKey.webDavDir, Icons.Rounded.Notes, getString(R.string.sub_dir), AppConfig.webDavDir ?: "legado"),
                    SettingsItemUi(PreferKey.webDavDeviceName, Icons.Rounded.Notes, getString(R.string.webdav_device_name), AppConfig.webDavDeviceName),
                    SettingsItemUi(PreferKey.syncBookProgress, Icons.Rounded.Notes, getString(R.string.sync_book_progress_t), getString(R.string.sync_book_progress_s), checked = getPrefBoolean(PreferKey.syncBookProgress, true)),
                    SettingsItemUi(PreferKey.syncBookProgressPlus, Icons.Rounded.Notes, getString(R.string.sync_book_progress_plus_t), getString(R.string.sync_book_progress_plus_s), checked = getPrefBoolean(PreferKey.syncBookProgressPlus, false))
                )
            ),
            SettingsSectionUi(
                label = getString(R.string.backup_restore),
                items = listOf(
                    SettingsItemUi(PreferKey.backupPath, Icons.Rounded.Notes, getString(R.string.backup_path), AppConfig.backupPath.ifNullOrBlank(getString(R.string.select_backup_path))),
                    SettingsItemUi("web_dav_backup", Icons.Rounded.Notes, getString(R.string.backup), getString(R.string.backup_summary)),
                    SettingsItemUi("web_dav_restore", Icons.Rounded.Notes, getString(R.string.restore), getString(R.string.restore_summary)),
                    SettingsItemUi(PreferKey.restoreIgnore, Icons.Rounded.Notes, getString(R.string.restore_ignore), getString(R.string.restore_ignore_summary)),
                    SettingsItemUi("import_old", Icons.Rounded.Notes, getString(R.string.menu_import_old_version), getString(R.string.import_old_summary)),
                    SettingsItemUi(PreferKey.onlyLatestBackup, Icons.Rounded.Notes, getString(R.string.only_latest_backup_t), getString(R.string.only_latest_backup_s), checked = getPrefBoolean(PreferKey.onlyLatestBackup, true)),
                    SettingsItemUi(PreferKey.autoCheckNewBackup, Icons.Rounded.Notes, getString(R.string.auto_check_new_backup_t), getString(R.string.auto_check_new_backup_s), checked = getPrefBoolean(PreferKey.autoCheckNewBackup, true))
                )
            )
        )
    }

    private fun handleItemClick(key: String) {
        when (key) {
            PreferKey.webDavUrl -> editTextValue(getString(R.string.web_dav_url), getPrefString(PreferKey.webDavUrl)) {
                putPrefString(PreferKey.webDavUrl, it)
                viewModel.upWebDavConfig()
                refreshState()
            }
            PreferKey.webDavAccount -> editTextValue(getString(R.string.web_dav_account), getPrefString(PreferKey.webDavAccount)) {
                putPrefString(PreferKey.webDavAccount, it)
                viewModel.upWebDavConfig()
                refreshState()
            }
            PreferKey.webDavPassword -> editTextValue(getString(R.string.web_dav_pw), getPrefString(PreferKey.webDavPassword), password = true) {
                putPrefString(PreferKey.webDavPassword, it)
                viewModel.upWebDavConfig()
                refreshState()
            }
            PreferKey.webDavDir -> editTextValue(getString(R.string.sub_dir), AppConfig.webDavDir) {
                putPrefString(PreferKey.webDavDir, it)
                refreshState()
            }
            PreferKey.webDavDeviceName -> editTextValue(getString(R.string.webdav_device_name), AppConfig.webDavDeviceName) {
                putPrefString(PreferKey.webDavDeviceName, it)
                refreshState()
            }
            PreferKey.backupPath -> selectBackupPath.launch()
            "web_dav_backup" -> backup()
            "web_dav_restore" -> restore()
            PreferKey.restoreIgnore -> backupIgnore()
            "import_old" -> restoreOld.launch()
        }
    }

    private fun handleSwitchChange(key: String, checked: Boolean) {
        putPrefBoolean(key, checked)
        refreshState()
    }

    private fun getPasswordSummary(): String {
        val value = getPrefString(PreferKey.webDavPassword)
        return if (value.isNullOrEmpty()) getString(R.string.web_dav_pw_s) else "*".repeat(value.length)
    }

    private fun editTextValue(
        title: String,
        value: String?,
        password: Boolean = false,
        onSave: (String) -> Unit
    ) {
        this@BackupComposeActivity.alert(title) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = title
                editView.setText(value)
                if (password) {
                    editView.inputType =
                        InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
                }
                editView.setSelection(editView.text?.length ?: 0)
            }
            customView { alertBinding.root }
            okButton {
                onSave(alertBinding.editView.text?.toString().orEmpty())
            }
            cancelButton()
        }
    }

    private fun backupIgnore() {
        val checkedItems = BooleanArray(BackupConfig.ignoreKeys.size) {
            BackupConfig.ignoreConfig[BackupConfig.ignoreKeys[it]] ?: false
        }
        this@BackupComposeActivity.alert(R.string.restore_ignore) {
            multiChoiceItems(BackupConfig.ignoreTitle, checkedItems) { _, which, isChecked ->
                BackupConfig.ignoreConfig[BackupConfig.ignoreKeys[which]] = isChecked
            }
            onDismiss {
                BackupConfig.saveIgnoreConfig()
            }
        }
    }

    private fun backup() {
        val backupPath = AppConfig.backupPath
        if (backupPath.isNullOrEmpty()) {
            backupDir.launch()
        } else if (backupPath.isContentScheme()) {
            lifecycleScope.launch {
                val canWrite = withContext(IO) {
                    FileDoc.fromDir(backupPath).checkWrite()
                }
                if (canWrite) {
                    backup(backupPath)
                } else {
                    backupDir.launch()
                }
            }
        } else {
            backup(backupPath)
        }
    }

    private fun backup(backupPath: String) {
        waitDialog.setText("备份中…")
        waitDialog.setOnCancelListener {
            backupJob?.cancel()
        }
        waitDialog.show()
        backupJob?.cancel()
        backupJob = lifecycleScope.launch {
            try {
                Backup.backupLocked(this@BackupComposeActivity, backupPath)
                appCtx.toastOnUi(R.string.backup_success)
            } catch (e: Throwable) {
                ensureActive()
                AppLog.put("备份出错\n${e.localizedMessage}", e)
                appCtx.toastOnUi(getString(R.string.backup_fail, e.localizedMessage))
            } finally {
                ensureActive()
                waitDialog.dismiss()
            }
        }
    }

    private fun restore() {
        waitDialog.setText(R.string.loading)
        waitDialog.setOnCancelListener {
            restoreJob?.cancel()
        }
        waitDialog.show()
        Coroutine.async {
            restoreJob = currentCoroutineContext()[Job]
            showRestoreDialog(this@BackupComposeActivity)
        }.onError {
            AppLog.put("恢复备份出错WebDavError\n${it.localizedMessage}", it)
            this@BackupComposeActivity.alert(titleResource = R.string.restore) {
                setMessage("WebDavError\n${it.localizedMessage}\n将从本地备份恢复。")
                okButton {
                    restoreFromLocal()
                }
                cancelButton()
            }
        }.onFinally {
            waitDialog.dismiss()
        }
    }

    private suspend fun showRestoreDialog(context: Context) {
        val names = withContext(IO) { AppWebDav.getBackupNames() }
        if (AppWebDav.isJianGuoYun && names.size > 700) {
            context.toastOnUi("由于坚果云限制列出文件数量，部分备份可能未显示，请及时清理旧备份")
        }
        if (names.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            withContext(Main) {
                context.selector(
                    title = context.getString(R.string.select_restore_file),
                    items = names
                ) { _, index ->
                    if (index in 0 until names.size) {
                        restoreWebDav(names[index])
                    }
                }
            }
        } else {
            throw NoStackTraceException("Web dav no back up file")
        }
    }

    private fun restoreWebDav(name: String) {
        waitDialog.setText("恢复中…")
        waitDialog.show()
        val task = Coroutine.async {
            AppWebDav.restoreWebDav(name)
        }.onError {
            AppLog.put("WebDav恢复出错\n${it.localizedMessage}", it)
            appCtx.toastOnUi("WebDav恢复出错\n${it.localizedMessage}")
        }.onFinally {
            waitDialog.dismiss()
        }
        waitDialog.setOnCancelListener {
            task.cancel()
        }
    }

    private fun restoreFromLocal() {
        restoreDoc.launch {
            title = getString(R.string.select_restore_file)
            mode = HandleFileContract.FILE
            allowExtensions = arrayOf("zip")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        waitDialog.dismiss()
    }
}

private fun String?.ifNullOrBlank(fallback: String): String {
    return if (this.isNullOrBlank()) fallback else this
}
