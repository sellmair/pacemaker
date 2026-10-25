package io.sellmair.pacemaker.wear

import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearRotaryContentTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun crownScrollingReachesControlsWithoutWorkoutAction() {
        var toggles = 0
        compose.setContent {
            WearUiFixture { WearAppContent(wearFixtureState(), WearFixtureNow, false, { toggles++ }) }
        }
        compose.waitForIdle()
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        repeat(4) {
            val time = SystemClock.uptimeMillis()
            val properties = MotionEvent.PointerProperties().apply { id = 0 }
            val coordinates = MotionEvent.PointerCoords().apply {
                setAxisValue(MotionEvent.AXIS_SCROLL, -3f)
            }
            val event = MotionEvent.obtain(
                time, time, MotionEvent.ACTION_SCROLL, 1,
                arrayOf(properties), arrayOf(coordinates),
                0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_ROTARY_ENCODER, 0,
            )
            try {
                assertTrue("Emulator must accept the native rotary event", automation.injectInputEvent(event, true))
            } finally {
                event.recycle()
            }
            compose.waitForIdle()
        }
        compose.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                compose.onNodeWithText(fixtureString(R.string.stop)).assertIsDisplayed()
                true
            }.getOrDefault(false)
        }
        compose.runOnIdle { assertEquals(0, toggles) }
    }
}
