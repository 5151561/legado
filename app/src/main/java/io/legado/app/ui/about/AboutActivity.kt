package io.legado.app.ui.about

import android.os.Bundle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.StarRate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import io.legado.app.R
import io.legado.app.base.BaseActivity
import io.legado.app.constant.AppConst.appInfo
import io.legado.app.constant.AppLog
import io.legado.app.databinding.ActivityAboutBinding
import io.legado.app.help.CrashHandler
import io.legado.app.help.config.AppConfig
import io.legado.app.help.coroutine.Coroutine
import io.legado.app.help.update.AppUpdate
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.compose.theme.SettingsItemUi
import io.legado.app.ui.compose.theme.SettingsMaterialScreen
import io.legado.app.ui.compose.theme.SettingsSectionUi
import io.legado.app.ui.widget.dialog.TextDialog
import io.legado.app.ui.widget.dialog.WaitDialog
import io.legado.app.utils.FileDoc
import io.legado.app.utils.compress.ZipUtils
import io.legado.app.utils.createFileIfNotExist
import io.legado.app.utils.createFolderIfNotExist
import io.legado.app.utils.delete
import io.legado.app.utils.externalCache
import io.legado.app.utils.find
import io.legado.app.utils.list
import io.legado.app.utils.openInputStream
import io.legado.app.utils.openOutputStream
import io.legado.app.utils.openUrl
import io.legado.app.utils.sendMail
import io.legado.app.utils.sendToClip
import io.legado.app.utils.share
import io.legado.app.utils.showDialogFragment
import io.legado.app.utils.toastOnUi
import io.legado.app.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.delay
import splitties.init.appCtx
import java.io.File

class AboutActivity : BaseActivity<ActivityAboutBinding>() {

    override val binding by viewBinding(ActivityAboutBinding::inflate)
    private val waitDialog by lazy { WaitDialog(this) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.composeAboutContent.setContent {
            LegadoComposeTheme {
                SettingsMaterialScreen(
                    title = getString(R.string.about),
                    subtitle = "${getString(R.string.app_name)} ${appInfo.versionName}",
                    sections = listOf(
                        SettingsSectionUi(
                            items = listOf(
                                SettingsItemUi("contributors", Icons.Rounded.Info, getString(R.string.contributors), getString(R.string.contributors_summary)),
                                SettingsItemUi("update_log", Icons.Rounded.Info, getString(R.string.update_log), "${getString(R.string.version)} ${appInfo.versionName}"),
                                SettingsItemUi("check_update", Icons.Rounded.Info, getString(R.string.check_update)),
                                SettingsItemUi("mail", Icons.Rounded.Info, getString(R.string.email)),
                                SettingsItemUi("gzGzh", Icons.Rounded.Info, getString(R.string.legado_gzh), "点击复制公众号")
                            )
                        ),
                        SettingsSectionUi(
                            label = getString(R.string.other),
                            items = listOf(
                                SettingsItemUi("crashLog", Icons.Rounded.Info, getString(R.string.crash_log)),
                                SettingsItemUi("saveLog", Icons.Rounded.Info, getString(R.string.save_log)),
                                SettingsItemUi("createHeapDump", Icons.Rounded.Info, getString(R.string.create_heap_dump)),
                                SettingsItemUi("privacyPolicy", Icons.Rounded.Info, getString(R.string.privacy_policy)),
                                SettingsItemUi("license", Icons.Rounded.Info, getString(R.string.license)),
                                SettingsItemUi("disclaimer", Icons.Rounded.Info, getString(R.string.disclaimer))
                            )
                        )
                    ),
                    onBackClick = ::finish,
                    actions = {
                        IconButton(onClick = { openUrl("market://details?id=$packageName") }) {
                            Icon(Icons.Rounded.StarRate, contentDescription = null)
                        }
                        IconButton(onClick = {
                            share(getString(R.string.app_share_description), getString(R.string.app_name))
                        }) {
                            Icon(Icons.Rounded.Share, contentDescription = null)
                        }
                    },
                    onItemClick = ::handleItemClick
                )
            }
        }
    }

