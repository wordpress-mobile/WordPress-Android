package org.wordpress.android.ui.rs

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.CommentListParams
import uniffi.wp_api.CommentsRequestExecutor
import uniffi.wp_api.CommentsRequestFilterListWithViewContextResponse
import uniffi.wp_api.SparseCommentFieldWithViewContext
import uniffi.wp_api.SparseCommentWithViewContext
import uniffi.wp_api.UniffiWpApiClient
import uniffi.wp_api.WpNetworkHeaderMap

/**
 * Tests for [RsCommentCountFetcher]: that one batched request covers a whole set of posts, that
 * posts with no comments are reported as zero rather than omitted, and that a batch which would
 * undercount falls back to reading each post's total header instead.
 *
 * The [wpApiClient] stub runs each request's builder lambda against a mocked [UniffiWpApiClient],
 * recording the params actually sent, so the tests pin the requests made rather than only counting
 * them.
 */
class RsCommentCountFetcherTest {
    private val wpApiClientProvider: WpApiClientProvider = mock()
    private val wpApiClient: WpApiClient = mock()
    private val uniffiClient: UniffiWpApiClient = mock()
    private val commentsExecutor: CommentsRequestExecutor = mock()
    private lateinit var fetcher: RsCommentCountFetcher

    private val recordedParams = mutableListOf<CommentListParams>()
    private val cannedResults = ArrayDeque<WpRequestResult<Any>>()

    private val site = SiteModel().apply { id = 1 }

    @Before
    fun setUp() {
        fetcher = RsCommentCountFetcher(wpApiClientProvider)
        whenever(wpApiClientProvider.getWpApiClient(any(), anyOrNull())).thenReturn(wpApiClient)
        whenever(uniffiClient.comments()).thenReturn(commentsExecutor)
        commentsExecutor.stub {
            on {
                filterListWithViewContext(any(), any<List<SparseCommentFieldWithViewContext>>())
            } doSuspendableAnswer { invocation ->
                recordedParams += invocation.getArgument<CommentListParams>(0)
                // The payload each test sees comes from the request-level stub below.
                CommentsRequestFilterListWithViewContextResponse(emptyList(), mock(), null, null)
            }
        }
        wpApiClient.stub {
            on { request<Any>(any()) } doSuspendableAnswer { invocation ->
                val builder = invocation.getArgument<suspend (UniffiWpApiClient) -> Any>(0)
                builder(uniffiClient)
                cannedResults.removeFirst()
            }
        }
    }

    @Test
    fun `one request counts every post in the batch`() = runTest {
        stubRequests(response(total = 3, postIds = listOf(5L, 5L, 7L)))

        val counts = fetcher.fetchCommentCounts(site, listOf(5L, 7L))

        assertThat(counts).isEqualTo(mapOf(5L to 2L, 7L to 1L))
        assertThat(recordedParams).hasSize(1)
        assertThat(recordedParams.single().post).containsExactly(5L, 7L)
    }

    @Test
    fun `a post with no comments counts zero rather than being omitted`() = runTest {
        stubRequests(response(total = 1, postIds = listOf(5L)))

        val counts = fetcher.fetchCommentCounts(site, listOf(5L, 7L))

        assertThat(counts).isEqualTo(mapOf(5L to 1L, 7L to 0L))
    }

    @Test
    fun `a batch that would undercount falls back to per-post totals`() = runTest {
        // The header reports more comments than the page returned, so counting the page would
        // undercount the busiest post. Each post is then asked for individually.
        stubRequests(
            response(total = 500, postIds = listOf(5L)),
            response(total = 480, postIds = emptyList()),
            response(total = 20, postIds = emptyList())
        )

        val counts = fetcher.fetchCommentCounts(site, listOf(5L, 7L))

        assertThat(counts).isEqualTo(mapOf(5L to 480L, 7L to 20L))
        assertThat(recordedParams).hasSize(3)
        assertThat(recordedParams[1].post).containsExactly(5L)
        assertThat(recordedParams[2].post).containsExactly(7L)
    }

    @Test
    fun `an empty request makes no network call`() = runTest {
        assertThat(fetcher.fetchCommentCounts(site, emptyList())).isEmpty()
        assertThat(recordedParams).isEmpty()
    }

    @Test
    fun `a failed batch gives up instead of fanning out into per-post requests`() = runTest {
        // A request that could not reach the site would only fail again per post, holding the
        // caller up for one timeout each to arrive at the same empty answer.
        stubRequests(failure())

        assertThat(fetcher.fetchCommentCounts(site, listOf(5L, 7L))).isEmpty()
        assertThat(recordedParams).hasSize(1)
    }

    @Test
    fun `a per-post request that fails simply omits that post`() = runTest {
        stubRequests(
            response(total = 500, postIds = listOf(5L)),
            failure(),
            response(total = 20, postIds = emptyList())
        )

        assertThat(fetcher.fetchCommentCounts(site, listOf(5L, 7L))).isEqualTo(mapOf(7L to 20L))
    }

    private fun stubRequests(vararg results: WpRequestResult<Any>) {
        cannedResults.clear()
        cannedResults.addAll(results.toList())
    }

    private fun response(total: Int, postIds: List<Long>): WpRequestResult<Any> {
        val headerMap: WpNetworkHeaderMap = mock()
        whenever(headerMap.wpTotal()).thenReturn(total.toUInt())
        return WpRequestResult.Success(
            CommentsRequestFilterListWithViewContextResponse(
                postIds.map { sparseComment(it) },
                headerMap,
                null,
                null
            )
        )
    }

    private fun failure(): WpRequestResult<Any> = mock<WpRequestResult.RequestExecutionFailed<Any>>()

    /** Only [postId] matters to the count; the rest of the sparse comment is left unset. */
    private fun sparseComment(postId: Long) = SparseCommentWithViewContext(
        null, null, null, null, null, null, null, null, null, postId, null, null, null, null
    )
}
