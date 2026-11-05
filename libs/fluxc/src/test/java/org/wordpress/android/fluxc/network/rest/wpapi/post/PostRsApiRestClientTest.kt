package org.wordpress.android.fluxc.network.rest.wpapi.post

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.action.UploadAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpAdditionalFieldsBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.network.rest.wpapi.taxonomy.TaxonomyRsApiRestClient
import org.wordpress.android.fluxc.store.PostStore.DeletedPostPayload
import org.wordpress.android.fluxc.store.PostStore.FetchPostStatusResponsePayload
import org.wordpress.android.fluxc.store.PostStore.PostDeleteActionType
import org.wordpress.android.fluxc.store.PostStore.PostErrorType
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.DateTimeUtils
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.PostContentWithEditContext
import uniffi.wp_api.PostCreateParams
import uniffi.wp_api.PostDeleteResponse
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostFormat
import uniffi.wp_api.PostGuidWithEditContext
import uniffi.wp_api.PostStatus
import uniffi.wp_api.PostTitleWithEditContext
import uniffi.wp_api.PostUpdateParams
import uniffi.wp_api.PostsRequestCreateResponse
import uniffi.wp_api.PostsRequestDeleteResponse
import uniffi.wp_api.PostsRequestExecutor
import uniffi.wp_api.PostsRequestFilterRetrieveWithEditContextResponse
import uniffi.wp_api.PostsRequestTrashResponse
import uniffi.wp_api.PostsRequestUpdateResponse
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.SparseAnyPostFieldWithEditContext
import uniffi.wp_api.SparseAnyPostWithEditContext
import uniffi.wp_api.SparsePostExcerpt
import uniffi.wp_api.UniffiWpApiClient
import uniffi.wp_api.WpDateString
import uniffi.wp_api.WpErrorCode
import uniffi.wp_api.WpNetworkHeaderMap
import java.util.Date

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Suppress("LargeClass")
class PostRsApiRestClientTest {
    @Mock private lateinit var dispatcher: Dispatcher
    @Mock private lateinit var appLogWrapper: AppLogWrapper
    @Mock private lateinit var wpApiClientProvider: WpApiClientProvider
    @Mock private lateinit var wpApiClient: WpApiClient
    @Mock private lateinit var additionalFieldsBuilder: WpAdditionalFieldsBuilder
    private lateinit var taxonomyRsApiRestClient: TaxonomyRsApiRestClient

    private lateinit var restClient: PostRsApiRestClient

    private val site = SiteModel().apply {
        id = 11
        siteId = 99L
        url = "https://self-hosted.example"
        setDefaultCommentStatus("open")
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        taxonomyRsApiRestClient = mock {
            onBlocking { resolveTagIds(any(), any()) } doReturn emptyList()
        }
        val testScope = CoroutineScope(UnconfinedTestDispatcher(TestCoroutineScheduler()))
        whenever(wpApiClientProvider.getWpApiClient(site)).thenReturn(wpApiClient)
        restClient = PostRsApiRestClient(
            scope = testScope,
            dispatcher = dispatcher,
            appLogWrapper = appLogWrapper,
            wpApiClientProvider = wpApiClientProvider,
            taxonomyRsApiRestClient = taxonomyRsApiRestClient,
            additionalFieldsBuilder = additionalFieldsBuilder,
            postMapper = RsPostMapper(),
        )
    }

    @Test
    fun `a local draft is created and the model picks up the new remote id`() = runTest {
        val executor = executor()
        stubRequests(executor)

        restClient.pushPost(localDraft(), site, isFirstTimePublish = true, true, null)

        verifyBlocking(executor) { create(eq(PostEndpointType.Posts), any()) }
        val payload = dispatchedPayload<RemotePostPayload>(UploadAction.PUSHED_POST)
        assertThat(payload.error).isNull()
        assertThat(payload.isFirstTimePublish).isTrue()
        assertThat(payload.post.remotePostId).isEqualTo(REMOTE_POST_ID)
        assertThat(payload.post.isLocalDraft).isFalse()
        assertThat(payload.post.isLocallyChanged).isFalse()
    }

    @Test
    fun `an existing post is updated by its remote id`() = runTest {
        val executor = executor()
        stubRequests(executor)

        restClient.pushPost(existingPost(), site, isFirstTimePublish = false, true, null)

        verifyBlocking(executor) { update(eq(PostEndpointType.Posts), eq(REMOTE_POST_ID), any()) }
    }

    @Test
    fun `a page is written to the pages endpoint`() = runTest {
        val executor = executor()
        stubRequests(executor)

        restClient.pushPost(localDraft().apply { setIsPage(true) }, site, false, true, null)

        verifyBlocking(executor) { create(eq(PostEndpointType.Pages), any()) }
    }