    private fun handleItemClick(key: String) {
        when (key) {
            "contributors" -> openUrl(getString(R.string.contributors_url))
            "update_log" -> showMdFile(getString(R.string.update_log), "updateLog.md")
            "check_update" -> checkUpdate()
            "mail" -> sendMail(getString(R.string.email))
            "license" -> showMdFile(getString(R.string.license), "LICENSE.md")
            "disclaimer" -> showMdFile(getString(R.string.disclaimer), "disclaimer.md")
            "privacyPolicy" -> showMdFile(getString(R.string.privacy_policy), "privacyPolicy.md")
            "gzGzh" -> sendToClip(getString(R.string.legado_gzh))
            "crashLog" -> showDialogFragment<CrashLogsDialog>()
            "saveLog" -> saveLog()
            "createHeapDump" -> createHeapDump()
        }
    }

    private fun showMdFile(title: String, fileName: String) {
        val mdText = String(assets.open(fileName).readBytes())
        showDialogFragment(TextDialog(title, mdText, TextDialog.Mode.MD))
    }

    private fun checkUpdate() {
        waitDialog.show()
        AppUpdate.gitHubUpdate?.run {
            check(lifecycleScope)
                .onSuccess {
                    showDialogFragment(UpdateDialog(it))
                }
                .onError {
                    appCtx.toastOnUi("${getString(R.string.check_update)}\n${it.localizedMessage}")
                }
                .onFinally {
                    waitDialog.dismiss()
                }
        }
    }

    private fun saveLog() {
        Coroutine.async {
            val backupPath = AppConfig.backupPath ?: let {
                appCtx.toastOnUi("未设置备份目录")
                return@async
            }
            if (!AppConfig.recordLog) {
                appCtx.toastOnUi("未开启日志记录，请去其他设置里打开记录日志")
                delay(3000)
            }
            val doc = FileDoc.fromUri(backupPath.toUri(), true)
            copyLogs(doc)
            copyHeapDump(doc)
            appCtx.toastOnUi("已保存至备份目录")
        }.onError {
            AppLog.put("保存日志出错\n${it.localizedMessage}", it, true)
        }
    }

    private fun createHeapDump() {
        Coroutine.async {
            val backupPath = AppConfig.backupPath ?: let {
                appCtx.toastOnUi("未设置备份目录")
                return@async
            }
            if (!AppConfig.recordHeapDump) {
                appCtx.toastOnUi("未开启堆转储记录，请去其他设置里打开记录堆转储")
                delay(3000)
            }
            appCtx.toastOnUi("开始创建堆转储")
            System.gc()
            CrashHandler.doHeapDump(true)
            val doc = FileDoc.fromUri(backupPath.toUri(), true)
            if (!copyHeapDump(doc)) {
                appCtx.toastOnUi("未找到堆转储文件")
            } else {
                appCtx.toastOnUi("已保存至备份目录")
            }
        }.onError {
            AppLog.put("保存堆转储失败\n${it.localizedMessage}", it)
        }
    }

    private fun copyLogs(doc: FileDoc) {
        val cacheDir = appCtx.externalCache
        val logFiles = File(cacheDir, "logs")
        val crashFiles = File(cacheDir, "crash")
        val logcatFile = File(cacheDir, "logcat.txt")

        dumpLogcat(logcatFile)

        val zipFile = File(cacheDir, "logs.zip")
        ZipUtils.zipFiles(arrayListOf(logFiles, crashFiles, logcatFile), zipFile)

        doc.find("logs.zip")?.delete()

        zipFile.inputStream().use { input ->
            doc.createFileIfNotExist("logs.zip").openOutputStream().getOrNull()?.use {
                input.copyTo(it)
            }
        }
        zipFile.delete()
    }

    private fun copyHeapDump(doc: FileDoc): Boolean {
        val heapFile = FileDoc.fromFile(File(appCtx.externalCache, "heapDump")).list()?.firstOrNull()
            ?: return false
        doc.find("heapDump")?.delete()
        val heapDumpDoc = doc.createFolderIfNotExist("heapDump")
        heapFile.openInputStream().getOrNull()?.use { input ->
            heapDumpDoc.createFileIfNotExist(heapFile.name).openOutputStream().getOrNull()?.use {
                input.copyTo(it)
            }
        }
        return true
    }

    private fun dumpLogcat(file: File) {
        try {
            val process = Runtime.getRuntime().exec("logcat -d")
            file.outputStream().use {
                process.inputStream.copyTo(it)
            }
        } catch (e: Exception) {
            AppLog.put("保存Logcat失败\n$e", e)
        }
    }
}
