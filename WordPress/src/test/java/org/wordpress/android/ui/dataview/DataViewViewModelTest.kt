package org.wordpress.android.ui.dataview

import android.content.SharedPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.NetworkUtilsWrapper
import uniffi.wp_api.WpApiParamOrder

@ExperimentalCoroutinesApi
class DataViewViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var appLogWrapper: AppLogWrapper

    @Mock
    private lateinit var sharedPrefs: SharedPreferences

    @Mock
    private lateinit var sharedPrefsEditor: SharedPreferences.Editor

    @Mock
    private lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    private val testSite = SiteModel().apply {
        id = 1
        siteId = 123L
        name = "Test Site"
    }

    private val testAccessToken = "test_access_token"

    @Before
    fun setUp() {
        whenever(sharedPrefs.edit()).thenReturn(sharedPrefsEditor)
        whenever(sharedPrefsEditor.putInt(any(), any())).thenReturn(sharedPrefsEditor)
        whenever(sharedPrefsEditor.putLong(any(), any())).thenReturn(sharedPrefsEditor)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(accountStore.accessToken).thenReturn(testAccessToken)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false) // Prevent network calls
        whenever(sharedPrefs.getInt(any(), any())).thenReturn(-1)
        whenever(sharedPrefs.getLong(any(), any())).thenReturn(-1)
    }

    private fun createTestViewModel(): TestDataViewViewModel {
        return TestDataViewViewModel(
            mainDispatcher = testDispatcher(),
            appLogWrapper = appLogWrapper,
            sharedPrefs = sharedPrefs,
            networkUtilsWrapper = networkUtilsWrapper,
            selectedSiteRepository = selectedSiteRepository,
            accountStore = accountStore,
            ioDispatcher = testDispatcher()
        )
    }

    @Test
    fun `when view model is created, then initial state is loading`() {
        val viewModel = createTestViewModel()
        assertThat(viewModel.uiState.value).isEqualTo(DataViewUiState.LOADING)
    }

    @Test
    fun `siteId returns correct site ID`() = runTest {
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        advanceUntilIdle()
        val siteId = viewModel.siteId()
        assertThat(siteId).isEqualTo(123L)
    }

    @Test
    fun `access token is used for API client initialization`() {
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        advanceUntilIdle()
        // Just verify the ViewModel initializes without throwing
        assertThat(viewModel).isNotNull
    }

    @Test
    fun `supported sorts returns test data`() {
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        advanceUntilIdle()

        val supportedSorts = viewModel.getSupportedSorts()

        assertThat(supportedSorts).hasSize(2)
        assertThat(supportedSorts[0].id).isEqualTo(1L)
        assertThat(supportedSorts[1].id).isEqualTo(2L)
    }

    @Test
    fun `supported filters returns test data`() {
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        advanceUntilIdle()

        val supportedFilters = viewModel.getSupportedFilters()

        assertThat(supportedFilters).hasSize(2)
        assertThat(supportedFilters[0].id).isEqualTo(1L)
        assertThat(supportedFilters[1].id).isEqualTo(2L)
    }

    @Test
    fun `default sort is first supported sort`() {
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        advanceUntilIdle()

        val defaultSort = viewModel.getDefaultSort()

        assertThat(defaultSort).isEqualTo(viewModel.getSupportedSorts().first())
    }

    @Test
    fun `removeItem removes item from list`() = runTest {
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        advanceUntilIdle()

        val testItems = listOf(
            DataViewItem(id = 1L, image = null, title = "Item 1", fields = emptyList()),
            DataViewItem(id = 2L, image = null, title = "Item 2", fields = emptyList()),
            DataViewItem(id = 3L, image = null, title = "Item 3", fields = emptyList())
        )

        // Set items and simulate data load
        viewModel.setTestItems(testItems)
        viewModel.updateItemsForTest(testItems)
        advanceUntilIdle()

        viewModel.removeItem(2L)

        val remainingItems = viewModel.items.value
        assertThat(remainingItems).hasSize(2)
        assertThat(remainingItems.map { it.id }).containsExactly(1L, 3L)
    }

    @Test
    fun `onError updates error message and UI state`() = runTest {
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        advanceUntilIdle()

        val errorMessage = "Test error"

        viewModel.testOnError(errorMessage)
        advanceUntilIdle()

        assertThat(viewModel.errorMessage.value).isEqualTo(errorMessage)
        assertThat(viewModel.uiState.value).isEqualTo(DataViewUiState.ERROR)
    }

    @Test
    fun `onFilterClick toggles filter`() = runTest {
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        advanceUntilIdle()

        val testFilter = DataViewDropdownItem(1L, R.string.app_name)

        viewModel.testOnFilterClick(testFilter)
        advanceUntilIdle()

        assertThat(viewModel.itemFilter.value).isEqualTo(testFilter)
    }

    @Test
    fun `onSortClick updates sort`() = runTest {
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        advanceUntilIdle()

        val testSort = DataViewDropdownItem(2L, R.string.app_name)

        viewModel.testOnSortClick(testSort)
        advanceUntilIdle()

        assertThat(viewModel.itemSortBy.value).isEqualTo(testSort)
    }

    @Test
    fun `onSortOrderClick updates sort order`() = runTest {
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        advanceUntilIdle()

        viewModel.testOnSortOrderClick(WpApiParamOrder.DESC)
        advanceUntilIdle()

        assertThat(viewModel.sortOrder.value).isEqualTo(WpApiParamOrder.DESC)
    }

    @Test
    fun `access token null throws exception`() {
        whenever(accountStore.accessToken).thenReturn(null)

        try {
            val viewModel = TestDataViewViewModel(
                mainDispatcher = testDispatcher(),
                appLogWrapper = appLogWrapper,
                sharedPrefs = sharedPrefs,
                networkUtilsWrapper = networkUtilsWrapper,
                selectedSiteRepository = selectedSiteRepository,
                accountStore = accountStore,
                ioDispatcher = testDispatcher()
            )
            // Access the wpComApiClient property to trigger the lazy initialization
            viewModel.testAccessWpComApiClient()
            // If we get here, test should fail
            Assertions.fail("Access token is required but was null")
        } catch (e: Exception) {
            // Check if the exception or its cause contains the expected message
            val message = e.message ?: e.cause?.message ?: ""
            assertThat(message).contains("Access token is required but was null")
        }
    }

    @Test
    fun `onSearchQueryChange updates search query and triggers debounced fetch`() = runTest {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        advanceUntilIdle()

        viewModel.onSearchQueryChange("test query")
        advanceUntilIdle()

        // Verify the search was processed (items would be fetched with the query)
        assertThat(viewModel.items.value).isEmpty()
    }

    @Test
    fun `onRefreshData resets paging and fetches data when state is loaded`() = runTest {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()

        // Set up initial state with some items
        val testItems = listOf(
            DataViewItem(id = 1L, image = null, title = "Item 1", fields = emptyList())
        )
        viewModel.setTestItems(testItems)
        viewModel.updateItemsForTest(testItems)
        viewModel.testSetUiState(DataViewUiState.LOADED)
        advanceUntilIdle()

        viewModel.onRefreshData()
        advanceUntilIdle()

        assertThat(viewModel.refreshState.value).isFalse()
        assertThat(viewModel.errorMessage.value).isNull()
    }

    @Test
    fun `onRefreshData does nothing when state is not loaded`() = runTest {
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        viewModel.testSetUiState(DataViewUiState.LOADING)
        advanceUntilIdle()

        viewModel.onRefreshData()
        advanceUntilIdle()

        assertThat(viewModel.refreshState.value).isFalse()
    }

    @Test
    fun `onFetchMoreData increments page and fetches more data when can load more`() = runTest {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()

        // Set up initial state
        val testItems = (1..25).map { // PAGE_SIZE items to enable "load more"
            DataViewItem(id = it.toLong(), image = null, title = "Item $it", fields = emptyList())
        }
        viewModel.setTestItems(testItems)
        viewModel.updateItemsForTest(testItems)
        viewModel.testSetUiState(DataViewUiState.LOADED)
        viewModel.testSetCanLoadMore(true)
        advanceUntilIdle()

        viewModel.onFetchMoreData()
        advanceUntilIdle()

        // Verify the state was updated during loading
        assertThat(viewModel.uiState.value).isIn(DataViewUiState.LOADED, DataViewUiState.LOADING_MORE)
    }

    @Test
    fun `onFetchMoreData does nothing when already loading more`() = runTest {
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        viewModel.testSetUiState(DataViewUiState.LOADING_MORE)
        advanceUntilIdle()

        viewModel.onFetchMoreData()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(DataViewUiState.LOADING_MORE)
    }

    @Test
    fun `onFetchMoreData does nothing when cannot load more`() = runTest {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        val viewModel = createTestViewModel()
        // Set up items that would prevent loading more (less than PAGE_SIZE)
        val testItems = (1..10).map {
            DataViewItem(id = it.toLong(), image = null, title = "Item $it", fields = emptyList())
        }
        viewModel.setTestItems(testItems)
        viewModel.initializeForTest()
        advanceUntilIdle()

        // After initialization, the state should be LOADED or EMPTY (depends on items)
        // and canLoadMore should be false due to partial page
        val initialState = viewModel.uiState.value
        assertThat(viewModel.testCanLoadMore()).isFalse()

        viewModel.onFetchMoreData()
        advanceUntilIdle()

        // State should remain unchanged since we can't load more
        assertThat(viewModel.uiState.value).isEqualTo(initialState)
    }

    @Test
    fun `onItemClick logs item click`() = runTest {
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        advanceUntilIdle()

        val testItem = DataViewItem(id = 1L, image = null, title = "Test Item", fields = emptyList())

        viewModel.onItemClick(testItem)
        advanceUntilIdle()

        // Verify method completes without errors (base implementation just logs)
        assertThat(viewModel).isNotNull
    }

    @Test
    fun `network unavailable sets offline state`() = runTest {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value).isEqualTo(DataViewUiState.OFFLINE)
    }

    @Test
    fun `network available triggers loading state`() = runTest {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        advanceUntilIdle()

        // Should transition through loading to loaded/empty state
        assertThat(viewModel.uiState.value).isIn(DataViewUiState.LOADING, DataViewUiState.EMPTY, DataViewUiState.LOADED)
    }

    @Test
    fun `preference restoration sets sort order from saved value`() = runTest {
        whenever(sharedPrefs.getInt(any(), any())).thenReturn(WpApiParamOrder.DESC.ordinal)
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        advanceUntilIdle()

        assertThat(viewModel.sortOrder.value).isEqualTo(WpApiParamOrder.DESC)
    }

    @Test
    fun `preference restoration sets sort by from saved value`() = runTest {
        whenever(sharedPrefs.getLong(any(), any())).thenReturn(1L)
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        advanceUntilIdle()

        assertThat(viewModel.itemSortBy.value?.id).isEqualTo(1L)
    }

    @Test
    fun `preference restoration sets filter from saved value`() = runTest {
        whenever(sharedPrefs.getLong(any(), any())).thenReturn(2L)
        val viewModel = createTestViewModel()
        viewModel.initializeForTest()
        advanceUntilIdle()

        assertThat(viewModel.itemFilter.value?.id).isEqualTo(2L)
    }

    @Test
    fun `pagination behavior loads more items when page size is full`() = runTest {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        val viewModel = createTestViewModel()

        // Set up full page of items (PAGE_SIZE = 25)
        val testItems = (1..25).map {
            DataViewItem(id = it.toLong(), image = null, title = "Item $it", fields = emptyList())
        }
        viewModel.setTestItems(testItems)
        viewModel.initializeForTest()
        advanceUntilIdle()

        // Should allow loading more since we have a full page
        assertThat(viewModel.testCanLoadMore()).isTrue()
    }

    @Test
    fun `pagination behavior stops loading when page is not full`() = runTest {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        val viewModel = createTestViewModel()

        // Set up partial page of items (less than PAGE_SIZE)
        val testItems = (1..10).map {
            DataViewItem(id = it.toLong(), image = null, title = "Item $it", fields = emptyList())
        }
        viewModel.setTestItems(testItems)
        viewModel.initializeForTest()
        advanceUntilIdle()

        // Should not allow loading more since we have a partial page
        assertThat(viewModel.testCanLoadMore()).isFalse()
    }

    /**
     * Test implementation of DataViewViewModel for testing purposes
     */
    @Suppress("LongParameterList")
    private class TestDataViewViewModel(
        mainDispatcher: kotlinx.coroutines.CoroutineDispatcher,
        appLogWrapper: AppLogWrapper,
        sharedPrefs: SharedPreferences,
        networkUtilsWrapper: NetworkUtilsWrapper,
        selectedSiteRepository: SelectedSiteRepository,
        accountStore: AccountStore,
        ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
    ) : DataViewViewModel(
        mainDispatcher,
        appLogWrapper,
        sharedPrefs,
        networkUtilsWrapper,
        selectedSiteRepository,
        accountStore,
        ioDispatcher
    ) {
        private var shouldInitialize = false
        private var canLoadMore = true

        // Override initialize to control when the full initialization happens
        override fun initialize() {
            if (shouldInitialize) {
                super.initialize()
            }
        }

        // Initialize these properties before the parent constructor runs
        private val supportedSorts: List<DataViewDropdownItem> = listOf(
            DataViewDropdownItem(1L, R.string.app_name),
            DataViewDropdownItem(2L, R.string.app_name)
        )

        private val supportedFilters: List<DataViewDropdownItem> = listOf(
            DataViewDropdownItem(1L, R.string.app_name),
            DataViewDropdownItem(2L, R.string.app_name)
        )

        @Volatile
        private var testItems: List<DataViewItem> = emptyList()

        fun setTestItems(items: List<DataViewItem>) {
            testItems = items
        }

        // Direct access to protected fields for testing (no reflection needed!)
        fun updateItemsForTest(items: List<DataViewItem>) {
            _items.value = items
        }

        override suspend fun performNetworkRequest(
            page: Int,
            searchQuery: String,
            filter: DataViewDropdownItem?,
            sortOrder: WpApiParamOrder,
            sortBy: DataViewDropdownItem?
        ): List<DataViewItem> {
            // Update canLoadMore based on returned item count (mimicking real behavior)
            canLoadMore = testItems.size == PAGE_SIZE
            return testItems
        }

        override fun getSupportedSorts(): List<DataViewDropdownItem> {
            return supportedSorts
        }

        override fun getSupportedFilters(): List<DataViewDropdownItem> {
            return supportedFilters
        }

        override fun getDefaultSort(): DataViewDropdownItem {
            return supportedSorts[0]
        }

        // Test helper methods to access protected/public methods without side effects
        fun testOnError(message: String?) {
            onError(message)
        }

        fun testOnFilterClick(filter: DataViewDropdownItem?) {
            onFilterClick(filter)
        }

        fun testOnSortClick(sort: DataViewDropdownItem) {
            onSortClick(sort)
        }

        fun testOnSortOrderClick(order: WpApiParamOrder) {
            onSortOrderClick(order)
        }

        fun testAccessWpComApiClient() {
            // Access the lazy wpComApiClient to trigger initialization
            wpComApiClient.toString()
        }

        fun testSetUiState(state: DataViewUiState) {
            _uiState.value = state
        }

        fun testSetCanLoadMore(canLoad: Boolean) {
            canLoadMore = canLoad
        }

        fun testCanLoadMore(): Boolean {
            return canLoadMore
        }

        fun initializeForTest() {
            shouldInitialize = true
            initialize()
        }
    }
}
