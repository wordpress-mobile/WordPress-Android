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
    fun `removeWPGallery removes multiple wp-gallery tags and its internals without affecting content in between`() {
        /**
         * This content is generated using the Block Editor to test a real-world example. It includes 2 galleries,
         * first one has 2 photos each with a caption and second one doesn't have a caption.
         * In between 2 galleries there is a "Test Content" paragraph.
         */
        val content = """
<!-- wp:gallery {"ids":[1554,1549]} -->
<figure class="wp-block-gallery columns-2 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><img src="https://leweo7test.files.wordpress.com/2020/01/pexels-photo-247502.jpeg" data-id="1554" class="wp-image-1554" /><figcaption class="blocks-gallery-item__caption">C1</figcaption></figure></li><li class="blocks-gallery-item"><figure><img src="https://leweo7test.files.wordpress.com/2019/04/img_20191023_150221-1.jpg" data-id="1549" class="wp-image-1549" /><figcaption class="blocks-gallery-item__caption">D2</figcaption></figure></li></ul></figure>
<!-- /wp:gallery -->

<!-- wp:paragraph -->
<p>Test content</p>
<!-- /wp:paragraph -->

<!-- wp:gallery {"ids":[1546]} -->
<figure class="wp-block-gallery columns-1 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><img src="https://leweo7test.files.wordpress.com/2019/04/kunkka_underlords.png" data-id="1546" class="wp-image-1546" /></figure></li></ul></figure>
<!-- /wp:gallery -->
"""
        val expectedResult = """


<!-- wp:paragraph -->
<p>Test content</p>
<!-- /wp:paragraph -->


"""
        assertThat(PostUtils.removeWPGallery(content)).isEqualTo(expectedResult)
    }

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