    @Test
    fun `creating a post with no categories sends no categories so the site default applies`() = runTest {
        val executor = executor()
        stubRequests(executor)

        restClient.pushPost(localDraft(), site, false, true, null)

        val params = captureCreateParams(executor)
        assertThat(params.categories).isEmpty()
        // Nothing to clear on a brand new post, so no empty arrays are forced into the body either.
        assertThat(params.additionalFields).isNull()
    }

    /**
     * The regression guard for the gap wordpress-rs leaves: an empty `categories`/`tags` list is
     * dropped from the request body, and wp/v2 reads an absent field as "leave unchanged", so an
     * edit that cleared every term would silently keep the old ones. XML-RPC sends an explicit
     * empty array instead, and `additionalFields` is how that is expressed here.
     */
    @Test
    fun `updating a post that cleared its terms sends explicit empty arrays`() = runTest {
        val executor = executor()
        stubRequests(executor)

        restClient.pushPost(existingPost(), site, false, true, null)

        verify(additionalFieldsBuilder).emptyArrays(listOf("categories", "tags"))
    }

    @Test
    fun `updating a post that cleared only its tags leaves its categories alone`() = runTest {
        val executor = executor()
        stubRequests(executor)

        restClient.pushPost(existingPost().apply { setCategoryIdList(listOf(5L)) }, site, false, true, null)

        verify(additionalFieldsBuilder).emptyArrays(listOf("tags"))
    }

    @Test
    fun `updating a post that still has terms sends them in the typed fields`() = runTest {
        val executor = executor()
        stubRequests(executor)
        whenever(taxonomyRsApiRestClient.resolveTagIds(site, listOf("alpha"))).thenReturn(listOf(77L))
        val post = existingPost().apply {
            setCategoryIdList(listOf(5L))
            setTagNameList(listOf("alpha"))
        }

        restClient.pushPost(post, site, false, true, null)

        val params = captureUpdateParams(executor)
        assertThat(params.categories).containsExactly(5L)
        assertThat(params.tags).containsExactly(77L)
        verify(additionalFieldsBuilder, never()).emptyArrays(any())
    }

    @Test
    fun `a page is written without taxonomies`() = runTest {
        val executor = executor()
        stubRequests(executor)
        val page = existingPost().apply {
            setIsPage(true)
            setCategoryIdList(listOf(5L))
            setTagNameList(listOf("alpha"))
            setParentId(4L)
        }

        restClient.pushPost(page, site, false, true, null)

        val params = captureUpdateParams(executor)
        assertThat(params.categories).isEmpty()
        assertThat(params.tags).isEmpty()
        assertThat(params.parent).isEqualTo(4L)
        verify(additionalFieldsBuilder, never()).emptyArrays(any())
        verifyBlocking(taxonomyRsApiRestClient, never()) { resolveTagIds(any(), any()) }
    }

    @Test
    fun `a remote copy modified after the local one is reported as an old revision`() = runTest {
        val executor = executor(remoteModifiedGmt = Date(MODIFIED_MILLIS + 60_000L))
        stubRequests(executor)
        val post = existingPost().apply { setLastModified(DateTimeUtils.iso8601UTCFromDate(Date(MODIFIED_MILLIS))) }

        restClient.pushPost(post, site, false, shouldSkipConflictResolutionCheck = false, null)

        val payload = dispatchedPayload<RemotePostPayload>(UploadAction.PUSHED_POST)
        assertThat(payload.error?.type).isEqualTo(PostErrorType.OLD_REVISION)
        verifyBlocking(executor, never()) { update(any(), any(), any()) }
    }

    @Test
    fun `an unchanged remote copy is written over`() = runTest {
        val executor = executor(remoteModifiedGmt = Date(MODIFIED_MILLIS))
        stubRequests(executor)
        val post = existingPost().apply { setLastModified(DateTimeUtils.iso8601UTCFromDate(Date(MODIFIED_MILLIS))) }

        restClient.pushPost(post, site, false, shouldSkipConflictResolutionCheck = false, null)

        verifyBlocking(executor) { update(any(), any(), any()) }
        assertThat(dispatchedPayload<RemotePostPayload>(UploadAction.PUSHED_POST).error).isNull()
    }

    @Test
    fun `skipping the conflict check costs no extra request`() = runTest {
        val executor = executor()
        stubRequests(executor)

        restClient.pushPost(existingPost(), site, false, shouldSkipConflictResolutionCheck = true, null)

        verifyBlocking(executor, never()) { filterRetrieveWithEditContext(any(), any(), any(), any()) }
    }

    @Test
    fun `restoring a post dispatches the restored action`() = runTest {
        stubRequests(executor())

        restClient.restorePost(existingPost(), site)

        assertThat(dispatchedPayload<RemotePostPayload>(PostAction.RESTORED_POST).error).isNull()
    }

