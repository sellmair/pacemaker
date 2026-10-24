package io.sellmair.pacemaker

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.sellmair.pacemaker.ui.SharedRhythmColors
import io.sellmair.pacemaker.ui.SharedRhythmTheme
import io.sellmair.pacemaker.ui.StartupScreen
import io.sellmair.pacemaker.ui.StartupStatus
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedRhythmStartupTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun deniedPermissionStartupPaintsDarkCanvasAndCanRetry() {
        var retries = 0
        compose.setContent {
            SharedRhythmTheme(dark = true) {
                StartupScreen(StartupStatus.PermissionRequired, { retries++ })
            }
        }
        compose.onNode(hasClickAction()).performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        val image = compose.onRoot().captureToImage()
        assertEquals(SharedRhythmColors.Charcoal.toArgb(), image.toPixelMap()[1, 1].toArgb())
        writeCapture("startup-permission-dark-native.png", image.asAndroidBitmap())
    }

    @Test
    fun deniedPermissionStartupPaintsLightCanvas() {
        compose.setContent {
            SharedRhythmTheme(dark = false) {
                StartupScreen(StartupStatus.PermissionRequired, {})
            }
        }
        compose.onNode(hasClickAction()).assertIsDisplayed()
        val image = compose.onRoot().captureToImage()
        assertEquals(SharedRhythmColors.Chalk.toArgb(), image.toPixelMap()[1, 1].toArgb())
        writeCapture("startup-permission-light-native.png", image.asAndroidBitmap())
    }

    @Test
    fun compactLargeFontStartupKeepsRetryReachableByScrolling() {
        compose.setContent {
            val density = LocalDensity.current
            SharedRhythmTheme(dark = true) {
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                    Box(Modifier.size(width = 320.dp, height = 240.dp)) {
                        StartupScreen(StartupStatus.PermissionRequired, {})
                    }
                }
            }
        }
        compose.onNode(hasClickAction()).performScrollTo().assertIsDisplayed()
    }

    private fun writeCapture(name: String, bitmap: Bitmap) {
        val folder = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "design-validation"
        ).apply { mkdirs() }
        val file = File(folder, name)
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        assertTrue(file.length() > 0)
    }
}
