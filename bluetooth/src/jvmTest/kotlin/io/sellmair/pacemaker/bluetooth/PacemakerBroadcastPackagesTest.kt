package io.sellmair.pacemaker.bluetooth

import io.sellmair.pacemaker.ble.BleDeviceId
import io.sellmair.pacemaker.ble.BleReceivedValue
import io.sellmair.pacemaker.model.HeartRate
import io.sellmair.pacemaker.model.UserId
import io.sellmair.pacemaker.model.encodeToByteArray
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PacemakerBroadcastPackagesTest {
    @Test
    fun decoderSubscriptionEndsWithItsConnectionAcrossReconnects() = runTest {
        val values = MutableSharedFlow<BleReceivedValue>()

        repeat(20) { index ->
            val connectionJob = Job()
            val connection = object : PacemakerBluetoothConnection {
                override val deviceId = BleDeviceId("sensor")
                override val receivedValues = values
                override val coroutineContext: CoroutineContext = connectionJob
            }
            val collector = backgroundScope.launch {
                connection.broadcastPackages().collect { }
            }

            runCurrent()
            assertEquals(1, values.subscriptionCount.value, "connection $index should have one decoder")

            connectionJob.cancel()
            runCurrent()
            assertTrue(collector.isCompleted, "connection $index collector should complete on disconnect")
            assertEquals(0, values.subscriptionCount.value, "connection $index decoder should be released")
            collector.cancelAndJoin()
        }
    }

    @Test
    fun decoderWaitsForNameAndKeepsGoodValuesAcrossMalformedUpdates() = runTest {
        val values = listOf(
            received(PacemakerServiceDescriptors.userIdCharacteristic, UserId(1).encodeToByteArray()),
            received(PacemakerServiceDescriptors.heartRateCharacteristic, HeartRate(100).encodeToByteArray()),
            received(PacemakerServiceDescriptors.heartRateLimitCharacteristic, HeartRate(150).encodeToByteArray()),
            received(PacemakerServiceDescriptors.userNameCharacteristic, byteArrayOf()),
            received(PacemakerServiceDescriptors.userNameCharacteristic, "Runner".encodeToByteArray()),
            received(PacemakerServiceDescriptors.userNameCharacteristic, byteArrayOf()),
            received(PacemakerServiceDescriptors.heartRateCharacteristic, byteArrayOf()),
            received(PacemakerServiceDescriptors.heartRateCharacteristic, HeartRate(101).encodeToByteArray())
        )

        val packages = values.asFlow().broadcastPackages().toList()

        assertTrue(packages.isNotEmpty())
        assertTrue(packages.all { it.userName == "Runner" })
        assertTrue(packages.any { it.heartRate == HeartRate(100) })
        assertEquals(HeartRate(101), packages.last().heartRate)
    }

    @Test
    fun decoderKeepsInterleavedDeviceSnapshotsIndependent() = runTest {
        val deviceA = BleDeviceId("a")
        val deviceB = BleDeviceId("b")
        val values = MutableSharedFlow<BleReceivedValue>()
        val packages = mutableListOf<PacemakerBroadcastPackage>()
        val collector = backgroundScope.launch { values.broadcastPackages().collect(packages::add) }
        runCurrent()

        values.emit(received(PacemakerServiceDescriptors.userIdCharacteristic, UserId(1).encodeToByteArray(), deviceA))
        values.emit(received(PacemakerServiceDescriptors.userNameCharacteristic, "Alice".encodeToByteArray(), deviceA))
        values.emit(received(PacemakerServiceDescriptors.heartRateCharacteristic, HeartRate(100).encodeToByteArray(), deviceA))
        values.emit(received(PacemakerServiceDescriptors.heartRateLimitCharacteristic, HeartRate(150).encodeToByteArray(), deviceA))
        values.emit(received(PacemakerServiceDescriptors.userIdCharacteristic, UserId(2).encodeToByteArray(), deviceB))
        values.emit(received(PacemakerServiceDescriptors.userNameCharacteristic, "Bob".encodeToByteArray(), deviceB))
        values.emit(received(PacemakerServiceDescriptors.heartRateCharacteristic, HeartRate(80).encodeToByteArray(), deviceB))
        values.emit(received(PacemakerServiceDescriptors.heartRateLimitCharacteristic, HeartRate(130).encodeToByteArray(), deviceB))
        runCurrent()

        assertTrue(packages.any { it.deviceId == deviceA && it.userName == "Alice" && it.heartRate == HeartRate(100) })
        assertTrue(packages.any { it.deviceId == deviceB && it.userName == "Bob" && it.heartRate == HeartRate(80) })
        collector.cancelAndJoin()
    }

    @Test
    fun firstSnapshotCanArriveNameAndMeasurementsBeforeIdentity() = runTest {
        val packages = listOf(
            received(PacemakerServiceDescriptors.userNameCharacteristic, "Runner".encodeToByteArray()),
            received(PacemakerServiceDescriptors.heartRateCharacteristic, HeartRate(95).encodeToByteArray()),
            received(PacemakerServiceDescriptors.heartRateLimitCharacteristic, HeartRate(150).encodeToByteArray()),
            received(PacemakerServiceDescriptors.userIdCharacteristic, UserId(8).encodeToByteArray())
        ).asFlow().broadcastPackages().toList()

        assertEquals(1, packages.size)
        assertEquals(UserId(8), packages.single().userId)
        assertEquals("Runner", packages.single().userName)
        assertEquals(HeartRate(95), packages.single().heartRate)
    }

    @Test
    fun clearedHeartRateIsNotRenewedByMetadataOrMalformedUpdates() = runTest {
        val values = MutableSharedFlow<BleReceivedValue>()
        val packages = mutableListOf<PacemakerBroadcastPackage>()
        val collector = backgroundScope.launch {
            values.broadcastPackages().collect(packages::add)
        }
        runCurrent()
        values.emit(received(PacemakerServiceDescriptors.userIdCharacteristic, UserId(9).encodeToByteArray()))
        values.emit(received(PacemakerServiceDescriptors.userNameCharacteristic, "Runner".encodeToByteArray()))
        values.emit(received(PacemakerServiceDescriptors.heartRateCharacteristic, HeartRate(110).encodeToByteArray()))
        values.emit(received(PacemakerServiceDescriptors.heartRateLimitCharacteristic, HeartRate(160).encodeToByteArray()))
        runCurrent()
        assertEquals(1, packages.size)

        values.emit(received(PacemakerServiceDescriptors.heartRateCharacteristic, byteArrayOf()))
        values.emit(received(PacemakerServiceDescriptors.userNameCharacteristic, byteArrayOf()))
        values.emit(received(PacemakerServiceDescriptors.heartRateCharacteristic, byteArrayOf(1)))
        values.emit(received(PacemakerServiceDescriptors.heartRateLimitCharacteristic, byteArrayOf()))
        values.emit(received(PacemakerServiceDescriptors.heartRateLimitCharacteristic, HeartRate(160).encodeToByteArray()))
        runCurrent()
        assertEquals(1, packages.size, "cleared or invalid rate must not renew the old measurement")

        values.emit(received(PacemakerServiceDescriptors.heartRateCharacteristic, HeartRate(111).encodeToByteArray()))
        runCurrent()
        assertEquals(HeartRate(111), packages.last().heartRate)
        collector.cancelAndJoin()
    }

    @Test
    fun serviceProcessesMoreThanSixteenSequentialReconnectsWithoutRetainingOldDecoders() = runTest {
        val connections = MutableSharedFlow<PacemakerBluetoothConnection>()
        val values = MutableSharedFlow<BleReceivedValue>()
        val service = object : PacemakerBluetoothService {
            override val newConnections = connections
            override val allConnections = MutableSharedFlow<List<PacemakerBluetoothConnection>>()
            override suspend fun write(write: suspend PacemakerBluetoothWritable.() -> Unit) = Unit
        }
        val packages = mutableListOf<PacemakerBroadcastPackage>()
        val collector = backgroundScope.launch { service.broadcastPackages().collect(packages::add) }
        runCurrent()

        repeat(20) { index ->
            val connectionJob = Job()
            val connection = object : PacemakerBluetoothConnection, ReadablePacemakerBluetoothConnection {
                override val deviceId = BleDeviceId("sensor")
                override val receivedValues = values
                override val coroutineContext: CoroutineContext = connectionJob

                override suspend fun requestInitialRead() {
                    assertEquals(1, values.subscriptionCount.value, "initial reads must follow decoder subscription")
                    values.emit(received(PacemakerServiceDescriptors.userIdCharacteristic, UserId(7).encodeToByteArray()))
                    values.emit(received(PacemakerServiceDescriptors.userNameCharacteristic, "Runner".encodeToByteArray()))
                    values.emit(received(PacemakerServiceDescriptors.heartRateCharacteristic, HeartRate(90 + index).encodeToByteArray()))
                    values.emit(received(PacemakerServiceDescriptors.heartRateLimitCharacteristic, HeartRate(160).encodeToByteArray()))
                }
            }
            connections.emit(connection)
            runCurrent()
            assertEquals(1, values.subscriptionCount.value, "reconnect $index should have one decoder")
            runCurrent()
            assertEquals(index + 1, packages.size)
            assertEquals("Runner", packages.last().userName)
            assertEquals(HeartRate(90 + index), packages.last().heartRate)

            connectionJob.cancel()
            runCurrent()
            assertEquals(0, values.subscriptionCount.value, "reconnect $index must release its decoder")
        }

        collector.cancelAndJoin()
    }

    @Test
    fun disconnectCancelsAReaderBlockedDuringInitialRead() = runTest {
        val values = MutableSharedFlow<BleReceivedValue>()
        val connectionJob = Job()
        val readStarted = CompletableDeferred<Unit>()
        val readCancelled = CompletableDeferred<Unit>()
        val connection = object : PacemakerBluetoothConnection, ReadablePacemakerBluetoothConnection {
            override val deviceId = BleDeviceId("sensor")
            override val receivedValues = values
            override val coroutineContext: CoroutineContext = connectionJob

            override suspend fun requestInitialRead() {
                readStarted.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    readCancelled.complete(Unit)
                }
            }
        }
        val collector = backgroundScope.launch { connection.broadcastPackages().collect { } }
        runCurrent()
        assertTrue(readStarted.isCompleted)
        assertEquals(1, values.subscriptionCount.value)

        connectionJob.cancel()
        runCurrent()
        assertTrue(readCancelled.isCompleted)
        assertTrue(collector.isCompleted)
        assertEquals(0, values.subscriptionCount.value)
        collector.cancelAndJoin()
    }

    @Test
    fun peripheralConnectionRetainsWritesReceivedBeforeDecoderSubscription() = runTest {
        val connection = PeripheralPacemakerBluetoothConnection(BleDeviceId("sensor"), backgroundScope)
        connection.receivedValues.emit(received(PacemakerServiceDescriptors.userIdCharacteristic, UserId(10).encodeToByteArray()))
        connection.receivedValues.emit(received(PacemakerServiceDescriptors.userNameCharacteristic, "Runner".encodeToByteArray()))
        connection.receivedValues.emit(received(PacemakerServiceDescriptors.heartRateCharacteristic, HeartRate(102).encodeToByteArray()))
        connection.receivedValues.emit(received(PacemakerServiceDescriptors.heartRateLimitCharacteristic, HeartRate(160).encodeToByteArray()))

        val packages = mutableListOf<PacemakerBroadcastPackage>()
        val collector = backgroundScope.launch { connection.broadcastPackages().collect(packages::add) }
        runCurrent()

        assertEquals(1, packages.size)
        assertEquals(UserId(10), packages.single().userId)
        assertEquals("Runner", packages.single().userName)
        assertEquals(HeartRate(102), packages.single().heartRate)

        connection.coroutineContext[Job]?.cancel()
        runCurrent()
        collector.cancelAndJoin()
    }

    private fun received(
        characteristic: io.sellmair.pacemaker.ble.BleCharacteristicDescriptor,
        data: ByteArray,
        deviceId: BleDeviceId = BleDeviceId("sensor")
    ) = BleReceivedValue(deviceId, characteristic, data)
}
