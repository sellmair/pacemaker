package io.sellmair.pacemaker.utils

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.*

class ConfigurationTest {

    object StringKey : ConfigurationKey<String>

    object StringKeyWithDefault : ConfigurationKey.WithDefault<String> {
        override val default: String = "foo"
    }

    @Test
    fun `test - no value attached`() = runTest {
        assertNull(StringKey.value())
    }

    @Test
    fun `test - attach value`() = runTest {
        assertNull(StringKey.value())

        withContext(StringKey("foo")) {
            assertEquals("foo", StringKey.value())
        }

        assertNull(StringKey.value())
    }

    @Test
    fun `test - attach value - with default`() = runTest {
        assertEquals("foo", StringKeyWithDefault.value())

        withContext(StringKey("bar")) {
            assertEquals("bar", StringKey.value())
        }

        assertEquals("foo", StringKeyWithDefault.value())
    }
}
