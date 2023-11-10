package io.sellmair.pacemaker.utils

import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StateBusTest {
    data class HotState(val id: Any?) : State {
        companion object Key : State.Key<HotState?> {
            override val default: HotState? = null
        }
    }

    class ColdState(val id: Any?) : State {
        data class Key(val id: Any? = null) : State.Key<ColdState?> {
            override val default: ColdState? = null
        }
    }

    @Test
    fun `test - cold producer`() = runTest(StateBus()) {
        val collectedStates = mutableListOf<ColdState?>()

        val producer = launchStateProducer { key: ColdState.Key ->
            if (key.id is List<*>) return@launchStateProducer
            emit(ColdState(key.id))
        }


        launch {
            ColdState.Key("Hello").get()
                .onEach { value -> collectedStates.add(value) }
                .collect { value ->
                    if (value != null) {
                        cancel()
                        producer.cancel()
                    }
                }
        }

        testScheduler.advanceUntilIdle()
        assertEquals(listOf(null, "Hello"), collectedStates.map { it?.id })
    }

    @Test
    fun `test - cold producer - state reset`() = runTest(StateBus()) {
        launchStateProducer { _: ColdState.Key ->
            var i = 0
            while (isActive) {
                emit(ColdState(i))
                yield()
                i++
            }
        }

        launch {
            ColdState.Key().get().collect { value ->
                if (value != null) cancel()
            }
        }.join()


        assertNotNull(ColdState.Key().get().value)

        while (isActive) {
            yield()
            if (ColdState.Key().get().value == null) {
                coroutineContext.job.cancelChildren()
                break
            }
        }
    }

    @Test
    fun `test - hot producer`() = runTest(StateBus()) {
        launchStateProducer(HotState.Key) {
            emit(HotState("Hello"))
        }

        yield()
        assertEquals(HotState("Hello"), HotState.get().value)
        coroutineContext.job.cancelChildren()
    }

    @Test
    fun `test - hot producer - is shared`() = runTest(StateBus()) {
        var isLaunched = false
        launchStateProducer(HotState.Key) {
            emit(HotState("Hello"))
            assertFalse(isLaunched)
            isLaunched = true
        }

        yield()
        assertTrue(isLaunched)
        HotState.get().take(1).collect()
        HotState.get().take(1).collect()
        coroutineContext.job.cancelChildren()
    }
}