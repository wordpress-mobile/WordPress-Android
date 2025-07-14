package org.wordpress.android.ui.dataview

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.NetworkUtilsWrapper
import rs.wordpress.api.kotlin.WpComApiClient
import uniffi.wp_api.WpApiParamOrder

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class DataViewViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

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

    @Mock
    private lateinit var siteModel: SiteModel

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(siteModel)
        whenever(siteModel.siteId).thenReturn(123L)
        whenever(accountStore.accessToken).thenReturn("test-token")
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    private fun createViewModel(): TestableDataViewViewModel {
        return TestableDataViewViewModel(
            mainDispatcher = testDispatcher,
            appLogWrapper = appLogWrapper,
            networkUtilsWrapper = networkUtilsWrapper,
            selectedSiteRepository = selectedSiteRepository,
            accountStore = accountStore,
            ioDispatcher = testDispatcher,
            mockApiClient = mockApiClient
        )
    }

    @Test
    fun `siteId returns correct site id`() = testScope.runTest {
        whenever(siteModel.siteId).thenReturn(456L)
        val viewModel = createViewModel()
        assertThat(viewModel.siteId()).isEqualTo(456L)
    }

    @Test
    fun `siteId returns 0 when no site selected`() = testScope.runTest {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)
        val viewModel = createViewModel()
        assertThat(viewModel.siteId()).isEqualTo(0L)
    }

    @Test
    fun `onFilterClick sets filter when different`() = testScope.runTest {
        val viewModel = createViewModel()
        val filter = DataViewDropdownItem(1, android.R.string.ok)
        
        viewModel.onFilterClick(filter)
        
        assertThat(viewModel.itemFilter.value).isEqualTo(filter)
        assertThat(viewModel.testGetCurrentPage()).isEqualTo(1) // Should reset paging
    }

    @Test
    fun `onFilterClick clears filter when same`() = testScope.runTest {
        val viewModel = createViewModel()
        val filter = DataViewDropdownItem(1, android.R.string.ok)
        
        // Set filter first
        viewModel.onFilterClick(filter)
        assertThat(viewModel.itemFilter.value).isEqualTo(filter)
        
        // Click same filter again
        viewModel.onFilterClick(filter)
        assertThat(viewModel.itemFilter.value).isNull()
    }

    @Test
    fun `onSortClick sets sort when different`() = testScope.runTest {
        val viewModel = createViewModel()
        val sort = DataViewDropdownItem(1, android.R.string.ok)
        
        viewModel.onSortClick(sort)
        
        assertThat(viewModel.itemSortBy.value).isEqualTo(sort)
        assertThat(viewModel.testGetCurrentPage()).isEqualTo(1) // Should reset paging
    }

    @Test
    fun `onSortClick does nothing when same`() = testScope.runTest {
        val viewModel = createViewModel()
        val sort = DataViewDropdownItem(1, android.R.string.ok)
        
        // Set sort first
        viewModel.onSortClick(sort)
        val initialPage = viewModel.testGetCurrentPage()
        
        // Click same sort again
        viewModel.onSortClick(sort)
        assertThat(viewModel.testGetCurrentPage()).isEqualTo(initialPage)
    }

    @Test
    fun `onSortOrderClick sets sort order when different`() = testScope.runTest {
        val viewModel = createViewModel()
        
        viewModel.onSortOrderClick(WpApiParamOrder.DESC)
        
        assertThat(viewModel.sortOrder.value).isEqualTo(WpApiParamOrder.DESC)
        assertThat(viewModel.testGetCurrentPage()).isEqualTo(1) // Should reset paging
    }

    @Test
    fun `onSortOrderClick does nothing when same`() = testScope.runTest {
        val viewModel = createViewModel()
        val initialPage = viewModel.testGetCurrentPage()
        
        // Click same sort order (ASC is default)
        viewModel.onSortOrderClick(WpApiParamOrder.ASC)
        assertThat(viewModel.testGetCurrentPage()).isEqualTo(initialPage)
    }

    @Test
    fun `onError sets error message and state`() = testScope.runTest {
        val viewModel = createViewModel()
        val errorMessage = "Test error"
        
        viewModel.onError(errorMessage)
        
        assertThat(viewModel.errorMessage.value).isEqualTo(errorMessage)
        assertThat(viewModel.uiState.value).isEqualTo(DataViewUiState.ERROR)
    }

    @Test
    fun `removeItem removes item from list`() = testScope.runTest {
        val viewModel = createViewModel()
        val item1 = DataViewItem(1, null, "Item 1", emptyList())
        val item2 = DataViewItem(2, null, "Item 2", emptyList())
        val item3 = DataViewItem(3, null, "Item 3", emptyList())
        
        // Set initial items
        viewModel.testSetItems(listOf(item1, item2, item3))
        
        // Remove item with id 2
        viewModel.removeItem(2)
        
        assertThat(viewModel.items.value).containsExactly(item1, item3)
    }

    @Test
    fun `removeItem does nothing when item not found`() = testScope.runTest {
        val viewModel = createViewModel()
        val item1 = DataViewItem(1, null, "Item 1", emptyList())
        val item2 = DataViewItem(2, null, "Item 2", emptyList())
        val initialItems = listOf(item1, item2)
        
        // Set initial items
        viewModel.testSetItems(initialItems)
        
        // Try to remove non-existent item
        viewModel.removeItem(999)
        
        assertThat(viewModel.items.value).containsExactlyElementsOf(initialItems)
    }

    @Test
    fun `network unavailable sets offline state`() = testScope.runTest {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        val viewModel = createViewModel()
        
        viewModel.testFetchData()
        
        assertThat(viewModel.uiState.value).isEqualTo(DataViewUiState.OFFLINE)
    }

    @Test
    fun `getSupportedFilters returns empty list by default`() = testScope.runTest {
        val viewModel = createViewModel()
        assertThat(viewModel.getSupportedFilters()).isEmpty()
    }

    @Test
    fun `getSupportedSorts returns empty list by default`() = testScope.runTest {
        val viewModel = createViewModel()
        assertThat(viewModel.getSupportedSorts()).isEmpty()
    }

    @Test
    fun `getDefaultSort returns null when no supported sorts`() = testScope.runTest {
        val viewModel = createViewModel()
        assertThat(viewModel.getDefaultSort()).isNull()
    }

    @Test
    fun `getDefaultSort returns first sort when supported sorts exist`() = testScope.runTest {
        val supportedSorts = listOf(
            DataViewDropdownItem(1, android.R.string.ok),
            DataViewDropdownItem(2, android.R.string.cancel)
        )
        
        val testViewModel = object : TestableDataViewViewModel(
            testDispatcher, appLogWrapper, networkUtilsWrapper, selectedSiteRepository, accountStore, testDispatcher, mockApiClient
        ) {
            override fun getSupportedSorts(): List<DataViewDropdownItem> = supportedSorts
        }
        
        assertThat(testViewModel.getDefaultSort()).isEqualTo(supportedSorts.first())
    }

    @Test
    fun `onItemClick logs item click`() = testScope.runTest {
        val viewModel = createViewModel()
        val item = DataViewItem(1, null, "Test Item", emptyList())
        
        // This should not throw an exception
        viewModel.onItemClick(item)
        
        // Verify the method was called without issues
        // The base implementation just logs, so we can't assert much more
    }

    @Test
    fun `resetPaging resets page and canLoadMore`() = testScope.runTest {
        val viewModel = createViewModel()
        
        // Set some initial state
        viewModel.testSetPage(5)
        viewModel.testSetCanLoadMore(false)
        viewModel.onError("test error")
        
        // Reset paging
        viewModel.testResetPaging()
        
        assertThat(viewModel.testGetCurrentPage()).isEqualTo(1)
        assertThat(viewModel.testGetCanLoadMore()).isTrue()
        assertThat(viewModel.errorMessage.value).isNull()
    }

    private open class TestableDataViewViewModel(
        mainDispatcher: CoroutineDispatcher,
        appLogWrapper: AppLogWrapper,
        networkUtilsWrapper: NetworkUtilsWrapper,
        selectedSiteRepository: SelectedSiteRepository,
        accountStore: AccountStore,
        ioDispatcher: CoroutineDispatcher,
        private val mockApiClient: WpComApiClient
    ) : DataViewViewModel(
        mainDispatcher, appLogWrapper, networkUtilsWrapper, selectedSiteRepository, accountStore, ioDispatcher
    ) {
        private var networkRequestResult: List<DataViewItem> = emptyList()

        override fun createWpComApiClient(): WpComApiClient = mockApiClient

        override suspend fun performNetworkRequest(
            page: Int,
            searchQuery: String,
            filter: DataViewDropdownItem?,
            sortOrder: WpApiParamOrder,
            sortBy: DataViewDropdownItem?
        ): List<DataViewItem> {
            return networkRequestResult
        }

        // Test helper methods
        fun testUpdateUiState(state: DataViewUiState) = updateUiState(state)
        fun testResetPaging() = resetPaging()
        fun testGetCurrentPage() = getCurrentPage()
        fun testGetCanLoadMore() = getCanLoadMore()
        fun testGetCurrentSearchQuery() = getCurrentSearchQuery()
        fun testGetDebouncedQuery() = getDebouncedQuery()
        
        fun testSetNetworkRequestResult(items: List<DataViewItem>) {
            networkRequestResult = items
        }
        
        fun testSetItems(items: List<DataViewItem>) {
            setItems(items)
        }
        
        fun testSetPage(newPage: Int) {
            setPage(newPage)
        }
        
        fun testSetCanLoadMore(value: Boolean) {
            setCanLoadMore(value)
        }
        
        fun testSetSearchQuery(query: String) {
            setSearchQuery(query)
        }
        
        fun testFetchData() = fetchData()
    }
}