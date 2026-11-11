package org.wordpress.android.fluxc.network.rest.wpapi.taxonomy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.TaxonomyAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.model.TermModel
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsResponsePayload
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermPayload
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyErrorType
import org.wordpress.android.fluxc.utils.AppLogWrapper
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AnyTermWithEditContext
import uniffi.wp_api.AnyTermWithViewContext
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.TermDeleteResponse
import uniffi.wp_api.TermEndpointType
import uniffi.wp_api.TermsRequestCreateResponse
import uniffi.wp_api.TermsRequestDeleteResponse
import uniffi.wp_api.TermsRequestExecutor
import uniffi.wp_api.TermsRequestListWithViewContextResponse
import uniffi.wp_api.UniffiWpApiClient
import uniffi.wp_api.TermsRequestUpdateResponse
import uniffi.wp_api.TaxonomyType
import uniffi.wp_api.TermCreateParams
import uniffi.wp_api.TermListParams
import uniffi.wp_api.WpErrorCode
import uniffi.wp_api.WpNetworkHeaderMap

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
@Suppress("LargeClass")
class TaxonomyRsApiRestClientTest {
    @Mock
    private lateinit var dispatcher: Dispatcher
    @Mock
    private lateinit var appLogWrapper: AppLogWrapper
    @Mock
    private lateinit var wpApiClientProvider: WpApiClientProvider
    @Mock
    private lateinit var wpApiClient: WpApiClient

    private lateinit var testScope: CoroutineScope
    private lateinit var taxonomyClient: TaxonomyRsApiRestClient

    private val testSite = SiteModel().apply {
        id = 123
        url = "https://test.wordpress.com"
    }

    private val testCategoryTermModel = TermModel(
        1, // id
        123, // localSiteId
        2L, // remoteTermId
        "category", // taxonomy
        "Test Category", // name
        "test-category", // slug
        "Test category description", // description
        0L, // parentRemoteId
        true, // isHierarchical
        0 // postCount
    )

    private val testTagTermModel = TermModel(
        2, // id
        123, // localSiteId
        3L, // remoteTermId
        "post_tag", // taxonomy
        "Test Tag", // name
        "test-tag", // slug
        "Test tag description", // description
        0L, // parentRemoteId
        false, // isHierarchical
        0 // postCount
    )

    private val testTagTaxonomyName = "post_tag"
    private val testCategoryTaxonomyName = "category"

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        val testScheduler = TestCoroutineScheduler()
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        testScope = CoroutineScope(testDispatcher)

        whenever(wpApiClientProvider.getWpApiClient(testSite)).thenReturn(wpApiClient)

