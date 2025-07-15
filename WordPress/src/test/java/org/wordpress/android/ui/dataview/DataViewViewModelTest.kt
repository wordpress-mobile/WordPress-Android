package org.wordpress.android.ui.dataview

import android.content.SharedPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
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
import kotlin.test.DefaultAsserter.fail

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
            fail("Access token is required but was null")
        } catch (e: Exception) {
            // Check if the exception or its cause contains the expected message
            val message = e.message ?: e.cause?.message ?: ""
            assertThat(message).contains("Access token is required but was null")
        }
    }

    /**
     * Test implementation of DataViewViewModel for testing purposes
     */
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

        fun initializeForTest() {
            shouldInitialize = true
            initialize()
        }
    }
}
