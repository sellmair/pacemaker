package io.sellmair.pacemaker

import android.graphics.Bitmap
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.ui.Page
import io.sellmair.pacemaker.ui.PageRouter
import io.sellmair.pacemaker.ui.SharedRhythmTheme
import io.sellmair.pacemaker.ui.settingsPage.ProfilePageContent
import io.sellmair.pacemaker.ui.settingsPage.ProfileScreenContentState
import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedRhythmProfileTest {
    private val originalLocales = android.os.LocaleList.getDefault()
    @After fun restoreLocale() { android.os.LocaleList.setDefault(originalLocales); Locale.setDefault(originalLocales[0]) }
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun validNameCommitsImmediatelyButBlankDraftCannotOverwriteSavedName() {
        val committed = mutableListOf<String>()
        render(onNameCommit = { committed += it })
        compose.onNode(hasSetTextAction()).performTextReplacement("Mila")
        compose.runOnIdle { assertEquals(listOf("Mila"), committed) }
        compose.onNode(hasSetTextAction()).performTextReplacement("   ")
        compose.runOnIdle { assertEquals(listOf("Mila"), committed) }
        compose.onNode(hasSetTextAction()).assertIsDisplayed()
    }

    @Test
    fun nameDraftSurvivesUnrelatedPreferenceStateUpdates() {
        val state = mutableStateOf(sample())
        render(initial = state.value, stateProvider = { state.value })
        compose.onNode(hasSetTextAction()).performTextReplacement("Unfinished")
        compose.runOnIdle {
            state.value = state.value.copy(utteranceState = UtteranceState.Silence)
        }
        compose.onNodeWithText("Unfinished").assertIsDisplayed()
    }

    @Test
    fun profileLimitUsesExistingHeartRateCallback() {
        val values = mutableListOf<HeartRate>()
        render(onLimitChanged = { values += it })
        compose.onNodeWithContentDescription("Increase limit").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(151f, values.single().value, 0f) }
    }

    @Test
    fun profileVoiceCanEmitAllThreeModes() {
        val modes = mutableListOf<UtteranceState>()
        render(onVoiceChanged = { modes += it })
        for ((label, mode) in listOf(
            "Off" to UtteranceState.Silence,
            "Warnings only" to UtteranceState.Warnings,
            "All updates" to UtteranceState.All
        )) {
            compose.onNodeWithText("Voice alerts", substring = true).performScrollTo()
                .performClick()
            compose.onAllNodesWithText(label).onLast().performClick()
            compose.runOnIdle { assertEquals(mode, modes.last()) }
        }
    }

    @Test
    fun identityHueRetainsTheFullEditableRange() {
        val hues = mutableListOf<Float>()
        render(onHueChanged = { hues += it })
        compose.onNodeWithContentDescription("Identity color").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(300f) }
        compose.runOnIdle { assertEquals(300f, hues.single(), 0.01f) }
    }

    @Test
    fun storedFractionalLimitOutsideRangeWarnsWithoutChangingIt() {
        val original = sample()
        val edits = mutableListOf<HeartRate>()
        render(
            initial = original.copy(me = original.me.copy(heartRateLimit = HeartRate(200.1f))),
            onLimitChanged = { edits += it })
        compose.onNodeWithText("Stored limit is outside", substring = true).performScrollTo()
            .assertIsDisplayed()
        compose.runOnIdle { assertTrue(edits.isEmpty()) }
    }

    @Test
    fun darkProfileHasNativeCapture() {
        render()
        compose.onNodeWithText("MAKE IT YOURS.").assertIsDisplayed()
        capture("profile-dark-native.png")
    }

    @Test
    fun lightProfileHasNativeCapture() {
        render(dark = false)
        compose.onNodeWithText("MAKE IT YOURS.").assertIsDisplayed()
        capture("profile-light-native.png")
    }

    @Test
    fun compactLargeFontProfileKeepsLowerSettingsReachable() {
        render(compactLargeFont = true)
        capture("profile-compact-large-font-initial-native.png")
        compose.onNodeWithText("Voice alerts", substring = true).performScrollTo()
            .assertIsDisplayed()
        capture("profile-compact-large-font-scrolled-native.png")
    }

    @Test
    fun germanCompactLargeFontProfileKeepsVoiceControlReachable() {
        render(compactLargeFont = true, locale = "de")
        compose.onNodeWithText("MACH ES ZU DEINEM.").assertIsDisplayed()
        capture("profile-german-large-font-initial-native.png")
        compose.onNodeWithText("Sprachmeldungen").performScrollTo().assertIsDisplayed()
        capture("profile-german-large-font-scrolled-native.png")
    }

    private fun render(
        initial: ProfileScreenContentState = sample(),
        stateProvider: (() -> ProfileScreenContentState)? = null,
        dark: Boolean = true,
        compactLargeFont: Boolean = false,
        locale: String? = null,
        onNameCommit: (String) -> Unit = {},
        onLimitChanged: (HeartRate) -> Unit = {},
        onVoiceChanged: (UtteranceState) -> Unit = {},
        onHueChanged: (Float) -> Unit = {}
    ) {
        locale?.let { val value = Locale.forLanguageTag(it); Locale.setDefault(value); android.os.LocaleList.setDefault(android.os.LocaleList(value)) }
        var draft by mutableStateOf(initial.displayNameDraft)
        compose.setContent {
            val density = LocalDensity.current
            val fixtureDensity =
                if (compactLargeFont) Density(density.density * 1.285714f, 1.6f) else density
            SharedRhythmTheme(dark = dark) {
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
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        PageRouter { page ->
                            if (page == Page.Profile) {
                                ProfilePageContent(
                                    state = (stateProvider?.invoke() ?: initial).copy(
                                        displayNameDraft = draft
                                    ),
                                    onDisplayNameDraftChanged = { draft = it },
                                    onNameCommit = onNameCommit,
                                    onHeartRateLimitChanged = onLimitChanged,
                                    onHueChanged = onHueChanged,
                                    onUtteranceStateChanged = onVoiceChanged
                                )
                            } else Text("Fixture: " + page.name)
                        }
                    }
                }
            }
        }
        compose.onNodeWithText(if (locale == "de") "Profil" else "Profile").performClick()
    }

    private fun sample() = ProfileScreenContentState(
        me = MeState(User(UserId(1), "Sebastian"), null, HeartRate(150f)),
        displayNameDraft = "Sebastian",
        hue = 72f,
        utteranceState = UtteranceState.All
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
}
