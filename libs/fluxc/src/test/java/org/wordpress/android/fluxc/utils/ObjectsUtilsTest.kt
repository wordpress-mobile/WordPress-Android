package org.wordpress.android.fluxc.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class ObjectsUtilsTest {
    @Test
    fun `equals returns true for same references`() {
        val first = Any()
        val second = first
        assertTrue(ObjectsUtils.equals(first, second))
    }

    @Test
    fun `equals return true if both arguments are null`() {
        val first = null
        val second = null
        assertTrue(ObjectsUtils.equals(first, second))
    }

    @Test
    fun `equals returns false for different references`() {
        val first = Any()
        val second = Any()
        assertFalse(ObjectsUtils.equals(first, second))
    }

    @Test
    fun `equals returns false when only first argument is null`() {
        val first = Any()
        val second = null
        assertFalse(ObjectsUtils.equals(first, second))
    }

    @Test
    fun `equals returns false when only second argument is null`() {
        val first = null
        val second = Any()
        assertFalse(ObjectsUtils.equals(first, second))
    }
}
