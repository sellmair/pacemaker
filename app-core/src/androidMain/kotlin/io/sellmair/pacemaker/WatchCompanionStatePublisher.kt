package io.sellmair.pacemaker

import android.content.SharedPreferences
import io.sellmair.evas.States
import io.sellmair.evas.collectEventsAsync
import io.sellmair.evas.flow
import io.sellmair.evas.statesOrThrow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.time.Clock

internal fun States.selectedWatchNodeId(): String? = getState(SelectedHeartRateSourceState).value.source
    ?.takeIf { it.kind == HeartRateSource.Kind.COMPANION_WATCH }?.companionDeviceId

/** Publishes phone state to the selected watch and reconciles discovery with source availability. */
internal class WatchCompanionStatePublisher(
    private val prefs: SharedPreferences,
    private val scope: CoroutineScope,
    private val transport: WatchCompanionTransport,
    private val stopDelivery: WatchMeasurementStopDelivery
) {
    private val states = scope.coroutineContext.statesOrThrow

    fun start() {
        scope.collectEventsAsync<WatchCompanionSnapshotRequested> { sendSnapshot(it.nodeId) }
        scope.collectEventsAsync<WatchCompanionNodeAvailable> { event ->
            if (event.nodeId == states.selectedWatchNodeId()) sendSnapshot(event.nodeId)
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            WatchCompanionDiscoveryState.flow().collect { onDiscovery(it) }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            combine(MeState.flow(), MeColorState.flow()) { me, color ->
                me?.let { watchConfigure(it, color, sourceSelected = true) }
            }.distinctUntilChanged().collect { configure ->
                val nodeId = states.selectedWatchNodeId()
                if (configure != null && nodeId != null) transport.sendTo(nodeId, configure)
            }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            var lastSnapshotAt = 0L
            GroupState.flow().collect {
                val now = Clock.System.now().toEpochMilliseconds()
                if (now - lastSnapshotAt < 2_000) return@collect
                lastSnapshotAt = now
                states.selectedWatchNodeId()?.let { node -> sendGroupSnapshot(node) }
            }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            var lastNodeId = prefs.getString("last-selected-watch-node-id", states.selectedWatchNodeId())
            SelectedHeartRateSourceState.flow().collect { selection ->
                val nextNodeId = selection.source
                    ?.takeIf { it.kind == HeartRateSource.Kind.COMPANION_WATCH }?.companionDeviceId
                if (nextNodeId == lastNodeId) return@collect
                val previousNodeId = lastNodeId
                if (previousNodeId != null) stopDelivery.enqueue(previousNodeId)
                lastNodeId = nextNodeId
                prefs.edit().putString("last-selected-watch-node-id", nextNodeId).apply()
                if (nextNodeId != null && !stopDelivery.hasPendingFor(nextNodeId)) sendSnapshot(nextNodeId)
                if (previousNodeId != null) stopDelivery.drain()
            }
        }
    }

    internal suspend fun onDiscovery(discovery: WatchCompanionDiscoveryState) {
        if (discovery.candidates.any { it.connected && it.id in stopDelivery.pendingTargets() }) {
            stopDelivery.drain()
        }
        // Delivery may suspend while selection and capability discovery both change.
        val currentDiscovery = states.getState(WatchCompanionDiscoveryState).value
        val selectedNodeId = states.selectedWatchNodeId()
        val selected = currentDiscovery.candidates.firstOrNull { it.id == selectedNodeId }
        states.setState(WatchCompanionDeviceState, WatchCompanionDeviceState(
            paired = currentDiscovery.candidates.isNotEmpty(),
            connected = selected?.connected == true,
            name = selected?.name ?: currentDiscovery.candidates.firstOrNull()?.name ?: "Wear OS watch",
            candidates = currentDiscovery.candidates,
            platformAvailable = currentDiscovery.platformAvailable
        ))
        val source = states.getState(SelectedHeartRateSourceState).value.source
            ?.takeIf { it.kind == HeartRateSource.Kind.COMPANION_WATCH } ?: return
        val availability = if (selected?.connected == true) HeartRateSource.Availability.AVAILABLE
            else HeartRateSource.Availability.DISCONNECTED
        if (source.availability != availability ||
            (availability == HeartRateSource.Availability.DISCONNECTED && source.heartRate != null)
        ) states.setState(SelectedHeartRateSourceState, SelectedHeartRateSourceState(source.copy(
            availability = availability,
            heartRate = source.heartRate.takeIf { availability == HeartRateSource.Availability.AVAILABLE },
            measuredAtEpochMillis = source.measuredAtEpochMillis
                .takeIf { availability == HeartRateSource.Availability.AVAILABLE }
        )))
    }

    private suspend fun sendSnapshot(peerId: String) {
        val me = states.getState(MeState).value ?: return
        val selected = states.selectedWatchNodeId() == peerId
        transport.sendTo(peerId, watchConfigure(me, states.getState(MeColorState).value, selected))
        if (selected && states.selectedWatchNodeId() == peerId) sendGroupSnapshot(peerId)
    }

    private suspend fun sendGroupSnapshot(peerId: String) {
        transport.sendTo(peerId, watchGroupSnapshot(
            states.getState(GroupState.Key).value,
            Clock.System.now().toEpochMilliseconds()
        ))
    }
}
