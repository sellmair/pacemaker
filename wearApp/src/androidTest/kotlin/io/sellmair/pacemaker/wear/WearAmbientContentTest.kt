package io.sellmair.pacemaker.wear

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearAmbientContentTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun nativeAmbientMatrixOmitsLiveReadingAndUsesBlackMonochromeSummary() {
        compose.setContent {
            WearUiFixture { WearAppContent(wearFixtureState(), WearFixtureNow, true, {}) }
        }
        compose.onNodeWithText("142").assertDoesNotExist()
        compose.onNodeWithText(fixtureString(R.string.stop)).assertDoesNotExist()
        compose.onNodeWithText(fixtureString(R.string.start)).assertDoesNotExist()
        compose.onNodeWithText(fixtureString(R.string.group_count, 3)).assertDoesNotExist()
        compose.onNodeWithText(fixtureString(R.string.wear_workout_active)).assertIsDisplayed()
        compose.onNodeWithText(fixtureString(R.string.status_connected)).assertIsDisplayed()
        compose.onNodeWithText(fixtureString(R.string.own_limit, 150)).assertIsDisplayed()
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        var black = 0
        for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
            val argb = pixels[x, y].toArgb()
            val red = (argb ushr 16) and 255
            val green = (argb ushr 8) and 255
            val blue = argb and 255
            assertTrue("Ambient pixel must be monochrome at $x,$y", maxOf(red, green, blue) - minOf(red, green, blue) <= 2)
            if (red == 0 && green == 0 && blue == 0) black++
        }
        assertTrue("Ambient background must be predominantly black", black.toDouble() / (pixels.width * pixels.height) > 0.8)
        compose.captureWearFixture("ambient")
    }

    @Test
    fun ambientEntryAndWakePreserveGroupPositionWithoutWorkoutCallbacks() {
        val ambient = mutableStateOf(false)
        var toggles = 0
        compose.setContent {
            WearUiFixture { WearAppContent(wearFixtureState(), WearFixtureNow, ambient.value, { toggles++ }) }
        }
        val group = fixtureString(R.string.group_count, 3)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(group))
        compose.onNodeWithText(group).performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Sam"))
        compose.runOnIdle { ambient.value = true }
        compose.onNodeWithText("Sam").assertDoesNotExist()
        compose.onNodeWithText("142").assertDoesNotExist()
        compose.onNodeWithText(fixtureString(R.string.wear_workout_active)).assertIsDisplayed()
        compose.runOnIdle { ambient.value = false }
        compose.onNodeWithText("Sam").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, toggles) }
    }

    @Test
    fun ambientStartingAndLostPhoneRemainExplicit() {
        compose.setContent {
            WearUiFixture {
                WearAppContent(
                    wearFixtureState().copy(isRunning = false, isStarting = true, companionConnected = false),
                    WearFixtureNow,
                    true,
                    {},
                )
            }
        }
        compose.onNodeWithText(fixtureString(R.string.status_starting)).assertIsDisplayed()
        compose.onNodeWithText(fixtureString(R.string.status_phone_lost)).assertIsDisplayed()
        compose.onNodeWithText(fixtureString(R.string.status_ready)).assertDoesNotExist()
        compose.onNodeWithText("142").assertDoesNotExist()
    }
}
