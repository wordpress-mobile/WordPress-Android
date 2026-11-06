package org.wordpress.android.ui.rs

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.post.RsPostMapper
import org.wordpress.android.ui.rs.data.RsSiteRestClient
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.PostContentWithEditContext
import uniffi.wp_api.SparsePostExcerpt
import uniffi.wp_api.TermEndpointType

// PostModel stores the tag list as a joined string via android.text.TextUtils, so these need a
// Robolectric runtime.
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class RsToFluxCMapperTest {
    private val restClient: RsSiteRestClient = mock()
    private val mapper = RsToFluxCMapper(restClient, RsPostMapper())
    private val site = SiteModel()

    /**
     * The field-by-field mapping is covered by `RsPostMapperTest` in FluxC; what this class adds is
     * turning the payload's tag ids into the names the editor shows.
     */
    @Test
    fun `tag ids are mapped to names in the order the payload lists them`() = runTest {
        val post = rsPost(tags = listOf(7L, 3L))
        whenever(restClient.fetchTermNames(site, listOf(7L, 3L), TermEndpointType.Tags))
            .thenReturn(mapOf(3L to "second", 7L to "first"))

        assertThat(mapper.map(post, site, isPage = false).tagNameList).containsExactly("first", "second")
    }

    @Test
    fun `a tag id the site no longer has is dropped`() = runTest {
        val post = rsPost(tags = listOf(7L, 3L))
        whenever(restClient.fetchTermNames(site, listOf(7L, 3L), TermEndpointType.Tags))
            .thenReturn(mapOf(7L to "first"))

        assertThat(mapper.map(post, site, isPage = false).tagNameList).containsExactly("first")
    }

    /**
     * The row the bridge inserts from this is what the editor saves back, and wp/v2 replaces a
     * post's whole tag set, so mapping the post with the names that did arrive would delete the
     * missing ones from the site on the next save. Failing instead leaves the row untouched
     * (CMM-2502).
     */
    @Test
    fun `a failed name lookup aborts the mapping rather than dropping tags`() = runTest {
        val post = rsPost(tags = listOf(7L, 3L))
        // A nullable-returning mock answers null on its own, so the lookup is verified below
        // rather than stubbed: without that this would pass for a mapper that never asks at all.
        val error = runCatching { mapper.map(post, site, isPage = false) }.exceptionOrNull()

        assertThat(error)
            .isInstanceOf(RsBridgeException::class.java)
            .hasMessageContaining("tag names")
        verify(restClient).fetchTermNames(site, listOf(7L, 3L), TermEndpointType.Tags)
    }

    @Test
    fun `a post without tags is mapped without asking the site for names`() = runTest {
        val post = rsPost(tags = emptyList())

        assertThat(mapper.map(post, site, isPage = false).tagNameList).isEmpty()
    }

    /**
     * wp/v2 returns a term name as WordPress stores it. Keeping the encoded form would show
     * `AT&amp;T` in the editor, and the REST post client - which encodes a name again to look it up
     * - would then search for `AT&amp;amp;T`, find nothing, and drop the tag from the post.
     */
    @Test
    fun `a tag name the site stores HTML-encoded is decoded`() = runTest {
        val post = rsPost(tags = listOf(7L))
        whenever(restClient.fetchTermNames(site, listOf(7L), TermEndpointType.Tags))
            .thenReturn(mapOf(7L to "AT&amp;T"))

        assertThat(mapper.map(post, site, isPage = false).tagNameList).containsExactly("AT&T")
    }

    /**
     * The parent is only copied for a page, so mapping one as a post leaves `parentId` at 0 and the
     * next save detaches the page from its parent on the site.
     */
    @Test
    fun `a page keeps the parent the payload carries`() = runTest {
        val mapped = mapper.map(rsPost(tags = emptyList(), parent = 12L), site, isPage = true)

        assertThat(mapped.isPage).isTrue()
        assertThat(mapped.parentId).isEqualTo(12L)
    }

    @Test
    fun `a mapped post is not a local draft and carries no local changes`() = runTest {
        val mapped = mapper.map(rsPost(tags = emptyList()), site, isPage = false)

        assertThat(mapped.isLocalDraft).isFalse()
        assertThat(mapped.isLocallyChanged).isFalse()
    }

    // Only a few fields are read in these tests; mocking avoids building the ~29-field data class
    // by hand.
    @Suppress("DoNotMockDataClass")
    private fun rsPost(tags: List<Long>, parent: Long? = null): AnyPostWithEditContext {
        val post: AnyPostWithEditContext = mock()
        whenever(post.tags).thenReturn(tags)
        whenever(post.parent).thenReturn(parent)
        whenever(post.excerpt).thenReturn(SparsePostExcerpt("", "", false))
        whenever(post.content).thenReturn(
            PostContentWithEditContext("Post content", "<p>Post content</p>", false, null)
        )
        return post
    }
}
