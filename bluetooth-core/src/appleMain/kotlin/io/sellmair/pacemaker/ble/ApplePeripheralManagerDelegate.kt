@file:Suppress("unused")

package io.sellmair.pacemaker.ble

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import platform.CoreBluetooth.CBATTRequest
import platform.CoreBluetooth.CBPeripheralManager
import platform.CoreBluetooth.CBPeripheralManagerDelegateProtocol
import platform.CoreBluetooth.CBService
import platform.Foundation.NSError
import platform.darwin.NSObject

internal class ApplePeripheralManagerDelegate(
    private val scope: CoroutineScope
) : NSObject(), CBPeripheralManagerDelegateProtocol {

    private val thisDelegate = this

    /* State */

    val state = MutableStateFlow<Long?>(null)

    override fun peripheralManagerDidUpdateState(peripheral: CBPeripheralManager) {
        state.value = peripheral.state
    }


    /* Is ready to update subscribers */

    val isReadyToUpdateSubscribers = MutableSharedFlow<Unit>()

    override fun peripheralManagerIsReadyToUpdateSubscribers(peripheral: CBPeripheralManager) {
        scope.launch {
            thisDelegate.isReadyToUpdateSubscribers.emit(Unit)
        }
    }

    /* Did receive read request */

    class DidReceiveReadRequest(val peripheral: CBPeripheralManager, val request: CBATTRequest)

    val didReceiveReadRequest = MutableSharedFlow<DidReceiveReadRequest>()

    override fun peripheralManager(peripheral: CBPeripheralManager, didReceiveReadRequest: CBATTRequest) {
        scope.launch {
            thisDelegate.didReceiveReadRequest.emit(DidReceiveReadRequest(peripheral, didReceiveReadRequest))
        }
    }

    /* Did receive write request */

    class DidReceiveWriteRequest(
        val peripheral: CBPeripheralManager, val request: CBATTRequest
    )

    val didReceiveWriteRequest = MutableSharedFlow<DidReceiveWriteRequest>()

    override fun peripheralManager(peripheral: CBPeripheralManager, didReceiveWriteRequests: List<*>) {
        didReceiveWriteRequests.forEach { writeRequest ->
            scope.launch {
                writeRequest as CBATTRequest
                thisDelegate.didReceiveWriteRequest.emit(DidReceiveWriteRequest(peripheral, writeRequest))
            }
        }
    }

    /* Did add service */

    class DidAddService(val peripheral: CBPeripheralManager, val service: CBService, val error: NSError?)

    val didAddService = MutableSharedFlow<DidAddService>()

    override fun peripheralManager(peripheral: CBPeripheralManager, didAddService: CBService, error: NSError?) {
        scope.launch {
            thisDelegate.didAddService.emit(DidAddService(peripheral, didAddService, error))
        }
    }

}