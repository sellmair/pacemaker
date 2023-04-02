package io.sellmair.pacemaker.ble

data class BleServiceDescriptor(
    val name: String,
    val uuid: BleUUID,
    val characteristics: Set<BleCharacteristicDescriptor>
) {
    private val characteristicsByUUID = characteristics.associateBy { it.uuid }

    fun findCharacteristic(uuid: BleUUID): BleCharacteristicDescriptor? = characteristicsByUUID[uuid]

    override fun toString(): String {
        return "Service($name)"
    }
}