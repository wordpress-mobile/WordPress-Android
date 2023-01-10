package org.wordpress.android.ui.comments.unified.extension

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.comments.unified.CommentEssentials

class CommentEssentialsExtensionKtTest {
    private val commentEssentials = CommentEssentials(
        userName = "name",
        commentText = "text",
        userUrl = "url",
        userEmail = "email"
    )

    @Test
    fun `Should return FALSE for isNotEqualTo if commentText, userEmail, userName and userUrl are the SAME`() {
        assertThat(commentEssentials.isNotEqualTo(commentEssentials)).isFalse
    }

    @Test
    fun `Should return TRUE for isNotEqualTo if commentText, userEmail, userName and userUrl are DIFFERENT`() {
        val commentEssentials = commentEssentials
        val commentEssentials2 = commentEssentials.copy(
            userName = "name2",
            commentText = "text2",
            userUrl = "url2",
            userEmail = "email2"
        )
        assertThat(commentEssentials.isNotEqualTo(commentEssentials2)).isTrue
    }

    @Test
    fun `Should return TRUE for isNotEqualTo if ONLY userName is DIFFERENT`() {
        val commentEssentials = commentEssentials
        val commentEssentials2 = commentEssentials.copy(
            userName = "name2"
        )
        assertThat(commentEssentials.isNotEqualTo(commentEssentials2)).isTrue
    }

    @Test
    fun `Should return TRUE for isNotEqualTo if ONLY commentText is DIFFERENT`() {
        val commentEssentials = commentEssentials
        val commentEssentials2 = commentEssentials.copy(
            commentText = "text2"
        )
        assertThat(commentEssentials.isNotEqualTo(commentEssentials2)).isTrue
    }

    @Test
    fun `Should return TRUE for isNotEqualTo if ONLY userUrl is DIFFERENT`() {
        val commentEssentials = commentEssentials
        val commentEssentials2 = commentEssentials.copy(
            userEmail = "url2"
        )
        assertThat(commentEssentials.isNotEqualTo(commentEssentials2)).isTrue
    }

    @Test
    fun `Should return TRUE for isNotEqualTo if ONLY userEmail is DIFFERENT`() {
        val commentEssentials = commentEssentials
        val commentEssentials2 = commentEssentials.copy(
            userEmail = "email2"
        )
        assertThat(commentEssentials.isNotEqualTo(commentEssentials2)).isTrue
    }
}
