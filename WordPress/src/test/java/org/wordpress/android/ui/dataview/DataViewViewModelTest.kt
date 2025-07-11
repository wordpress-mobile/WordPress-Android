package org.wordpress.android.ui.dataview

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.wordpress.android.BaseUnitTest
import rs.wordpress.api.kotlin.WpComApiClient
import uniffi.wp_api.WpApiParamOrder

@ExperimentalCoroutinesApi
class DataViewViewModelTest : BaseUnitTest() {
    private fun createTestViewModel(): DataViewViewModel {
        return spy(object : DataViewViewModel(
            mainDispatcher = testDispatcher(),
            appLogWrapper = mock(),
            networkUtilsWrapper = mock {
                on { isNetworkAvailable() } doReturn true
            },
            selectedSiteRepository = mock(),
            accountStore = mock(),
            ioDispatcher = testDispatcher()
        ) {
            override suspend fun performNetworkRequest(
                page: Int,
                searchQuery: String,
                filter: DataViewDropdownItem?,
                sortOrder: WpApiParamOrder,
                sortBy: DataViewDropdownItem?
            ): List<DataViewItem> = emptyList()

            override fun createWpComApiClient(): WpComApiClient = mock()

            override fun startInitialDataFetch() {
                // Skip initial data fetch for tests
            }
        })
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
}
