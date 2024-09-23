@file:Suppress("OPT_IN_USAGE")
@file:OptIn(ExperimentalCoroutinesApi::class)

package io.sellmair.pacemaker

import io.sellmair.pacemaker.bluetooth.HeartRateSensorBluetoothService
import io.sellmair.pacemaker.bluetooth.toEvent
import io.sellmair.evas.emit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.launch

internal fun CoroutineScope.launchHeartRateSensorMeasurement(
    heartRateSensorBluetoothService: Deferred<HeartRateSensorBluetoothService>
) = launch {
    /* Connecting our hr receiver and emit hr measurement events  */
    heartRateSensorBluetoothService.await().newSensorsNearby
        .flatMapMerge { sensor -> sensor.heartRate }
        .collect { measurement -> measurement.toEvent().emit() }
}

