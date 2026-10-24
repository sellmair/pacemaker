package io.sellmair.pacemaker

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

const val WATCH_PHONE_CAPABILITY = "pacemaker_phone_v1"
const val WATCH_COMPANION_CAPABILITY = "pacemaker_watch_v1"
const val WATCH_COMPANION_MESSAGE_PATH = "/pacemaker/watch-companion/v1"

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
    private val remoteCapability: String,
    private val selectedNodeId: () -> String?,
    private val onPresenceChanged: suspend (WatchCompanionDeviceState) -> Unit,
    private val onNodeAvailable: suspend () -> Unit
) {
    private val appContext = context.applicationContext
    private val capabilityClient = Wearable.getCapabilityClient(appContext)
    private val nodeClient = Wearable.getNodeClient(appContext)
    private val messageClient = Wearable.getMessageClient(appContext)
    private data class PendingAck(val peerId: String, val result: CompletableDeferred<Boolean>)
    private val acknowledgements = mutableMapOf<String, PendingAck>()
    @Volatile private var reachableNodes: Set<String> = emptySet()
    val isSelectedNodeReachable: Boolean get() = selectedNodeId()?.let { it in reachableNodes } == true
    private val capabilityListener = CapabilityClient.OnCapabilityChangedListener { info ->
        scope.launch(Dispatchers.Main.immediate) { updatePresence(info) }
    }

    fun start() {
        runCatching { capabilityClient.addListener(capabilityListener, remoteCapability) }
        scope.launch(Dispatchers.Main.immediate) { refreshPresence() }
    }

    fun close() {
        runCatching { capabilityClient.removeListener(capabilityListener) }
        acknowledgements.values.forEach { it.result.complete(false) }
        acknowledgements.clear()
    }

    suspend fun send(message: WatchCompanionMessage): Boolean {
        val nodeId = selectedNodeId() ?: return false
        return sendTo(nodeId, message)
    }

    suspend fun sendTo(nodeId: String, message: WatchCompanionMessage): Boolean = runCatching {
        val payload = WatchCompanionCodec.encode(message)
        messageClient.sendMessage(nodeId, WATCH_COMPANION_MESSAGE_PATH, payload).awaitPacemaker()
        true
    }.getOrDefault(false)

    suspend fun sendReliable(message: WatchCompanionMessage, retries: Int = 3): Boolean {
        val nodeId = selectedNodeId() ?: return false
        return sendReliableTo(nodeId, message, retries)
    }

    suspend fun sendReliableTo(nodeId: String, message: WatchCompanionMessage, retries: Int = 3): Boolean {
        return sendReliableToOutcome(nodeId, message, retries) == true
    }

    suspend fun sendReliableOutcome(message: WatchCompanionMessage, retries: Int = 3): Boolean? {
        val nodeId = selectedNodeId() ?: return null
        return sendReliableToOutcome(nodeId, message, retries)
    }

    suspend fun sendReliableToOutcome(nodeId: String, message: WatchCompanionMessage, retries: Int = 3): Boolean? {
        val waiter = acknowledgements.getOrPut(message.messageId) { PendingAck(nodeId, CompletableDeferred()) }
        if (waiter.peerId != nodeId) return false
        return try {
            repeat(retries) {
                if (sendTo(nodeId, message)) {
                    withTimeoutOrNull(3_000) { waiter.result.await() }?.let { return it }
                }
                delay(150)
            }
            null
        } finally {
            acknowledgements.remove(message.messageId)
        }
    }

    fun onMessage(nodeId: String, message: WatchCompanionMessage) {
        if (message is WatchCompanionMessage.Ack) {
            val pending = acknowledgements[message.messageId]
            if (pending?.peerId == nodeId) {
                acknowledgements.remove(message.messageId)?.result?.complete(message.accepted)
            }
            return
        }
        if (nodeId != selectedNodeId()) return
        if (message is WatchCompanionMessage.Hello) {
            scope.launch(Dispatchers.Main.immediate) { onNodeAvailable() }
        }
    }

    suspend fun refreshPresence() {
        val info = runCatching {
            capabilityClient.getCapability(remoteCapability, CapabilityClient.FILTER_ALL).awaitPacemaker()
        }.getOrNull() ?: run {
            onPresenceChanged(WatchCompanionDeviceState(platformAvailable = false))
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
        reachableNodes = reachable + directNodes
        val candidates = nodes.sortedBy { it.displayName }.map { node ->
            WatchCompanionCandidate(node.id, node.displayName, paired = true, connected = node.id in reachable || node.id in directNodes)
        }
        onPresenceChanged(WatchCompanionDeviceState(
            paired = candidates.isNotEmpty(),
            connected = selectedNodeId()?.let { it in reachable || it in directNodes } == true,
            name = candidates.firstOrNull { it.id == selectedNodeId() }?.name ?: candidates.firstOrNull()?.name ?: "Wear OS watch",
            candidates = candidates
        ))
        if (selectedNodeId()?.let { it in reachable || it in directNodes } == true) onNodeAvailable()
    }
}
