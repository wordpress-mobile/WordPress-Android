package org.wordpress.android.localcontentmigration

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.localcontentmigration.LocalContentEntity.Post

class LocalContentEntityTest {
    private val mockPostId = 42
    private val mockPostPath = "Post/42"
    private val mockPostsPath = "Post"

    @Test
    fun `when a post URI path is generated without an entity id`() {
        val path = Post.getPathForContent(localEntityId = null)
        assertThat(path).isEqualTo(mockPostsPath)
    }

    @Test
    fun `when a post URI path is generated with an entity id`() {
        val path = Post.getPathForContent(localEntityId = mockPostId)
        assertThat(path).isEqualTo(mockPostPath)
    }

    @Test
    fun `when a post URI path is captured without an entity id`() {
        val captures = Post.contentIdCapturePattern.matchEntire(mockPostsPath)?.groups?.mapNotNull { it?.value }!!
        assertThat(captures.getOrNull(1)).isNull()
    }

    @Test
    fun `when a post URI path is captured with an entity id`() {
        val captures = Post.contentIdCapturePattern.matchEntire(mockPostPath)?.groups?.mapNotNull { it?.value }!!
        assertThat(captures.getOrNull(1)).isEqualTo("$mockPostId")
    }
}
