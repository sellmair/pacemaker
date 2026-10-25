package io.sellmair.pacemaker

import android.graphics.Bitmap
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.ui.SharedRhythmColors
import io.sellmair.pacemaker.ui.SharedRhythmTheme
import io.sellmair.pacemaker.ui.Page
import io.sellmair.pacemaker.ui.PageRouter
import io.sellmair.pacemaker.ui.mainPage.LivePageContent
import io.sellmair.pacemaker.ui.mainPage.LiveScreenContentState
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Fixed samples enter the same stateless screen used by the real Evas route. */
@RunWith(AndroidJUnit4::class)
class SharedRhythmLiveTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun automaticSourceKeepsFreshReadingAndDeduplicatesSelfById() {
        val state = sample()
        render(state.copy(group = GroupState(listOf(
            UserState(me, false, HeartRate(199f), HeartRate(150f), UserColors.default(me.id), now),
            peer(136f)
        ))))
        compose.onAllNodesWithText("142").assertCountEquals(2)
        compose.onAllNodesWithText("199").assertCountEquals(0)
        compose.onNodeWithText("136").assertIsDisplayed()
    }

    @Test
    fun selectedAvailableSourceShowsFreshMeasurement() {
        render(sample().copy(selectedSource = source(HeartRateSource.Availability.AVAILABLE)))
        compose.onAllNodesWithText("142").assertCountEquals(2)
    }

    @Test
    fun expiredOwnValueIsNotShownAsLiveWhilePeerRemains() {
        render(sample().copy(me = MeState(me, HeartRate(142f), HeartRate(150f), now - 60_001)))
        compose.onAllNodesWithText("142").assertCountEquals(0)
        compose.onNodeWithText("136").assertIsDisplayed()
    }

    @Test
    fun futureDatedValueIsNotShownAsLive() {
        render(sample().copy(me = MeState(me, HeartRate(142f), HeartRate(150f), now + 1_000)))
        compose.onAllNodesWithText("142").assertCountEquals(0)
    }

    @Test
    fun unavailableExplicitSourceDoesNotHideFreshPeer() {
        render(sample().copy(selectedSource = source(HeartRateSource.Availability.DISCONNECTED)))
        compose.onAllNodesWithText("142").assertCountEquals(0)
        compose.onNodeWithText("136").assertIsDisplayed()
    }

    @Test
    fun clearedAuthoritativeSelfReadingDoesNotFallBackToGroupMeasurement() {
        render(sample().copy(
            me = MeState(me, null, HeartRate(150f), null),
            group = GroupState(listOf(
                UserState(me, true, HeartRate(142f), HeartRate(150f), UserColors.default(me.id), now),
                peer(136f)
            )),
            selectedSource = source(HeartRateSource.Availability.AVAILABLE)
        ))
        compose.onAllNodesWithText("142").assertCountEquals(0)
        compose.onNodeWithText("136").assertIsDisplayed()
    }

    @Test
    fun unknownPersonalLimitIsNotInvented() {
        render(sample().copy(me = null, group = GroupState()))
        compose.onAllNodesWithText("150").assertCountEquals(0)
    }

    @Test
    fun readingEqualToLimitIsWithinLimit() {
        render(sample().copy(me = MeState(me, HeartRate(150f), HeartRate(150f), now), group = GroupState()))
        compose.onAllNodesWithText("Within your limit")[0].assertIsDisplayed()
    }

    @Test
    fun retainedPeerWithoutFreshSampleDoesNotDisplayOldNumber() {
        render(sample().copy(group = GroupState(listOf(peer(136f, now - 60_001)))))
        compose.onAllNodesWithText("136").assertCountEquals(0)
        compose.onNodeWithText("Mila").assertIsDisplayed()
    }

    @Test
    fun sensorAndPeerReadingsStayVisibleThroughNormalUpdateGaps() {
        render(sample().copy(
            me = MeState(me, HeartRate(142f), HeartRate(150f), now - 59_999),
            group = GroupState(listOf(peer(136f, now - 59_999)))
        ))
        compose.onAllNodesWithText("142").assertCountEquals(2)
        compose.onNodeWithText("136").assertIsDisplayed()
    }

    @Test
    fun lostPeerRemainsVisibleThenRemovalSmoothlyMovesTheFollowingRow() {
        val state = mutableStateOf(sample().copy(group = GroupState(listOf(
            peer(136f, now - 60_001).copy(isConnectionLost = true), alex()
        ))))
        render(state.value, stateProvider = { state.value })
        compose.onNodeWithText("Mila").assertIsDisplayed()
        compose.onNodeWithText("Connection lost").assertIsDisplayed()
        compose.onAllNodesWithText("136").assertCountEquals(0)
        capture("live-retained-lost-peer.png")
        val before = compose.onNodeWithText("Alex").getUnclippedBoundsInRoot().top
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { state.value = state.value.copy(group = GroupState(listOf(alex()))) }
        compose.mainClock.advanceTimeBy(96)
        compose.onAllNodesWithText("Mila").assertCountEquals(1)
        val during = compose.onNodeWithText("Alex").getUnclippedBoundsInRoot().top
        assertTrue("The following row must move gradually", during < before)
        capture("live-peer-removal-midpoint.png")
        compose.mainClock.advanceTimeBy(400)
        compose.onAllNodesWithText("Mila").assertCountEquals(0)
        val after = compose.onNodeWithText("Alex").getUnclippedBoundsInRoot().top
        assertTrue("The collapse must continue after the intermediate frame", after < during)
        capture("live-peer-removal-complete.png")
    }

    @Test
    fun recoveryDuringRemovalKeepsTheSamePeerVisible() {
        val state = mutableStateOf(sample().copy(group = GroupState(listOf(peer(136f)))))
        render(state.value, stateProvider = { state.value })
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { state.value = state.value.copy(group = GroupState()) }
        compose.mainClock.advanceTimeBy(96)
        compose.runOnIdle { state.value = state.value.copy(group = GroupState(listOf(peer(137f)))) }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("Mila").assertIsDisplayed()
        compose.onNodeWithText("137").assertIsDisplayed()
    }

    @Test
    fun aboveLimitDarkScreenHasExplicitStatusAndNativeCapture() {
        render(sample().copy(me = MeState(me, HeartRate(157f), HeartRate(150f), now)))
        compose.onAllNodesWithText("Above your limit")[0].assertIsDisplayed()
        capture("live-above-limit-dark-native.png")
    }

    @Test
    fun peerWithoutPersonalLimitKeepsReadingAndExplicitMissingLimit() {
        render(sample().copy(group = GroupState(listOf(peer(136f).copy(heartRateLimit = null)))))
        compose.onNodeWithText("136").assertIsDisplayed()
        compose.onNodeWithText("Limit not set").assertIsDisplayed()
    }

    @Test
    fun meterExpandsToIncludeReadingAndStoredLimitOutsideDefaultDomain() {
        render(sample().copy(me = MeState(me, HeartRate(30), HeartRate(220), now)))
        compose.onNodeWithText("30 bpm").assertIsDisplayed()
        compose.onNodeWithText("220 bpm").assertIsDisplayed()
        compose.onNodeWithText("Your limit 220").assertIsDisplayed()
    }

    @Test
    fun freshLightScreenHasNativeCapture() {
        render(sample(), dark = false)
        compose.onAllNodesWithText("142").assertCountEquals(2)
        capture("live-within-limit-light-native.png")
    }

    @Test
    fun lostLocalSignalKeepsPeersInDarkNativeCapture() {
        render(sample().copy(
            me = MeState(me, null, HeartRate(150f), null),
            selectedSource = source(HeartRateSource.Availability.DISCONNECTED)
        ))
        compose.onNodeWithText("136").assertIsDisplayed()
        capture("live-no-local-signal-dark-native.png")
    }

    @Test
    fun compactVoiceSelectorCanEmitAllThreeModes() {
        val changes = mutableListOf<UtteranceState>()
        render(sample(), onUtteranceChanged = { changes += it })
        for ((label, expected) in listOf(
            "Off" to UtteranceState.Silence,
            "Warnings only" to UtteranceState.Warnings,
            "All updates" to UtteranceState.All
        )) {
            compose.onNodeWithText("Voice alerts", substring = true).performScrollTo().performClick()
            compose.onNodeWithText(label).performClick()
            compose.runOnIdle { assertEquals(expected, changes.last()) }
        }
    }

    @Test
    fun compactLargeFontLiveKeepsVoiceControlReachable() {
        render(sample(), compactLargeFont = true)
        capture("live-compact-large-font-initial-dark-native.png")
        compose.onNodeWithText("Voice alerts", substring = true).performScrollTo().assertIsDisplayed()
        capture("live-compact-large-font-dark-native.png")
    }

    private fun render(
        state: LiveScreenContentState,
        dark: Boolean = true,
        compactLargeFont: Boolean = false,
        onUtteranceChanged: (UtteranceState) -> Unit = {},
        stateProvider: (() -> LiveScreenContentState)? = null
    ) {
        compose.setContent {
            val density = LocalDensity.current
            val fixtureDensity = if (compactLargeFont) Density(density.density * 1.285714f, 1.6f) else density
            SharedRhythmTheme(dark = dark) {
                CompositionLocalProvider(LocalDensity provides fixtureDensity) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PageRouter { page ->
                        if (page == Page.Live) {
                            LivePageContent(
                                state = stateProvider?.invoke() ?: state,
                                identityColor = SharedRhythmColors.Acid,
                                onUtteranceStateChanged = onUtteranceChanged
                            )
                        } else {
                            Text("Fixture: " + page.name)
                        }
                    }
                }
                }
            }
        }
    }

    private fun sample() = LiveScreenContentState(
        me = MeState(me, HeartRate(142f), HeartRate(150f), now),
        group = GroupState(listOf(peer(136f), alex())),
        selectedSource = null,
        utteranceState = UtteranceState.All,
        nowEpochMillis = now
    )

    private fun peer(bpm: Float, measuredAt: Long = now) = UserState(
        user = User(UserId(2), "Mila"),
        isMe = false,
        heartRate = HeartRate(bpm),
        heartRateLimit = HeartRate(145f),
        color = HSLColor(207.41379f, 1f, 0.772549f),
        measuredAtEpochMillis = measuredAt
    )

    private fun alex() = UserState(
        user = User(UserId(3), "Alex"),
        isMe = false,
        heartRate = HeartRate(148f),
        heartRateLimit = HeartRate(155f),
        color = HSLColor(259.3846f, 1f, 0.872549f),
        measuredAtEpochMillis = now
    )

    private fun source(availability: HeartRateSource.Availability) = HeartRateSource(
        id = "fixture-strap",
        kind = HeartRateSource.Kind.BLE_SENSOR,
        availability = availability,
        heartRate = HeartRate(142f),
        measuredAtEpochMillis = now,
        connectionName = "Polar H10"
    )

    private fun capture(name: String) {
        compose.waitForIdle()
        val directory = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "design-validation"
        ).apply { mkdirs() }
        val file = File(directory, name)
        file.outputStream().use { output ->
            check(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        assertTrue(file.length() > 0)
    }

    private companion object {
        const val now = 1_800_000_000_000L
        val me = User(UserId(1), "Sebastian")
    }
}
