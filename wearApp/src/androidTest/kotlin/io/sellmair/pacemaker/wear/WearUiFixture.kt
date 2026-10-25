package io.sellmair.pacemaker.wear

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.LocaleList
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.Locale
import org.junit.Assert.assertTrue

internal const val WearFixtureNow = 1_000_000L

internal fun wearFixtureState() = WearWorkoutState(
    isRunning = true,
    companionConnected = true,
    sourceSelected = true,
    heartRate = 142,
    status = "With phone",
    userId = "preview-you",
    userName = "Preview user",
    heartRateLimit = 150,
    measuredAtEpochMillis = WearFixtureNow,
    group = listOf(
        WearGroupMember("preview-you", "You", 142, 150, WearFixtureNow),
        WearGroupMember("preview-alex", "Alex", 136, 145, WearFixtureNow),
        WearGroupMember("preview-sam", "Sam", 151, 160, WearFixtureNow),
    ),
)

private fun fixtureConfiguration(original: Configuration): Configuration {
    val args = InstrumentationRegistry.getArguments()
    return Configuration(original).apply {
        setLocales(LocaleList(Locale.forLanguageTag(args.getString("fixtureLocale") ?: "en")))
        fontScale = args.getString("fixtureFontScale")?.toFloat() ?: 1f
    }
}

@Composable
internal fun WearUiFixture(content: @Composable () -> Unit) {
    val original = LocalContext.current
    val density = LocalDensity.current.density
    val baseConfiguration = LocalConfiguration.current
    val configuration = remember(baseConfiguration) { fixtureConfiguration(baseConfiguration) }
    val localized = remember(original, configuration) { original.createConfigurationContext(configuration) }
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides configuration,
        LocalDensity provides Density(density, configuration.fontScale),
    ) {
        WearSharedRhythmTheme(content)
    }
}

internal fun fixtureString(@StringRes id: Int, vararg args: Any): String {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    return context.createConfigurationContext(fixtureConfiguration(context.resources.configuration))
        .getString(id, *args)
}

internal fun ComposeContentTestRule.captureWearFixture(name: String) {
    waitForIdle()
    val args = InstrumentationRegistry.getArguments()
    val profile = args.getString("fixtureProfile") ?: "small"
    val locale = args.getString("fixtureLocale") ?: "en"
    val scale = args.getString("fixtureFontScale") ?: "1.0"
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val directory = File(context.getExternalFilesDir(null), "design-validation").apply { mkdirs() }
    val capture = File(directory, "$name-$profile-$locale-$scale.png")
    capture.outputStream().use { output ->
        assertTrue(
            "Native fixture should encode as PNG",
            onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, output),
        )
    }
    assertTrue("Expected complete native capture", capture.length() > 0)
}

internal fun ComposeContentTestRule.assertOpeningInkFitsRoundDisplay() {
    val pixels = onRoot().captureToImage().toPixelMap()
    val centerX = pixels.width / 2.0
    val centerY = pixels.height / 2.0
    val radius = minOf(pixels.width, pixels.height) / 2.0 - 1.0
    var outside = 0
    var first = ""
    for (y in 0 until pixels.height) for (x in 0 until pixels.width) {
        val argb = pixels[x, y].toArgb()
        val red = (argb ushr 16) and 255
        val green = (argb ushr 8) and 255
        val blue = argb and 255
        val differsFromCanvas = maxOf(kotlin.math.abs(red - 0x17), kotlin.math.abs(green - 0x1A), kotlin.math.abs(blue - 0x19)) > 8
        val dx = x + 0.5 - centerX
        val dy = y + 0.5 - centerY
        if (differsFromCanvas && dx * dx + dy * dy > radius * radius) {
            outside++
            if (first.isEmpty()) first = "$x,$y"
        }
    }
    assertTrue("Opening ink would clip on the physical circle: $outside pixels, first at $first", outside == 0)
}
