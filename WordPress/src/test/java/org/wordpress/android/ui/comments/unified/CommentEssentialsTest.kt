package org.wordpress.android.ui.comments.unified

import org.junit.Assert.assertEquals
import org.junit.Test

class CommentEssentialsTest {
    @Test
    fun `Should return isValid TRUE if commentId is EQUAL to 0`() {
        val expected = true
        val actual = CommentEssentials(commentId = 0).isValid()
        assertEquals(expected, actual)
    }

    @Test
    fun `Should return isValid TRUE if commentId is GREATER than 0`() {
        val expected = true
        val actual = CommentEssentials(commentId = 1).isValid()
        assertEquals(expected, actual)
    }

    @Test
    fun `Should return isValid FALSE if commentId is EQUAL to -1`() {
        val expected = false
        val actual = CommentEssentials(commentId = -1).isValid()
        assertEquals(expected, actual)
    }

    @Test
    fun `Should return the expected default parameters`() {
        val expected = CommentEssentials(
            commentId = -1,
            userName = "",
            commentText = "",
            userUrl = "",
            userEmail = ""
        )
        val actual = CommentEssentials()
        assertEquals(expected, actual)
    }
}
