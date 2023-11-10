package io.sellmair.pacemaker.utils

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

class StateBusTest {
    data class HotState(val id: Any?) : State {
        companion object Key : State.Key<HotState?> {
            override val default: HotState? = null
        }
    }

    data class ColdState(val id: Any?) : State {
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test - keepActive`() = runTest(StateBus()) {
        launchStateProducer(keepActive = 3.seconds) { _: ColdState.Key ->
            while(currentCoroutineContext().isActive) {
                emit(ColdState(currentTime))
                delay(1.seconds)
            }
        }

        launch {
            ColdState.Key().get().takeWhile { it == null }.collect()
        }.join()

        assertEquals(ColdState(0L), ColdState.Key().get().value)

        testScheduler.advanceTimeBy(1.seconds)
        yield()
        assertEquals(ColdState(1000L), ColdState.Key().get().value)

        testScheduler.advanceTimeBy(1.seconds)
        yield()
        assertEquals(ColdState(2000L), ColdState.Key().get().value)

        testScheduler.advanceTimeBy(1.seconds)
        yield()
        assertEquals(ColdState(3000L), ColdState.Key().get().value)


        testScheduler.advanceTimeBy(1.seconds)
        yield()
        assertEquals(null, ColdState.Key().get().value)

        coroutineContext.job.cancelChildren()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `test - keepActive - resubscribe`() = runTest(StateBus()) {
        var launched = false
        launchStateProducer(keepActive = 2.seconds) { _: ColdState.Key ->
            assertFalse(launched, "Another producer was already launched!")
            launched = true
            while(currentCoroutineContext().isActive) {
                emit(ColdState(currentTime))
                delay(1.seconds)
            }
        }

        launch {
            ColdState.Key().get().takeWhile { it == null }.collect()
        }.join()

        assertEquals(ColdState(0L), ColdState.Key().get().value)

        testScheduler.advanceTimeBy(1.seconds)
        yield()
        assertEquals(ColdState(1000L), ColdState.Key().get().value)

        // Launch coroutine that will receive current state and will wait for one more state
        // Giving us three more emissions
        launch {
            ColdState.Key().get().take(2).collect()
        }

        testScheduler.advanceTimeBy(3.seconds)
        yield()
        assertEquals(ColdState(4000L), ColdState.Key().get().value)

        testScheduler.advanceTimeBy(3.seconds)
        yield()
        assertEquals(null, ColdState.Key().get().value)

        coroutineContext.job.cancelChildren()
    }
}