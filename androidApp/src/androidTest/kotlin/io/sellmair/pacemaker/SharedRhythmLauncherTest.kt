package io.sellmair.pacemaker

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Draws the installed phone manifest's real launcher layers using Android's icon mask. */
class SharedRhythmLauncherTest {
    @Test
    @SdkSuppress(minSdkVersion = 33)
    fun installedApplicationHasAdaptiveAndMonochromeIdentityLayers() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val installedIcon = context.packageManager.getApplicationIcon(context.packageName)
        assertTrue("Phone manifest must resolve to the new adaptive icon", installedIcon is AdaptiveIconDrawable)
        val adaptive = installedIcon as AdaptiveIconDrawable
        val monochrome = adaptive.monochrome
        assertNotNull("Themed launchers need the monochrome layer", monochrome)
        val directory = File(context.getExternalFilesDir(null), "design-validation").apply { mkdirs() }
        for ((name, drawable) in listOf(
            "launcher-adaptive-native.png" to adaptive,
            "launcher-monochrome-native.png" to AdaptiveIconDrawable(
                ColorDrawable(Color.rgb(243, 245, 237)),
                monochrome!!.constantState!!.newDrawable().mutate().apply { setTint(Color.rgb(23, 26, 25)) }
            )
        )) {
            val bitmap = Bitmap.createBitmap(432, 432, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, 432, 432)
            drawable.draw(Canvas(bitmap))
            File(directory, name).outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        }
    }
}
