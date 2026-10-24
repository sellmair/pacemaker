package io.sellmair.pacemaker

import android.app.ActivityManager
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidStartupTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Before
    fun grantRuntimePermissions() {
        val permissions = buildList {
            add("android.permission.BLUETOOTH_CONNECT")
            add("android.permission.BLUETOOTH_SCAN")
            add("android.permission.BLUETOOTH_ADVERTISE")
            if (Build.VERSION.SDK_INT >= 33) add("android.permission.POST_NOTIFICATIONS")
        }

        permissions.forEach { permission ->
            val command = "pm grant ${instrumentation.targetContext.packageName} $permission"
            instrumentation.uiAutomation.executeShellCommand(command).use { descriptor ->
                ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
            }
        }
    }

    @Test
    fun launchesActivityAndKeepsBackendForegroundServiceRunning() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertTrue(activity.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED))
            }

            val activityManager = instrumentation.targetContext
                .getSystemService(ActivityManager::class.java)
            val deadline = System.currentTimeMillis() + 10_000
            var backendIsForeground = false
            while (System.currentTimeMillis() < deadline && !backendIsForeground) {
                backendIsForeground = activityManager.getRunningServices(Int.MAX_VALUE).any { service ->
                    service.service.className == AndroidApplicationBackend::class.java.name && service.foreground
                }
                if (!backendIsForeground) Thread.sleep(100)
            }

            assertTrue("Backend service did not reach foreground state", backendIsForeground)
        }
    }
}
