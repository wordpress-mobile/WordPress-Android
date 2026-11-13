package org.wordpress.android.ui.reader.saved

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType

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
    fun `a local save leaves out empty fields`() {
        val save = post(blogId = 1, postId = 2).apply { title = "Title" }.toLocalSave()

        assertThat(save.title).isEqualTo("Title")
        assertThat(save.excerpt).isNull()
        assertThat(save.dateSaved).isNull()
    }

    @Test
    fun `saved post details carry the card's fields and leave out empty ones`() {
        val details = post(blogId = 1, postId = 2).apply {
            title = "Title"
            featuredImage = "https://example.com/image.jpg"
            numReplies = 3
            tags = ReaderTagList().apply {
                add(ReaderTag("travel", "Travel", "", "", ReaderTagType.DEFAULT))
            }
        }.toSavedPostDetails()

        assertThat(details.title).isEqualTo("Title")
        assertThat(details.displayImageUrl).isEqualTo("https://example.com/image.jpg")
        assertThat(details.commentCount).isEqualTo(3UL)
        assertThat(details.tags.map { it.slug to it.name }).containsExactly("travel" to "Travel")
        assertThat(details.excerpt).isNull()
        assertThat(details.siteIconUrl).isNull()
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
