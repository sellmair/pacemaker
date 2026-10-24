package io.sellmair.pacemaker

import android.os.ParcelFileDescriptor
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performSemanticsAction
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Runs only on the test emulator: real Activity, Evas routes, and persistence actors. */
@RunWith(AndroidJUnit4::class)
class SharedRhythmPreferencesIntegrationTest {
    @get:Rule
    val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    fun grantPermissions() {
        listOf(
            "BLUETOOTH_CONNECT",
            "BLUETOOTH_SCAN",
            "BLUETOOTH_ADVERTISE",
            "POST_NOTIFICATIONS"
        ).forEach {
            shell("pm grant " + instrumentation.targetContext.packageName + " android.permission." + it)
        }
    }

    @Test
    fun preferencesSurviveRecreationAndImeBackPrecedesNavigation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            compose.waitUntil(15_000) {
                compose.onAllNodesWithText("Profile").fetchSemanticsNodes().isNotEmpty()
            }
            val activePackage =
                instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()
            if (activePackage == "com.android.settings" || activePackage == "com.android.bluetooth") {
                shell("input keyevent 4")
            }
            compose.onNodeWithText("Profile").performClick()
            compose.onNode(hasSetTextAction()).performClick()
                .performTextReplacement("Couch preview")
            compose.waitUntil(5_000) { imeVisible(scenario) }
            shell("input keyevent 4")
            compose.waitUntil(5_000) { !imeVisible(scenario) }
            compose.onNodeWithText("MAKE IT YOURS.").assertIsDisplayed()
            compose.onNodeWithContentDescription("Heart-rate limit").performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(146f) }
            compose.waitUntil(5_000) { compose.onNodeWithContentDescription("Heart-rate limit").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current == 146f }
            compose.onNodeWithContentDescription("Identity color").performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(300f) }
            compose.waitUntil(5_000) { compose.onNodeWithContentDescription("Identity color").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current == 300f }
            compose.onNodeWithText("Voice alerts", substring = true).performScrollTo()
                .performClick()
            compose.onAllNodesWithText("Warnings only").onLast().performClick()
            compose.onNodeWithText("Warnings only", substring = true).assertIsDisplayed()
            scenario.recreate()
            compose.waitUntil(15_000) {
                runCatching {
                    compose.onAllNodesWithText("MAKE IT YOURS.").fetchSemanticsNodes().isNotEmpty()
                }.getOrDefault(false)
            }
            compose.onNodeWithText("Couch preview").performScrollTo().assertIsDisplayed()
            compose.waitUntil(5_000) { compose.onNodeWithContentDescription("Heart-rate limit").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current == 146f }
            compose.waitUntil(5_000) { compose.onNodeWithContentDescription("Identity color").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current == 300f }
            compose.onNodeWithText("Warnings only", substring = true).performScrollTo()
                .assertIsDisplayed()
            shell("input keyevent 4")
            compose.onNodeWithText("YOUR HEART RATE").assertIsDisplayed()
        }
    }

    private fun imeVisible(scenario: ActivityScenario<MainActivity>): Boolean {
        var visible = false
        scenario.onActivity { activity ->
            activity.window.decorView.rootWindowInsets?.let {
                visible = WindowInsetsCompat.toWindowInsetsCompat(it)
                    .isVisible(WindowInsetsCompat.Type.ime())
            }
        }
        return visible
    }

    private fun shell(command: String) {
        instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        }
    }
}
