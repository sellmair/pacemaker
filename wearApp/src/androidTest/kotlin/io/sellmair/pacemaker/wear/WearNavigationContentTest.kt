package io.sellmair.pacemaker.wear

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearNavigationContentTest {
    @get:Rule val compose = createComposeRule()

    private fun groupLabel(): String = fixtureString(R.string.group_count, 3)

    private fun openGroup() {
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(groupLabel()))
        compose.onNodeWithText(groupLabel()).performClick()
        compose.onNodeWithText(fixtureString(R.string.close)).assertIsDisplayed()
    }

    @Test
    fun closeRestoresHomeScrollWithoutWorkoutAction() {
        var toggles = 0
        compose.setContent {
            WearUiFixture { WearAppContent(wearFixtureState(), WearFixtureNow, false, { toggles++ }) }
        }
        openGroup()
        compose.onNodeWithText(fixtureString(R.string.close)).performClick()
        compose.onNodeWithText(groupLabel()).assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, toggles) }
    }

    @Test
    fun systemBackAndSwipeReturnWithoutStoppingWorkout() {
        var toggles = 0
        compose.setContent {
            WearUiFixture { WearAppContent(wearFixtureState(), WearFixtureNow, false, { toggles++ }) }
        }
        openGroup()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithText(groupLabel()).assertIsDisplayed()
        openGroup()
        compose.onRoot().performTouchInput { swipeRight() }
        compose.waitForIdle()
        compose.onNodeWithText(groupLabel()).assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, toggles) }
    }

    @Test
    fun savedRouteAndHomeScrollSurviveRestoration() {
        var toggles = 0
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WearUiFixture { WearAppContent(wearFixtureState(), WearFixtureNow, false, { toggles++ }) }
        }
        openGroup()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(fixtureString(R.string.close)).assertIsDisplayed()
        compose.onNodeWithText(fixtureString(R.string.close)).performClick()
        compose.onNodeWithText(groupLabel()).assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, toggles) }
    }

    @Test
    fun groupScrollSurvivesRestorationAndRoundTrip() {
        var toggles = 0
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            WearUiFixture { WearAppContent(wearFixtureState(), WearFixtureNow, false, { toggles++ }) }
        }
        openGroup()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Sam"))
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Sam").assertIsDisplayed()
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
        compose.onNodeWithText(groupLabel()).assertIsDisplayed().performClick()
        compose.onNodeWithText("Sam").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, toggles) }
    }

    @Test
    fun nativeGroupMatrix() {
        compose.setContent {
            WearUiFixture { WearAppContent(wearFixtureState(), WearFixtureNow, false, {}) }
        }
        val label = groupLabel()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(label))
        compose.onNodeWithText(label).performClick()
        compose.captureWearFixture("group-opening")
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(4)
        compose.captureWearFixture("group-members")
    }
}
