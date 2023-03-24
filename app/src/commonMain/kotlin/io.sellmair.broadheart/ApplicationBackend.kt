package io.sellmair.broadheart

interface ApplicationBackend {
    val bluetoothService: BluetoothService
    val userService: UserService
    val groupService: GroupService
}