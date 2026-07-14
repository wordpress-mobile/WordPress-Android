package org.wordpress.android.ui.comments.unified

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.CommentContentWithEditContext
import uniffi.wp_api.CommentStatus
import uniffi.wp_api.CommentType
import uniffi.wp_api.CommentWithEditContext
import uniffi.wp_api.CommentsRequestExecutor
import uniffi.wp_api.CommentsRequestRetrieveWithEditContextResponse
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostListParams
import uniffi.wp_api.PostsRequestExecutor
import uniffi.wp_api.PostsRequestFilterListWithViewContextResponse
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.SparseAnyPostWithViewContext
import uniffi.wp_api.SparsePostTitleWithViewContext
import uniffi.wp_api.UniffiWpApiClient
import uniffi.wp_api.WpErrorCode
import java.util.Date

/**
 * Tests for [CommentsRsDataSource.fetchPostTitles] — the per-site title cache, the posts→pages
 * endpoint fallback, negative caching of unresolvable ids, and request chunking — and for
 * [CommentsRsDataSource.getCommentForEdit]'s edit-context mapping and error handling.
 *
 * The [wpApiClient] stub executes each request's builder lambda against a mocked
 * [UniffiWpApiClient], recording the endpoint and paging params actually sent — so the tests
 * pin the requests made, not just how many there were.
 */
class CommentsRsDataSourceTest {
    private data class RecordedRequest(
        val endpointType: PostEndpointType,
        val includeCount: Int,
        val perPage: UInt?
    )

    private val wpApiClientProvider: WpApiClientProvider = mock()
    private val wpApiClient: WpApiClient = mock()
    private val uniffiClient: UniffiWpApiClient = mock()
    private val postsExecutor: PostsRequestExecutor = mock()
    private val commentsExecutor: CommentsRequestExecutor = mock()
    private lateinit var dataSource: CommentsRsDataSource

    private val recordedRequests = mutableListOf<RecordedRequest>()
    private val cannedResults = ArrayDeque<WpRequestResult<Any>>()

    /** Invoked after each request completes; lets a test simulate concurrent work mid-fetch. */
    private var afterRequest: (() -> Unit)? = null

    private val siteA = SiteModel().apply { id = 1 }
    private val siteB = SiteModel().apply { id = 2 }

    @Before
    fun setUp() {
        dataSource = CommentsRsDataSource(wpApiClientProvider)
        whenever(wpApiClientProvider.getWpApiClient(any(), anyOrNull())).thenReturn(wpApiClient)
        whenever(uniffiClient.posts()).thenReturn(postsExecutor)
        whenever(uniffiClient.comments()).thenReturn(commentsExecutor)
        commentsExecutor.stub {
            // Must return non-null to satisfy the mock's suspend contract; the payload each test
            // actually sees comes from the request-level stub below.
            on { retrieveWithEditContext(any(), any()) } doReturn
                CommentsRequestRetrieveWithEditContextResponse(editContextComment(), mock())
        }
        postsExecutor.stub {
            on { filterListWithViewContext(any(), any(), any()) } doSuspendableAnswer { invocation ->
                val params = invocation.getArgument<PostListParams>(1)
                recordedRequests += RecordedRequest(
                    endpointType = invocation.getArgument(0),
                    includeCount = params.include.size,
                    perPage = params.perPage
                )
                // The payload is decided by the request-level stub below; this value is unused.
                PostsRequestFilterListWithViewContextResponse(emptyList(), mock(), null, null)
            }
        }
        wpApiClient.stub {
            on { request<Any>(any()) } doSuspendableAnswer { invocation ->
                // Run the builder lambda so the executor stub above records what was requested.
                val executor = invocation.getArgument<suspend (UniffiWpApiClient) -> Any>(0)
                executor(uniffiClient)
                val result = cannedResults.removeFirst()
                afterRequest?.invoke()
                result
            }
        }
    }

    @Test
    fun `resolved titles are cached and skip the network`() = runTest {
        stubRequests(successResponse(sparsePost(5, "Hello")))

        assertThat(dataSource.fetchPostTitles(siteA, listOf(5))).isEqualTo(mapOf(5L to "Hello"))
        assertThat(dataSource.fetchPostTitles(siteA, listOf(5))).isEqualTo(mapOf(5L to "Hello"))

        assertThat(recordedRequests).hasSize(1)
    }

