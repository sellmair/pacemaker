package io.sellmair.pacemaker

import io.sellmair.pacemaker.bluetooth.PacemakerBluetoothService
import io.sellmair.pacemaker.bluetooth.PacemakerBroadcastPackageEvent
import io.sellmair.pacemaker.bluetooth.broadcastPackages
import io.sellmair.pacemaker.model.User
import io.sellmair.pacemaker.utils.emit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

internal fun CoroutineScope.launchPacemakerBroadcastReceiver(
    userService: UserService, pacemakerBluetoothService: Deferred<PacemakerBluetoothService>
) = launch {
    pacemakerBluetoothService.await().broadcastPackages().conflate().collect { received ->
        val user = User(id = received.userId, name = received.userName)
        userService.saveUser(user)
        userService.saveHeartRateLimit(user, received.heartRateLimit)
        PacemakerBroadcastPackageEvent(received).emit()
    }
}