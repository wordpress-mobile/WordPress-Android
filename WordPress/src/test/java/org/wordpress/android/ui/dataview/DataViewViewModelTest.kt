package org.wordpress.android.ui.dataview

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.NetworkUtilsWrapper
import rs.wordpress.api.kotlin.WpComApiClient
import uniffi.wp_api.WpApiParamOrder

@ExperimentalCoroutinesApi
class DataViewViewModelTest : BaseUnitTest() {

    @Mock
    private lateinit var appLogWrapper: AppLogWrapper
    
    @Mock
    private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository
    
    @Mock
    private lateinit var accountStore: AccountStore
    
    @Mock
    private lateinit var mockApiClient: WpComApiClient

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var viewModel: TestDataViewViewModel

    @Before
    fun setUp() {
        testDispatcher = UnconfinedTestDispatcher()
        
        // Setup AccountStore for API client creation
        whenever(accountStore.accessToken).thenReturn("test_token")
        
        viewModel = TestDataViewViewModel(
            mainDispatcher = testDispatcher,
            appLogWrapper = appLogWrapper
        ).apply {
            networkUtilsWrapper = this@DataViewViewModelTest.networkUtilsWrapper
            selectedSiteRepository = this@DataViewViewModelTest.selectedSiteRepository
            accountStore = this@DataViewViewModelTest.accountStore
            ioDispatcher = testDispatcher
        }
    }

    @Test
    fun `constructor initializes with loading state and default sort`() = runTest {
        assertThat(viewModel.uiState.first()).isEqualTo(DataViewUiState.LOADING)
        assertThat(viewModel.items.first()).isEmpty()
        assertThat(viewModel.itemFilter.first()).isNull()
        assertThat(viewModel.sortOrder.first()).isEqualTo(WpApiParamOrder.ASC)
        assertThat(viewModel.errorMessage.first()).isNull()
        assertThat(viewModel.refreshState.first()).isFalse()
        
        // Should set default sort
        assertThat(viewModel.itemSortBy.first()).isEqualTo(viewModel.testDefaultSort)
    }

    @Test
    fun `onFilterClick toggles filter correctly`() = runTest {
        val testFilter = DataViewDropdownItem(id = 123L, titleRes = org.wordpress.android.R.string.app_name)
        
        // Apply filter
        viewModel.onFilterClick(testFilter)
        assertThat(viewModel.itemFilter.first()).isEqualTo(testFilter)
        
        // Clear filter by clicking same filter
        viewModel.onFilterClick(testFilter)
        assertThat(viewModel.itemFilter.first()).isNull()
        
        // Apply different filter
        val otherFilter = DataViewDropdownItem(id = 456L, titleRes = org.wordpress.android.R.string.cancel)
        viewModel.onFilterClick(otherFilter)
        assertThat(viewModel.itemFilter.first()).isEqualTo(otherFilter)
    }

    @Test
    fun `onSortClick updates sort when different`() = runTest {
        val testSort = DataViewDropdownItem(id = 456L, titleRes = org.wordpress.android.R.string.cancel)
        
        viewModel.onSortClick(testSort)
        
        assertThat(viewModel.itemSortBy.first()).isEqualTo(testSort)
        verify(networkUtilsWrapper).isNetworkAvailable()
    }

    @Test
    fun `onSortClick does nothing when same sort`() = runTest {
        val currentSort = viewModel.itemSortBy.first()
        val fetchCallsBefore = viewModel.fetchDataCallCount
        
        viewModel.onSortClick(currentSort!!)
        
        assertThat(viewModel.itemSortBy.first()).isEqualTo(currentSort)
        assertThat(viewModel.fetchDataCallCount).isEqualTo(fetchCallsBefore) // No additional fetch
    }

    @Test
    fun `onSortOrderClick updates order when different`() = runTest {
        viewModel.onSortOrderClick(WpApiParamOrder.DESC)
        
        assertThat(viewModel.sortOrder.first()).isEqualTo(WpApiParamOrder.DESC)
        verify(networkUtilsWrapper).isNetworkAvailable()
    }

    @Test
    fun `onSortOrderClick does nothing when same order`() = runTest {
        val currentOrder = viewModel.sortOrder.first()
        val fetchCallsBefore = viewModel.fetchDataCallCount
        
        viewModel.onSortOrderClick(currentOrder)
        
        assertThat(viewModel.sortOrder.first()).isEqualTo(currentOrder)
        assertThat(viewModel.fetchDataCallCount).isEqualTo(fetchCallsBefore) // No additional fetch
    }

    @Test
    fun `onSearchQueryChange triggers debounced search`() = runTest {
        viewModel.onSearchQueryChange("test query")
        
        // Search should be debounced, so verify logging was called
        verify(appLogWrapper).d(any(), any())
    }

