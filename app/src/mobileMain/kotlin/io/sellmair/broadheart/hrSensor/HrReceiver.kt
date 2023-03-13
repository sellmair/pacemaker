@file:Suppress("OPT_IN_USAGE")

package io.sellmair.broadheart.hrSensor

import io.sellmair.broadheart.model.HeartRateMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flattenMerge

interface HrReceiver {
    val measurements: Flow<HeartRateMeasurement>
}

fun HrReceiver(vararg receiver: HrReceiver?): HrReceiver = HrReceiver(receiver.toList())

fun HrReceiver(receivers: List<HrReceiver?>): HrReceiver = object : HrReceiver {
    override val measurements: Flow<HeartRateMeasurement>
        get() = receivers.filterNotNull()
            .map { receiver -> receiver.measurements }
            .asFlow()
            .flattenMerge()

}