package io.sellmair.pacemaker.wear

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearHomeContentTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun freshnessExpiresFromClockWithoutNewBackendEmission() {
        val now = mutableStateOf(WearFixtureNow + 15_000L)
        compose.setContent {
            WearUiFixture {
                WearAppContent(wearFixtureState(), now.value, false, {})
            }
        }
        compose.onNodeWithText("142").assertIsDisplayed()
        compose.runOnIdle { now.value += 1L }
        compose.onNodeWithText("142").assertDoesNotExist()
        compose.onNodeWithText("—").assertIsDisplayed()
        compose.onNodeWithText(fixtureString(R.string.wear_bpm_limit, 150)).assertExists()
        compose.onNodeWithText("Within your limit", substring = true).assertDoesNotExist()
    }

    @Test
    fun missingAndFutureTimestampsNeverClaimLiveBpm() {
        val state = mutableStateOf(wearFixtureState().copy(measuredAtEpochMillis = null))
        compose.setContent {
            WearUiFixture { WearAppContent(state.value, WearFixtureNow, false, {}) }
        }
        compose.onNodeWithText("142").assertDoesNotExist()
        compose.onNodeWithText("—").assertIsDisplayed()
        compose.runOnIdle { state.value = state.value.copy(measuredAtEpochMillis = WearFixtureNow + 1L) }
        compose.onNodeWithText("142").assertDoesNotExist()
        compose.onNodeWithText("—").assertIsDisplayed()
    }

    @Test
    fun equalityAndAboveLimitHaveDistinctExplicitCopy() {
        val state = mutableStateOf(wearFixtureState().copy(heartRate = 150))
        compose.setContent {
            WearUiFixture { WearAppContent(state.value, WearFixtureNow, false, {}) }
        }
        compose.onNodeWithText("At your limit", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Above your limit", substring = true).assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(heartRate = 151) }
        compose.onNodeWithText("Above your limit", substring = true).assertIsDisplayed()
        compose.onNodeWithText("At your limit", substring = true).assertDoesNotExist()
    }

    @Test
    fun absentLimitRetainsFreshReadingWithoutThresholdClaim() {
        compose.setContent {
            WearUiFixture {
                WearAppContent(wearFixtureState().copy(heartRateLimit = null), WearFixtureNow, false, {})
            }
        }
        compose.onNodeWithText("142").assertIsDisplayed()
        compose.onNodeWithText("Within your limit", substring = true).assertDoesNotExist()
        compose.onNodeWithText("At your limit", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Above your limit", substring = true).assertDoesNotExist()
    }

    @Test
    fun freshWristReadingSurvivesPhoneLossAndStopCallsBack() {
        var toggles = 0
        compose.setContent {
            WearUiFixture {
                WearAppContent(
                    wearFixtureState().copy(companionConnected = false),
                    WearFixtureNow,
                    false,
                    { toggles++ },
                )
            }
        }
        compose.onNodeWithText("142").assertIsDisplayed()
        compose.onNodeWithText("With phone").assertDoesNotExist()
        val stop = fixtureString(R.string.stop)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(stop))
        compose.onNodeWithText(stop).assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, toggles) }
    }

    @Test
    fun startDisablesWhileStartingAndConnectKeepsItsCallback() {
        val state = mutableStateOf(wearFixtureState().copy(isRunning = false, heartRate = null))
        var toggles = 0
        compose.setContent {
            WearUiFixture { WearAppContent(state.value, WearFixtureNow, false, { toggles++ }) }
        }
        val start = fixtureString(R.string.start)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(start))
        compose.onNodeWithText(start).assertIsEnabled().performClick()
        compose.runOnIdle { state.value = state.value.copy(isStarting = true) }
        compose.onNodeWithText(start).assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(1, toggles)
            state.value = state.value.copy(isStarting = false, sourceSelected = false)
        }
        val connect = fixtureString(R.string.connect_phone)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(connect))
        compose.onNodeWithText(connect).assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(2, toggles) }
    }

    @Test
    fun stopFailureKeepsActionableExplanationAndEnabledStop() {
        compose.setContent {
            WearUiFixture {
                WearAppContent(
                    wearFixtureState().copy(heartRate = null, status = "Could not stop workout"),
                    WearFixtureNow,
                    false,
                    {},
                )
            }
        }
        val failure = fixtureString(R.string.status_stop_failed)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(failure, substring = true))
        compose.onNodeWithText(failure, substring = true).assertIsDisplayed()
        val stop = fixtureString(R.string.stop)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(stop))
        compose.onNodeWithText(stop).assertIsEnabled()
    }

    @Test
    fun importantOperationalNoticesSurviveFreshWristReadings() {
        val state = mutableStateOf(wearFixtureState())
        compose.setContent {
            WearUiFixture { WearAppContent(state.value, WearFixtureNow, false, {}) }
        }
        val cases = listOf(
            "Session sync unavailable" to R.string.status_session_sync,
            "Allow heart rate access in Settings" to R.string.status_permissions,
            "Could not start workout" to R.string.status_start_failed,
            "Could not stop workout" to R.string.status_stop_failed,
            "Google Play services is unavailable" to R.string.status_platform_unavailable,
        )
        cases.forEach { (status, resource) ->
            compose.runOnIdle { state.value = state.value.copy(status = status) }
            compose.onNodeWithText("142").assertExists()
            compose.onNodeWithText(fixtureString(resource), substring = true).assertExists()
        }
    }

    @Test
    fun openingReadingFitsTheRoundSafeAreaAndActionsStayBelowIt() {
        compose.setContent {
            WearUiFixture { WearAppContent(wearFixtureState(), WearFixtureNow, false, {}) }
        }
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val radius = minOf(root.width, root.height) / 2f
        val center = root.center
        listOf("YOUR HEART RATE", "142", "bpm · limit 150", "Within your limit · 3 together").forEach { text ->
            val bounds = compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
            listOf(bounds.topLeft, bounds.topRight, bounds.bottomLeft, bounds.bottomRight).forEach { corner ->
                val dx = corner.x - center.x
                val dy = corner.y - center.y
                assertTrue("Opening text outside the round display: $text at $bounds", dx * dx + dy * dy <= radius * radius)
            }
        }
        compose.onNodeWithText(fixtureString(R.string.stop)).assertIsNotDisplayed()
        compose.onNodeWithText("Within your limit · 3 together")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                val layouts = mutableListOf<TextLayoutResult>()
                assertTrue(action(layouts))
                assertEquals("Default reference status should fit one line", 1, layouts.single().lineCount)
            }
    }

    @Test
    fun healthyIdlePreservesReadyAndConnectedStatus() {
        val state = mutableStateOf(wearFixtureState().copy(isRunning = false, heartRate = null, status = "Ready"))
        compose.setContent {
            WearUiFixture { WearAppContent(state.value, WearFixtureNow, false, {}) }
        }
        compose.onNodeWithText("—").assertIsDisplayed()
        compose.onNodeWithText(fixtureString(R.string.status_ready)).assertExists()
        compose.onNodeWithText(fixtureString(R.string.wear_bpm_limit, 150)).assertExists()
        compose.runOnIdle { state.value = state.value.copy(status = "Connected to Pacemaker") }
        compose.onNodeWithText(fixtureString(R.string.status_connected)).assertExists()
        compose.onNodeWithText(fixtureString(R.string.status_hr_unavailable)).assertDoesNotExist()
        val start = fixtureString(R.string.start)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(start))
        compose.onNodeWithText(start).assertIsEnabled()
    }

    @Test
    fun nativeHomeMatrixOpeningAndControls() {
        compose.setContent {
            WearUiFixture { WearAppContent(wearFixtureState(), WearFixtureNow, false, {}) }
        }
        compose.onNodeWithText("142").assertIsDisplayed()
        compose.onNodeWithText("Preview user").assertDoesNotExist()
        compose.onNodeWithText("SHARED RHYTHM").assertDoesNotExist()
        compose.captureWearFixture("home-opening")
        compose.assertOpeningInkFitsRoundDisplay()
        val stop = fixtureString(R.string.stop)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(stop))
        compose.onNodeWithText(stop).assertIsDisplayed().assertIsEnabled()
        compose.captureWearFixture("home-controls")
    }
}
