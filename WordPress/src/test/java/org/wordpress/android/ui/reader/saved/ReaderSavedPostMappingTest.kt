package org.wordpress.android.ui.reader.saved

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.models.ReaderPost

class ReaderSavedPostMappingTest {
    @Test
    fun `a WordPress-com post maps its blog and post IDs`() {
        val identity = post(blogId = 123, postId = 45).toSaveIdentity()

        assertThat(identity.id).isEqualTo(45UL)
        assertThat(identity.siteId).isEqualTo(123UL)
        assertThat(identity.feedId).isNull()
        assertThat(identity.feedItemId).isNull()
        assertThat(identity.isExternal).isFalse()
    }

    @Test
    fun `an external feed item keeps its feed IDs and flag`() {
        val identity = post(postId = 9, feedId = 7, feedItemId = 9, isExternal = true).toSaveIdentity()

        assertThat(identity.siteId).isNull()
        assertThat(identity.feedId).isEqualTo(7UL)
        assertThat(identity.feedItemId).isEqualTo(9UL)
        assertThat(identity.isExternal).isTrue()
    }

    @Test
    fun `a Jetpack post keeps both its blog and feed item IDs`() {
        val identity = post(blogId = 123, postId = 45, feedId = 7, feedItemId = 99).toSaveIdentity()

        assertThat(identity.siteId).isEqualTo(123UL)
        assertThat(identity.feedItemId).isEqualTo(99UL)
    }

    @Test
    fun `the local IDs round trip`() {
        val identity = post(blogId = 123, postId = 45).toSaveIdentity()

        assertThat(identity.localBlogId).isEqualTo(123L)
        assertThat(identity.localPostId).isEqualTo(45L)
    }

    @Test
    fun `a local save leaves out empty fields`() {
        val save = post(blogId = 1, postId = 2).apply { title = "Title" }.toLocalSave()

        assertThat(save.title).isEqualTo("Title")
        assertThat(save.excerpt).isNull()
        assertThat(save.dateSaved).isNull()
    }

    @Suppress("LongParameterList")
    private fun post(
        blogId: Long = 0,
        postId: Long = 0,
        feedId: Long = 0,
        feedItemId: Long = 0,
        isExternal: Boolean = false
    ) = ReaderPost().also {
        it.blogId = blogId
        it.postId = postId
        it.feedId = feedId
        it.feedItemId = feedItemId
        it.isExternal = isExternal
    }
}
