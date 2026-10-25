package io.sellmair.pacemaker.wear

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearIdentityTest {
    @Test
    fun installedAdaptiveAndMonochromeMarksRenderThroughAndroidResources() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val icon = context.applicationInfo.loadIcon(context.packageManager)
        assertTrue("Installed watch icon must be adaptive", icon is AdaptiveIconDrawable)
        val adaptive = icon as AdaptiveIconDrawable
        val monochrome = adaptive.monochrome
        assertNotNull("Installed watch icon must expose the supplied monochrome mark", monochrome)
        val directory = File(context.getExternalFilesDir(null), "design-validation").apply { mkdirs() }
        fun capture(drawable: Drawable, name: String, backdrop: Int = Color.TRANSPARENT) {
            val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(backdrop)
            drawable.setBounds(0, 0, 256, 256)
            drawable.draw(canvas)
            File(directory, name).outputStream().use {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
            }
        }
        capture(adaptive, "icon-adaptive-native.png")
        capture(monochrome!!, "icon-monochrome-native.png", Color.BLACK)
    }
}
