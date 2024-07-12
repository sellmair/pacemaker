@file:Suppress("unused")

package io.sellmair.pacemaker.ble

import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.debug
import kotlinx.cinterop.ObjCSignatureOverride
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

internal class AppleCentralManagerDelegate(
    private val scope: CoroutineScope
) : NSObject(), CBCentralManagerDelegateProtocol {

    private val thisDelegate = this

    /* Did Update State */

    val state = MutableStateFlow<Long?>(null)

    override fun centralManagerDidUpdateState(central: CBCentralManager) {
        state.value = central.state
    }


    /* Discover Peripherals */

    class DidDiscoverPeripheral(
        val central: CBCentralManager,
        val peripheral: CBPeripheral,
        val advertisementData: Map<Any?, *>,
        val RSSI: NSNumber
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

    class DidConnectPeripheral(val central: CBCentralManager, val peripheral: CBPeripheral)

    val didConnectPeripheral = MutableSharedFlow<DidConnectPeripheral>()

    override fun centralManager(central: CBCentralManager, didConnectPeripheral: CBPeripheral) {
        log.debug("'didConnectPeripheral'")
        scope.launch {
            thisDelegate.didConnectPeripheral.emit(
                DidConnectPeripheral(central = central, peripheral = didConnectPeripheral)
            )
        }
    }

    /* Did Fail to Connect to Peripheral */

    class DidFailConnectToPeripheral(
        val central: CBCentralManager, val peripheral: CBPeripheral, val error: NSError?
    )

    val didFailConnectToPeripheral = MutableSharedFlow<DidFailConnectToPeripheral>()

    @ObjCSignatureOverride
    override fun centralManager(central: CBCentralManager, didFailToConnectPeripheral: CBPeripheral, error: NSError?) {
        log.debug("'didFailToConnectPeripheral'")
        scope.launch {
            thisDelegate.didFailConnectToPeripheral.emit(
                DidFailConnectToPeripheral(central = central, peripheral = didFailToConnectPeripheral, error = error)
            )
        }
    }


    /* Did Disconnect Peripheral */

    class DidDisconnectPeripheral(val central: CBCentralManager, val peripheral: CBPeripheral, val error: NSError?)

    val didDisconnectPeripheral = MutableSharedFlow<DidDisconnectPeripheral>()

    @ObjCSignatureOverride
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
        val log get() = LogTag.ble.with("AppleCentralManagerDelegate")
    }
}