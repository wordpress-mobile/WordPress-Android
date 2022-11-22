package org.wordpress.android.fluxc.example.utils

import org.assertj.core.api.Assertions.assertThat

import org.junit.Test

class ArrayUtilsTest {
    /* CONTAINS */

    @Test
    fun `given valid string, when checking contains start of array, then the result is true`() {
        val stringArray = arrayOf("one", "two", "three", "four", "five")

        val result = ArrayUtils.contains(stringArray, "three")

        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `given valid string, when checking contains middle of array, then the result is true`() {
        val stringArray = arrayOf("one", "two", "three", "four", "five")

        val result = ArrayUtils.contains(stringArray, "one")

        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `given valid string, when checking contains end of array, then the result is true`() {
        val stringArray = arrayOf("one", "two", "three", "four", "five")

        val result = ArrayUtils.contains(stringArray, "five")

        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `given invalid string, when checking contains of array, then the result is false`() {
        val stringArray = arrayOf("one", "two", "three", "four", "five")

        val result = ArrayUtils.contains(stringArray, "ottff")

        assertThat(result).isEqualTo(false)
    }
}
