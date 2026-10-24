package io.sellmair.pacemaker

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.sellmair.pacemaker.ui.PageRouter
import io.sellmair.pacemaker.ui.SharedRhythmTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedRhythmNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun eachBottomTabDirectlyOpensItsScreen() {
        compose.setContent {
            SharedRhythmTheme(dark = true) {
                PageRouter { page -> Text("Screen: " + page.name) }
            }
        }
        compose.onNodeWithText("Screen: Live").assertIsDisplayed()
        compose.onNodeWithText("Devices").performClick()
        compose.onNodeWithText("Screen: Devices").assertIsDisplayed()
        compose.onNodeWithText("Profile").performClick()
        compose.onNodeWithText("Screen: Profile").assertIsDisplayed()
        compose.onNodeWithText("Live").performClick()
        compose.onNodeWithText("Screen: Live").assertIsDisplayed()
    }

    @Test
    fun backFromDevicesAndProfileReturnsToLive() {
        compose.setContent {
            SharedRhythmTheme(dark = true) {
                PageRouter { page -> Text("Screen: " + page.name) }
            }
        }
        for (tab in listOf("Devices", "Profile")) {
            compose.onNodeWithText(tab).performClick()
            compose.onNodeWithText("Screen: " + tab).assertIsDisplayed()
            compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            compose.onNodeWithText("Screen: Live").assertIsDisplayed()
        }
    }

    @Test
    fun selectedTabSurvivesSavedStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            SharedRhythmTheme(dark = true) {
                PageRouter { page -> Text("Screen: " + page.name) }
            }
        }
        compose.onNodeWithText("Profile").performClick()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Screen: Profile").assertIsDisplayed()
    }
}
