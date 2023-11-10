package io.sellmair.pacemaker

import io.sellmair.pacemaker.sql.PacemakerDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
private val databaseBackgroundThread: CoroutineDispatcher = newSingleThreadContext("database")

internal class SafePacemakerDatabase(
    private val database: PacemakerDatabase
) {
    suspend operator fun <T> invoke(block: suspend PacemakerDatabase.() -> T): T {
        return withContext(databaseBackgroundThread) {
            block(database)
        }
    }

    fun <T> flow(block: PacemakerDatabase.() -> Flow<T>): Flow<T> {
        return database.block().flowOn(databaseBackgroundThread)
    }
}