    @Test
    fun `titles are cached per site, not just per post id`() = runTest {
        stubRequests(
            successResponse(sparsePost(5, "Site A post")),
            successResponse(sparsePost(5, "Site B post"))
        )

        assertThat(dataSource.fetchPostTitles(siteA, listOf(5))).isEqualTo(mapOf(5L to "Site A post"))
        assertThat(dataSource.fetchPostTitles(siteB, listOf(5))).isEqualTo(mapOf(5L to "Site B post"))

        assertThat(recordedRequests).hasSize(2)
    }

    @Test
    fun `a posts endpoint failure still tries the pages endpoint`() = runTest {
        stubRequests(wpError(), successResponse(sparsePost(7, "About")))

        assertThat(dataSource.fetchPostTitles(siteA, listOf(7))).isEqualTo(mapOf(7L to "About"))

        assertThat(recordedRequests.map { it.endpointType })
            .containsExactly(PostEndpointType.Posts, PostEndpointType.Pages)
    }

    @Test
    fun `ids neither endpoint returns are negative-cached once both succeed`() = runTest {
        stubRequests(successResponse(), successResponse())

        assertThat(dataSource.fetchPostTitles(siteA, listOf(9))).isEqualTo(mapOf(9L to ""))
        assertThat(dataSource.fetchPostTitles(siteA, listOf(9))).isEqualTo(mapOf(9L to ""))

        // Posts + pages for the first call, none for the second.
        assertThat(recordedRequests.map { it.endpointType })
            .containsExactly(PostEndpointType.Posts, PostEndpointType.Pages)
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

        assertThat(recordedRequests.map { it.endpointType })
            .containsExactly(PostEndpointType.Posts, PostEndpointType.Pages, PostEndpointType.Posts)
    }

    @Test
    fun `clearPostTitles retries negative-cached ids`() = runTest {
        stubRequests(
            successResponse(), // posts: not found
            successResponse(), // pages: not found → negative-cached
            successResponse(sparsePost(9, "Now published"))
        )
        assertThat(dataSource.fetchPostTitles(siteA, listOf(9))).isEqualTo(mapOf(9L to ""))

        dataSource.clearPostTitles(siteA)

        assertThat(dataSource.fetchPostTitles(siteA, listOf(9))).isEqualTo(mapOf(9L to "Now published"))
        assertThat(recordedRequests).hasSize(3)
    }

    @Test
    fun `clearPostTitles evicts resolved titles so a refresh re-fetches them`() = runTest {
        stubRequests(
            successResponse(sparsePost(5, "Old title")),
            successResponse(sparsePost(5, "Renamed title"))
        )
        assertThat(dataSource.fetchPostTitles(siteA, listOf(5))).isEqualTo(mapOf(5L to "Old title"))

        dataSource.clearPostTitles(siteA)

        assertThat(dataSource.fetchPostTitles(siteA, listOf(5))).isEqualTo(mapOf(5L to "Renamed title"))
        assertThat(recordedRequests).hasSize(2)
    }

    @Test
    fun `clearPostTitles only evicts the given site's titles`() = runTest {
        stubRequests(
            successResponse(sparsePost(5, "Site A post")),
            successResponse(sparsePost(5, "Site B post")),
            successResponse(sparsePost(5, "Site A refetched"))
        )
        dataSource.fetchPostTitles(siteA, listOf(5))
        dataSource.fetchPostTitles(siteB, listOf(5))

        dataSource.clearPostTitles(siteA)

        assertThat(dataSource.fetchPostTitles(siteB, listOf(5))).isEqualTo(mapOf(5L to "Site B post"))
        assertThat(dataSource.fetchPostTitles(siteA, listOf(5))).isEqualTo(mapOf(5L to "Site A refetched"))
        assertThat(recordedRequests).hasSize(3)
    }

