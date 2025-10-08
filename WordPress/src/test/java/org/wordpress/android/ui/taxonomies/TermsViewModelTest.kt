package org.wordpress.android.ui.taxonomies

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.store.AccountStore
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

    @Before
    fun setUp() {
        // Minimal setup - add more mocks in individual tests as needed
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
            ioDispatcher = testDispatcher()
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
}
