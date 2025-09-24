package org.wordpress.android.fluxc.network.rest.wpapi.taxonomy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.TaxonomyAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsResponsePayload
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyErrorType
import org.wordpress.android.fluxc.utils.AppLogWrapper
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.CategoryWithEditContext
import uniffi.wp_api.CategoriesRequestListWithEditContextResponse
import uniffi.wp_api.TagWithEditContext
import uniffi.wp_api.TagsRequestListWithEditContextResponse
import uniffi.wp_api.TaxonomyType
import uniffi.wp_api.WpNetworkHeaderMap

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
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
    fun `fetchPostTags with error response dispatches error action`() = runTest {
        // Use a concrete error type that we can create - UnknownError requires statusCode and response
        val errorResponse = WpRequestResult.UnknownError<Any>(
            statusCode = 500u,
            response = "Internal Server Error"
        )

        whenever(wpApiClient.request<Any>(any())).thenReturn(errorResponse)

        taxonomyClient.fetchPostTags(testSite)

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
    fun `fetchPostTags with success response dispatches success action`() = runTest {
        val tagWithEditContext = listOf(
            createTestTagWithEditContext(),
            createTestTagWithEditContext()
        )

        // Create the correct response structure following the MediaRSApiRestClientTest pattern
        val tagResponse = TagsRequestListWithEditContextResponse(
            tagWithEditContext,
            mock<WpNetworkHeaderMap>(),
            null,
            null
        )

        val successResponse: WpRequestResult<TagsRequestListWithEditContextResponse> = WpRequestResult.Success(
            response = tagResponse
        )

        whenever(wpApiClient.request<TagsRequestListWithEditContextResponse>(any())).thenReturn(successResponse)

        taxonomyClient.fetchPostTags(testSite)

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
    fun `fetchPostCategories with error response dispatches error action`() = runTest {
        // Use a concrete error type that we can create - UnknownError requires statusCode and response
        val errorResponse = WpRequestResult.UnknownError<Any>(
            statusCode = 500u,
            response = "Internal Server Error"
        )

        whenever(wpApiClient.request<Any>(any())).thenReturn(errorResponse)

        taxonomyClient.fetchPostCategories(testSite)

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
    fun `fetchPostCategories with success response dispatches success action`() = runTest {
        val categoryWithEditContext = listOf(
            createTestCategoryWithEditContext(),
            createTestCategoryWithEditContext()
        )

        // Create the correct response structure following the MediaRSApiRestClientTest pattern
        val categoryResponse = CategoriesRequestListWithEditContextResponse(
            categoryWithEditContext,
            mock<WpNetworkHeaderMap>(),
            null,
            null
        )

        val successResponse: WpRequestResult<CategoriesRequestListWithEditContextResponse> = WpRequestResult.Success(
            response = categoryResponse
        )

        whenever(wpApiClient.request<CategoriesRequestListWithEditContextResponse>(any())).thenReturn(successResponse)

        taxonomyClient.fetchPostCategories(testSite)

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

    private fun createTestCategoryWithEditContext(): CategoryWithEditContext {
        return CategoryWithEditContext(
            id = 2L,
            count = 3L,
            description = "Test category description",
            link = "https://example.com/category/test",
            name = "Test Category",
            slug = "test-category",
            taxonomy = TaxonomyType.Category,
            parent = 0L
        )
    }

    private fun createTestTagWithEditContext(): TagWithEditContext {
        return TagWithEditContext(
            id = 1L,
            count = 5L,
            description = "Test tag description",
            link = "https://example.com/tag/test",
            name = "Test Tag",
            slug = "test-tag",
            taxonomy = TaxonomyType.PostTag
        )
    }
}

