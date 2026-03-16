package io.legado.app.ui.theme

import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeResolverTest {

    @Test
    fun `day theme keeps light background and opaque surfaces`() {
        val state = ThemeResolver.resolve(
            snapshot(
                isDark = false,
                backgroundColor = 0xFFF5F1EB.toInt(),
                bottomBackgroundColor = 0xFFE8DED3.toInt()
            )
        )

        assertFalse(state.isDark)
        assertEquals(0xFFF5F1EB.toInt(), state.background.toArgb())
        assertEquals(0xFFE8DED3.toInt(), state.surfaceContainer.toArgb())
    }

    @Test
    fun `night theme falls back when background is too light`() {
        val state = ThemeResolver.resolve(
            snapshot(
                isDark = true,
                backgroundColor = 0xFFF4F4F4.toInt(),
                fallbackBackgroundColor = 0xFF111111.toInt()
            )
        )

        assertTrue(state.isDark)
        assertEquals(0xFF111111.toInt(), state.background.toArgb())
    }

    @Test
    fun `pure black overrides dark background roles`() {
        val state = ThemeResolver.resolve(
            snapshot(
                isDark = true,
                isPureBlack = true,
                backgroundColor = 0xFF202020.toInt(),
                bottomBackgroundColor = 0xFF303030.toInt()
            )
        )

        assertEquals(0xFF000000.toInt(), state.background.toArgb())
        assertEquals(0xFF000000.toInt(), state.surface.toArgb())
        assertEquals(0xFF121212.toInt(), state.surfaceContainer.toArgb())
    }

    @Test
    fun `eink overrides all colors to monochrome`() {
        val state = ThemeResolver.resolve(snapshot(isEInk = true))

        assertTrue(state.isEInk)
        assertEquals(0xFFFFFFFF.toInt(), state.background.toArgb())
        assertEquals(0xFF000000.toInt(), state.onSurface.toArgb())
        assertEquals(0xFF000000.toInt(), state.accentCompat.toArgb())
    }

    @Test
    fun `background image makes surfaces transparent`() {
        val state = ThemeResolver.resolve(
            snapshot(
                backgroundImagePath = "/tmp/bg.png"
            )
        )

        assertTrue(state.hasBackgroundImage)
        assertTrue(state.isTransparent)
        assertEquals(0x00000000, state.background.toArgb())
        assertEquals(0x00000000, state.surface.toArgb())
        assertEquals(0x00000000, state.surfaceContainer.toArgb())
    }

    @Test
    fun `immersive navigation flag controls navigation color`() {
        val immersive = ThemeResolver.resolve(
            snapshot(
                immNavigationBar = true,
                bottomBackgroundColor = 0xFFE0E7F0.toInt()
            )
        )
        val nonImmersive = ThemeResolver.resolve(
            snapshot(
                immNavigationBar = false,
                bottomBackgroundColor = 0xFFE0E7F0.toInt()
            )
        )

        assertEquals(0xFFE0E7F0.toInt(), immersive.navigationBar.toArgb())
        assertTrue(nonImmersive.navigationBar.toArgb() != immersive.navigationBar.toArgb())
    }

    private fun snapshot(
        isDark: Boolean = false,
        isEInk: Boolean = false,
        isTransparentStatusBar: Boolean = false,
        immNavigationBar: Boolean = true,
        isPureBlack: Boolean = false,
        primaryColor: Int = 0xFF8A5A44.toInt(),
        accentColor: Int = 0xFFB84F39.toInt(),
        backgroundColor: Int = 0xFFF2EEE8.toInt(),
        bottomBackgroundColor: Int = 0xFFE8DDD2.toInt(),
        fallbackBackgroundColor: Int = if (isDark) 0xFF101418.toInt() else 0xFFF2EEE8.toInt(),
        fallbackBottomBackgroundColor: Int = if (isDark) 0xFF171B1F.toInt() else 0xFFE8DDD2.toInt(),
        backgroundImagePath: String? = null
    ): ThemeInputSnapshot {
        return ThemeInputSnapshot(
            isDark = isDark,
            isEInk = isEInk,
            isTransparentStatusBar = isTransparentStatusBar,
            immNavigationBar = immNavigationBar,
            isPureBlack = isPureBlack,
            primaryColor = primaryColor,
            accentColor = accentColor,
            backgroundColor = backgroundColor,
            bottomBackgroundColor = bottomBackgroundColor,
            fallbackBackgroundColor = fallbackBackgroundColor,
            fallbackBottomBackgroundColor = fallbackBottomBackgroundColor,
            backgroundImagePath = backgroundImagePath
        )
    }
}
