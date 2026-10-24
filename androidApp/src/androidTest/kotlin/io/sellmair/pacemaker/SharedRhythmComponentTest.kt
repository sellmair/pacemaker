package io.sellmair.pacemaker

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.sellmair.pacemaker.ui.PacemakerIcons
import io.sellmair.pacemaker.ui.SharedRhythmTheme
import io.sellmair.pacemaker.ui.SharedRhythmColors
import io.sellmair.pacemaker.ui.widget.PacemakerDeviceCard
import io.sellmair.pacemaker.ui.widget.PacemakerLabeledInput
import io.sellmair.pacemaker.ui.widget.PacemakerLimitControl
import io.sellmair.pacemaker.ui.widget.PacemakerMemberRow
import io.sellmair.pacemaker.ui.widget.PacemakerNoticeTone
import io.sellmair.pacemaker.ui.widget.PacemakerPrimaryButton
import io.sellmair.pacemaker.ui.widget.PacemakerSecondaryButton
import io.sellmair.pacemaker.ui.widget.PacemakerStatusNotice
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Controlled production composables: no sensor, database or saved profile is changed. */
@RunWith(AndroidJUnit4::class)
class SharedRhythmComponentTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun limitButtonsEmitValuesAndRespectEditableBoundaries() {
        val value = mutableIntStateOf(150)
        compose.setContent {
            SharedRhythmTheme(dark = false) {
                PacemakerLimitControl(value.intValue, { value.intValue = it })
            }
        }
        compose.onNodeWithContentDescription("Increase limit").performClick()
        compose.runOnIdle { assertEquals(151, value.intValue) }
        compose.onNodeWithContentDescription("Decrease limit").performClick()
        compose.runOnIdle { assertEquals(150, value.intValue) }
        compose.runOnIdle { value.intValue = 40 }
        compose.onNodeWithContentDescription("Decrease limit").assertIsNotEnabled()
        compose.runOnIdle { value.intValue = 200 }
        compose.onNodeWithContentDescription("Increase limit").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Your heart-rate limit")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "200 bpm"))
    }

    @Test
    fun storedOutOfRangeLimitIsNotSilentlyChangedByRendering() {
        val callbacks = mutableListOf<Int>()
        compose.setContent {
            SharedRhythmTheme(dark = true) {
                PacemakerLimitControl(220, { callbacks += it })
            }
        }
        compose.onNodeWithText("220").assertIsDisplayed()
        compose.runOnIdle { assertTrue(callbacks.isEmpty()) }
        compose.onNodeWithContentDescription("Increase limit").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Decrease limit").performClick()
        compose.runOnIdle { assertEquals(listOf(200), callbacks) }
    }

    @Test
    fun selectedDeviceExposesSelectionAndInvokesOnlyRequestedAction() {
        var selections = 0
        compose.setContent {
            SharedRhythmTheme(dark = false) {
                PacemakerDeviceCard(
                    title = "Polar H10",
                    detail = "Connected",
                    icon = PacemakerIcons.Sensor,
                    selected = true,
                    actionLabel = "Use this sensor",
                    onAction = { selections++ }
                )
            }
        }
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            .assertExists()
        compose.onNodeWithText("Use this sensor").performClick()
        compose.runOnIdle { assertEquals(1, selections) }
    }

    @Test
    fun darkComponentsRenderWithLocalFonts() = captureComponents(dark = true)

    @Test
    fun lightComponentsRenderWithLocalFonts() = captureComponents(dark = false)

    private fun captureComponents(dark: Boolean) {
        compose.setContent {
            SharedRhythmTheme(dark = dark) {
                ComponentBoard()
            }
        }
        compose.onAllNodesWithText("142")[0].assertIsDisplayed()
        compose.onAllNodesWithText("Sebastian")[0].assertIsDisplayed()
        compose.waitForIdle()
        val directory = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "design-validation"
        ).apply { mkdirs() }
        val file = File(directory, if (dark) "components-dark-native.png" else "components-light-native.png")
        file.outputStream().use { output ->
            check(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        assertTrue("Expected native capture at " + file.absolutePath, file.length() > 0)
    }
}

@Composable
private fun ComponentBoard() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(PacemakerIcons.Mark, contentDescription = "Pacemaker", tint = SharedRhythmColors().accentText)
            Text("SHARED RHYTHM", style = MaterialTheme.typography.headlineLarge)
            Text("142", style = MaterialTheme.typography.displayLarge)
            PacemakerMemberRow("Sebastian", "SE", 142, 150, Color(0xFF9DAA65))
            PacemakerLabeledInput("Display name", "Sebastian", {})
            PacemakerDeviceCard("Polar H10", "Connected", PacemakerIcons.Sensor, true, "Use this sensor", {})
            PacemakerPrimaryButton("Connect", {})
            PacemakerSecondaryButton("Disconnect", {})
            PacemakerStatusNotice("Above your limit", "157 bpm · your limit is 150.", PacemakerNoticeTone.Danger)
        }
    }
}
