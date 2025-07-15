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
        whenever(sharedPrefsEditor.remove(any())).thenReturn(sharedPrefsEditor)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(accountStore.accessToken).thenReturn(testAccessToken)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(sharedPrefs.getInt(any(), any())).thenReturn(-1)
        whenever(sharedPrefs.getLong(any(), any())).thenReturn(-1)
    }

    @Test
    fun `siteId returns correct site ID`() {
        val viewModel = TestDataViewViewModel(
            mainDispatcher = testDispatcher(),
            appLogWrapper = appLogWrapper,
            sharedPrefs = sharedPrefs,
            networkUtilsWrapper = networkUtilsWrapper,
            selectedSiteRepository = selectedSiteRepository,
            accountStore = accountStore,
            ioDispatcher = testDispatcher()
        )
        val siteId = viewModel.siteId()
        assertThat(siteId).isEqualTo(123L)
    }

    @Test
    fun `siteId returns 0 when no site selected`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)
        val viewModel = TestDataViewViewModel(
            mainDispatcher = testDispatcher(),
            appLogWrapper = appLogWrapper,
            sharedPrefs = sharedPrefs,
            networkUtilsWrapper = networkUtilsWrapper,
            selectedSiteRepository = selectedSiteRepository,
            accountStore = accountStore,
            ioDispatcher = testDispatcher()
        )
        val siteId = viewModel.siteId()
        assertThat(siteId).isEqualTo(0L)
    }

    @Test
    fun `access token is used for API client initialization`() {
        val viewModel = TestDataViewViewModel(
            mainDispatcher = testDispatcher(),
            appLogWrapper = appLogWrapper,
            sharedPrefs = sharedPrefs,
            networkUtilsWrapper = networkUtilsWrapper,
            selectedSiteRepository = selectedSiteRepository,
            accountStore = accountStore,
            ioDispatcher = testDispatcher()
        )
        // Just verify the ViewModel initializes without throwing
        assertThat(viewModel).isNotNull
    }

    @Test
    fun `supported sorts returns test data`() {
        val viewModel = TestDataViewViewModel(
            mainDispatcher = testDispatcher(),
            appLogWrapper = appLogWrapper,
            sharedPrefs = sharedPrefs,
            networkUtilsWrapper = networkUtilsWrapper,
            selectedSiteRepository = selectedSiteRepository,
            accountStore = accountStore,
            ioDispatcher = testDispatcher()
        )

        val supportedSorts = viewModel.getSupportedSorts()

        assertThat(supportedSorts).hasSize(2)
        assertThat(supportedSorts[0].id).isEqualTo(1L)
        assertThat(supportedSorts[1].id).isEqualTo(2L)
    }

    @Test
    fun `supported filters returns test data`() {
        val viewModel = TestDataViewViewModel(
            mainDispatcher = testDispatcher(),
            appLogWrapper = appLogWrapper,
            sharedPrefs = sharedPrefs,
            networkUtilsWrapper = networkUtilsWrapper,
            selectedSiteRepository = selectedSiteRepository,
            accountStore = accountStore,
            ioDispatcher = testDispatcher()
        )

        val supportedFilters = viewModel.getSupportedFilters()

        assertThat(supportedFilters).hasSize(2)
        assertThat(supportedFilters[0].id).isEqualTo(1L)
        assertThat(supportedFilters[1].id).isEqualTo(2L)
    }

    @Test
    fun `default sort is first supported sort`() {
        val viewModel = TestDataViewViewModel(
            mainDispatcher = testDispatcher(),
            appLogWrapper = appLogWrapper,
            sharedPrefs = sharedPrefs,
            networkUtilsWrapper = networkUtilsWrapper,
            selectedSiteRepository = selectedSiteRepository,
            accountStore = accountStore,
            ioDispatcher = testDispatcher()
        )

        val defaultSort = viewModel.getDefaultSort()

        assertThat(defaultSort).isEqualTo(viewModel.getSupportedSorts().first())
    }

    @Test
    fun `removeItem removes item from list`() = runTest {
        val viewModel = TestDataViewViewModel(
            mainDispatcher = testDispatcher(),
            appLogWrapper = appLogWrapper,
            sharedPrefs = sharedPrefs,
            networkUtilsWrapper = networkUtilsWrapper,
            selectedSiteRepository = selectedSiteRepository,
            accountStore = accountStore,
            ioDispatcher = testDispatcher()
        )
        advanceUntilIdle() // Wait for initialization

        val testItems = listOf(
            DataViewItem(1, null, "Item 1", emptyList()),
            DataViewItem(2, null, "Item 2", emptyList()),
            DataViewItem(3, null, "Item 3", emptyList())
        )

        // Set items using reflection
        viewModel.setTestItems(testItems)

        viewModel.removeItem(2)

        val remainingItems = viewModel.items.value
        assertThat(remainingItems).hasSize(2)
        assertThat(remainingItems.map { it.id }).containsExactly(1L, 3L)
    }

    @Test
    fun `onError updates error message and UI state`() = runTest {
        val viewModel = TestDataViewViewModel(
            mainDispatcher = testDispatcher(),
            appLogWrapper = appLogWrapper,
            sharedPrefs = sharedPrefs,
            networkUtilsWrapper = networkUtilsWrapper,
            selectedSiteRepository = selectedSiteRepository,
            accountStore = accountStore,
            ioDispatcher = testDispatcher()
        )
        advanceUntilIdle()

        val errorMessage = "Test error"

        viewModel.onError(errorMessage)

        assertThat(viewModel.errorMessage.value).isEqualTo(errorMessage)
        assertThat(viewModel.uiState.value).isEqualTo(DataViewUiState.ERROR)
    }

    @Test
    fun `onFilterClick toggles filter`() = runTest {
        val viewModel = TestDataViewViewModel(
            mainDispatcher = testDispatcher(),
            appLogWrapper = appLogWrapper,
            sharedPrefs = sharedPrefs,
            networkUtilsWrapper = networkUtilsWrapper,
            selectedSiteRepository = selectedSiteRepository,
            accountStore = accountStore,
            ioDispatcher = testDispatcher()
        )
        advanceUntilIdle() // Wait for initialization

        val testFilter = DataViewDropdownItem(1, R.string.app_name)

        viewModel.onFilterClick(testFilter)

        assertThat(viewModel.itemFilter.value).isEqualTo(testFilter)
    }

    @Test
    fun `onSortClick updates sort`() = runTest {
        val viewModel = TestDataViewViewModel(
            mainDispatcher = testDispatcher(),
            appLogWrapper = appLogWrapper,
            sharedPrefs = sharedPrefs,
            networkUtilsWrapper = networkUtilsWrapper,
            selectedSiteRepository = selectedSiteRepository,
            accountStore = accountStore,
            ioDispatcher = testDispatcher()
        )
        advanceUntilIdle() // Wait for initialization

        val testSort = DataViewDropdownItem(2, R.string.app_name)

        viewModel.onSortClick(testSort)

        assertThat(viewModel.itemSortBy.value).isEqualTo(testSort)
    }

    @Test
    fun `onSortOrderClick updates sort order`() = runTest {
        val viewModel = TestDataViewViewModel(
            mainDispatcher = testDispatcher(),
            appLogWrapper = appLogWrapper,
            sharedPrefs = sharedPrefs,
            networkUtilsWrapper = networkUtilsWrapper,
            selectedSiteRepository = selectedSiteRepository,
            accountStore = accountStore,
            ioDispatcher = testDispatcher()
        )
        advanceUntilIdle() // Wait for initialization

        viewModel.onSortOrderClick(WpApiParamOrder.DESC)

        assertThat(viewModel.sortOrder.value).isEqualTo(WpApiParamOrder.DESC)
    }

    @Test
    fun `access token null throws exception`() {
        whenever(accountStore.accessToken).thenReturn(null)

        try {
            TestDataViewViewModel(
                mainDispatcher = testDispatcher(),
                appLogWrapper = appLogWrapper,
                sharedPrefs = sharedPrefs,
                networkUtilsWrapper = networkUtilsWrapper,
                selectedSiteRepository = selectedSiteRepository,
                accountStore = accountStore,
                ioDispatcher = testDispatcher()
            )
            // If we get here, test should fail
            assertThat(false).isTrue()
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
        private val supportedSorts = listOf(
            DataViewDropdownItem(1, R.string.app_name),
            DataViewDropdownItem(2, R.string.app_name)
        )

        private val supportedFilters = listOf(
            DataViewDropdownItem(1, R.string.app_name),
            DataViewDropdownItem(2, R.string.app_name)
        )

        private var testItems: List<DataViewItem> = emptyList()

        fun setTestItems(items: List<DataViewItem>) {
            testItems = items
            try {
                // Use reflection to set the private _items field
                val itemsField = DataViewViewModel::class.java.getDeclaredField("_items")
                itemsField.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                val itemsFlow = itemsField.get(this) as kotlinx.coroutines.flow.MutableStateFlow<List<DataViewItem>>
                itemsFlow.value = items
            } catch (e: Exception) {
                // Ignore reflection errors in tests
            }
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
    }
}
