package io.sellmair.broadheart

import android.Manifest.permission
import android.app.Activity
import android.content.pm.PackageManager
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiDefaultImpl
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

class PolarHeartRateService {
    private val permissionRequestCode = 1
    fun start(activity: Activity) {
        activity.requestPermissions(
            arrayOf(permission.BLUETOOTH_SCAN, permission.BLUETOOTH_CONNECT),
            permissionRequestCode
        )

        if (activity.checkSelfPermission(permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            val api = PolarBleApiDefaultImpl.defaultImplementation(activity, PolarBleApi.FEATURE_HR)

            MainScope().launch {
                api.startListenForPolarHrBroadcasts(null).asFlow().collect { broadcast ->
                    Me.myHeartRate = HeartRate(broadcast.hr)
                }
            }
        }
    }

}