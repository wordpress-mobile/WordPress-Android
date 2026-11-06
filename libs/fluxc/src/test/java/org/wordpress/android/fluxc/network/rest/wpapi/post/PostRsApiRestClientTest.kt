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
import org.wordpress.android.fluxc.network.rest.wpapi.taxonomy.TaxonomyRsApiRestClient.TagResolution
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
            onBlocking { resolveTagIds(any(), any()) } doReturn TagResolution(emptyList(), complete = true)
            onBlocking { resolveTagNames(any(), any()) } doReturn emptyList()
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
        whenever(taxonomyRsApiRestClient.resolveTagIds(site, listOf("alpha")))
            .thenReturn(TagResolution(listOf(77L), complete = true))
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

    /**
     * The guard for the other half of that gap: a tag the site would not resolve also leaves
     * `tagIds` empty, and treating that as "the author cleared every tag" would send the explicit
     * empty array and delete tags from the post that the author never touched (CMM-2502).
     */
    @Test
    fun `a post whose tags could not be resolved leaves the ones on the site alone`() = runTest {
        val executor = executor()
        stubRequests(executor)
        whenever(taxonomyRsApiRestClient.resolveTagIds(site, listOf("alpha")))
            .thenReturn(TagResolution(emptyList(), complete = false))
        val post = existingPost().apply {
            setCategoryIdList(listOf(5L))
            setTagNameList(listOf("alpha"))
        }

        restClient.pushPost(post, site, false, true, null)

        // Nothing resolved, so the typed field is empty and wordpress-rs drops it from the body —
        // which wp/v2 reads as "leave the tags as they are".
        assertThat(captureUpdateParams(executor).tags).isEmpty()
        verify(additionalFieldsBuilder, never()).emptyArrays(any())
    }

    /**
     * The harder half of the same trap: some names resolved, so the ids look usable, but wp/v2
     * replaces the whole tag set and would delete the tags behind the names that did not.
     */
    @Test
    fun `a post whose tags resolved only partly leaves the ones on the site alone`() = runTest {
        val executor = executor()
        stubRequests(executor)
        whenever(taxonomyRsApiRestClient.resolveTagIds(site, listOf("alpha", "beta")))
            .thenReturn(TagResolution(listOf(77L), complete = false))
        val post = existingPost().apply {
            setCategoryIdList(listOf(5L))
            setTagNameList(listOf("alpha", "beta"))
        }

        restClient.pushPost(post, site, false, true, null)

        assertThat(captureUpdateParams(executor).tags).isEmpty()
        verify(additionalFieldsBuilder, never()).emptyArrays(any())
    }

    /**
     * A post being created has no tags to lose, so a partial resolution is still worth sending:
     * dropping them would publish the post with no tags at all.
     */
    @Test
    fun `creating a post sends the tags that resolved even when some did not`() = runTest {
        val executor = executor()
        stubRequests(executor)
        whenever(taxonomyRsApiRestClient.resolveTagIds(site, listOf("alpha", "beta")))
            .thenReturn(TagResolution(listOf(77L), complete = false))
        val post = existingPost().apply {
            setIsLocalDraft(true)
            setTagNameList(listOf("alpha", "beta"))
        }

        restClient.pushPost(post, site, false, true, null)

        assertThat(captureCreateParams(executor).tags).containsExactly(77L)
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

    /**
     * A site that turns the trash off (`EMPTY_TRASH_DAYS = 0`) refuses the non-forced delete, and
     * Trash is the only action the UI offers for a live post — so without the retry the post could
     * not be removed at all, where `wp.deletePost` used to fall through to a permanent delete.
     */
    @Test
    fun `a site that refuses to trash deletes the post permanently instead`() = runTest {
        val executor = executor()
        stubTrashRefusedThenSucceeds(executor)

        restClient.deletePost(existingPost(), site, PostDeleteActionType.TRASH)

        verifyBlocking(executor) { trash(PostEndpointType.Posts, REMOTE_POST_ID) }
        verifyBlocking(executor) { delete(PostEndpointType.Posts, REMOTE_POST_ID) }
        val payload = dispatchedPayload<DeletedPostPayload>(PostAction.DELETED_POST)
        assertThat(payload.error).isNull()
        // Reported as the permanent delete it turned out to be, so PostStore removes the local row
        // instead of keeping a trashed one for a post that no longer exists.
        assertThat(payload.postDeleteActionType).isEqualTo(PostDeleteActionType.DELETE)
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

    /**
     * A fetch has no author-supplied tag names to reuse, and PostFetcher fetches with a bare
     * PostModel, so taking the names from the local copy would cache the post with no tags at all —
     * and the next save would then clear them on the site.
     */
    @Test
    fun `a fetched post takes its tag names from the response ids`() = runTest {
        stubRequests(executor(remoteTags = listOf(7L, 3L)))
        whenever(taxonomyRsApiRestClient.resolveTagNames(site, listOf(7L, 3L)))
            .thenReturn(listOf("alpha", "beta"))

        restClient.fetchPost(existingPost(), site)

        val payload = dispatchedPayload<RemotePostPayload>(PostAction.FETCHED_POST)
        assertThat(payload.post.tagNameList).containsExactly("alpha", "beta")
    }

    /**
     * A successful payload carrying an emptied list would be stored as the truth and read as "the
     * author removed every tag" by the next save, deleting them all on the site (CMM-2502).
     * Reporting success while keeping the names already held is no safer: everything else in the
     * row would be refreshed around them, including the modified dates the push conflict
     * pre-check compares, so the stale tags would go out on the next save with nothing left to
     * flag them. The fetch fails instead, and the stored row is left exactly as it was.
     */
    @Test
    fun `a fetch whose tag lookup fails leaves the row alone rather than refreshing it`() = runTest {
        stubRequests(executor(remoteTags = listOf(7L, 3L)))
        whenever(taxonomyRsApiRestClient.resolveTagNames(site, listOf(7L, 3L))).thenReturn(null)
        val local = existingPost().apply { setTagNameList(listOf("alpha", "beta")) }

        restClient.fetchPost(local, site)

        val payload = dispatchedPayload<RemotePostPayload>(PostAction.FETCHED_POST)
        assertThat(payload.error?.type).isEqualTo(PostErrorType.GENERIC_ERROR)
        assertThat(payload.post).isSameAs(local)
        assertThat(payload.post.tagNameList).containsExactly("alpha", "beta")
    }

    /**
     * PostFetcher fetches with a bare PostModel, so the error branch has to hand that model back
     * untouched rather than anything mapped from the response - which is what stops PostStore
     * storing a row whose tags were never read.
     */
    @Test
    fun `a fetch whose tag lookup fails returns the caller's model, not the mapped response`() = runTest {
        stubRequests(executor(remoteTags = listOf(7L, 3L)))
        whenever(taxonomyRsApiRestClient.resolveTagNames(site, listOf(7L, 3L))).thenReturn(null)
        val bare = PostModel().apply { setRemotePostId(REMOTE_POST_ID) }

        restClient.fetchPost(bare, site)

        val payload = dispatchedPayload<RemotePostPayload>(PostAction.FETCHED_POST)
        assertThat(payload.error?.type).isEqualTo(PostErrorType.GENERIC_ERROR)
        // `serverPost` carries a title, so an empty one proves nothing was mapped in.
        assertThat(payload.post).isSameAs(bare)
        assertThat(payload.post.title).isEmpty()
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

    /**
     * A 404 on its own cannot tell a deleted post from a route the host blocks, and only the error
     * code is conclusive. Claiming the post is gone when it is fine also suppresses the retry
     * wording a generic failure gets.
     */
    @Test
    fun `a bare 404 reports a generic error rather than a missing post`() = runTest {
        stubFailure(
            WpRequestResult.InvalidHttpStatusCode<Any>(
                statusCode = 404u,
                requestUrl = "",
                requestMethod = RequestMethod.POST
            )
        )

        restClient.pushPost(existingPost(), site, false, true, null)

        assertThat(dispatchedPayload<RemotePostPayload>(UploadAction.PUSHED_POST).error?.type)
            .isEqualTo(PostErrorType.GENERIC_ERROR)
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
        remoteStatus: PostStatus? = PostStatus.Publish,
        remoteTags: List<Long> = emptyList()
    ): PostsRequestExecutor {
        val headers = mock<WpNetworkHeaderMap>()
        @Suppress("DoNotMockDataClass") // 29 nullable fields; only two are read back here.
        val sparse = mock<SparseAnyPostWithEditContext> {
            on { modifiedGmt } doReturn remoteModifiedGmt
            on { status } doReturn remoteStatus
        }
        val post = serverPost(tags = remoteTags)
        return mock {
            onBlocking { create(any(), any()) } doReturn PostsRequestCreateResponse(post, headers)
            onBlocking { update(any(), any(), any()) } doReturn PostsRequestUpdateResponse(post, headers)
            onBlocking { trash(any(), any()) } doReturn PostsRequestTrashResponse(post, headers)
            onBlocking { delete(any(), any()) } doReturn
                    PostsRequestDeleteResponse(PostDeleteResponse(true, post), headers)
            onBlocking { retrieveWithEditContext(any(), any(), any()) } doReturn
                    uniffi.wp_api.PostsRequestRetrieveWithEditContextResponse(post, headers)
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

    /**
     * Answers the first request with wp/v2's refusal to trash and every later one normally, so the
     * client's fallback to a forced delete can be observed. The request lambda runs either way, so
     * the route it picked is still verifiable on the executor.
     */
    private suspend fun stubTrashRefusedThenSucceeds(executor: PostsRequestExecutor) {
        val uniffiClient = mock<UniffiWpApiClient> { on { posts() } doReturn executor }
        var refuseNext = true
        whenever(wpApiClient.request<Any>(any())).doSuspendableAnswer { invocation ->
            val executeRequest = invocation.getArgument<suspend (UniffiWpApiClient) -> Any>(0)
            val response = executeRequest(uniffiClient)
            if (refuseNext) {
                refuseNext = false
                WpRequestResult.WpError(
                    errorCode = WpErrorCode.TrashNotSupported(),
                    errorMessage = "The post does not support trashing.",
                    statusCode = 501u,
                    response = "",
                    requestUrl = "",
                    requestMethod = RequestMethod.DELETE
                )
            } else {
                WpRequestResult.Success(response = response)
            }
        }
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

    private fun serverPost(tags: List<Long> = emptyList()) = AnyPostWithEditContext(
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
        tags = tags,
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
