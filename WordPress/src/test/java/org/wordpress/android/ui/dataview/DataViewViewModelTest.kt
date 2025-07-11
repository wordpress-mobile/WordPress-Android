package org.wordpress.android.ui.dataview

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.NetworkUtilsWrapper
import rs.wordpress.api.kotlin.WpComApiClient
import uniffi.wp_api.WpApiParamOrder

@ExperimentalCoroutinesApi
class DataViewViewModelTest : BaseUnitTest() {
    val networkUtilsWrapper = mock<NetworkUtilsWrapper>()

    private fun createTestViewModel(): TestableDataViewViewModel {
        val appLogWrapper = mock<AppLogWrapper>()
        val networkUtilsWrapper = networkUtilsWrapper
        val selectedSiteRepository = mock<SelectedSiteRepository>()
        val accountStore = mock<AccountStore>()

        val testDependencies = DataViewTestDependencies(
            mainDispatcher = testDispatcher(),
            appLogWrapper = appLogWrapper,
            networkUtilsWrapper = networkUtilsWrapper,
            selectedSiteRepository = selectedSiteRepository,
            accountStore = accountStore,
            ioDispatcher = testDispatcher(),
        )
        return TestableDataViewViewModel(testDependencies)
    }

    @Test
    fun `when initialized, then ui state starts as loading`() = runTest {
        val viewModel = createTestViewModel()
        assertThat(viewModel.uiState.value).isEqualTo(DataViewUiState.LOADING)
    }

    @Test
    fun `when initialized, then items are empty`() = runTest {
        val viewModel = createTestViewModel()
        assertThat(viewModel.items.value).isEmpty()
    }

    @Test
    fun `when error occurs, then ui state is error`() = runTest {
        val viewModel = createTestViewModel()
        viewModel.onError("Test error message")
        assertThat(viewModel.uiState.value).isEqualTo(DataViewUiState.ERROR)
        assertThat(viewModel.errorMessage.value).isEqualTo("Test error message")
    }

    @Test
    fun `when sort order is clicked, then sort order is applied`() = runTest {
        val viewModel = createTestViewModel()
        viewModel.onSortOrderClick(WpApiParamOrder.DESC)
        assertThat(viewModel.sortOrder.value).isEqualTo(WpApiParamOrder.DESC)
    }

    private class TestableDataViewViewModel(
        testDependencies: DataViewTestDependencies
    ) : DataViewViewModel(testDependencies) {
        override suspend fun performNetworkRequest(
            page: Int,
            searchQuery: String,
            filter: DataViewDropdownItem?,
            sortOrder: WpApiParamOrder,
            sortBy: DataViewDropdownItem?
        ): List<DataViewItem> {
            return emptyList()
        }

        override fun createWpComApiClient(): WpComApiClient {
            return mock()
        }

        override fun startInitialDataFetch() {
            // Skip initial data fetch for tests
        }
    }
}
