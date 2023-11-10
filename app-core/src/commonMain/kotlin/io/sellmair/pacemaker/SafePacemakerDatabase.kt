package io.sellmair.pacemaker

import io.sellmair.pacemaker.sql.PacemakerDatabase
import io.sellmair.pacemaker.utils.ConfigurationKey
import io.sellmair.pacemaker.utils.value
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOn

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
internal val databaseBackgroundThread: CoroutineDispatcher = newSingleThreadContext("database")

internal object DatabaseBackgroundDispatcher: ConfigurationKey.WithDefault<CoroutineDispatcher> {
    override val default: CoroutineDispatcher = databaseBackgroundThread
}

internal class SafePacemakerDatabase(
    private val database: PacemakerDatabase
) {
    suspend operator fun <T> invoke(block: suspend PacemakerDatabase.() -> T): T {
        return withContext(DatabaseBackgroundDispatcher.value()) {
            block(database)
        }
    }

    fun <T> flow(block: PacemakerDatabase.() -> Flow<T>): Flow<T> {
        return kotlinx.coroutines.flow.flow {
            emitAll(database.block().flowOn(DatabaseBackgroundDispatcher.value()))
        }
    }
}
