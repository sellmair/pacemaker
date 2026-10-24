package io.sellmair.pacemaker.spoof

import io.sellmair.evas.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.util.concurrent.TimeUnit

internal fun CoroutineScope.launchSpoofToolActor() = launchState(SpoofToolState) {
    var process: Process? = null
    var input: BufferedWriter? = null
    var shutdownHook: Thread? = null

    fun send(command: String) {
        runCatching {
            input?.apply {
                write(command)
                newLine()
                flush()
            }
        }
    }

    collectEvents<SpoofToolIntent> { intent ->
        val current = SpoofToolState.value()
        when (intent) {
            SpoofToolIntent.Start -> {
                if (process?.isAlive == true) return@collectEvents
                if (current.userId.toLongOrNull() == null) {
                    current.copy(
                        status = SpoofToolState.Status.Error,
                        message = "Enter a valid numeric user ID before starting.",
                    ).emit()
                    return@collectEvents
                }
                if (!System.getProperty("os.name").startsWith("Mac", ignoreCase = true)) {
                    SpoofToolState(
                        status = SpoofToolState.Status.Error,
                        message = "The BLE advertiser requires macOS.",
                        userId = current.userId,
                        userName = current.userName,
                        heartRate = current.heartRate,
                        heartRateLimit = current.heartRateLimit,
                    ).emit()
                    return@collectEvents
                }

                val executablePath = System.getProperty("pacemaker.spoof.executable")
                    ?: "build/bin/macosArm64/debugExecutable/spoof-tool.kexe"
                val executable = File(executablePath).absoluteFile
                if (!executable.isFile || !executable.canExecute()) {
                    SpoofToolState(
                        status = SpoofToolState.Status.Error,
                        message = "Advertiser binary not found. Run :spoof-tool:linkDebugExecutableMacosArm64.",
                        userId = current.userId,
                        userName = current.userName,
                        heartRate = current.heartRate,
                        heartRateLimit = current.heartRateLimit,
                    ).emit()
                    return@collectEvents
                }

                try {
                    val child = withContext(Dispatchers.IO) {
                        ProcessBuilder(executable.absolutePath)
                            .redirectErrorStream(true)
                            .start()
                    }
                    process = child
                    input = child.outputStream.bufferedWriter()
                    val hook = Thread(
                        { if (child.isAlive) child.destroyForcibly() },
                        "spoof-advertiser-shutdown",
                    )
                    Runtime.getRuntime().addShutdownHook(hook)
                    shutdownHook = hook
                    SpoofToolState(
                        status = SpoofToolState.Status.Starting,
                        message = "Starting BLE advertiser…",
                        userId = current.userId,
                        userName = current.userName,
                        heartRate = current.heartRate,
                        heartRateLimit = current.heartRateLimit,
                    ).emit()

                    launch(Dispatchers.IO) {
                        child.inputStream.bufferedReader().useLines { lines ->
                            lines.forEach { line -> SpoofToolIntent.NativeOutput(child, line).emit() }
                        }
                        SpoofToolIntent.NativeExited(child, child.waitFor()).emit()
                    }

                    send("user-id ${current.userId}")
                    send("user-name ${current.userName}")
                    send("heart-rate ${current.heartRate}")
                    send("heart-rate-limit ${current.heartRateLimit}")
                } catch (failure: Exception) {
                    process = null
                    input = null
                    SpoofToolState(
                        status = SpoofToolState.Status.Error,
                        message = failure.message ?: "Could not start the BLE advertiser.",
                        userId = current.userId,
                        userName = current.userName,
                        heartRate = current.heartRate,
                        heartRateLimit = current.heartRateLimit,
                    ).emit()
                }
            }

            SpoofToolIntent.Stop -> {
                val child = process ?: return@collectEvents
                val childInput = input
                val childShutdownHook = shutdownHook
                process = null
                input = null
                shutdownHook = null
                current.copy(status = SpoofToolState.Status.Stopping, message = "Stopping advertiser…").emit()
                launch(Dispatchers.IO) {
                    runCatching { childInput?.close() }
                    if (!child.waitFor(3, TimeUnit.SECONDS)) child.destroyForcibly()
                    childShutdownHook?.let { hook -> runCatching { Runtime.getRuntime().removeShutdownHook(hook) } }
                    SpoofToolIntent.NativeStopped.emit()
                }
            }

            is SpoofToolIntent.SetUserId -> {
                current.copy(userId = intent.value).emit()
                if (intent.value.toLongOrNull() != null) send("user-id ${intent.value}")
            }

            is SpoofToolIntent.SetUserName -> {
                current.copy(userName = intent.value).emit()
                send("user-name ${intent.value.replace('\n', ' ')}")
            }

            is SpoofToolIntent.SetHeartRate -> {
                val value = intent.value.coerceIn(30, 220)
                current.copy(heartRate = value).emit()
                send("heart-rate $value")
            }

            is SpoofToolIntent.SetHeartRateLimit -> {
                val value = intent.value.coerceIn(30, 220)
                current.copy(heartRateLimit = value).emit()
                send("heart-rate-limit $value")
            }

            is SpoofToolIntent.NativeOutput -> {
                if (intent.process != process) return@collectEvents
                val ready = intent.line == "READY"
                current.copy(
                    status = if (ready) SpoofToolState.Status.Advertising else current.status,
                    message = if (ready) "Advertising over Bluetooth LE" else intent.line,
                    recentOutput = (current.recentOutput + intent.line).takeLast(5),
                ).emit()
            }

            is SpoofToolIntent.NativeExited -> {
                if (intent.process != process) return@collectEvents
                process = null
                input = null
                shutdownHook?.let { hook -> runCatching { Runtime.getRuntime().removeShutdownHook(hook) } }
                shutdownHook = null
                current.copy(
                    status = SpoofToolState.Status.Stopped,
                    message = "Advertiser exited (${intent.exitCode})",
                ).emit()
            }

            SpoofToolIntent.NativeStopped -> {
                current.copy(status = SpoofToolState.Status.Stopped, message = "Advertiser stopped").emit()
            }
        }
    }

}
