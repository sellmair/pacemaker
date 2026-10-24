package io.sellmair.pacemaker

import android.graphics.Bitmap
import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.sellmair.pacemaker.ble.BleConnectable
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.HeartRateSensorId
import io.sellmair.pacemaker.ui.Page
import io.sellmair.pacemaker.ui.PageRouter
import io.sellmair.pacemaker.ui.SharedRhythmTheme
import io.sellmair.pacemaker.ui.settingsPage.DeviceSensorContentState
import io.sellmair.pacemaker.ui.settingsPage.DevicesPageContent
import io.sellmair.pacemaker.ui.settingsPage.DevicesScreenContentState
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedRhythmDevicesTest {
    private val originalLocales = android.os.LocaleList.getDefault()
    @After fun restoreLocale() { android.os.LocaleList.setDefault(originalLocales); Locale.setDefault(originalLocales[0]) }
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun automaticSelectionClearsExplicitChoice() {
        val selected = mutableListOf<HeartRateSource?>()
        render(sample().copy(selectedSource = selectedWatch()), onSource = { selected += it })
        compose.onNodeWithText("Automatic").performClick()
        compose.runOnIdle { assertEquals(1, selected.size); assertNull(selected.single()) }
    }

    @Test
    fun selectingSensorDoesNotPretendToConnectIt() {
        val selected = mutableListOf<HeartRateSource?>()
        val connections = mutableListOf<HeartRateSensorId>()
        render(
            sample(connected = false),
            onSource = { selected += it },
            onConnect = { connections += it })
        compose.onNodeWithText("Use this sensor").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(sensorId.value, selected.single()?.id)
            assertEquals(HeartRateSource.Availability.DISCONNECTED, selected.single()?.availability)
            assertTrue(connections.isEmpty())
        }
    }

    @Test
    fun explicitConnectKeepsTheSensorId() {
        val connections = mutableListOf<HeartRateSensorId>()
        render(sample(connected = false), onConnect = { connections += it })
        compose.onNodeWithText("Connect").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(sensorId), connections) }
    }

    @Test
    fun explicitDisconnectKeepsTheSensorId() {
        val connections = mutableListOf<HeartRateSensorId>()
        render(sample(), onDisconnect = { connections += it })
        compose.onNodeWithText("Disconnect").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf(sensorId), connections) }
    }

    @Test
    fun watchSelectionKeepsCandidateIdentity() {
        val selected = mutableListOf<HeartRateSource?>()
        render(sample(), onSource = { selected += it })
        compose.onNodeWithText("Use wrist watch").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("watch-42", selected.single()?.companionDeviceId)
            assertEquals(HeartRateSource.Kind.COMPANION_WATCH, selected.single()?.kind)
        }
    }

    @Test
    fun absentSelectedWatchRemainsVisibleWithoutInventedLiveValue() {
        render(
            sample().copy(
                selectedSource = selectedWatch(),
                companion = WatchCompanionDeviceState()
            )
        )
        compose.onNodeWithText("Couch watch").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("142").assertCountEquals(0)
        capture("devices-selected-offline-watch-native.png")
    }

    @Test
    fun darkDevicesHaveNativeCaptureWithoutTimestampLessLiveReading() {
        render(sample())
        compose.onNodeWithText("YOUR SIGNAL.").assertIsDisplayed()
        compose.onAllNodesWithText("142").assertCountEquals(0)
        capture("devices-dark-native.png")
    }

    @Test
    fun lightDevicesHaveNativeCapture() {
        render(sample(), dark = false)
        compose.onNodeWithText("YOUR SIGNAL.").assertIsDisplayed()
        compose.onNodeWithText("Automatic").assertIsDisplayed()
        android.os.SystemClock.sleep(1200)
        capture("devices-light-native.png")
    }

    @Test
    fun disconnectingSensorDoesNotExposeConnectAction() {
        val state = sample()
        render(state.copy(sensors = state.sensors.map {
            it.copy(
                connection = HeartRateSensorConnectionState(
                    false,
                    null
                )
            )
        }))
        compose.onNodeWithText("Disconnecting").assertIsDisplayed()
        compose.onAllNodesWithText("Connect").assertCountEquals(0)
    }

    @Test
    fun disabledBluetoothRemainsVisibleWithRetainedSensorRows() {
        render(sample().copy(bluetooth = BluetoothState(false, true)))
        compose.onNodeWithText("Bluetooth is off").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun watchSegmentDoesNotReplaceMissingExplicitWatchWithAnotherCandidate() {
        val selections = mutableListOf<HeartRateSource?>()
        render(sample().copy(selectedSource = selectedWatch()), onSource = { selections += it })
        compose.onNodeWithText("Watch").performClick()
        compose.runOnIdle { assertTrue(selections.all { it?.companionDeviceId == "watch-offline" }) }
    }

    @Test
    fun sensorSegmentKeepsExplicitSecondSensorIdentity() {
        val state = sample()
        val second = state.sensors.single().copy(
            sensor = state.sensors.single().sensor.copy(
                id = HeartRateSensorId("second-strap"),
                name = "Second strap"
            )
        )
        val selected = HeartRateSource(
            "second-strap",
            HeartRateSource.Kind.BLE_SENSOR,
            HeartRateSource.Availability.DISCONNECTED
        )
        val selections = mutableListOf<HeartRateSource?>()
        render(
            state.copy(selectedSource = selected, sensors = state.sensors + second),
            onSource = { selections += it })
        compose.onNodeWithText("Sensor").performClick()
        compose.runOnIdle { assertTrue(selections.all { it?.id == "second-strap" }) }
    }

    @Test
    fun unavailableWatchPlatformCannotSelectAnAvailableCandidate() {
        val state = sample()
        val selections = mutableListOf<HeartRateSource?>()
        render(
            state.copy(companion = state.companion.copy(platformAvailable = false)),
            onSource = { selections += it })
        compose.onNodeWithText("Watch").performClick()
        compose.runOnIdle { assertTrue(selections.none { it?.availability == HeartRateSource.Availability.AVAILABLE }) }
    }

    @Test
    fun italianCompactDevicesKeepSelectionAndBlockingNoticeReachable() {
        render(
            sample().copy(bluetooth = BluetoothState(false, true)),
            locale = "it",
            compactLargeFont = true
        )
        compose.onNodeWithText("IL TUO SEGNALE.").assertIsDisplayed()
        capture("devices-italian-large-font-initial-native.png")
        compose.onAllNodesWithText("Bluetooth", substring = true).onLast().performScrollTo().assertIsDisplayed()
        capture("devices-italian-large-font-scrolled-native.png")
    }

    @Test
    fun disabledBluetoothEmptyStateDoesNotClaimScanning() {
        render(sample().copy(sensors = emptyList(), bluetooth = BluetoothState(false, true)))
        compose.onAllNodesWithText("Looking for devices").assertCountEquals(0)
        capture("devices-bluetooth-off-native.png")
    }

    private fun render(
        state: DevicesScreenContentState,
        dark: Boolean = true,
        locale: String? = null,
        compactLargeFont: Boolean = false,
        onSource: (HeartRateSource?) -> Unit = {},
        onConnect: (HeartRateSensorId) -> Unit = {},
        onDisconnect: (HeartRateSensorId) -> Unit = {}
    ) {
        locale?.let { val value = Locale.forLanguageTag(it); Locale.setDefault(value); android.os.LocaleList.setDefault(android.os.LocaleList(value)) }
        compose.setContent {
            val density = LocalDensity.current
            val fixtureDensity =
                if (compactLargeFont) Density(density.density * 1.285714f, 1.6f) else density
            val configuration = Configuration(LocalConfiguration.current).apply {
                locale?.let {
                    setLocale(
                        Locale.forLanguageTag(it)
                    )
                }
            }
            CompositionLocalProvider(
                LocalDensity provides fixtureDensity,
                LocalConfiguration provides configuration
            ) {
                SharedRhythmTheme(dark = dark) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        PageRouter { page ->
                            if (page == Page.Devices) DevicesPageContent(
                                state = state, onSourceSelected = onSource,
                                onSensorConnect = onConnect, onSensorDisconnect = onDisconnect
                            ) else Text("Fixture: " + page.name)
                        }
                    }
                }
            }
        }
        compose.onNodeWithText(if (locale == "it") "Dispositivi" else "Devices").performClick()
    }

    private fun sample(connected: Boolean = true) = DevicesScreenContentState(
        selectedSource = null,
        sensors = listOf(
            DeviceSensorContentState(
                sensor = HeartRateSensorState(
                    sensorId,
                    "Polar H10",
                    -55,
                    HeartRate(142),
                    null,
                    null
                ),
                connection = HeartRateSensorConnectionState(
                    connected,
                    if (connected) BleConnectable.ConnectionState.Connected else BleConnectable.ConnectionState.Disconnected
                )
            )
        ),
        companion = WatchCompanionDeviceState(
            paired = true, connected = true, name = "Wear OS watch",
            candidates = listOf(
                WatchCompanionCandidate(
                    "watch-42",
                    "Wear OS watch",
                    connected = true
                )
            )
        ),
        bluetooth = BluetoothState(true, true)
    )

    private fun selectedWatch() = HeartRateSource(
        id = WATCH_COMPANION_SOURCE_ID,
        kind = HeartRateSource.Kind.COMPANION_WATCH,
        availability = HeartRateSource.Availability.DISCONNECTED,
        connectionName = "Couch watch",
        companionDeviceId = "watch-offline"
    )

    private fun capture(name: String) {
        compose.mainClock.advanceTimeBy(500)
        android.os.SystemClock.sleep(550)
        compose.waitForIdle()
        val directory = File(
            InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),
            "design-validation"
        ).apply { mkdirs() }
        val suffix = InstrumentationRegistry.getArguments().getString("captureSuffix")
        val file = File(directory, if (suffix.isNullOrBlank()) name else name.removeSuffix(".png") + "-" + suffix + ".png")
        file.outputStream().use { output ->
            check(
                compose.onRoot().captureToImage().asAndroidBitmap()
                    .compress(Bitmap.CompressFormat.PNG, 100, output)
            )
        }
        assertTrue(file.length() > 0)
    }

    private companion object {
        val sensorId = HeartRateSensorId("fixture-strap")
    }
}
