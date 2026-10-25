package io.sellmair.pacemaker

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.Wearable
import io.sellmair.evas.Event
import io.sellmair.evas.State
import io.sellmair.evas.collectEventsAsync
import io.sellmair.evas.eventsOrThrow
import io.sellmair.evas.statesOrThrow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

const val WATCH_PHONE_CAPABILITY = "pacemaker_phone_v1"
const val WATCH_COMPANION_CAPABILITY = "pacemaker_watch_v1"
const val WATCH_COMPANION_MESSAGE_PATH = "/pacemaker/watch-companion/v1"

/** Raw capability discovery; each endpoint decides which companion it trusts. */
data class WatchCompanionDiscoveryState(
    val candidates: List<WatchCompanionCandidate> = emptyList(),
    val platformAvailable: Boolean = true
) : State {
    companion object Key : State.Key<WatchCompanionDiscoveryState> {
        override val default = WatchCompanionDiscoveryState()
    }
}

data class WatchCompanionNodeAvailable(val nodeId: String) : Event

data class WatchCompanionInboundMessage(val nodeId: String, val message: WatchCompanionMessage) : Event
data class WatchCompanionTransportAck(val nodeId: String, val ack: WatchCompanionMessage.Ack) : Event

interface WatchCompanionTransport {
    suspend fun sendTo(nodeId: String, message: WatchCompanionMessage): Boolean
    suspend fun sendReliableToOutcome(nodeId: String, message: WatchCompanionMessage, retries: Int = 3): Boolean?
    fun onMessage(nodeId: String, message: WatchCompanionMessage)
}

/** ACKs are bound to both the command ID and the peer selected when it was sent. */
internal class WatchAcknowledgements {
    private data class Pending(val peerId: String, val result: CompletableDeferred<Boolean>)
    private val pending = mutableMapOf<String, Pending>()

    fun register(peerId: String, messageId: String): CompletableDeferred<Boolean>? {
        val waiter = pending.getOrPut(messageId) { Pending(peerId, CompletableDeferred()) }
        return waiter.result.takeIf { waiter.peerId == peerId }
    }

    fun acknowledge(peerId: String, ack: WatchCompanionMessage.Ack) {
        val waiter = pending[ack.messageId] ?: return
        if (waiter.peerId == peerId) pending.remove(ack.messageId)?.result?.complete(ack.accepted)
    }

    fun remove(messageId: String) { pending.remove(messageId) }
    fun close() {
        pending.values.forEach { it.result.complete(false) }
        pending.clear()
    }
}

/** Await a Play Services task without blocking the main thread. */
suspend fun <T> Task<T>.awaitPacemaker(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { value -> if (continuation.isActive) continuation.resume(value) }
    addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
    addOnCanceledListener { if (continuation.isActive) continuation.cancel() }
}

/** Phone-side Data Layer transport. Health samples are sent as messages and never persisted here. */
class WatchCompanionDataLayer(
    context: Context,
    private val scope: CoroutineScope,
    private val remoteCapability: String
) : WatchCompanionTransport {
    private val events = scope.coroutineContext.eventsOrThrow
    private val states = scope.coroutineContext.statesOrThrow
    private val appContext = context.applicationContext
    private val capabilityClient = Wearable.getCapabilityClient(appContext)
    private val nodeClient = Wearable.getNodeClient(appContext)
    private val messageClient = Wearable.getMessageClient(appContext)
    private val acknowledgements = WatchAcknowledgements()
    private var ackCollector: Job? = null
    private val capabilityListener = CapabilityClient.OnCapabilityChangedListener { info ->
        scope.launch(Dispatchers.Main.immediate) { updatePresence(info) }
    }

    fun start() {
        ackCollector = scope.collectEventsAsync<WatchCompanionTransportAck> { onMessage(it.nodeId, it.ack) }
        runCatching { capabilityClient.addListener(capabilityListener, remoteCapability) }
        scope.launch(Dispatchers.Main.immediate) { refreshPresence() }
    }

    fun close() {
        ackCollector?.cancel()
        runCatching { capabilityClient.removeListener(capabilityListener) }
        acknowledgements.close()
    }

    override suspend fun sendTo(nodeId: String, message: WatchCompanionMessage): Boolean = runCatching {
        val payload = WatchCompanionCodec.encode(message)
        messageClient.sendMessage(nodeId, WATCH_COMPANION_MESSAGE_PATH, payload).awaitPacemaker()
        true
    }.getOrDefault(false)

    suspend fun sendReliableTo(nodeId: String, message: WatchCompanionMessage, retries: Int = 3): Boolean {
        return sendReliableToOutcome(nodeId, message, retries) == true
    }

    override suspend fun sendReliableToOutcome(nodeId: String, message: WatchCompanionMessage, retries: Int): Boolean? {
        val waiter = acknowledgements.register(nodeId, message.messageId) ?: return false
        return try {
            repeat(retries) {
                if (sendTo(nodeId, message)) {
                    withTimeoutOrNull(3_000) { waiter.await() }?.let { return it }
                }
                delay(150)
            }
            null
        } finally {
            acknowledgements.remove(message.messageId)
        }
    }

    override fun onMessage(nodeId: String, message: WatchCompanionMessage) {
        if (message is WatchCompanionMessage.Ack) {
            acknowledgements.acknowledge(nodeId, message)
            return
        }
        if (message is WatchCompanionMessage.Hello) {
            events.emitAsync(WatchCompanionNodeAvailable(nodeId))
        }
    }

    suspend fun refreshPresence() {
        val info = runCatching {
            capabilityClient.getCapability(remoteCapability, CapabilityClient.FILTER_ALL).awaitPacemaker()
        }.getOrNull() ?: run {
            states.setState(WatchCompanionDiscoveryState, WatchCompanionDiscoveryState(platformAvailable = false))
            return
        }
        updatePresence(info)
    }

    private suspend fun updatePresence(info: CapabilityInfo) {
        val reachable = runCatching {
            capabilityClient.getCapability(remoteCapability, CapabilityClient.FILTER_REACHABLE)
                .awaitPacemaker().nodes.mapTo(mutableSetOf()) { it.id }
        }.getOrDefault(emptySet())
        val directNodes = runCatching { nodeClient.connectedNodes.awaitPacemaker().mapTo(mutableSetOf()) { it.id } }
            .getOrDefault(emptySet())
        val localId = runCatching { nodeClient.localNode.awaitPacemaker().id }.getOrNull()
        val nodes = info.nodes.filter { it.id != localId }
        val candidates = nodes.sortedBy { it.displayName }.map { node ->
            WatchCompanionCandidate(node.id, node.displayName, paired = true, connected = node.id in reachable || node.id in directNodes)
        }
        states.setState(WatchCompanionDiscoveryState, WatchCompanionDiscoveryState(candidates))
        candidates.filter { it.connected }.forEach { events.emitAsync(WatchCompanionNodeAvailable(it.id)) }
    }
}
