package io.sellmair.pacemaker.ble

import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.debug
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBPeripheral
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.darwin.NSObject
import kotlin.native.internal.createCleaner

internal class FlowCentralManagerDelegate(
    private val scope: CoroutineScope
) : NSObject(), CBCentralManagerDelegateProtocol {
    private val thisDelegate = this

    /* Did Update State */

    private val state = MutableStateFlow<Long?>(null)

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        state.value = central.state
    }


    /* Discover Peripherals */

    class DidDiscoverPeripheral(
        central: CBCentralManager, peripheral: CBPeripheral, advertisementData: Map<Any?, *>, RSSI: NSNumber
    )

    val didDiscoverPeripheral = MutableSharedFlow<DidDiscoverPeripheral>()

    override fun centralManager(
        central: CBCentralManager, didDiscoverPeripheral: CBPeripheral, advertisementData: Map<Any?, *>, RSSI: NSNumber
    ) {
        scope.launch {
            thisDelegate.didDiscoverPeripheral.emit(
                DidDiscoverPeripheral(
                    central = central,
                    peripheral = didDiscoverPeripheral,
                    advertisementData = advertisementData,
                    RSSI = RSSI
                )
            )
        }
    }


    /* Did Connect Peripheral */

    class DidConnectPeripheral(central: CBCentralManager, peripheral: CBPeripheral)

    val didConnectPeripheral = MutableSharedFlow<DidConnectPeripheral>()

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
        scope.launch {
            thisDelegate.didConnectPeripheral.emit(
                DidConnectPeripheral(central = central, peripheral = didConnectPeripheral)
            )
        }
    }

    /* Did Fail to Connect to Peripheral */

    class DidFailConnectToPeripheral(
        central: CBCentralManager, peripheral: CBPeripheral, error: NSError?
    )

    val didFailConnectToPeripheral = MutableSharedFlow<DidFailConnectToPeripheral>()

    @Suppress("CONFLICTING_OVERLOADS", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun centralManager(central: CBCentralManager, didFailToConnectPeripheral: CBPeripheral, error: NSError?) {
        scope.launch {
            thisDelegate.didFailConnectToPeripheral.emit(
                DidFailConnectToPeripheral(central = central, peripheral = didFailToConnectPeripheral, error = error)
            )
        }
    }


    /* Did Disconnect Peripheral */

    class DidDisconnectPeripheral(val central: CBCentralManager, peripheral: CBPeripheral, error: NSError?)

    val didDisconnectPeripheral = MutableSharedFlow<DidDisconnectPeripheral>()

    @Suppress("CONFLICTING_OVERLOADS", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun centralManager(central: CBCentralManager, didDisconnectPeripheral: CBPeripheral, error: NSError?) {
        scope.launch {
            thisDelegate.didDisconnectPeripheral.emit(
                DidDisconnectPeripheral(
                    central = central,
                    peripheral = didDisconnectPeripheral,
                    error = error
                )
            )
        }
    }

    companion object {
        val log get() = LogTag.ble.forClass<FlowCentralManagerDelegate>()
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Suppress("unused")
    val cleaner = createCleaner(Unit) { log.debug("removed") }
}