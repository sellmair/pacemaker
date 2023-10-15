package io.sellmair.pacemaker.utils

import kotlin.test.*

class ContextTest {

    object StringKey : Context.Key<String>

    object StringKeyWithDefault : Context.Key.WithDefault<String> {
        override val default: String = "foo"
    }

    internal data class MyElement(val value: Int) : Context.Element<MyElement> {
        override val key: Context.Key<MyElement> = Key

        companion object Key : Context.Key.WithDefault<MyElement> {
            override val default: MyElement = MyElement(0)
        }
    }

    @Test
    fun `test - empty context`() {
        val empty = Context.empty
        assertTrue(StringKey !in empty)
        assertTrue(StringKeyWithDefault !in empty)
        assertTrue(MyElement !in empty)

        assertNull(empty[StringKey])
        assertEquals("foo", empty[StringKeyWithDefault])
        assertSame(MyElement.default, empty[MyElement])
    }

    @Test
    fun `test - adding elements to empty context`() {
        val empty = Context.empty
        val context0 = empty.plus(StringKey, "value0")
        assertEquals("value0", context0[StringKey])

        val context1 = context0
            .plus(StringKey, "value1")
            .plus(StringKeyWithDefault, "a")

        assertEquals("value1", context1[StringKey])
        assertEquals("a", context1[StringKeyWithDefault])
        assertSame(MyElement.default, context1[MyElement])

        val context2 = context1.plus(MyElement(1))
        assertEquals(MyElement(1), context2[MyElement])
    }

    @Test
    fun `test - creating non-empty context`() {
        val context = Context(MyElement(1), StringKeyWithDefault plus "bar")
        assertEquals(MyElement(1), context[MyElement])
        assertEquals("bar", context[StringKeyWithDefault])
        assertNull(context[StringKey])
    }

    @Test
    fun `test - merging two context`() {
        val contextA = Context(MyElement(1), StringKey plus "stringKeyValueA", StringKeyWithDefault plus "stringKeyWithDefaultValueA")
        val contextB = Context(MyElement(2), StringKey plus "stringKeyValueB")

        run {
            val merged = contextA + contextB
            assertEquals(MyElement(2), merged[MyElement])
            assertEquals("stringKeyValueB", merged[StringKey])
            assertEquals("stringKeyWithDefaultValueA", merged[StringKeyWithDefault])
        }

        run {
            val merged = contextB + contextA
            assertEquals(MyElement(1), merged[MyElement])
            assertEquals("stringKeyValueA", merged[StringKey])
            assertEquals("stringKeyWithDefaultValueA", merged[StringKeyWithDefault])
        }
    }
}
