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
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
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
import uniffi.wp_api.TermsRequestCreateResponse
import uniffi.wp_api.TermsRequestDeleteResponse
import uniffi.wp_api.TermsRequestListWithViewContextResponse
import uniffi.wp_api.TermsRequestUpdateResponse
import uniffi.wp_api.TaxonomyType
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
        assertEquals(anyTermWithEditContext.id.toInt(), payload.term.id)
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
        assertEquals(anyTermWithEditContext.id.toInt(), payload.term.id)
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
    fun `deleteTerm category with success response dispatches success action`() = runTest {
        val categoryDeleteData = createTestCategoryDeleteData(deleted = true)

        // Create the correct response structure following the MediaRsApiRestClientTest pattern
        val categoryResponse = TermsRequestDeleteResponse(
            categoryDeleteData,
            mock<WpNetworkHeaderMap>()
        )

        val successResponse: WpRequestResult<TermsRequestDeleteResponse> = WpRequestResult.Success(
            response = categoryResponse
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
        assertEquals(testCategoryTermModel.id.toLong(), payload.term.remoteTermId)
        assertEquals(testCategoryTaxonomyName, payload.term.taxonomy)
        assertEquals(testCategoryTermModel.name, payload.term.name)
        assertEquals(testCategoryTermModel.slug, payload.term.slug)
        assertEquals(testCategoryTermModel.description, payload.term.description)
        assertEquals(testCategoryTermModel.postCount, payload.term.postCount)
        assertNull(payload.error)
    }

    @Test
    fun `deleteTerm category with failed deletion response dispatches error action`() = runTest {
        val categoryDeleteData = createTestCategoryDeleteData(deleted = false)

        // Create the correct response structure with deleted = false
        val categoryResponse = TermsRequestDeleteResponse(
            categoryDeleteData,
            mock<WpNetworkHeaderMap>()
        )

        val successResponse: WpRequestResult<TermsRequestDeleteResponse> = WpRequestResult.Success(
            response = categoryResponse
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
        val tagDeleteData = createTestTagDeleteData(deleted = true)

        // Create the correct response structure following the MediaRsApiRestClientTest pattern
        val tagResponse = TermsRequestDeleteResponse(
            tagDeleteData,
            mock<WpNetworkHeaderMap>()
        )

        val successResponse: WpRequestResult<TermsRequestDeleteResponse> = WpRequestResult.Success(
            response = tagResponse
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
        assertEquals(testTagTermModel.id.toLong(), payload.term.remoteTermId)
        assertEquals(testTagTaxonomyName, payload.term.taxonomy)
        assertEquals(testTagTermModel.name, payload.term.name)
        assertEquals(testTagTermModel.slug, payload.term.slug)
        assertEquals(testTagTermModel.description, payload.term.description)
        assertEquals(testTagTermModel.postCount, payload.term.postCount)
        assertNull(payload.error)
    }

    @Test
    fun `deleteTerm tag with failed deletion response dispatches error action`() = runTest {
        val tagDeleteData = createTestTagDeleteData(deleted = false)

        // Create the correct response structure with deleted = false
        val tagResponse = TermsRequestDeleteResponse(
            tagDeleteData,
            mock<WpNetworkHeaderMap>()
        )

        val successResponse: WpRequestResult<TermsRequestDeleteResponse> = WpRequestResult.Success(
            response = tagResponse
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
        assertEquals(anyTermWithEditContext.id.toInt(), payload.term.id)
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
        assertEquals(anyTermWithEditContext.id.toInt(), payload.term.id)
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

    private fun createTestCategoryDeleteData(deleted: Boolean): TermDeleteResponse {
        return TermDeleteResponse(deleted, createTestAnyTermWithEditContext())
    }

    private fun createTestTagDeleteData(deleted: Boolean): TermDeleteResponse {
        return TermDeleteResponse(deleted, createTestAnyTermWithEditContext())
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

