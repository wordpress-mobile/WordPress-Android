package org.wordpress.android.ui.taxonomies

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.TrackNetworkRequestsInterceptor
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpNetworkAvailabilityProvider
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.TaxonomyStore
import org.wordpress.android.fluxc.store.TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY
import org.wordpress.android.fluxc.store.TaxonomyStore.DEFAULT_TAXONOMY_TAG
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.dataview.LoadingState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.NetworkUtilsWrapper
import rs.wordpress.api.kotlin.WpApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.AnyTermWithEditContext
import uniffi.wp_api.RequestMethod
import uniffi.wp_api.TaxonomyType
import uniffi.wp_api.TermListParams
import uniffi.wp_api.TermsRequestListWithEditContextResponse
import uniffi.wp_api.WpNetworkHeaderMap

@ExperimentalCoroutinesApi
class TermsViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var wpApiClientProvider: WpApiClientProvider

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var sharedPrefs: SharedPreferences

    @Mock
    private lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    private lateinit var taxonomyStore: TaxonomyStore

    @Mock
    private lateinit var fluxCDispatcher: Dispatcher

    @Mock
    private lateinit var trackNetworkRequestsInterceptor: TrackNetworkRequestsInterceptor

    @Mock
    private lateinit var networkAvailabilityProvider: WpNetworkAvailabilityProvider

    @Mock
    private lateinit var wpApiClient: WpApiClient

    @Mock
    private lateinit var resources: Resources

    private val testSite = SiteModel()

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
    }

    private fun createViewModel(): TermsViewModel {
        return TermsViewModel(
            context = context,
            wpApiClientProvider = wpApiClientProvider,
            appLogWrapper = appLogWrapper,
            selectedSiteRepository = selectedSiteRepository,
            accountStore = accountStore,
            mainDispatcher = testDispatcher(),
            sharedPrefs = sharedPrefs,
            networkUtilsWrapper = networkUtilsWrapper,
            ioDispatcher = testDispatcher(),
            taxonomyStore = taxonomyStore,
            fluxCDispatcher = fluxCDispatcher,
            trackNetworkRequestsInterceptor = trackNetworkRequestsInterceptor,
            networkAvailabilityProvider = networkAvailabilityProvider
        )
    }

    @Test
    fun `getSupportedSorts returns empty list for hierarchical taxonomies`() {
        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_CATEGORY, isHierarchical = true)

        val supportedSorts = viewModel.getSupportedSorts()

        assertThat(supportedSorts).isEmpty()
    }

    @Test
    fun `getSupportedSorts returns sort options for non-hierarchical taxonomies`() {
        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_TAG, isHierarchical = false)

        val supportedSorts = viewModel.getSupportedSorts()

        assertThat(supportedSorts).hasSize(2)
        assertThat(supportedSorts[0].titleRes).isEqualTo(R.string.term_sort_by_name)
        assertThat(supportedSorts[1].titleRes).isEqualTo(R.string.term_sort_by_count)
    }

    @Test
    fun `network unavailable sets offline state`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_CATEGORY, isHierarchical = true)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.loadingState)
            .isEqualTo(org.wordpress.android.ui.dataview.LoadingState.OFFLINE)
    }

    @Test
    fun `setNavController stores navigation controller`() {
        val viewModel = createViewModel()
        val navController = mock<NavHostController>()

        viewModel.setNavController(navController)

        // Should not throw - just verify it's stored
        assertThat(viewModel).isNotNull
    }

    @Test
    fun `navigateToCreateTerm sets empty term detail state`() = test {
        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_CATEGORY, isHierarchical = true)

        viewModel.navigateToCreateTerm()

        val state = viewModel.termDetailState.first()
        assertThat(state).isNotNull
        assertThat(state?.termId).isEqualTo(0L)
        assertThat(state?.name).isEmpty()
        assertThat(state?.slug).isEmpty()
        assertThat(state?.description).isEmpty()
    }

    @Test
    fun `navigateToCreateTerm includes available parents for hierarchical taxonomy`() = test {
        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_CATEGORY, isHierarchical = true)

        viewModel.navigateToCreateTerm()

        val state = viewModel.termDetailState.first()
        // availableParents should be non-null for hierarchical taxonomies
        assertThat(state?.availableParents).isNotNull
    }

    @Test
    fun `navigateToCreateTerm excludes available parents for non-hierarchical taxonomy`() = test {
        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_TAG, isHierarchical = false)

        viewModel.navigateToCreateTerm()

        val state = viewModel.termDetailState.first()
        assertThat(state?.availableParents).isNull()
    }

    @Test
    fun `navigateBack clears term detail state`() = test {
        val viewModel = createViewModel()
        val navController = mock<NavHostController>()
        viewModel.setNavController(navController)
        viewModel.initialize(DEFAULT_TAXONOMY_CATEGORY, isHierarchical = true)
        viewModel.navigateToCreateTerm()

        viewModel.navigateBack()

        val state = viewModel.termDetailState.first()
        assertThat(state).isNull()
        verify(navController).navigateUp()
    }

    @Test
    fun `updateTermName updates term detail state`() = test {
        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_CATEGORY, isHierarchical = true)
        viewModel.navigateToCreateTerm()

        viewModel.updateTermName("New Term Name")

        val state = viewModel.termDetailState.first()
        assertThat(state?.name).isEqualTo("New Term Name")
    }

    @Test
    fun `updateTermSlug updates term detail state`() = test {
        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_CATEGORY, isHierarchical = true)
        viewModel.navigateToCreateTerm()

        viewModel.updateTermSlug("new-slug")

        val state = viewModel.termDetailState.first()
        assertThat(state?.slug).isEqualTo("new-slug")
    }

    @Test
    fun `updateTermDescription updates term detail state`() = test {
        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_CATEGORY, isHierarchical = true)
        viewModel.navigateToCreateTerm()

        viewModel.updateTermDescription("New description")

        val state = viewModel.termDetailState.first()
        assertThat(state?.description).isEqualTo("New description")
    }

    @Test
    fun `updateTermParent updates term detail state`() = test {
        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_CATEGORY, isHierarchical = true)
        viewModel.navigateToCreateTerm()

        viewModel.updateTermParent(123L)

        val state = viewModel.termDetailState.first()
        assertThat(state?.parentId).isEqualTo(123L)
    }

    @Test
    fun `clearTermDetail sets state to null`() = test {
        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_CATEGORY, isHierarchical = true)
        viewModel.navigateToCreateTerm()

        viewModel.clearTermDetail()

        val state = viewModel.termDetailState.first()
        assertThat(state).isNull()
    }

    @Test
    fun `saveTerm sets error when site is null`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)
        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_CATEGORY, isHierarchical = true)
        viewModel.navigateToCreateTerm()

        viewModel.saveTerm()
        advanceUntilIdle()

        val event = viewModel.uiEvent.first()
        assertThat(event).isInstanceOf(UiEvent.ShowError::class.java)
        assertThat((event as UiEvent.ShowError).messageRes).isEqualTo(R.string.error_saving_term)
    }

    @Test
    fun `saveTerm sets error when term detail is null`() = test {
        val site = SiteModel()
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_CATEGORY, isHierarchical = true)

        viewModel.saveTerm()
        advanceUntilIdle()

        val event = viewModel.uiEvent.first()
        assertThat(event).isInstanceOf(UiEvent.ShowError::class.java)
    }

    @Test
    fun `deleteTerm sets error when site is null`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)
        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_CATEGORY, isHierarchical = true)

        viewModel.deleteTerm(123L)
        advanceUntilIdle()

        val event = viewModel.uiEvent.first()
        assertThat(event).isInstanceOf(UiEvent.ShowError::class.java)
        assertThat((event as UiEvent.ShowError).messageRes).isEqualTo(R.string.error_deleting_term)
    }

    @Test
    fun `performNetworkRequest follows pagination and combines all pages`() = test {
        stubSuccessfulFetch()
        val firstPage = createListResponse(
            terms = List(2) { createTestTerm(id = it.toLong()) },
            nextPageParams = TermListParams(perPage = 100u)
        )
        val secondPage = createListResponse(
            terms = List(3) { createTestTerm(id = (it + 2).toLong()) },
            nextPageParams = null
        )
        whenever(wpApiClient.request<TermsRequestListWithEditContextResponse>(any()))
            .thenReturn(firstPage, secondPage)

        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_TAG, isHierarchical = false)
        advanceUntilIdle()

        // Two pages requested, all 5 terms combined into the list
        verify(wpApiClient, times(2)).request<TermsRequestListWithEditContextResponse>(any())
        assertThat(viewModel.uiState.value.items).hasSize(5)
        assertThat(viewModel.uiState.value.loadingState).isEqualTo(LoadingState.LOADED)
    }

    @Test
    fun `canLoadMore stays false even when a full page is returned`() = test {
        stubSuccessfulFetch()
        // A single page that is exactly PAGE_SIZE (25) long with no further pages. Without the
        // supportsLoadMore override the base canLoadMore check would flip to true here.
        val fullPage = createListResponse(
            terms = List(25) { createTestTerm(id = it.toLong()) },
            nextPageParams = null
        )
        whenever(wpApiClient.request<TermsRequestListWithEditContextResponse>(any()))
            .thenReturn(fullPage)

        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_TAG, isHierarchical = false)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.items).hasSize(25)
        assertThat(viewModel.uiState.value.canLoadMore).isFalse
    }

    @Test
    fun `partial failure on a later page keeps already fetched terms`() = test {
        stubSuccessfulFetch()
        val firstPage = createListResponse(
            terms = List(2) { createTestTerm(id = it.toLong()) },
            nextPageParams = TermListParams(perPage = 100u)
        )
        val errorResponse = WpRequestResult.UnknownError<TermsRequestListWithEditContextResponse>(
            statusCode = 500u,
            response = "Internal Server Error",
            requestUrl = "",
            requestMethod = RequestMethod.GET
        )
        whenever(wpApiClient.request<TermsRequestListWithEditContextResponse>(any()))
            .thenReturn(firstPage, errorResponse)

        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_TAG, isHierarchical = false)
        advanceUntilIdle()

        // The first page is preserved and the error state is not surfaced
        assertThat(viewModel.uiState.value.items).hasSize(2)
        assertThat(viewModel.uiState.value.loadingState).isEqualTo(LoadingState.LOADED)
    }

    @Test
    fun `failure on the first page surfaces the error state`() = test {
        stubSuccessfulFetch()
        val errorResponse = WpRequestResult.UnknownError<TermsRequestListWithEditContextResponse>(
            statusCode = 500u,
            response = "Internal Server Error",
            requestUrl = "",
            requestMethod = RequestMethod.GET
        )
        whenever(wpApiClient.request<TermsRequestListWithEditContextResponse>(any()))
            .thenReturn(errorResponse)

        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_TAG, isHierarchical = false)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.loadingState).isEqualTo(LoadingState.ERROR)
        assertThat(viewModel.uiState.value.items).isEmpty()
    }

    @Test
    fun `hierarchy is built across pages so a child paged after its parent nests under it`() = test {
        stubSuccessfulFetch()
        // The child is returned on the first page and its parent only on the second. Building the
        // tree per-page would leave the child an orphan root; building from the full set must nest
        // it. This is the headline bug this change fixes.
        val firstPage = createListResponse(
            terms = listOf(createTestTerm(id = 2L, parent = 1L)),
            nextPageParams = TermListParams(perPage = 100u)
        )
        val secondPage = createListResponse(
            terms = listOf(createTestTerm(id = 1L, parent = 0L)),
            nextPageParams = null
        )
        whenever(wpApiClient.request<TermsRequestListWithEditContextResponse>(any()))
            .thenReturn(firstPage, secondPage)

        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_CATEGORY, isHierarchical = true)
        advanceUntilIdle()

        val items = viewModel.uiState.value.items
        assertThat(items).hasSize(2)
        // Parent first at the root level, then the child indented one level (INDENTATION_IN_DP=10)
        assertThat(items[0].id).isEqualTo(1L)
        assertThat(items[0].indentation).isEqualTo(0.dp)
        assertThat(items[1].id).isEqualTo(2L)
        assertThat(items[1].indentation).isEqualTo(10.dp)
    }

    @Test
    fun `hierarchy indents grandchildren split across pages by their depth`() = test {
        stubSuccessfulFetch()
        // root (id 1) -> child (id 2) -> grandchild (id 3), each delivered on a different page so
        // the full chain only exists once every page has been combined.
        val firstPage = createListResponse(
            terms = listOf(createTestTerm(id = 3L, parent = 2L)),
            nextPageParams = TermListParams(perPage = 100u)
        )
        val secondPage = createListResponse(
            terms = listOf(createTestTerm(id = 2L, parent = 1L)),
            nextPageParams = TermListParams(perPage = 100u)
        )
        val thirdPage = createListResponse(
            terms = listOf(createTestTerm(id = 1L, parent = 0L)),
            nextPageParams = null
        )
        whenever(wpApiClient.request<TermsRequestListWithEditContextResponse>(any()))
            .thenReturn(firstPage, secondPage, thirdPage)

        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_CATEGORY, isHierarchical = true)
        advanceUntilIdle()

        val items = viewModel.uiState.value.items
        assertThat(items.map { it.id }).containsExactly(1L, 2L, 3L)
        assertThat(items.map { it.indentation }).containsExactly(0.dp, 10.dp, 20.dp)
    }

    @Test
    fun `complete fetch stores the terms locally`() = test {
        stubSuccessfulFetch()
        val page = createListResponse(
            terms = List(2) { createTestTerm(id = it.toLong()) },
            nextPageParams = null
        )
        whenever(wpApiClient.request<TermsRequestListWithEditContextResponse>(any()))
            .thenReturn(page)

        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_TAG, isHierarchical = false)
        advanceUntilIdle()

        verify(fluxCDispatcher).dispatch(any())
    }

    @Test
    fun `partial failure does not store the incomplete terms locally`() = test {
        stubSuccessfulFetch()
        val firstPage = createListResponse(
            terms = List(2) { createTestTerm(id = it.toLong()) },
            nextPageParams = TermListParams(perPage = 100u)
        )
        val errorResponse = WpRequestResult.UnknownError<TermsRequestListWithEditContextResponse>(
            statusCode = 500u,
            response = "Internal Server Error",
            requestUrl = "",
            requestMethod = RequestMethod.GET
        )
        whenever(wpApiClient.request<TermsRequestListWithEditContextResponse>(any()))
            .thenReturn(firstPage, errorResponse)

        val viewModel = createViewModel()
        viewModel.initialize(DEFAULT_TAXONOMY_TAG, isHierarchical = false)
        advanceUntilIdle()

        // The fetched terms are still shown, but an incomplete list must not poison the cache
        assertThat(viewModel.uiState.value.items).hasSize(2)
        verify(fluxCDispatcher, never()).dispatch(any())
    }

    private fun stubSuccessfulFetch() {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        // Raw site instance (not a matcher): getWpApiClient is a concrete method with a default
        // parameter, which trips up Mockito matchers.
        whenever(wpApiClientProvider.getWpApiClient(testSite)).thenReturn(wpApiClient)
        whenever(context.resources).thenReturn(resources)
        // Raw values (not matchers) so the vararg getString overload stubs cleanly; every test
        // term uses count = 0L.
        whenever(resources.getString(R.string.term_count, 0L)).thenReturn("count")
    }

    private fun createListResponse(
        terms: List<AnyTermWithEditContext>,
        nextPageParams: TermListParams?
    ): WpRequestResult<TermsRequestListWithEditContextResponse> = WpRequestResult.Success(
        response = TermsRequestListWithEditContextResponse(
            terms,
            mock<WpNetworkHeaderMap>(),
            nextPageParams,
            null
        )
    )

    private fun createTestTerm(id: Long, parent: Long = 0L): AnyTermWithEditContext =
        AnyTermWithEditContext(
            id = id,
            count = 0L,
            description = "",
            link = "https://example.com/tag/$id",
            name = "Term $id",
            slug = "term-$id",
            taxonomy = TaxonomyType.PostTag,
            parent = parent
        )
}
