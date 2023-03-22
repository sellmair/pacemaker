import io.sellmair.broadheart.HeartRateReceiver
import io.sellmair.broadheart.bluetooth.Ble
import io.sellmair.broadheart.bluetooth.receiveHeartRateMeasurements
import io.sellmair.broadheart.model.HeartRateMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

fun AndroidBleHeartRateReceiver(ble: Ble): HeartRateReceiver {
    return object : HeartRateReceiver {
        override val measurements: Flow<HeartRateMeasurement>
            get() = flow { emitAll(ble.receiveHeartRateMeasurements()) }
    }
}