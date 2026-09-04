package org.wordpress.android.ui.postsrs

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.postsrs.data.PostRsRestClient
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.PostContentWithEditContext
import uniffi.wp_api.SparsePostExcerpt

class PostRsToFluxCMapperTest {
    private val restClient: PostRsRestClient = mock()
    private val mapper = PostRsToFluxCMapper(restClient)
    private val site = SiteModel()

    /**
     * The regression guard for CMM-2392: `rendered` is the summary WordPress generates from the
     * content when the post has no excerpt, so mapping it would hand the editor an excerpt the
     * author never wrote — which the next Update would then save to the server.
     */
    @Test
    fun `blank raw excerpt does not fall back to the auto-generated rendered excerpt`() = runTest {
        val post = rsPost(SparsePostExcerpt("", AUTO_GENERATED, false))

        assertThat(mapper.map(post, site).excerpt).isEmpty()
    }

    @Test
    fun `author-written excerpt is mapped as-is`() = runTest {
        val post = rsPost(SparsePostExcerpt("Written by hand", AUTO_GENERATED, false))

        assertThat(mapper.map(post, site).excerpt).isEqualTo("Written by hand")
    }

    @Test
    fun `missing excerpt maps to an empty excerpt`() = runTest {
        val post = rsPost(excerpt = null)

        assertThat(mapper.map(post, site).excerpt).isEmpty()
    }

    @Test
    fun `null raw excerpt does not fall back to the rendered excerpt`() = runTest {
        val post = rsPost(SparsePostExcerpt(null, AUTO_GENERATED, false))

        assertThat(mapper.map(post, site).excerpt).isEmpty()
    }

    // Only the excerpt and content are read in these tests; mocking avoids building the
    // ~29-field data class by hand.
    @Suppress("DoNotMockDataClass")
    private fun rsPost(excerpt: SparsePostExcerpt?): AnyPostWithEditContext {
        val post: AnyPostWithEditContext = mock()
        whenever(post.excerpt).thenReturn(excerpt)
        whenever(post.content).thenReturn(
            PostContentWithEditContext("Post content", "<p>Post content</p>", false, null)
        )
        return post
    }

    companion object {
        private const val AUTO_GENERATED = "<p>Post content&hellip;</p>"
    }
}