    @Test
    fun `a clear during an in-flight fetch prevents negative caching`() = runTest {
        stubRequests(
            successResponse(), // posts: not found
            successResponse(), // pages: not found — but a clear lands before the cache write
            successResponse(), // retry: posts
            successResponse() // retry: pages
        )
        afterRequest = {
            // Simulates another surface clearing while this fetch is between its network
            // responses and its negative-cache write.
            if (recordedRequests.size == 2) dataSource.clearPostTitles(siteA)
        }

        assertThat(dataSource.fetchPostTitles(siteA, listOf(9))).isEmpty()

        // Not negative-cached, so a later fetch goes back to the network.
        afterRequest = null
        assertThat(dataSource.fetchPostTitles(siteA, listOf(9))).isEqualTo(mapOf(9L to ""))
        assertThat(recordedRequests).hasSize(4)
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
        // Two posts-endpoint requests with the batch split at 100, each with a matching
        // explicit perPage — NOT one oversized request plus a pages fallback.
        assertThat(recordedRequests.map { Triple(it.endpointType, it.includeCount, it.perPage) })
            .containsExactly(
                Triple(PostEndpointType.Posts, 100, 100u),
                Triple(PostEndpointType.Posts, 1, 1u)
            )
    }

    @Test
    fun `getCommentForEdit maps the edit-context response`() = runTest {
        val serverComment = editContextComment(
            authorName = "author",
            authorEmail = "author@example.com",
            authorUrl = "https://example.com",
            contentRaw = "raw content"
        )
        stubRequests(WpRequestResult.Success(CommentsRequestRetrieveWithEditContextResponse(serverComment, mock())))

        val result = dataSource.getCommentForEdit(siteA, 42L)

        assertThat(result).isEqualTo(
            CommentsRsDataSource.RsEditedComment(
                authorName = "author",
                authorEmail = "author@example.com",
                authorUrl = "https://example.com",
                contentRaw = "raw content"
            )
        )
    }

    @Test
    fun `getCommentForEdit returns null on a server error`() = runTest {
        stubRequests(wpError())

        assertThat(dataSource.getCommentForEdit(siteA, 42L)).isNull()
    }

    @Test
    fun `getCommentForEdit returns null when the request throws`() = runTest {
        wpApiClient.stub {
            on { request<Any>(any()) } doSuspendableAnswer { throw IllegalStateException("boom") }
        }

        assertThat(dataSource.getCommentForEdit(siteA, 42L)).isNull()
    }

    // An edit-context comment with the four fields the editor consumes; everything else dummy.
    private fun editContextComment(
        authorName: String = "",
        authorEmail: String = "",
        authorUrl: String = "",
        contentRaw: String = ""
    ) = CommentWithEditContext(
        id = 42L,
        author = 1L,
        authorEmail = authorEmail,
        authorIp = "",
        authorName = authorName,
        authorUrl = authorUrl,
        authorUserAgent = "",
        content = CommentContentWithEditContext(raw = contentRaw, rendered = ""),
        date = "",
        dateGmt = Date(0),
        link = "",
        parent = 0L,
        post = 0L,
        status = CommentStatus.Approved,
        commentType = CommentType.Comment,
        authorAvatarUrls = emptyMap(),
        additionalFields = mock()
    )

    // A sparse post as returned by the id+title sparse-field request: everything else null.
    private fun sparsePost(id: Long, title: String) = SparseAnyPostWithViewContext(
        id = id,
        date = null,
        dateGmt = null,
        guid = null,
        link = null,
        modified = null,
        modifiedGmt = null,
        slug = null,
        status = null,
        postType = null,
        title = SparsePostTitleWithViewContext(rendered = title),
        content = null,
        author = null,
        excerpt = null,
        featuredMedia = null,
        commentStatus = null,
        pingStatus = null,
        format = null,
        meta = null,
        sticky = null,
        template = null,
        categories = null,
        tags = null,
        parent = null,
        menuOrder = null,
        additionalFields = null
    )

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
    private fun stubRequests(vararg responses: WpRequestResult<*>) {
        recordedRequests.clear()
        cannedResults.clear()
        responses.forEach { cannedResults.add(it as WpRequestResult<Any>) }
    }
}
