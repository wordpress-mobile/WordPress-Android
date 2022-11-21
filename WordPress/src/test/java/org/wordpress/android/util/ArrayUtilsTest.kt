package org.wordpress.android.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ArrayUtilsTest {
    /* REMOVE */

    @Test
    fun `given valid index, when removing from start of array, then the result is the expected one`() {
        val stringArray = arrayOf("one", "two", "three", "four", "five")

        val result = ArrayUtils.remove(stringArray, 0)

        val expected = arrayOf("two", "three", "four", "five")
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `given valid index, when removing from middle of array, then the result is the expected one`() {
        val stringArray = arrayOf("one", "two", "three", "four", "five")

        val result = ArrayUtils.remove(stringArray, 2)

        val expected = arrayOf("one", "two", "four", "five")
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `given valid index, when removing from end of array, then the result is the expected one`() {
        val stringArray = arrayOf("one", "two", "three", "four", "five")

        val result = ArrayUtils.remove(stringArray, 4)

        val expected = arrayOf("one", "two", "three", "four")
        assertThat(result).isEqualTo(expected)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `given invalid negative index, when removing from array, then throw index out of bounds exception`() {
        val stringArray = arrayOf("one", "two", "three", "four", "five")

        val result = ArrayUtils.remove(stringArray, -1)

        val expected = arrayOf("one", "two", "four", "five")
        assertThat(result).isEqualTo(expected)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `given invalid positive index, when removing from array, then throw index out of bounds exception`() {
        val stringArray = arrayOf("one", "two", "three", "four", "five")

        val result = ArrayUtils.remove(stringArray, 5)

        val expected = arrayOf("one", "two", "four", "five")
        assertThat(result).isEqualTo(expected)
    }
}
