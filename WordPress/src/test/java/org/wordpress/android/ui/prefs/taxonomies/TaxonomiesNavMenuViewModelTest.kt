package org.wordpress.android.ui.prefs.taxonomies

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.TaxonomiesRequestListWithEditContextResponse
import uniffi.wp_api.TaxonomyType
import uniffi.wp_api.TaxonomyTypeDetailsWithEditContext
import uniffi.wp_api.TaxonomyTypesResponseWithEditContext
import uniffi.wp_api.TaxonomyTypeVisibility
import uniffi.wp_api.WpNetworkHeaderMap

@ExperimentalCoroutinesApi
class TaxonomiesNavMenuViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var wpApiClientProvider: WpApiClientProvider

    @Mock
    private lateinit var wpApiClient: WpApiClient

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    private lateinit var viewModel: TaxonomiesNavMenuViewModel

    private var taxonomies: List<TaxonomyTypeDetailsWithEditContext> = listOf()

    private val testSite = SiteModel().apply {
        id = 123
        url = "https://test.wordpress.com"
        apiRestUsernamePlain = "user"
        apiRestPasswordPlain = "pass"
        setIsWPCom(false)
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        whenever(wpApiClientProvider.getWpApiClient(testSite)).thenReturn(wpApiClient)

        viewModel = TaxonomiesNavMenuViewModel(
            wpApiClientProvider,
            appLogWrapper
        )
        viewModel.taxonomies.observeForever { taxonomies = it }
    }

    @Test
    fun `when site does not support self-hosted rest api, then taxonomies are not fetched`() = test {
        testSite.setIsWPCom(true)

        viewModel.fetchTaxonomies(testSite)
        advanceUntilIdle()

        verify(wpApiClientProvider, never()).getWpApiClient(any(), any())
        verify(appLogWrapper).d(
            AppLog.T.API,
            "Taxonomies - Taxonomies cannot be fetched: Application Password not available"
        )
        assertTrue(taxonomies.isEmpty())
    }

    @Test
    fun `when LiveData is observed, it starts with null value`() {
        assertNotNull(viewModel.taxonomies)
        assertEquals(null, viewModel.taxonomies.value)
    }

    @Test
    fun `fetch taxonomies with success response dispatches success action`() = runTest {
        // Create the correct response structure following the MediaRSApiRestClientTest pattern
        val response = TaxonomiesRequestListWithEditContextResponse(
            createTestTaxonomyTypesResponseWithEditContext(),
            mock<WpNetworkHeaderMap>(),
        )

        val successResponse: WpRequestResult<TaxonomiesRequestListWithEditContextResponse> = WpRequestResult.Success(
            response = response
        )

        whenever(wpApiClient.request<TaxonomiesRequestListWithEditContextResponse>(any())).thenReturn(successResponse)

        viewModel.fetchTaxonomies(testSite)
        advanceUntilIdle()

        val responseList: List<TaxonomyTypeDetailsWithEditContext> = response.data.taxonomyTypes.map { it.value }
        assertEquals(responseList, taxonomies)
    }

    @Test
    fun `fetch taxonomies with error response do nothing`() = runTest {
        // Use a concrete error type that we can create - UnknownError requires statusCode and response
        val errorResponse = WpRequestResult.UnknownError<Any>(
            statusCode = 500u,
            response = "Internal Server Error"
        )

        whenever(wpApiClient.request<Any>(any())).thenReturn(errorResponse)

        viewModel.fetchTaxonomies(testSite)

        verify(appLogWrapper).e(any(), any())
        assertTrue(taxonomies.isEmpty())
    }

    private fun createTestTaxonomyTypesResponseWithEditContext(): TaxonomyTypesResponseWithEditContext {
        val visibility = TaxonomyTypeVisibility(
            public = true,
            publiclyQueryable = true,
            showUi = true,
            showAdminColumn = true,
            showInNavMenus = true,
            showInQuickEdit = true
        )

        val categoryDetails = TaxonomyTypeDetailsWithEditContext(
            name = "Categories",
            slug = "category",
            description = "Test categories",
            visibility = visibility,
            restBase = "categories",
            restNamespace = "wp/v2",
            types = listOf("post"),
            hierarchical = true,
            showCloud = true,
            capabilities = mock(),
            labels = mock()
        )

        val tagDetails = TaxonomyTypeDetailsWithEditContext(
            name = "Tags",
            slug = "post_tag",
            description = "Test tags",
            visibility = visibility,
            restBase = "tags",
            restNamespace = "wp/v2",
            types = listOf("post"),
            hierarchical = false,
            showCloud = true,
            capabilities = mock(),
            labels = mock()
        )

        return TaxonomyTypesResponseWithEditContext(
            mapOf(
                TaxonomyType.Category to categoryDetails,
                TaxonomyType.PostTag to tagDetails
            )
        )
    }
}
