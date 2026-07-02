package org.wordpress.android.ui.comments.unified

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.PostsRequestFilterListWithViewContextResponse
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.SparseAnyPostWithViewContext
import uniffi.wp_api.SparsePostTitleWithViewContext
import uniffi.wp_api.WpErrorCode

/**
 * Tests for [CommentsRsDataSource.fetchPostTitles]: the per-site title cache, the posts→pages
 * endpoint fallback, negative caching of unresolvable ids, and request chunking.
 */
class CommentsRsDataSourceTest {
    private val wpApiClient: WpApiClient = mock()
    private val wpApiClientProvider: WpApiClientProvider = mock()
    private lateinit var dataSource: CommentsRsDataSource

    private val siteA = SiteModel().apply { id = 1 }
    private val siteB = SiteModel().apply { id = 2 }

    @Before
    fun setUp() {
        dataSource = CommentsRsDataSource(wpApiClientProvider)
        whenever(wpApiClientProvider.getWpApiClient(any(), anyOrNull())).thenReturn(wpApiClient)
    }

    @Test
    fun `resolved titles are cached and skip the network`() = runTest {
        stubRequests(successResponse(sparsePost(5, "Hello")))

        assertThat(dataSource.fetchPostTitles(siteA, listOf(5))).isEqualTo(mapOf(5L to "Hello"))
        assertThat(dataSource.fetchPostTitles(siteA, listOf(5))).isEqualTo(mapOf(5L to "Hello"))

        verify(wpApiClient, times(1)).request<Any>(any())
    }

    @Test
    fun `titles are cached per site, not just per post id`() = runTest {
        stubRequests(
            successResponse(sparsePost(5, "Site A post")),
            successResponse(sparsePost(5, "Site B post"))
        )

        assertThat(dataSource.fetchPostTitles(siteA, listOf(5))).isEqualTo(mapOf(5L to "Site A post"))
        assertThat(dataSource.fetchPostTitles(siteB, listOf(5))).isEqualTo(mapOf(5L to "Site B post"))

        verify(wpApiClient, times(2)).request<Any>(any())
    }

    @Test
    fun `a posts endpoint failure still tries the pages endpoint`() = runTest {
        stubRequests(wpError(), successResponse(sparsePost(7, "About")))

        assertThat(dataSource.fetchPostTitles(siteA, listOf(7))).isEqualTo(mapOf(7L to "About"))

        verify(wpApiClient, times(2)).request<Any>(any())
    }

    @Test
    fun `ids neither endpoint returns are negative-cached once both succeed`() = runTest {
        stubRequests(successResponse(), successResponse())

        assertThat(dataSource.fetchPostTitles(siteA, listOf(9))).isEqualTo(mapOf(9L to ""))
        assertThat(dataSource.fetchPostTitles(siteA, listOf(9))).isEqualTo(mapOf(9L to ""))

        // Two requests (posts + pages) for the first call, none for the second.
        verify(wpApiClient, times(2)).request<Any>(any())
    }

    @Test
    fun `a transient failure is not negative-cached`() = runTest {
        stubRequests(
            successResponse(), // posts: id not found
            wpError(), // pages: transient failure — must NOT negative-cache
            successResponse(sparsePost(9, "Resolved later"))
        )

        assertThat(dataSource.fetchPostTitles(siteA, listOf(9))).isEmpty()
        assertThat(dataSource.fetchPostTitles(siteA, listOf(9))).isEqualTo(mapOf(9L to "Resolved later"))

        verify(wpApiClient, times(3)).request<Any>(any())
    }

    @Test
    fun `clearUnresolvedPostTitles retries negative-cached ids`() = runTest {
        stubRequests(
            successResponse(), // posts: not found
            successResponse(), // pages: not found → negative-cached
            successResponse(sparsePost(9, "Now published"))
        )
        assertThat(dataSource.fetchPostTitles(siteA, listOf(9))).isEqualTo(mapOf(9L to ""))

        dataSource.clearUnresolvedPostTitles(siteA)

        assertThat(dataSource.fetchPostTitles(siteA, listOf(9))).isEqualTo(mapOf(9L to "Now published"))
        verify(wpApiClient, times(3)).request<Any>(any())
    }

    @Test
    fun `batches over 100 ids are chunked to the per_page maximum`() = runTest {
        val ids = (1L..101L).toList()
        stubRequests(
            successResponse(*ids.take(100).map { sparsePost(it, "Post $it") }.toTypedArray()),
            successResponse(sparsePost(101, "Post 101"))
        )

        val titles = dataSource.fetchPostTitles(siteA, ids)

        assertThat(titles).hasSize(101)
        assertThat(titles[101L]).isEqualTo("Post 101")
        verify(wpApiClient, times(2)).request<Any>(any())
    }

    private fun sparsePost(id: Long, title: String): SparseAnyPostWithViewContext {
        val sparseTitle = mock<SparsePostTitleWithViewContext>()
        whenever(sparseTitle.rendered).thenReturn(title)
        val post = mock<SparseAnyPostWithViewContext>()
        whenever(post.id).thenReturn(id)
        whenever(post.title).thenReturn(sparseTitle)
        return post
    }

    private fun successResponse(vararg posts: SparseAnyPostWithViewContext) = WpRequestResult.Success(
        response = PostsRequestFilterListWithViewContextResponse(posts.toList(), mock(), null, null)
    )

    private fun wpError() = WpRequestResult.WpError<Any>(
        errorCode = WpErrorCode.Forbidden(),
        errorMessage = "server said no",
        statusCode = 403u,
        response = "",
        requestUrl = "https://example.com",
        requestMethod = RequestMethod.GET
    )

    @Suppress("UNCHECKED_CAST")
    private suspend fun stubRequests(vararg responses: WpRequestResult<*>) {
        var stubbing = whenever(wpApiClient.request<Any>(any()))
        for (response in responses) {
            stubbing = stubbing.thenReturn(response as WpRequestResult<Any>)
        }
    }
}