    @Test
    fun `onError sets error message and error state`() = runTest {
        val errorMessage = "Test error message"
        
        viewModel.onError(errorMessage)
        
        assertThat(viewModel.errorMessage.first()).isEqualTo(errorMessage)
        assertThat(viewModel.uiState.first()).isEqualTo(DataViewUiState.ERROR)
    }

    @Test
    fun `removeItem filters out item from list`() = runTest {
        val testItems = listOf(
            createTestDataViewItem(1L),
            createTestDataViewItem(2L),
            createTestDataViewItem(3L)
        )
        
        viewModel.setTestItems(testItems)
        
        viewModel.removeItem(2L)
        
        val remainingItems = viewModel.items.first()
        assertThat(remainingItems).hasSize(2)
        assertThat(remainingItems.map { it.id }).containsExactly(1L, 3L)
    }

    @Test
    fun `siteId returns selected site id or 0`() {
        val testSiteId = 123456L
        whenever(selectedSiteRepository.getSelectedSite()?.siteId).thenReturn(testSiteId)
        
        assertThat(viewModel.siteId()).isEqualTo(testSiteId)
    }

    @Test
    fun `onRefreshData calls fetchData when in loaded state`() = runTest {
        // Set to loaded state first
        viewModel.setTestUiState(DataViewUiState.LOADED)
        val fetchCallsBefore = viewModel.fetchDataCallCount
        
        viewModel.onRefreshData()
        
        assertThat(viewModel.fetchDataCallCount).isGreaterThan(fetchCallsBefore)
        assertThat(viewModel.refreshState.first()).isFalse() // Should be reset after fetch
    }

    @Test
    fun `onRefreshData does nothing when not in loaded state`() = runTest {
        viewModel.setTestUiState(DataViewUiState.LOADING)
        val fetchCallsBefore = viewModel.fetchDataCallCount
        
        viewModel.onRefreshData()
        
        assertThat(viewModel.fetchDataCallCount).isEqualTo(fetchCallsBefore)
    }

    @Test
    fun `onFetchMoreData increments page and calls fetchData`() = runTest {
        // Set to loaded state first
        viewModel.setTestUiState(DataViewUiState.LOADED)
        val fetchCallsBefore = viewModel.fetchDataCallCount
        
        viewModel.onFetchMoreData()
        
        assertThat(viewModel.fetchDataCallCount).isGreaterThan(fetchCallsBefore)
    }

    @Test
    fun `onItemClick logs the item click`() {
        val testItem = createTestDataViewItem(123L)
        
        viewModel.onItemClick(testItem)
        
        verify(appLogWrapper).d(any(), any())
    }

    private fun createTestDataViewItem(id: Long): DataViewItem {
        return DataViewItem(
            id = id,
            image = null,
            title = "Test Item $id",
            fields = listOf(
                DataViewItemField(
                    value = "Test Item $id",
                    valueType = DataViewFieldType.TEXT
                )
            )
        )
    }

    /**
     * Test implementation of DataViewViewModel that provides controlled behavior
     * without requiring additional files or classes
     */
    private class TestDataViewViewModel(
        mainDispatcher: TestDispatcher,
        appLogWrapper: AppLogWrapper
    ) : DataViewViewModel(mainDispatcher, appLogWrapper) {
        
        var fetchDataCallCount = 0
            private set
        
        val testDefaultSort = DataViewDropdownItem(id = 1L, titleRes = org.wordpress.android.R.string.ok)
        
        override fun getSupportedFilters(): List<DataViewDropdownItem> = listOf(
            DataViewDropdownItem(id = 1L, titleRes = org.wordpress.android.R.string.cancel),
            DataViewDropdownItem(id = 2L, titleRes = org.wordpress.android.R.string.app_name)
        )
        
        override fun getSupportedSorts(): List<DataViewDropdownItem> = listOf(
            testDefaultSort,
            DataViewDropdownItem(id = 2L, titleRes = org.wordpress.android.R.string.no)
        )
        
        // Test helper methods to control internal state
        private val testItems = mutableListOf<DataViewItem>()
        
        fun setTestItems(items: List<DataViewItem>) {
            testItems.clear()
            testItems.addAll(items)
        }
        
        override suspend fun performNetworkRequest(
            page: Int,
            searchQuery: String,
            filter: DataViewDropdownItem?,
            sortOrder: WpApiParamOrder,
            sortBy: DataViewDropdownItem?
        ): List<DataViewItem> {
            fetchDataCallCount++
            return testItems.toList()
        }
        
        fun setTestUiState(state: DataViewUiState) {
            // Can't access private _uiState, so we'll work around this limitation
            when (state) {
                DataViewUiState.LOADED -> {
                    // Trigger a state that will result in LOADED by having items
                    setTestItems(listOf(DataViewItem(1L, null, "Test", emptyList())))
                }
                DataViewUiState.ERROR -> onError("Test error")
                else -> {
                    // For other states, we'll test indirectly
                }
            }
        }
    }
}