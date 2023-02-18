@file:Suppress("OPT_IN_USAGE")

package io.sellmair.broadheart.hrSensor

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flattenMerge

interface HrReceiver {
    val measurements: Flow<HrMeasurement>
}

fun HrReceiver(vararg receiver: HrReceiver?): HrReceiver = HrReceiver(receiver.toList())

fun HrReceiver(receivers: List<HrReceiver?>): HrReceiver = object : HrReceiver {
    override val measurements: Flow<HrMeasurement>
        get() = receivers.filterNotNull()
            .map { receiver -> receiver.measurements }
            .asFlow()
            .flattenMerge()

}