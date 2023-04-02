package io.sellmair.pacemaker.ble

data class BleCharacteristicDescriptor(
    val name: String,
    val uuid: BleUUID,
    val isReadable: Boolean = true,
    val isWritable: Boolean = false,
    val isNotificationsEnabled: Boolean = false
) {
    override fun toString(): String {
        return "Characteristic($name)"
    }
}