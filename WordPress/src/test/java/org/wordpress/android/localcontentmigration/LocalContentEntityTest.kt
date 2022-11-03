package org.wordpress.android.localcontentmigration

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.localcontentmigration.LocalContentEntity.Post

class LocalContentEntityTest {
    private val mockSiteId = 7
    private val mockPostId = 42
    private val mockPostPath = "site/7/Post/42"
    private val mockPostsPath = "site/7/Post"

    @Test
    fun `when a post URI path is generated without an entity id`() {
        val path = Post.getPathForContent(localSiteId = mockSiteId, localEntityId = null)
        assertThat(path).isEqualTo(mockPostsPath)
    }

    @Test
    fun `when a post URI path is generated with an entity id`() {
        val path = Post.getPathForContent(localSiteId = mockSiteId, localEntityId = mockPostId)
        assertThat(path).isEqualTo(mockPostPath)
    }

    @Test
    fun `when a post URI path is captured without an entity id`() {
        val captures = Post.contentIdCapturePattern.matchEntire(mockPostsPath)?.groups?.mapNotNull { it?.value }!!
        assertThat(captures.getOrNull(1)).isEqualTo("$mockSiteId")
        assertThat(captures.getOrNull(2)).isNull()
    }

    @Test
    fun `when a post URI path is captured with an entity id`() {
        val captures = Post.contentIdCapturePattern.matchEntire(mockPostPath)?.groups?.mapNotNull { it?.value }!!
        assertThat(captures.getOrNull(1)).isEqualTo("$mockSiteId")
        assertThat(captures.getOrNull(2)).isEqualTo("$mockPostId")
    }
}
