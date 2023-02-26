package io.sellmair.broadheart.hrSensor.polar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiDefaultImpl
import io.sellmair.broadheart.hrSensor.HrReceiver
import io.sellmair.broadheart.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlin.time.TimeSource

class PolarHrReceiver(private val context: Context) : HrReceiver {
    override val measurements: Flow<HeartRateMeasurement> = flow {
        /* Check for permissions. Only run once permissions are granted */
        while (context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            delay(1000)
        }

        val api = PolarBleApiDefaultImpl.defaultImplementation(context, PolarBleApi.FEATURE_HR)

        val measurements = api.startListenForPolarHrBroadcasts(null).asFlow().map { data ->
            HeartRateMeasurement(
                heartRate = HeartRate(data.hr.toFloat()),
                sensorInfo = HeartRateSensorInfo(
                    id = HeartRateSensorId(data.polarDeviceInfo.deviceId),
                    address = data.polarDeviceInfo.address,
                    vendor = HeartRateSensorInfo.Vendor.Polar,
                    rssi = data.polarDeviceInfo.rssi
                ),
                receivedTime = TimeSource.Monotonic.markNow()
            )
        }

        emitAll(measurements)
    }
}
