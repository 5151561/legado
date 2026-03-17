package io.legado.app.base

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import io.legado.app.constant.Theme
import io.legado.app.help.config.ThemeConfig
import io.legado.app.ui.compose.theme.LegadoComposeTheme
import io.legado.app.ui.theme.ThemeResolver
import io.legado.app.utils.*
import android.graphics.drawable.BitmapDrawable
import androidx.activity.addCallback
import androidx.compose.ui.graphics.toArgb
import io.legado.app.constant.AppLog
import io.legado.app.lib.theme.backgroundColor

/**
 * 不使用 ViewBinding 的 Compose 基类
 */
abstract class BaseComposeActivity(
    val fullScreen: Boolean = true,
    private val theme: Theme = Theme.Auto,
    private val toolBarTheme: Theme = Theme.Auto,
    private val imageBg: Boolean = true
) : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppContextWrapper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.decorView.disableAutoFill()
        initTheme()
        super.onCreate(savedInstanceState)
        setupSystemBar()
        upBackgroundImage()
        
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }

        setContent {
            LegadoComposeTheme {
                Content()
            }
        }
        
        observeLiveBus()
        onActivityCreated(savedInstanceState)
    }

    @Composable
    abstract fun Content()

    open fun onActivityCreated(savedInstanceState: Bundle?) {}

    open fun initTheme() {
        val themeState = ThemeResolver.resolve(this)
        when (theme) {
            Theme.Dark -> {
                setTheme(io.legado.app.R.style.AppTheme_Dark)
                window.decorView.applyBackgroundTint(backgroundColor)
            }
            Theme.Light -> {
                setTheme(io.legado.app.R.style.AppTheme_Light)
                window.decorView.applyBackgroundTint(backgroundColor)
            }
            else -> {
                if (themeState.isDark) {
                    setTheme(io.legado.app.R.style.AppTheme_Dark)
                } else {
                    setTheme(io.legado.app.R.style.AppTheme_Light)
                }
                window.decorView.applyBackgroundTint(themeState.background.toArgb())
            }
        }
    }

    open fun upBackgroundImage() {
        if (imageBg) {
            try {
                ThemeConfig.getBgImage(this, windowManager.windowSize)?.let {
                    window.decorView.background = BitmapDrawable(resources, it)
                }
            } catch (e: OutOfMemoryError) {
                toastOnUi("背景图片太大,内存溢出")
            } catch (e: Exception) {
                AppLog.put("加载背景出错\n${e.localizedMessage}", e)
            }
        }
    }

    open fun setupSystemBar() {
        val themeState = ThemeResolver.resolve(this)
        if (fullScreen) {
            fullScreen()
        }
        setStatusBarColorAuto(themeState.statusBar.toArgb(), themeState.isTransparent, fullScreen)
        if (toolBarTheme == Theme.Dark) {
            setLightStatusBar(false)
        } else if (toolBarTheme == Theme.Light) {
            setLightStatusBar(true)
        }
        upNavigationBarColor()
    }

    open fun upNavigationBarColor() {
        setNavigationBarColorAuto(ThemeResolver.resolve(this).navigationBar.toArgb())
    }

    open fun observeLiveBus() {}

    override fun finish() {
        currentFocus?.hideSoftInput()
        super.finish()
    }
}