    @Test
    fun `trashing a live post uses the trash route and returns the trashed copy`() = runTest {
        val executor = executor()
        stubRequests(executor)

        restClient.deletePost(existingPost(), site, PostDeleteActionType.TRASH)

        verifyBlocking(executor) { trash(PostEndpointType.Posts, REMOTE_POST_ID) }
        val payload = dispatchedPayload<DeletedPostPayload>(PostAction.DELETED_POST)
        assertThat(payload.error).isNull()
        assertThat(payload.deletedPostResponse?.id).isEqualTo(LOCAL_POST_ID)
    }

    @Test
    fun `deleting an already trashed post uses the delete route and stores nothing`() = runTest {
        val executor = executor()
        stubRequests(executor)

        restClient.deletePost(existingPost(), site, PostDeleteActionType.DELETE)

        verifyBlocking(executor) { delete(PostEndpointType.Posts, REMOTE_POST_ID) }
        val payload = dispatchedPayload<DeletedPostPayload>(PostAction.DELETED_POST)
        assertThat(payload.error).isNull()
        assertThat(payload.deletedPostResponse).isNull()
    }

    @Test
    fun `fetchPostStatus asks only for the status field and reports its slug`() = runTest {
        val executor = executor(remoteStatus = PostStatus.Pending)
        stubRequests(executor)

        restClient.fetchPostStatus(existingPost(), site)

        verifyBlocking(executor) {
            filterRetrieveWithEditContext(
                eq(PostEndpointType.Posts),
                eq(REMOTE_POST_ID),
                any(),
                eq(listOf(SparseAnyPostFieldWithEditContext.STATUS))
            )
        }
        val payload = dispatchedPayload<FetchPostStatusResponsePayload>(PostAction.FETCHED_POST_STATUS)
        assertThat(payload.remotePostStatus).isEqualTo("pending")
    }

    @Test
    fun `a status response without a status is an invalid response`() = runTest {
        stubRequests(executor(remoteStatus = null))

        restClient.fetchPostStatus(existingPost(), site)

        val payload = dispatchedPayload<FetchPostStatusResponsePayload>(PostAction.FETCHED_POST_STATUS)
        assertThat(payload.error?.type).isEqualTo(PostErrorType.INVALID_RESPONSE)
    }

    @Test
    fun `a fetched post keeps the local row it belongs to`() = runTest {
        stubRequests(executor())

        restClient.fetchPost(existingPost(), site)

        val payload = dispatchedPayload<RemotePostPayload>(PostAction.FETCHED_POST)
        assertThat(payload.post.id).isEqualTo(LOCAL_POST_ID)
        assertThat(payload.post.remotePostId).isEqualTo(REMOTE_POST_ID)
    }

    @Test
    fun `a rejected credential reports an unauthorized error`() = runTest {
        stubFailure(
            WpRequestResult.InvalidHttpStatusCode<Any>(
                statusCode = 401u,
                requestUrl = "",
                requestMethod = RequestMethod.POST
            )
        )

        restClient.pushPost(localDraft(), site, false, true, null)

        assertThat(dispatchedPayload<RemotePostPayload>(UploadAction.PUSHED_POST).error?.type)
            .isEqualTo(PostErrorType.UNAUTHORIZED)
    }

    @Test
    fun `a post the site no longer has reports an unknown post`() = runTest {
        stubFailure(
            WpRequestResult.WpError<Any>(
                errorCode = WpErrorCode.PostInvalidId(),
                errorMessage = "Invalid post ID.",
                statusCode = 404u,
                response = "",
                requestUrl = "",
                requestMethod = RequestMethod.POST
            )
        )

        restClient.pushPost(existingPost(), site, false, true, null)

        assertThat(dispatchedPayload<RemotePostPayload>(UploadAction.PUSHED_POST).error?.type)
            .isEqualTo(PostErrorType.UNKNOWN_POST)
    }

    @Test
    fun `a server failure reports a generic error`() = runTest {
        stubFailure(
            WpRequestResult.UnknownError<Any>(
                statusCode = 500u,
                response = "Internal Server Error",
                requestUrl = "",
                requestMethod = RequestMethod.POST
            )
        )

        restClient.pushPost(localDraft(), site, false, true, null)

        assertThat(dispatchedPayload<RemotePostPayload>(UploadAction.PUSHED_POST).error?.type)
            .isEqualTo(PostErrorType.GENERIC_ERROR)
    }

    private fun localDraft() = PostModel().apply {
        setId(LOCAL_POST_ID)
        setLocalSiteId(site.id)
        setIsLocalDraft(true)
        setStatus("draft")
        setTitle("A draft")
        setContent("Body")
    }

