@file:Suppress("OPT_IN_USAGE")

package io.sellmair.broadheart.hrSensor

import io.sellmair.broadheart.model.HeartRateMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flattenMerge

interface HeartRateReceiver {
    val measurements: Flow<HeartRateMeasurement>
}

fun HeartRateReceiver(vararg receiver: HeartRateReceiver?): HeartRateReceiver = HeartRateReceiver(receiver.toList())

fun HeartRateReceiver(receivers: List<HeartRateReceiver?>): HeartRateReceiver = object : HeartRateReceiver {
    override val measurements: Flow<HeartRateMeasurement>
        get() = receivers.filterNotNull()
            .map { receiver -> receiver.measurements }
            .asFlow()
            .flattenMerge()

}