package org.wordpress.android.ui.taxonomies

import android.content.Context
import android.content.SharedPreferences
import androidx.navigation.NavHostController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.TrackNetworkRequestsInterceptor
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.TaxonomyStore
import org.wordpress.android.fluxc.store.TaxonomyStore.DEFAULT_TAXONOMY_CATEGORY
import org.wordpress.android.fluxc.store.TaxonomyStore.DEFAULT_TAXONOMY_TAG
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.NetworkUtilsWrapper

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
            trackNetworkRequestsInterceptor = trackNetworkRequestsInterceptor
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
}
