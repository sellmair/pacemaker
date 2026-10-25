package io.sellmair.pacemaker.wear

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearStateVisualTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun nativeStatePressureBatch() {
        val state = mutableStateOf(wearFixtureState())
        compose.setContent {
            WearUiFixture { WearAppContent(state.value, WearFixtureNow, false, {}) }
        }
        val cases = listOf(
            "at-limit" to wearFixtureState().copy(heartRate = 150),
            "above-limit" to wearFixtureState().copy(heartRate = 151),
            "no-reading" to wearFixtureState().copy(measuredAtEpochMillis = WearFixtureNow - 15_001L),
            "phone-lost" to wearFixtureState().copy(companionConnected = false),
            "permissions" to wearFixtureState().copy(heartRate = null, status = "Allow heart rate access in Settings"),
            "starting" to wearFixtureState().copy(isRunning = false, isStarting = true, heartRate = null, status = "Starting workout"),
        )
        cases.forEach { (name, value) ->
            compose.runOnIdle { state.value = value }
            compose.captureWearFixture("state-$name")
            if (name == "permissions" || name == "no-reading") {
                val scroll = compose.onRoot().fetchSemanticsNode().boundsInRoot.height * 0.2f
                compose.onNode(hasScrollToIndexAction())
                    .performSemanticsAction(SemanticsActions.ScrollBy) { action -> action(0f, scroll) }
                compose.captureWearFixture("state-$name-detail")
                compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
            }
        }
        compose.onNodeWithText(fixtureString(R.string.status_starting)).assertExists()
    }
}
