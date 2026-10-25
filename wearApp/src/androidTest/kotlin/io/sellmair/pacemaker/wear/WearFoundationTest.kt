package io.sellmair.pacemaker.wear

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearFoundationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun sharedRhythmThemeAndScaffoldsRenderBrandTypographyAndPalette() {
        var background: Color? = null
        var foreground: Color? = null
        var primary: Color? = null
        var onPrimary: Color? = null
        var success: Color? = null
        var warning: Color? = null
        var error: Color? = null
        var muted: Color? = null
        var surface: Color? = null
        var raisedSurface: Color? = null
        var numeralFont: FontFamily? = null
        var numeralWeight: FontWeight? = null
        var bodyFont: FontFamily? = null

        compose.setContent {
            WearSharedRhythmTheme {
                val colors = MaterialTheme.colorScheme
                val typography = MaterialTheme.typography
                SideEffect {
                    background = colors.background
                    foreground = colors.onBackground
                    primary = colors.primary
                    onPrimary = colors.onPrimary
                    success = colors.secondary
                    warning = colors.tertiary
                    error = colors.error
                    muted = colors.onSurfaceVariant
                    surface = colors.surfaceContainer
                    raisedSurface = colors.surfaceContainerHigh
                    numeralFont = typography.numeralExtraLarge.fontFamily
                    numeralWeight = typography.numeralExtraLarge.fontWeight
                    bodyFont = typography.bodySmall.fontFamily
                }
                AppScaffold(
                    modifier = Modifier.fillMaxSize(),
                    timeText = {},
                    containerColor = colors.background,
                    contentColor = colors.onBackground,
                ) {
                    ScreenScaffold(
                        scrollState = rememberLazyListState(),
                        timeText = {},
                        scrollIndicator = null,
                    ) { contentPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding)
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            androidx.wear.compose.material3.Text(
                                text = "Your heart rate",
                                style = typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                            androidx.wear.compose.material3.Text(
                                text = "142",
                                style = typography.numeralExtraLarge,
                                color = colors.primary,
                            )
                            androidx.wear.compose.material3.Text(
                                text = "bpm · limit 150",
                                style = typography.bodySmall,
                                color = colors.onBackground,
                            )
                            androidx.wear.compose.material3.Text(
                                text = "Within your limit · 3 together",
                                style = typography.labelSmall,
                                color = colors.secondary,
                            )
                        }
                    }
                }
            }
        }

        compose.onNodeWithText("Your heart rate").assertIsDisplayed()
        compose.onNodeWithText("142").assertIsDisplayed()
        compose.onNodeWithText("bpm · limit 150").assertIsDisplayed()
        compose.onNodeWithText("Within your limit · 3 together").assertIsDisplayed()
        compose.waitForIdle()

        assertEquals(Color(0xFF171A19), background)
        assertEquals(Color(0xFFF3F5ED), foreground)
        assertEquals(Color(0xFFDBFF00), primary)
        assertEquals(Color(0xFF171A19), onPrimary)
        assertEquals(Color(0xFFA3E8BC), success)
        assertEquals(Color(0xFFFFB86B), warning)
        assertEquals(Color(0xFFFF8E84), error)
        assertEquals(Color(0xFFA9B3AA), muted)
        assertEquals(Color(0xFF222624), surface)
        assertEquals(Color(0xFF2C312E), raisedSurface)
        assertEquals(
            FontFamily(Font(R.font.barlow_condensed_semibold, weight = FontWeight.SemiBold)),
            numeralFont,
        )
        assertEquals(
            FontFamily(Font(R.font.inter_variable, weight = FontWeight.Normal)),
            bodyFont,
        )
        assertEquals(FontWeight.SemiBold, numeralWeight)

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory =
            File(context.getExternalFilesDir(null), "design-validation").apply { mkdirs() }
        val capture = File(directory, "foundation-native.png")
        capture.outputStream().use { output ->
            assertTrue(
                "Wear foundation capture should encode as PNG",
                compose.onRoot().captureToImage().asAndroidBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, output),
            )
        }
        assertTrue("Expected native capture at ${capture.absolutePath}", capture.length() > 0)
    }
}