    private fun existingPost() = PostModel().apply {
        setId(LOCAL_POST_ID)
        setLocalSiteId(site.id)
        setRemotePostId(REMOTE_POST_ID)
        setIsLocalDraft(false)
        setStatus("publish")
        setTitle("A post")
        setContent("Body")
    }

    /**
     * Stubs a `PostsRequestExecutor` that answers every write with [serverPost], so the client's
     * request lambdas can be run for real and the params it builds observed.
     */
    private fun executor(
        remoteModifiedGmt: Date = Date(MODIFIED_MILLIS),
        remoteStatus: PostStatus? = PostStatus.Publish
    ): PostsRequestExecutor {
        val headers = mock<WpNetworkHeaderMap>()
        @Suppress("DoNotMockDataClass") // 29 nullable fields; only two are read back here.
        val sparse = mock<SparseAnyPostWithEditContext> {
            on { modifiedGmt } doReturn remoteModifiedGmt
            on { status } doReturn remoteStatus
        }
        return mock {
            onBlocking { create(any(), any()) } doReturn PostsRequestCreateResponse(serverPost(), headers)
            onBlocking { update(any(), any(), any()) } doReturn PostsRequestUpdateResponse(serverPost(), headers)
            onBlocking { trash(any(), any()) } doReturn PostsRequestTrashResponse(serverPost(), headers)
            onBlocking { delete(any(), any()) } doReturn
                    PostsRequestDeleteResponse(PostDeleteResponse(true, serverPost()), headers)
            onBlocking { retrieveWithEditContext(any(), any(), any()) } doReturn
                    uniffi.wp_api.PostsRequestRetrieveWithEditContextResponse(serverPost(), headers)
            onBlocking { filterRetrieveWithEditContext(any(), any(), any(), any()) } doReturn
                    PostsRequestFilterRetrieveWithEditContextResponse(sparse, headers)
        }
    }

    private suspend fun stubRequests(executor: PostsRequestExecutor) {
        val uniffiClient = mock<UniffiWpApiClient> { on { posts() } doReturn executor }
        whenever(wpApiClient.request<Any>(any())).doSuspendableAnswer { invocation ->
            val executeRequest = invocation.getArgument<suspend (UniffiWpApiClient) -> Any>(0)
            WpRequestResult.Success(response = executeRequest(uniffiClient))
        }
    }

    private suspend fun stubFailure(result: WpRequestResult<Any>) {
        whenever(wpApiClient.request<Any>(any())).thenReturn(result)
    }

    private fun captureCreateParams(executor: PostsRequestExecutor): PostCreateParams {
        val captor = argumentCaptor<PostCreateParams>()
        verifyBlocking(executor) { create(any(), captor.capture()) }
        return captor.firstValue
    }

    private fun captureUpdateParams(executor: PostsRequestExecutor): PostUpdateParams {
        val captor = argumentCaptor<PostUpdateParams>()
        verifyBlocking(executor) { update(any(), any(), captor.capture()) }
        return captor.firstValue
    }

    private inline fun <reified T> dispatchedPayload(expectedType: Any): T {
        val captor = argumentCaptor<Action<*>>()
        verify(dispatcher).dispatch(captor.capture())
        assertThat(captor.firstValue.type).isEqualTo(expectedType)
        return captor.firstValue.payload as T
    }

    private fun serverPost() = AnyPostWithEditContext(
        id = REMOTE_POST_ID,
        date = WpDateString("2026-10-01T10:00:00"),
        dateGmt = Date(MODIFIED_MILLIS),
        guid = PostGuidWithEditContext(null, "https://self-hosted.example/?p=$REMOTE_POST_ID"),
        link = "https://self-hosted.example/a-post",
        modified = WpDateString("2026-10-01T10:00:00"),
        modifiedGmt = Date(MODIFIED_MILLIS),
        slug = "a-post",
        status = PostStatus.Publish,
        postType = "post",
        password = null,
        permalinkTemplate = null,
        generatedSlug = null,
        title = PostTitleWithEditContext("A post", "A post"),
        content = PostContentWithEditContext("Body", "<p>Body</p>", false, null),
        author = 1L,
        excerpt = SparsePostExcerpt("", "", false),
        featuredMedia = 0L,
        commentStatus = null,
        pingStatus = null,
        format = PostFormat.Standard,
        meta = null,
        sticky = false,
        template = "",
        categories = emptyList(),
        tags = emptyList(),
        parent = null,
        menuOrder = null,
        additionalFields = null,
    )

    companion object {
        private const val LOCAL_POST_ID = 3
        private const val REMOTE_POST_ID = 1234L
        private const val MODIFIED_MILLIS = 1_760_000_000_000L
    }
}