        taxonomyClient = TaxonomyRsApiRestClient(
            testScope,
            dispatcher,
            appLogWrapper,
            wpApiClientProvider
        )
    }

    @Test
    fun `fetchTerms tags with error response dispatches error action`() = runTest {
        // Use a concrete error type that we can create - UnknownError requires statusCode and response
        val errorResponse = WpRequestResult.UnknownError<Any>(
            statusCode = 500u,
            response = "Internal Server Error",
            requestUrl = "",
            requestMethod = RequestMethod.GET
        )

        whenever(wpApiClient.request<Any>(any())).thenReturn(errorResponse)

        taxonomyClient.fetchTerms(testSite, testTagTaxonomyName)

        // Verify dispatcher was called with error action
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as FetchTermsResponsePayload
        assertEquals(capturedAction.type, TaxonomyAction.FETCHED_TERMS)
        assertEquals(testTagTaxonomyName, payload.taxonomy)
        assertNotNull(payload.error)
        assertEquals(TaxonomyErrorType.GENERIC_ERROR, payload.error?.type)
    }

    @Test
    fun `fetchTerms tags with success response dispatches success action`() = runTest {
        val anyTermWithViewContext = listOf(
            createTestAnyTermWithViewContext(),
            createTestAnyTermWithViewContext()
        )

        // Create the correct response structure following the MediaRSApiRestClientTest pattern
        val tagResponse = TermsRequestListWithViewContextResponse(
            anyTermWithViewContext,
            mock<WpNetworkHeaderMap>(),
            null,
            null
        )

        val successResponse: WpRequestResult<TermsRequestListWithViewContextResponse> = WpRequestResult.Success(
            response = tagResponse
        )

        whenever(wpApiClient.request<TermsRequestListWithViewContextResponse>(any())).thenReturn(successResponse)

        taxonomyClient.fetchTerms(testSite, testTagTaxonomyName)

        // Verify dispatcher was called with success action
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as FetchTermsResponsePayload
        assertEquals(capturedAction.type, TaxonomyAction.FETCHED_TERMS)
        assertEquals(testTagTaxonomyName, payload.taxonomy)
        assertEquals(testSite, payload.site)
        assertNotNull(payload.terms)
        assertEquals(2, payload.terms.terms.size)
        assertNull(payload.error)
        // The local row id is left for the database to assign; reusing the remote id overwrote unrelated rows
        payload.terms.terms.forEach {
            assertEquals(0, it.id)
            assertEquals(TEST_TERM_ID, it.remoteTermId)
        }
    }

    @Test
    fun `fetchTerms categories with error response dispatches error action`() = runTest {
        // Use a concrete error type that we can create - UnknownError requires statusCode and response
        val errorResponse = WpRequestResult.UnknownError<Any>(
            statusCode = 500u,
            response = "Internal Server Error",
            requestUrl = "",
            requestMethod = RequestMethod.GET
        )

        whenever(wpApiClient.request<Any>(any())).thenReturn(errorResponse)

        taxonomyClient.fetchTerms(testSite, testCategoryTaxonomyName)

        // Verify dispatcher was called with error action
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as FetchTermsResponsePayload
        assertEquals(capturedAction.type, TaxonomyAction.FETCHED_TERMS)
        assertEquals(testCategoryTaxonomyName, payload.taxonomy)
        assertNotNull(payload.error)
        assertEquals(TaxonomyErrorType.GENERIC_ERROR, payload.error?.type)
    }

    @Test
    fun `fetchTerms categories with success response dispatches success action`() = runTest {
        val anyTermWithViewContext = listOf(
            createTestAnyTermWithViewContext(),
            createTestAnyTermWithViewContext()
        )

        // Create the correct response structure following the MediaRSApiRestClientTest pattern
        val categoryResponse = TermsRequestListWithViewContextResponse(
            anyTermWithViewContext,
            mock<WpNetworkHeaderMap>(),
            null,
            null
        )

        val successResponse: WpRequestResult<TermsRequestListWithViewContextResponse> = WpRequestResult.Success(
            response = categoryResponse
        )

        whenever(wpApiClient.request<TermsRequestListWithViewContextResponse>(any())).thenReturn(successResponse)

        taxonomyClient.fetchTerms(testSite, testCategoryTaxonomyName)

        // Verify dispatcher was called with success action
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as FetchTermsResponsePayload
        assertEquals(capturedAction.type, TaxonomyAction.FETCHED_TERMS)
        assertEquals(testCategoryTaxonomyName, payload.taxonomy)
        assertEquals(testSite, payload.site)
        assertNotNull(payload.terms)
        assertEquals(2, payload.terms.terms.size)
        assertNull(payload.error)
    }

    @Test
    fun `fetchTerms categories follows pagination and combines all pages`() = runTest {
        val firstPage = createListResponse(
            terms = List(2) { createTestAnyTermWithViewContext() },
            nextPageParams = TermListParams(perPage = 100u)
        )
        val secondPage = createListResponse(
            terms = List(3) { createTestAnyTermWithViewContext() },
            nextPageParams = null
        )

        whenever(wpApiClient.request<TermsRequestListWithViewContextResponse>(any()))
            .thenReturn(firstPage, secondPage)

        taxonomyClient.fetchTerms(testSite, testCategoryTaxonomyName)

        // Two pages should be requested before the terminating null nextPageParams
        verify(wpApiClient, times(2)).request<TermsRequestListWithViewContextResponse>(any())

        // A single success action should be dispatched with the combined results
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as FetchTermsResponsePayload
        assertEquals(capturedAction.type, TaxonomyAction.FETCHED_TERMS)
        assertEquals(testCategoryTaxonomyName, payload.taxonomy)
        assertEquals(testSite, payload.site)
        assertEquals(5, payload.terms.terms.size)
        assertNull(payload.error)
        // All pages retrieved, so the store is free to replace the cached list.
        assertTrue(payload.complete)
    }

    @Test
    fun `fetchTerms categories with error on a later page keeps already-fetched terms`() = runTest {
        val firstPage = createListResponse(
            terms = List(2) { createTestAnyTermWithViewContext() },
            nextPageParams = TermListParams(perPage = 100u)
        )
        val errorResponse = WpRequestResult.UnknownError<TermsRequestListWithViewContextResponse>(
            statusCode = 500u,
            response = "Internal Server Error",
            requestUrl = "",
            requestMethod = RequestMethod.GET
        )

        whenever(wpApiClient.request<TermsRequestListWithViewContextResponse>(any()))
            .thenReturn(firstPage, errorResponse)

        taxonomyClient.fetchTerms(testSite, testCategoryTaxonomyName)

        // A transient failure mid-pagination degrades gracefully: the terms already fetched from
        // the first page are dispatched as a success instead of the whole list being dropped.
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as FetchTermsResponsePayload
        assertEquals(capturedAction.type, TaxonomyAction.FETCHED_TERMS)
        assertEquals(testCategoryTaxonomyName, payload.taxonomy)
        assertEquals(testSite, payload.site)
        assertEquals(2, payload.terms.terms.size)
        assertNull(payload.error)
        // Marked incomplete so the store keeps existing cached terms instead of clearing them.
        assertFalse(payload.complete)
    }

    @Test
    fun `fetchTerms categories with error on the first page dispatches error action`() = runTest {
        val errorResponse = WpRequestResult.UnknownError<TermsRequestListWithViewContextResponse>(
            statusCode = 500u,
            response = "Internal Server Error",
            requestUrl = "",
            requestMethod = RequestMethod.GET
        )

        whenever(wpApiClient.request<TermsRequestListWithViewContextResponse>(any()))
            .thenReturn(errorResponse)

        taxonomyClient.fetchTerms(testSite, testCategoryTaxonomyName)

        // Nothing could be fetched, so the error is surfaced and the cached list is left untouched.
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as FetchTermsResponsePayload
        assertEquals(capturedAction.type, TaxonomyAction.FETCHED_TERMS)
        assertEquals(testCategoryTaxonomyName, payload.taxonomy)
        assertNotNull(payload.error)
        assertEquals(TaxonomyErrorType.GENERIC_ERROR, payload.error?.type)
    }

    @Test
    fun `fetchTerms categories with a forbidden response dispatches an unauthorized error`() = runTest {
        // An Author or Contributor lacks the taxonomy's edit_terms capability, so the REST API
        // answers 403 rest_forbidden_context. That is a permission limit rather than a failed
        // request, and the distinction is what lets the UI say so.
        val errorResponse = WpRequestResult.WpError<TermsRequestListWithViewContextResponse>(
            errorCode = WpErrorCode.ForbiddenContext(),
            errorMessage = "Sorry, you are not allowed to edit terms in this taxonomy.",
            statusCode = 403u,
            response = "",
            requestUrl = "",
            requestMethod = RequestMethod.GET
        )

        whenever(wpApiClient.request<TermsRequestListWithViewContextResponse>(any()))
            .thenReturn(errorResponse)

        taxonomyClient.fetchTerms(testSite, testCategoryTaxonomyName)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val payload = actionCaptor.value.payload as FetchTermsResponsePayload
        assertEquals(TaxonomyErrorType.UNAUTHORIZED, payload.error?.type)
    }

    @Test
    fun `fetchTerms categories with an unauthenticated response dispatches an unauthorized error`() = runTest {
        val errorResponse = WpRequestResult.UnknownError<TermsRequestListWithViewContextResponse>(
            statusCode = 401u,
            response = "",
            requestUrl = "",
            requestMethod = RequestMethod.GET
        )

        whenever(wpApiClient.request<TermsRequestListWithViewContextResponse>(any()))
            .thenReturn(errorResponse)

        taxonomyClient.fetchTerms(testSite, testCategoryTaxonomyName)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val payload = actionCaptor.value.payload as FetchTermsResponsePayload
        assertEquals(TaxonomyErrorType.UNAUTHORIZED, payload.error?.type)
    }

    @Test
    fun `createTerm category with a forbidden response dispatches an unauthorized error`() = runTest {
        val errorResponse = WpRequestResult.WpError<Any>(
            errorCode = WpErrorCode.CannotCreate(),
            errorMessage = "Sorry, you are not allowed to create terms in this taxonomy.",
            statusCode = 403u,
            response = "",
            requestUrl = "",
            requestMethod = RequestMethod.POST
        )

        whenever(wpApiClient.request<Any>(any())).thenReturn(errorResponse)

        taxonomyClient.createTerm(testSite, testCategoryTermModel)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val payload = actionCaptor.value.payload as RemoteTermPayload
        assertEquals(TaxonomyErrorType.UNAUTHORIZED, payload.error?.type)
    }

    @Test
    fun `createTerm category with error response dispatches error action`() = runTest {
        // Use a concrete error type that we can create - UnknownError requires statusCode and response
        val errorResponse = WpRequestResult.UnknownError<Any>(
            statusCode = 500u,
            response = "Internal Server Error",
            requestUrl = "",
            requestMethod = RequestMethod.GET
        )

        whenever(wpApiClient.request<Any>(any())).thenReturn(errorResponse)

        taxonomyClient.createTerm(testSite, testCategoryTermModel)

        // Verify dispatcher was called with error action
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as RemoteTermPayload
        assertEquals(capturedAction.type, TaxonomyAction.PUSHED_TERM)
        assertEquals(testSite, payload.site)
        assertEquals(testCategoryTermModel, payload.term)
        assertNotNull(payload.error)
        assertEquals(TaxonomyErrorType.GENERIC_ERROR, payload.error?.type)
    }

    @Test
    fun `createTerm category with success response dispatches success action`() = runTest {
        val anyTermWithEditContext = createTestAnyTermWithEditContext()

        // Create the correct response structure following the MediaRSApiRestClientTest pattern
        val categoryResponse = TermsRequestCreateResponse(
            anyTermWithEditContext,
            mock<WpNetworkHeaderMap>()
        )

        val successResponse: WpRequestResult<TermsRequestCreateResponse> = WpRequestResult.Success(
            response = categoryResponse
        )

        whenever(wpApiClient.request<TermsRequestCreateResponse>(any())).thenReturn(successResponse)

        taxonomyClient.createTerm(testSite, testCategoryTermModel)

        // Verify dispatcher was called with success action
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as RemoteTermPayload
        assertEquals(capturedAction.type, TaxonomyAction.PUSHED_TERM)
        assertEquals(testSite, payload.site)
        assertNotNull(payload.term)
        // Verify the created term has the correct properties
        assertEquals(0, payload.term.id)
        assertEquals(testSite.id, payload.term.localSiteId)
        assertEquals(anyTermWithEditContext.id, payload.term.remoteTermId)
        assertEquals(testCategoryTaxonomyName, payload.term.taxonomy)
        assertEquals(anyTermWithEditContext.name, payload.term.name)
        assertEquals(anyTermWithEditContext.slug, payload.term.slug)
        assertEquals(anyTermWithEditContext.description, payload.term.description)
        assertEquals(anyTermWithEditContext.count.toInt(), payload.term.postCount)
        assertNull(payload.error)
    }

    @Test
    fun `createTerm tag with error response dispatches error action`() = runTest {
        // Use a concrete error type that we can create - UnknownError requires statusCode and response
        val errorResponse = WpRequestResult.UnknownError<Any>(
            statusCode = 500u,
            response = "Internal Server Error",
            requestUrl = "",
            requestMethod = RequestMethod.GET
        )

        whenever(wpApiClient.request<Any>(any())).thenReturn(errorResponse)

        taxonomyClient.createTerm(testSite, testTagTermModel)

        // Verify dispatcher was called with error action
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as RemoteTermPayload
        assertEquals(capturedAction.type, TaxonomyAction.PUSHED_TERM)
        assertEquals(testSite, payload.site)
        assertEquals(testTagTermModel, payload.term)
        assertNotNull(payload.error)
        assertEquals(TaxonomyErrorType.GENERIC_ERROR, payload.error?.type)
    }

    @Test
    fun `createTerm tag with success response dispatches success action`() = runTest {
        val anyTermWithEditContext = createTestAnyTermWithEditContext()

        // Create the correct response structure following the MediaRSApiRestClientTest pattern
        val tagResponse = TermsRequestCreateResponse(
            anyTermWithEditContext,
            mock<WpNetworkHeaderMap>()
        )

        val successResponse: WpRequestResult<TermsRequestCreateResponse> = WpRequestResult.Success(
            response = tagResponse
        )

        whenever(wpApiClient.request<TermsRequestCreateResponse>(any())).thenReturn(successResponse)

        taxonomyClient.createTerm(testSite, testTagTermModel)

        // Verify dispatcher was called with success action
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as RemoteTermPayload
        assertEquals(capturedAction.type, TaxonomyAction.PUSHED_TERM)
        assertEquals(testSite, payload.site)
        assertNotNull(payload.term)
        // Verify the created term has the correct properties
        assertEquals(0, payload.term.id)
        assertEquals(testSite.id, payload.term.localSiteId)
        assertEquals(anyTermWithEditContext.id, payload.term.remoteTermId)
        assertEquals(testTagTaxonomyName, payload.term.taxonomy)
        assertEquals(anyTermWithEditContext.name, payload.term.name)
        assertEquals(anyTermWithEditContext.slug, payload.term.slug)
        assertEquals(anyTermWithEditContext.description, payload.term.description)
        assertEquals(anyTermWithEditContext.count.toInt(), payload.term.postCount)
        assertNull(payload.error)
    }

    @Test
    fun `deleteTerm category with error response dispatches error action`() = runTest {
        // Use a concrete error type that we can create - UnknownError requires statusCode and response
        val errorResponse = WpRequestResult.UnknownError<Any>(
            statusCode = 500u,
            response = "Internal Server Error",
            requestUrl = "",
            requestMethod = RequestMethod.GET
        )

        whenever(wpApiClient.request<Any>(any())).thenReturn(errorResponse)

        taxonomyClient.deleteTerm(testSite, testCategoryTermModel)

        // Verify dispatcher was called with error action
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as RemoteTermPayload
        assertEquals(capturedAction.type, TaxonomyAction.DELETED_TERM)
        assertEquals(testSite, payload.site)
        assertEquals(testCategoryTermModel, payload.term)
        assertNotNull(payload.error)
        assertEquals(TaxonomyErrorType.GENERIC_ERROR, payload.error?.type)
    }

    @Test
    fun `deleteTerm category deletes by the remote term id`() = runTest {
        val termsExecutor = mock<TermsRequestExecutor> {
            onBlocking { delete(any(), any()) } doReturn createTestDeleteResponse(deleted = true)
        }
        val uniffiClient = mock<UniffiWpApiClient> {
            on { terms() } doReturn termsExecutor
        }

        // The other delete tests stub the request wholesale, which leaves the id sent to the API
        // unobservable. Running the request against a stubbed executor exposes it: TermModel
        // carries both a local row id and a remote one, and only the remote one identifies the
        // term to the API.
        whenever(wpApiClient.request<TermsRequestDeleteResponse>(any())).doSuspendableAnswer { invocation ->
            val executeRequest =
                invocation.getArgument<suspend (UniffiWpApiClient) -> TermsRequestDeleteResponse>(0)
            WpRequestResult.Success(response = executeRequest(uniffiClient))
        }

        taxonomyClient.deleteTerm(testSite, testCategoryTermModel)

        verifyBlocking(termsExecutor) {
            delete(TermEndpointType.Categories, testCategoryTermModel.remoteTermId)
        }
    }

    @Test
    fun `deleteTerm category with success response dispatches success action`() = runTest {
        val successResponse: WpRequestResult<TermsRequestDeleteResponse> = WpRequestResult.Success(
            response = createTestDeleteResponse(deleted = true)
        )

        whenever(wpApiClient.request<TermsRequestDeleteResponse>(any())).thenReturn(successResponse)

        taxonomyClient.deleteTerm(testSite, testCategoryTermModel)

        // Verify dispatcher was called with success action
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as RemoteTermPayload
        assertEquals(capturedAction.type, TaxonomyAction.DELETED_TERM)
        assertEquals(testSite, payload.site)
        assertNotNull(payload.term)
        // Verify the deleted term has the correct properties
        assertEquals(testCategoryTermModel.id, payload.term.id)
        assertEquals(testSite.id, payload.term.localSiteId)
        assertEquals(testCategoryTermModel.remoteTermId, payload.term.remoteTermId)
        assertEquals(testCategoryTaxonomyName, payload.term.taxonomy)
        assertEquals(testCategoryTermModel.name, payload.term.name)
        assertEquals(testCategoryTermModel.slug, payload.term.slug)
        assertEquals(testCategoryTermModel.description, payload.term.description)
        assertEquals(testCategoryTermModel.postCount, payload.term.postCount)
        assertNull(payload.error)
    }

    @Test
    fun `deleteTerm category with failed deletion response dispatches error action`() = runTest {
        val successResponse: WpRequestResult<TermsRequestDeleteResponse> = WpRequestResult.Success(
            response = createTestDeleteResponse(deleted = false)
        )

        whenever(wpApiClient.request<TermsRequestDeleteResponse>(any())).thenReturn(successResponse)

        taxonomyClient.deleteTerm(testSite, testCategoryTermModel)

        // Verify dispatcher was called with error action
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as RemoteTermPayload
        assertEquals(capturedAction.type, TaxonomyAction.DELETED_TERM)
        assertEquals(testSite, payload.site)
        assertEquals(testCategoryTermModel, payload.term)
        assertNotNull(payload.error)
        assertEquals(TaxonomyErrorType.GENERIC_ERROR, payload.error?.type)
    }

    @Test
    fun `deleteTerm tag with error response dispatches error action`() = runTest {
        // Use a concrete error type that we can create - UnknownError requires statusCode and response
        val errorResponse = WpRequestResult.UnknownError<Any>(
            statusCode = 500u,
            response = "Internal Server Error",
            requestUrl = "",
            requestMethod = RequestMethod.GET
        )

        whenever(wpApiClient.request<Any>(any())).thenReturn(errorResponse)

        taxonomyClient.deleteTerm(testSite, testTagTermModel)

        // Verify dispatcher was called with error action
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as RemoteTermPayload
        assertEquals(capturedAction.type, TaxonomyAction.DELETED_TERM)
        assertEquals(testSite, payload.site)
        assertEquals(testTagTermModel, payload.term)
        assertNotNull(payload.error)
        assertEquals(TaxonomyErrorType.GENERIC_ERROR, payload.error?.type)
    }

    @Test
    fun `deleteTerm tag with success response dispatches success action`() = runTest {
        val successResponse: WpRequestResult<TermsRequestDeleteResponse> = WpRequestResult.Success(
            response = createTestDeleteResponse(deleted = true)
        )

        whenever(wpApiClient.request<TermsRequestDeleteResponse>(any())).thenReturn(successResponse)

        taxonomyClient.deleteTerm(testSite, testTagTermModel)

        // Verify dispatcher was called with success action
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as RemoteTermPayload
        assertEquals(capturedAction.type, TaxonomyAction.DELETED_TERM)
        assertEquals(testSite, payload.site)
        assertNotNull(payload.term)
        // Verify the deleted term has the correct properties
        assertEquals(testTagTermModel.id, payload.term.id)
        assertEquals(testSite.id, payload.term.localSiteId)
        assertEquals(testTagTermModel.remoteTermId, payload.term.remoteTermId)
        assertEquals(testTagTaxonomyName, payload.term.taxonomy)
        assertEquals(testTagTermModel.name, payload.term.name)
        assertEquals(testTagTermModel.slug, payload.term.slug)
        assertEquals(testTagTermModel.description, payload.term.description)
        assertEquals(testTagTermModel.postCount, payload.term.postCount)
        assertNull(payload.error)
    }

    @Test
    fun `deleteTerm tag with failed deletion response dispatches error action`() = runTest {
        val successResponse: WpRequestResult<TermsRequestDeleteResponse> = WpRequestResult.Success(
            response = createTestDeleteResponse(deleted = false)
        )

        whenever(wpApiClient.request<TermsRequestDeleteResponse>(any())).thenReturn(successResponse)

        taxonomyClient.deleteTerm(testSite, testTagTermModel)

        // Verify dispatcher was called with error action
        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as RemoteTermPayload
        assertEquals(capturedAction.type, TaxonomyAction.DELETED_TERM)
        assertEquals(testSite, payload.site)
        assertEquals(testTagTermModel, payload.term)
        assertNotNull(payload.error)
        assertEquals(TaxonomyErrorType.GENERIC_ERROR, payload.error?.type)
    }

    @Test
    fun `updateTerm category with error response dispatches error action`() = runTest {
        val errorResponse = WpRequestResult.UnknownError<Any>(
            statusCode = 500u,
            response = "Internal Server Error",
            requestUrl = "",
            requestMethod = RequestMethod.GET
        )

        whenever(wpApiClient.request<Any>(any())).thenReturn(errorResponse)

        taxonomyClient.updateTerm(testSite, testCategoryTermModel)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as RemoteTermPayload
        assertEquals(capturedAction.type, TaxonomyAction.PUSHED_TERM)
        assertEquals(testSite, payload.site)
        assertEquals(testCategoryTermModel, payload.term)
        assertNotNull(payload.error)
        assertEquals(TaxonomyErrorType.GENERIC_ERROR, payload.error?.type)
    }

    @Test
    fun `updateTerm category with success response dispatches success action`() = runTest {
        val anyTermWithEditContext = createTestAnyTermWithEditContext()

        val categoryResponse = TermsRequestUpdateResponse(
            anyTermWithEditContext,
            mock<WpNetworkHeaderMap>()
        )

        val successResponse: WpRequestResult<TermsRequestUpdateResponse> = WpRequestResult.Success(
            response = categoryResponse
        )

        whenever(wpApiClient.request<TermsRequestUpdateResponse>(any())).thenReturn(successResponse)

        taxonomyClient.updateTerm(testSite, testCategoryTermModel)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as RemoteTermPayload
        assertEquals(capturedAction.type, TaxonomyAction.PUSHED_TERM)
        assertEquals(testSite, payload.site)
        assertNotNull(payload.term)
        assertEquals(0, payload.term.id)
        assertEquals(testSite.id, payload.term.localSiteId)
        assertEquals(anyTermWithEditContext.id, payload.term.remoteTermId)
        assertEquals(testCategoryTaxonomyName, payload.term.taxonomy)
        assertEquals(anyTermWithEditContext.name, payload.term.name)
        assertEquals(anyTermWithEditContext.slug, payload.term.slug)
        assertEquals(anyTermWithEditContext.description, payload.term.description)
        assertEquals(anyTermWithEditContext.count.toInt(), payload.term.postCount)
        assertNull(payload.error)
    }

    @Test
    fun `updateTerm category with invalid id dispatches error action`() = runTest {
        val invalidTermModel = TermModel(
            testCategoryTermModel.id,
            testCategoryTermModel.localSiteId,
            -1L, // invalid remoteTermId
            testCategoryTermModel.taxonomy,
            testCategoryTermModel.name,
            testCategoryTermModel.slug,
            testCategoryTermModel.description,
            testCategoryTermModel.parentRemoteId,
            testCategoryTermModel.isHierarchical,
            testCategoryTermModel.postCount
        )

        taxonomyClient.updateTerm(testSite, invalidTermModel)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as RemoteTermPayload
        assertEquals(capturedAction.type, TaxonomyAction.PUSHED_TERM)
        assertEquals(testSite, payload.site)
        assertEquals(invalidTermModel, payload.term)
        assertNotNull(payload.error)
        assertEquals(TaxonomyErrorType.GENERIC_ERROR, payload.error?.type)
    }

    @Test
    fun `updateTerm tag with error response dispatches error action`() = runTest {
        val errorResponse = WpRequestResult.UnknownError<Any>(
            statusCode = 500u,
            response = "Internal Server Error",
            requestUrl = "",
            requestMethod = RequestMethod.GET
        )

        whenever(wpApiClient.request<Any>(any())).thenReturn(errorResponse)

        taxonomyClient.updateTerm(testSite, testTagTermModel)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as RemoteTermPayload
        assertEquals(capturedAction.type, TaxonomyAction.PUSHED_TERM)
        assertEquals(testSite, payload.site)
        assertEquals(testTagTermModel, payload.term)
        assertNotNull(payload.error)
        assertEquals(TaxonomyErrorType.GENERIC_ERROR, payload.error?.type)
    }

    @Test
    fun `updateTerm tag with success response dispatches success action`() = runTest {
        val anyTermWithEditContext = createTestAnyTermWithEditContext()

        val tagResponse = TermsRequestUpdateResponse(
            anyTermWithEditContext,
            mock<WpNetworkHeaderMap>()
        )

        val successResponse: WpRequestResult<TermsRequestUpdateResponse> = WpRequestResult.Success(
            response = tagResponse
        )

        whenever(wpApiClient.request<TermsRequestUpdateResponse>(any())).thenReturn(successResponse)

        taxonomyClient.updateTerm(testSite, testTagTermModel)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as RemoteTermPayload
        assertEquals(capturedAction.type, TaxonomyAction.PUSHED_TERM)
        assertEquals(testSite, payload.site)
        assertNotNull(payload.term)
        assertEquals(0, payload.term.id)
        assertEquals(testSite.id, payload.term.localSiteId)
        assertEquals(anyTermWithEditContext.id, payload.term.remoteTermId)
        assertEquals(testTagTaxonomyName, payload.term.taxonomy)
        assertEquals(anyTermWithEditContext.name, payload.term.name)
        assertEquals(anyTermWithEditContext.slug, payload.term.slug)
        assertEquals(anyTermWithEditContext.description, payload.term.description)
        assertEquals(anyTermWithEditContext.count.toInt(), payload.term.postCount)
        assertNull(payload.error)
    }

    @Test
    fun `updateTerm tag with invalid id dispatches error action`() = runTest {
        val invalidTagTermModel = TermModel(
            testTagTermModel.id,
            testTagTermModel.localSiteId,
            -1L, // invalid remoteTermId
            testTagTermModel.taxonomy,
            testTagTermModel.name,
            testTagTermModel.slug,
            testTagTermModel.description,
            testTagTermModel.parentRemoteId,
            testTagTermModel.isHierarchical,
            testTagTermModel.postCount
        )

        taxonomyClient.updateTerm(testSite, invalidTagTermModel)

        val actionCaptor = ArgumentCaptor.forClass(Action::class.java)
        verify(dispatcher).dispatch(actionCaptor.capture())

        val capturedAction = actionCaptor.value
        val payload = capturedAction.payload as RemoteTermPayload
        assertEquals(capturedAction.type, TaxonomyAction.PUSHED_TERM)
        assertEquals(testSite, payload.site)
        assertEquals(invalidTagTermModel, payload.term)
        assertNotNull(payload.error)
        assertEquals(TaxonomyErrorType.GENERIC_ERROR, payload.error?.type)
    }

    /**
     * The post REST client needs tag *ids*, but PostModel (and XML-RPC before it) carries tag
     * *names*, so these cover the name/id translation it depends on.
     */
    @Test
    fun `resolveTagIds returns the id of an exactly matching tag without creating one`() = runTest {
        val termsExecutor = mock<TermsRequestExecutor> {
            onBlocking { listWithViewContext(any(), any()) } doReturn
                    listResponse(listOf(term(5L, "newsletter"), term(9L, "News")))
        }
        driveRequestsThrough(termsExecutor)

        val resolution = taxonomyClient.resolveTagIds(testSite, listOf("news"))

        assertEquals(listOf(9L), resolution.ids)
        assertTrue(resolution.complete)
        verifyBlocking(termsExecutor, never()) { create(any(), any()) }
    }

    /**
     * WordPress stores a term name HTML-encoded (`pre_term_name` runs `_wp_specialchars`), so
     * `search` has to carry the encoded form or the `LIKE` matches nothing, and the comparison has
     * to decode before matching. Without both, a tag like `AT&T` is looked for in vain on every
     * save, re-created, refused as `term_exists`, and then dropped from the post.
     */
    @Test
    fun `resolveTagIds matches a tag whose name the site stores HTML-encoded`() = runTest {
        val termsExecutor = mock<TermsRequestExecutor> {
            onBlocking { listWithViewContext(any(), any()) } doReturn listResponse(listOf(term(9L, "AT&amp;T")))
        }
        driveRequestsThrough(termsExecutor)

        val ids = taxonomyClient.resolveTagIds(testSite, listOf("AT&T")).ids

        assertEquals(listOf(9L), ids)
        verifyBlocking(termsExecutor) {
            listWithViewContext(TermEndpointType.Tags, TermListParams(search = "AT&amp;T", perPage = 100u))
        }
        verifyBlocking(termsExecutor, never()) { create(any(), any()) }
    }

    @Test
    fun `resolveTagIds creates a tag when only a substring of the name matches`() = runTest {
        val termsExecutor = mock<TermsRequestExecutor> {
            onBlocking { listWithViewContext(any(), any()) } doReturn listResponse(listOf(term(5L, "newsletter")))
            onBlocking { create(any(), any()) } doReturn createResponse(term(31L, "news"))
        }
        driveRequestsThrough(termsExecutor)

        val ids = taxonomyClient.resolveTagIds(testSite, listOf("news")).ids

        assertEquals(listOf(31L), ids)
        verifyBlocking(termsExecutor) { create(TermEndpointType.Tags, TermCreateParams(name = "news")) }
    }

    /**
     * `wp_insert_term` refuses a name it already holds, case-insensitively, after WordPress has
     * normalised what was submitted - so a name the single-page `search` missed can still come
     * back as `term_exists`. The error carries the existing id, so reading it keeps the tag on the
     * post instead of dropping it. This is a name conflict, not a slug conflict.
     */
    @Test
    fun `resolveTagIds recovers the id a term_exists error carries`() = runTest {
        val termsExecutor = mock<TermsRequestExecutor> {
            onBlocking { listWithViewContext(any(), any()) } doReturn listResponse(emptyList())
            onBlocking { create(any(), any()) } doReturn createResponse(term(99L, "news"))
        }
        driveRequestsThrough(termsExecutor, failingRequest = 2, error = termExistsError(termId = 42L))

        val resolution = taxonomyClient.resolveTagIds(testSite, listOf("news"))

        // 42 is only in the error body; 99 is the response the create would have returned, so this
        // also pins that the failing request really was the create.
        assertEquals(listOf(42L), resolution.ids)
        assertTrue(resolution.complete)
        verifyBlocking(termsExecutor) { create(TermEndpointType.Tags, TermCreateParams(name = "news")) }
    }

    /**
     * Only `term_exists` carries an id to recover, however term-id-shaped another error's data
     * happens to look.
     */
    @Test
    fun `resolveTagIds does not read an id out of an unrelated error`() = runTest {
        val termsExecutor = mock<TermsRequestExecutor> {
            onBlocking { listWithViewContext(any(), any()) } doReturn listResponse(emptyList())
            onBlocking { create(any(), any()) } doReturn createResponse(term(99L, "news"))
        }
        driveRequestsThrough(
            termsExecutor,
            failingRequest = 2,
            error = WpRequestResult.WpError(
                errorCode = WpErrorCode.CannotCreate(),
                errorMessage = "Sorry, you are not allowed to create terms in this taxonomy.",
                statusCode = 403u,
                response = """{"code":"rest_cannot_create","message":"not allowed",""" +
                        """"data":{"status":403,"term_id":42}}""",
                requestUrl = "",
                requestMethod = RequestMethod.POST
            )
        )

        val resolution = taxonomyClient.resolveTagIds(testSite, listOf("news"))

        assertTrue(resolution.ids.isEmpty())
    }

    /**
     * A create the site turns down on permissions - an Author, say, whose role has no
     * `manage_categories` - is as resolved as that name is ever going to get. Holding the whole
     * tag set back for it would throw away every other tag edit in the save, and in every save
     * after it, so only the refused name is dropped.
     */
    @Test
    fun `a name the site refuses to create is dropped without blocking the other tags`() = runTest {
        val termsExecutor = mock<TermsRequestExecutor> {
            onBlocking { listWithViewContext(any(), argThat { search == "news" }) } doReturn
                    listResponse(listOf(term(9L, "news")))
            onBlocking { listWithViewContext(any(), argThat { search == "new-tag" }) } doReturn
                    listResponse(emptyList())
            onBlocking { create(any(), any()) } doReturn createResponse(term(31L, "new-tag"))
        }
        driveRequestsThrough(
            termsExecutor,
            failingRequest = 3,
            error = WpRequestResult.WpError(
                errorCode = WpErrorCode.CannotCreate(),
                errorMessage = "Sorry, you are not allowed to create terms in this taxonomy.",
                statusCode = 403u,
                response = """{"code":"rest_cannot_create","message":"not allowed","data":{"status":403}}""",
                requestUrl = "",
                requestMethod = RequestMethod.POST
            )
        )

        val resolution = taxonomyClient.resolveTagIds(testSite, listOf("news", "new-tag"))

        // Only the found tag: 31 is what the create would have returned, so its absence pins that
        // the request the 403 answered really was the create.
        assertEquals(listOf(9L), resolution.ids)
        assertTrue(resolution.complete)
        verifyBlocking(termsExecutor) { create(TermEndpointType.Tags, TermCreateParams(name = "new-tag")) }
    }

    /**
     * A lookup that never got an answer says nothing about whether the site has the tag, so
     * creating one would be as likely to duplicate a tag it already holds as to add a missing
     * one - and a duplicate is a second tag the author never asked for, on a site they may share.
     */
    @Test
    fun `resolveTagIds does not create a tag when the lookup itself failed`() = runTest {
        val termsExecutor = mock<TermsRequestExecutor> {
            onBlocking { listWithViewContext(any(), any()) } doReturn listResponse(emptyList())
            onBlocking { create(any(), any()) } doReturn createResponse(term(31L, "news"))
        }
        driveRequestsThrough(
            termsExecutor,
            failingRequest = 1,
            error = WpRequestResult.WpError(
                errorCode = WpErrorCode.CustomException("http_request_failed"),
                errorMessage = "cURL error 28: Operation timed out",
                statusCode = 500u,
                response = "",
                requestUrl = "",
                requestMethod = RequestMethod.GET
            )
        )

        val resolution = taxonomyClient.resolveTagIds(testSite, listOf("news"))

        assertTrue(resolution.ids.isEmpty())
        // Unresolved, not refused: an update has to leave the site's tags alone until a retry
        // either finds the tag or settles that it is not there.
        assertFalse(resolution.complete)
        verifyBlocking(termsExecutor, never()) { create(any(), any()) }
    }

    @Test
    fun `resolveTagIds trims blank names and keeps each name once`() = runTest {
        val termsExecutor = mock<TermsRequestExecutor> {
            onBlocking { listWithViewContext(any(), any()) } doReturn listResponse(listOf(term(9L, "news")))
        }
        driveRequestsThrough(termsExecutor)

        val ids = taxonomyClient.resolveTagIds(testSite, listOf(" news ", "", "news", "   ")).ids

        assertEquals(listOf(9L), ids)
        verifyBlocking(termsExecutor, times(1)) { listWithViewContext(any(), any()) }
    }

    /**
     * Losing one tag is a better outcome than losing the post edit that carried it.
     */
    @Test
    fun `resolveTagIds drops a name it can neither find nor create`() = runTest {
        whenever(wpApiClient.request<Any>(any())).thenReturn(
            WpRequestResult.UnknownError(
                statusCode = 500u,
                response = "Internal Server Error",
                requestUrl = "",
                requestMethod = RequestMethod.GET
            )
        )

        assertTrue(taxonomyClient.resolveTagIds(testSite, listOf("news")).ids.isEmpty())
    }

    /**
     * The ids on their own cannot say a name was dropped, and the post client has to know: wp/v2
     * replaces a post's whole tag set, so sending a short list deletes the tags missing from it.
     */
    @Test
    fun `resolveTagIds reports an incomplete resolution when a name is dropped`() = runTest {
        whenever(wpApiClient.request<Any>(any())).thenReturn(
            WpRequestResult.UnknownError(
                statusCode = 500u,
                response = "Internal Server Error",
                requestUrl = "",
                requestMethod = RequestMethod.GET
            )
        )

        assertFalse(taxonomyClient.resolveTagIds(testSite, listOf("news")).complete)
    }

    /**
     * A partial list is indistinguishable from the real one, so returning one would have the
     * caller store it and delete the names it is missing on the next save.
     */
    @Test
    fun `resolveTagNames reports failure rather than the names it did read`() = runTest {
        whenever(wpApiClient.request<Any>(any())).thenReturn(
            WpRequestResult.UnknownError(
                statusCode = 500u,
                response = "Internal Server Error",
                requestUrl = "",
                requestMethod = RequestMethod.GET
            )
        )

        assertNull(taxonomyClient.resolveTagNames(testSite, listOf(7L, 3L)))
    }

    /**
     * The decoded name is what the tag picker shows and what [resolveTagIds] expects back, so
     * storing the encoded form on the PostModel would re-encode it on the next save.
     */
    @Test
    fun `resolveTagNames decodes a name the site stores HTML-encoded`() = runTest {
        val termsExecutor = mock<TermsRequestExecutor> {
            onBlocking { listWithViewContext(any(), any()) } doReturn listResponse(listOf(term(9L, "AT&amp;T")))
        }
        driveRequestsThrough(termsExecutor)

        assertEquals(listOf("AT&T"), taxonomyClient.resolveTagNames(testSite, listOf(9L)))
    }

    @Test
    fun `resolveTagNames returns the names in the order the ids were given`() = runTest {
        val termsExecutor = mock<TermsRequestExecutor> {
            onBlocking { listWithViewContext(any(), any()) } doReturn
                    listResponse(listOf(term(3L, "beta"), term(7L, "alpha")))
        }
        driveRequestsThrough(termsExecutor)

        assertEquals(listOf("alpha", "beta"), taxonomyClient.resolveTagNames(testSite, listOf(7L, 3L)))
    }

    @Test
    fun `resolveTagNames drops an id the site no longer has`() = runTest {
        val termsExecutor = mock<TermsRequestExecutor> {
            onBlocking { listWithViewContext(any(), any()) } doReturn listResponse(listOf(term(7L, "alpha")))
        }
        driveRequestsThrough(termsExecutor)

        assertEquals(listOf("alpha"), taxonomyClient.resolveTagNames(testSite, listOf(7L, 3L)))
    }

    /**
     * The REST API caps how many terms one page returns, so a post carrying more than a page of
     * tags would otherwise lose every name past the first page (the same trap as CMM-2122).
     */
    @Test
    fun `resolveTagNames follows pagination`() = runTest {
        val termsExecutor = mock<TermsRequestExecutor> {
            onBlocking { listWithViewContext(any(), any()) }.doReturn(
                listResponse(listOf(term(7L, "alpha")), nextPageParams = TermListParams(page = 2u)),
                listResponse(listOf(term(3L, "beta")))
            )
        }
        driveRequestsThrough(termsExecutor)

        assertEquals(listOf("alpha", "beta"), taxonomyClient.resolveTagNames(testSite, listOf(7L, 3L)))
        verifyBlocking(termsExecutor, times(2)) { listWithViewContext(any(), any()) }
    }

    @Test
    fun `resolveTagNames asks for nothing when there are no ids`() = runTest {
        assertEquals(emptyList<String>(), taxonomyClient.resolveTagNames(testSite, emptyList()))
        verifyBlocking(wpApiClient, never()) { request<Any>(any()) }
    }

    /**
     * Runs the client's request lambdas against [termsExecutor] instead of stubbing `request`
     * wholesale, so the params the client builds are observable.
     */
    private suspend fun driveRequestsThrough(termsExecutor: TermsRequestExecutor) {
        val uniffiClient = mock<UniffiWpApiClient> { on { terms() } doReturn termsExecutor }
        whenever(wpApiClient.request<Any>(any())).doSuspendableAnswer { invocation ->
            val executeRequest = invocation.getArgument<suspend (UniffiWpApiClient) -> Any>(0)
            WpRequestResult.Success(response = executeRequest(uniffiClient))
        }
    }

    /**
     * As [driveRequestsThrough], but the [failingRequest]th request comes back as [error] while
     * still being run against the executor - so a test can fail one specific request and still
     * assert, with `verifyBlocking`, which request that was. Positional stubbing alone cannot:
     * it would keep passing if another request were ever added ahead of the one under test.
     */
    private suspend fun driveRequestsThrough(
        termsExecutor: TermsRequestExecutor,
        failingRequest: Int,
        error: WpRequestResult<Any>
    ) {
        val uniffiClient = mock<UniffiWpApiClient> { on { terms() } doReturn termsExecutor }
        var requests = 0
        whenever(wpApiClient.request<Any>(any())).doSuspendableAnswer { invocation ->
            requests++
            val executeRequest = invocation.getArgument<suspend (UniffiWpApiClient) -> Any>(0)
            val response = executeRequest(uniffiClient)
            if (requests == failingRequest) error else WpRequestResult.Success(response)
        }
    }

    /** The shape WordPress answers a create for a name it already holds with. */
    private fun termExistsError(termId: Long) = WpRequestResult.WpError<Any>(
        errorCode = WpErrorCode.CustomException("term_exists"),
        errorMessage = "A term with the name provided already exists in this taxonomy.",
        statusCode = 400u,
        response = """{"code":"term_exists","message":"A term with the name provided already """ +
                """exists in this taxonomy.","data":{"status":400,"term_id":$termId},""" +
                """"additional_data":[$termId,$termId]}""",
        requestUrl = "",
        requestMethod = RequestMethod.POST
    )

    private fun listResponse(
        terms: List<AnyTermWithViewContext>,
        nextPageParams: TermListParams? = null
    ) = TermsRequestListWithViewContextResponse(terms, mock<WpNetworkHeaderMap>(), nextPageParams, null)

    private fun createResponse(term: AnyTermWithViewContext) =
        TermsRequestCreateResponse(
            AnyTermWithEditContext(
                id = term.id,
                count = term.count,
                description = term.description,
                link = term.link,
                name = term.name,
                slug = term.slug,
                taxonomy = TaxonomyType.PostTag,
                parent = term.parent
            ),
            mock<WpNetworkHeaderMap>()
        )

    private fun term(id: Long, name: String) = AnyTermWithViewContext(
        id = id,
        count = 0L,
        description = "",
        link = "https://example.com/tag/$name",
        name = name,
        slug = name,
        taxonomy = TaxonomyType.PostTag,
        parent = 0L
    )

    private fun createListResponse(
        terms: List<AnyTermWithViewContext>,
        nextPageParams: TermListParams?
    ): WpRequestResult<TermsRequestListWithViewContextResponse> = WpRequestResult.Success(
        response = TermsRequestListWithViewContextResponse(
            terms,
            mock<WpNetworkHeaderMap>(),
            nextPageParams,
            null
        )
    )

    private fun createTestDeleteResponse(deleted: Boolean): TermsRequestDeleteResponse {
        return TermsRequestDeleteResponse(
            TermDeleteResponse(deleted, createTestAnyTermWithEditContext()),
            mock<WpNetworkHeaderMap>()
        )
    }

    // The edit and view context terms are separate generated types with no common supertype, so the
    // one term these fixtures describe is defined once here and built into each of them.
    private fun createTestAnyTermWithEditContext(): AnyTermWithEditContext {
        return AnyTermWithEditContext(
            id = TEST_TERM_ID,
            count = TEST_TERM_COUNT,
            description = TEST_TERM_DESCRIPTION,
            link = TEST_TERM_LINK,
            name = TEST_TERM_NAME,
            slug = TEST_TERM_SLUG,
            taxonomy = TaxonomyType.Category,
            parent = TEST_TERM_PARENT
        )
    }

    private fun createTestAnyTermWithViewContext(): AnyTermWithViewContext {
        return AnyTermWithViewContext(
            id = TEST_TERM_ID,
            count = TEST_TERM_COUNT,
            description = TEST_TERM_DESCRIPTION,
            link = TEST_TERM_LINK,
            name = TEST_TERM_NAME,
            slug = TEST_TERM_SLUG,
            taxonomy = TaxonomyType.Category,
            parent = TEST_TERM_PARENT
        )
    }

    companion object {
        private const val TEST_TERM_ID = 2L
        private const val TEST_TERM_COUNT = 3L
        private const val TEST_TERM_DESCRIPTION = "Test category description"
        private const val TEST_TERM_LINK = "https://example.com/category/test"
        private const val TEST_TERM_NAME = "Test Category"
        private const val TEST_TERM_SLUG = "test-category"
        private const val TEST_TERM_PARENT = 0L
    }
}

