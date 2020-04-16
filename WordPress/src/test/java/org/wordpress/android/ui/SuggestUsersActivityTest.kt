package org.wordpress.android.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.wordpress.android.ui.SuggestUsersActivity.Companion.getOnlyElement

class SuggestUsersActivityTest {
    @Test
    fun `getOnlyElement null list`() {
        assertNull(getOnlyElement(null))
    }

    @Test
    fun `getOnlyElement empty list`() {
        assertNull(getOnlyElement(emptyList<Any>()))
    }

    @Test
    fun `getOnlyElement list of 1`() {
        assertEquals("expected_list_item",
                getOnlyElement(listOf("expected_list_item"))
        )
    }

    @Test
    fun `getOnlyElement list of more than 1`() {
        assertNull(getOnlyElement(listOf(1, 2)))
    }

    @Test
    fun `getOnlyElement from list that mutates immediately after size call`() {
        assertNull(getOnlyElement(object : ArrayList<String>() {
            init {
                add("expected_list_item")
            }

            override val size: Int
                get() {
                    val size = super.size
                    // Simulate the only item in the list being removed from another thread
                    // immediately after the list's size is retrieved
                    removeAt(0)
                    return size
                }
        }))
    }
}
