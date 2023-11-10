@file:Suppress("OPT_IN_USAGE")

package io.sellmair.pacemaker.ble

import io.sellmair.pacemaker.utils.LogTag
import io.sellmair.pacemaker.utils.debug
import io.sellmair.pacemaker.utils.toNSData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import okio.ByteString.Companion.toByteString
import platform.CoreBluetooth.CBCharacteristic
import platform.CoreBluetooth.CBCharacteristicWriteWithoutResponse
import platform.CoreBluetooth.CBService

internal class AppleConnectableController(
    private val scope: CoroutineScope,
    private val centralHardware: AppleCentralHardware,
    private val connectableHardware: AppleConnectableHardware,
) : BleConnectableController {

    override val deviceName: String? = connectableHardware.peripheral.name

    override val deviceId: BleDeviceId = connectableHardware.peripheral.deviceId

    override val isConnected = MutableStateFlow(false)

    override val values: SharedFlow<BleReceivedValue>
        get() = connectableHardware.delegate.didUpdateValue
            .mapNotNull { event ->
                val characteristic = connectableHardware.serviceDescriptor
                    .findCharacteristic(event.characteristic.UUID) ?: return@mapNotNull null
                val value = event.characteristic.value?.toByteString()?.toByteArray() ?: return@mapNotNull null
                BleReceivedValue(deviceId, characteristic, value)
            }
            .shareIn(scope, SharingStarted.WhileSubscribed())

    override val rssi: MutableStateFlow<Int> = MutableStateFlow(-1)

    override suspend fun connect(): BleResult<Unit> {
        log.debug("connect()")
        centralHardware.manager.connectPeripheral(connectableHardware.peripheral, mapOf<Any?, Any>())
        val didConnectFlow = centralHardware.delegate.didConnectPeripheral
            .filter { it.peripheral == connectableHardware.peripheral }

        val didFailFlow = centralHardware.delegate.didFailConnectToPeripheral
            .filter { it.peripheral == connectableHardware.peripheral }

        val result = flowOf(didConnectFlow, didFailFlow).flattenMerge().first()

        if (result is AppleCentralManagerDelegate.DidFailConnectToPeripheral) {
            return BleFailure.Message(result.error?.localizedDescription ?: "N/A")
        }

        if (result is AppleCentralManagerDelegate.DidConnectPeripheral) {
            return BleSuccess()
        }

        throw IllegalStateException("Unexpected result type")
    }

    override suspend fun disconnect(): BleResult<Unit> {
        centralHardware.manager.cancelPeripheralConnection(connectableHardware.peripheral)
        return BleSuccess()
    }

    override suspend fun discoverService(): BleResult<Set<BleUUID>> {
        connectableHardware.peripheral.discoverServices(listOf(connectableHardware.serviceDescriptor.uuid))
        val result = connectableHardware.delegate.didDiscoverServices.first()
        return if (result.error == null) BleSuccess(setOf(connectableHardware.serviceDescriptor.uuid))
        else BleFailure.Message(result.error.localizedDescription)
    }

    override suspend fun discoverCharacteristics(): BleResult<Unit> {
        val service = connectableHardware.peripheral.services.orEmpty().map { it as CBService }.find { service ->
            service.UUID == connectableHardware.serviceDescriptor.uuid
        } ?: return BleFailure.Message("${connectableHardware.serviceDescriptor} not found")

        connectableHardware.peripheral.discoverCharacteristics(
            connectableHardware.serviceDescriptor.characteristics.map { it.uuid }, service
        )

        val result = connectableHardware.delegate.didDiscoverCharacteristics
            .first { it.service == service }

        if (result.error != null) {
            return BleFailure.Message(result.error.localizedDescription)
        }

        return BleSuccess()
    }

    override suspend fun enableNotification(characteristicDescriptor: BleCharacteristicDescriptor): BleResult<Unit> {
        return resolve(characteristicDescriptor).map { characteristic ->
            connectableHardware.peripheral.setNotifyValue(true, characteristic)
            return BleSuccess()
        }
    }

    override suspend fun readValue(characteristicDescriptor: BleCharacteristicDescriptor): BleResult<ByteArray> {
        return resolve(characteristicDescriptor).flatMap { characteristic ->
            connectableHardware.peripheral.readValueForCharacteristic(characteristic)
            val result = connectableHardware.delegate.didUpdateValue.first { it.characteristic == characteristic }
            val value = characteristic.value
            if (result.error != null) {
                BleFailure.Message(result.error.localizedDescription)
            } else if (value == null) {
                BleFailure.Message("No data received")
            } else BleSuccess(value.toByteString().toByteArray())
        }
    }

    override suspend fun writeValue(
        characteristicDescriptor: BleCharacteristicDescriptor,
        value: ByteArray
    ): BleResult<Unit> {
        return resolve(characteristicDescriptor).flatMap { characteristic ->
            connectableHardware.peripheral.writeValue(
                value.toNSData(),
                characteristic,
                CBCharacteristicWriteWithoutResponse
            )
            BleSuccess()
        }
    }

    private fun resolve(serviceDescriptor: BleServiceDescriptor): BleResult<CBService> {
        val service = connectableHardware.peripheral.services.orEmpty().map { it as CBService }.find { service ->
            service.UUID == serviceDescriptor.uuid
        } ?: return BleFailure.Message("${connectableHardware.serviceDescriptor} not found")
        return BleSuccess(service)
    }

    private fun resolve(characteristicDescriptor: BleCharacteristicDescriptor): BleResult<CBCharacteristic> {
        return resolve(connectableHardware.serviceDescriptor).flatMap { service ->
            val characteristic = service.characteristics.orEmpty().map { it as CBCharacteristic }
                .find { it.UUID == characteristicDescriptor.uuid }
                ?: return@flatMap BleFailure.Message("$characteristicDescriptor not found")
            BleSuccess(characteristic)
        }
    }

    init {
        centralHardware.delegate.didConnectPeripheral
            .filter { it.peripheral == connectableHardware.peripheral }
            .onEach { isConnected.value = true }
            .launchIn(scope)

        centralHardware.delegate.didDisconnectPeripheral
            .filter { it.peripheral == connectableHardware.peripheral }
            .onEach { isConnected.value = false }
            .launchIn(scope)
    }

    companion object {
        val log = LogTag.ble.forClass<AppleConnectableController>()
    }
}