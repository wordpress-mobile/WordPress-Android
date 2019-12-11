package org.wordpress.android.ui.posts

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE

@RunWith(MockitoJUnitRunner::class)
class PostUtilsUnitTest {
    @Test
    fun `prepareForPublish updates dateLocallyChanged`() {
        val post = invokePreparePostForPublish()
        assertThat(post.dateLocallyChanged).isNotEmpty()
    }

    @Test
    fun `prepareForPublish marks the post as locally changed`() {
        val post = invokePreparePostForPublish()
        assertThat(post.isLocallyChanged).isTrue()
    }

    @Test
    fun `prepareForPublish updates changes confirmed hashcode`() {
        val post = invokePreparePostForPublish()
        assertThat(post.changesConfirmedContentHashcode).isEqualTo(post.contentHashcode())
    }

    @Test
    fun `prepareForPublish updates post status to Pending when the user doesn't have publish rights`() {
        val post = invokePreparePostForPublish(false)
        assertThat(post.status).isEqualTo(PostStatus.PENDING.toString())
    }

    @Test
    fun `prepareForPublish updates post status to Publish when the user has publish rights`() {
        val post = invokePreparePostForPublish(true)
        assertThat(post.status).isEqualTo(PostStatus.PUBLISHED.toString())
    }

    @Test
    fun `prepareForPublish updates post status to Publish when on self-hosted site`() {
        val firstPost = invokePreparePostForPublish(
                false,
                isSelfHosted = true
        )
        val secondPost = invokePreparePostForPublish(
                true,
                isSelfHosted = true
        )

        assertThat(firstPost.status).isEqualTo(PostStatus.PUBLISHED.toString())
        assertThat(secondPost.status).isEqualTo(PostStatus.PUBLISHED.toString())
    }

    @Test
    fun `prepareForPublish does not change post status when it's a Private post`() {
        val post = invokePreparePostForPublish(postStatus = PRIVATE)
        assertThat(post.status).isEqualTo(PostStatus.PRIVATE.toString())
    }

    private companion object Fixtures {
        fun invokePreparePostForPublish(
            hasCapabilityPublishPosts: Boolean = true,
            isSelfHosted: Boolean = false,
            postStatus: PostStatus = DRAFT,
            dateLocallyChanged: String? = null
        ): PostModel {
            val post = PostModel()
            post.setStatus(postStatus.toString())
            if (dateLocallyChanged != null) {
                post.setDateLocallyChanged(dateLocallyChanged)
            }

            val site = SiteModel()
            site.hasCapabilityPublishPosts = hasCapabilityPublishPosts
            site.origin = if (isSelfHosted) SiteModel.ORIGIN_XMLRPC else SiteModel.ORIGIN_WPCOM_REST

            PostUtils.preparePostForPublish(post, site)
            return post
        }
    }
}
