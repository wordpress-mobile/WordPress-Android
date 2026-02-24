package org.wordpress.android.ui.newstats.clicks

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.StatsCardType
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.mostviewed.MostViewedCardUiState
import org.wordpress.android.ui.newstats.mostviewed.MostViewedChange
import org.wordpress.android.ui.newstats.repository.ClickItemData
import org.wordpress.android.ui.newstats.repository.ClicksResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class ClicksViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: ClicksViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite())
            .thenReturn(testSite)
        whenever(accountStore.accessToken).thenReturn(TEST_ACCESS_TOKEN)
        whenever(resourceProvider.getString(R.string.stats_error_no_site))
            .thenReturn("No site selected")
        whenever(resourceProvider.getString(R.string.stats_error_api))
            .thenReturn("API error")
    }

    private fun initViewModel() {
        viewModel = ClicksViewModel(
            selectedSiteRepository,
            accountStore,
            statsRepository,
            resourceProvider
        )
        viewModel.onPeriodChanged(StatsPeriod.Last7Days)
    }

    // region Error states
    @Test
    fun `when no site selected, then error state is emitted`() = test {
        whenever(selectedSiteRepository.getSelectedSite())
            .thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state)
            .isInstanceOf(MostViewedCardUiState.Error::class.java)
    }

    @Test
    fun `when no access token, then error state is emitted`() = test {
        whenever(accountStore.accessToken).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state)
            .isInstanceOf(MostViewedCardUiState.Error::class.java)
    }

    @Test
    fun `when fetch fails, then error state is emitted`() = test {
        whenever(resourceProvider.getString(R.string.stats_error_api))
            .thenReturn("API error")
        whenever(statsRepository.fetchClicks(any(), any()))
            .thenReturn(
                ClicksResult.Error(R.string.stats_error_api)
            )

        initViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state)
            .isInstanceOf(MostViewedCardUiState.Error::class.java)
        assertThat((state as MostViewedCardUiState.Error).message)
            .isEqualTo("API error")
    }
    // endregion

    // region Success states
    @Test
    fun `when data loads successfully, then loaded state is emitted`() =
        test {
            whenever(statsRepository.fetchClicks(any(), any()))
                .thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state)
                .isInstanceOf(MostViewedCardUiState.Loaded::class.java)
        }

    @Test
    fun `when data loads, then items contain correct values`() = test {
        whenever(statsRepository.fetchClicks(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        val state =
            viewModel.uiState.value as MostViewedCardUiState.Loaded
        assertThat(state.items).hasSize(2)
        assertThat(state.items[0].title).isEqualTo(TEST_ITEM_NAME_1)
        assertThat(state.items[0].views).isEqualTo(TEST_ITEM_CLICKS_1)
        assertThat(state.items[1].title).isEqualTo(TEST_ITEM_NAME_2)
        assertThat(state.items[1].views).isEqualTo(TEST_ITEM_CLICKS_2)
    }

    @Test
    fun `when data loads, then maxViewsForBar is set to first item value`() =
        test {
            whenever(statsRepository.fetchClicks(any(), any()))
                .thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            val state =
                viewModel.uiState.value as MostViewedCardUiState.Loaded
            assertThat(state.maxViewsForBar)
                .isEqualTo(TEST_ITEM_CLICKS_1)
        }

    @Test
    fun `when data loads with more than 10 items, then only 10 are shown`() =
        test {
            val manyItems = (1..15).map { index ->
                ClickItemData(
                    name = "Link $index",
                    clicks = (100 - index).toLong(),
                    previousClicks = (90 - index).toLong()
                )
            }
            whenever(statsRepository.fetchClicks(any(), any()))
                .thenReturn(
                    ClicksResult.Success(
                        items = manyItems,
                        totalClicks = 1000,
                        totalClicksChange = 100,
                        totalClicksChangePercent = 10.0
                    )
                )

            initViewModel()
            advanceUntilIdle()

            val state =
                viewModel.uiState.value as MostViewedCardUiState.Loaded
            assertThat(state.items).hasSize(10)
        }

    @Test
    fun `when data loads with empty items, then loaded state with empty list`() =
        test {
            whenever(statsRepository.fetchClicks(any(), any()))
                .thenReturn(
                    ClicksResult.Success(
                        items = emptyList(),
                        totalClicks = 0,
                        totalClicksChange = 0,
                        totalClicksChangePercent = 0.0
                    )
                )

            initViewModel()
            advanceUntilIdle()

            val state =
                viewModel.uiState.value as MostViewedCardUiState.Loaded
            assertThat(state.items).isEmpty()
            assertThat(state.maxViewsForBar).isEqualTo(0L)
        }
    // endregion

    // region Period changes
    @Test
    fun `when period changes, then data is reloaded`() = test {
        whenever(statsRepository.fetchClicks(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.onPeriodChanged(StatsPeriod.Last30Days)
        advanceUntilIdle()

        verify(statsRepository, times(2))
            .fetchClicks(any(), any())
        verify(statsRepository)
            .fetchClicks(eq(TEST_SITE_ID), eq(StatsPeriod.Last30Days))
    }

    @Test
    fun `when same period is selected, then data is not reloaded`() =
        test {
            whenever(statsRepository.fetchClicks(any(), any()))
                .thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            viewModel.onPeriodChanged(StatsPeriod.Last7Days)
            advanceUntilIdle()

            verify(statsRepository, times(1))
                .fetchClicks(any(), any())
        }
    // endregion

    // region Refresh
    @Test
    fun `when refresh is called, then isRefreshing becomes true then false`() =
        test {
            whenever(statsRepository.fetchClicks(any(), any()))
                .thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            assertThat(viewModel.isRefreshing.value).isFalse()

            viewModel.refresh()
            advanceUntilIdle()

            assertThat(viewModel.isRefreshing.value).isFalse()
        }

    @Test
    fun `when refresh is called, then data is fetched`() = test {
        whenever(statsRepository.fetchClicks(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        verify(statsRepository, times(2))
            .fetchClicks(eq(TEST_SITE_ID), any())
    }
    // endregion

    // region Retry
    @Test
    fun `when onRetry is called, then data is reloaded`() = test {
        whenever(statsRepository.fetchClicks(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.onRetry()
        advanceUntilIdle()

        verify(statsRepository, times(2))
            .fetchClicks(any(), any())
    }
    // endregion

    // region getDetailData
    @Test
    fun `when getDetailData is called, then returns cached data`() =
        test {
            whenever(statsRepository.fetchClicks(any(), any()))
                .thenReturn(createSuccessResult())
            whenever(
                resourceProvider.getString(
                    R.string.stats_period_last_7_days
                )
            ).thenReturn("Last 7 days")

            initViewModel()
            advanceUntilIdle()

            val detailData = viewModel.getDetailData()

            assertThat(detailData.cardType)
                .isEqualTo(StatsCardType.CLICKS)
            assertThat(detailData.items).hasSize(2)
            assertThat(detailData.totalViews)
                .isEqualTo(TEST_TOTAL_CLICKS)
            assertThat(detailData.totalViewsChange)
                .isEqualTo(TEST_TOTAL_CLICKS_CHANGE)
            assertThat(detailData.totalViewsChangePercent)
                .isEqualTo(TEST_TOTAL_CLICKS_CHANGE_PERCENT)
        }

    @Test
    fun `when getDetailData called, then all items returned not just card items`() =
        test {
            val manyItems = (1..15).map { index ->
                ClickItemData(
                    name = "Link $index",
                    clicks = (100 - index).toLong(),
                    previousClicks = (90 - index).toLong()
                )
            }
            whenever(
                resourceProvider.getString(
                    R.string.stats_period_last_7_days
                )
            ).thenReturn("Last 7 days")
            whenever(statsRepository.fetchClicks(any(), any()))
                .thenReturn(
                    ClicksResult.Success(
                        items = manyItems,
                        totalClicks = 1000,
                        totalClicksChange = 100,
                        totalClicksChangePercent = 10.0
                    )
                )

            initViewModel()
            advanceUntilIdle()

            val detailData = viewModel.getDetailData()
            assertThat(detailData.items).hasSize(15)
        }
    // endregion

    // region Change calculations
    @Test
    fun `when item has positive change, then Positive is returned`() =
        test {
            val items = listOf(
                ClickItemData(
                    name = "Link 1",
                    clicks = 150,
                    previousClicks = 100
                )
            )
            whenever(statsRepository.fetchClicks(any(), any()))
                .thenReturn(
                    ClicksResult.Success(
                        items = items,
                        totalClicks = 150,
                        totalClicksChange = 50,
                        totalClicksChangePercent = 50.0
                    )
                )

            initViewModel()
            advanceUntilIdle()

            val state =
                viewModel.uiState.value as MostViewedCardUiState.Loaded
            assertThat(state.items[0].change)
                .isInstanceOf(MostViewedChange.Positive::class.java)
            val change =
                state.items[0].change as MostViewedChange.Positive
            assertThat(change.value).isEqualTo(50)
            assertThat(change.percentage).isEqualTo(50.0)
        }

    @Test
    fun `when item has negative change, then Negative is returned`() =
        test {
            val items = listOf(
                ClickItemData(
                    name = "Link 1",
                    clicks = 50,
                    previousClicks = 100
                )
            )
            whenever(statsRepository.fetchClicks(any(), any()))
                .thenReturn(
                    ClicksResult.Success(
                        items = items,
                        totalClicks = 50,
                        totalClicksChange = -50,
                        totalClicksChangePercent = -50.0
                    )
                )

            initViewModel()
            advanceUntilIdle()

            val state =
                viewModel.uiState.value as MostViewedCardUiState.Loaded
            assertThat(state.items[0].change)
                .isInstanceOf(MostViewedChange.Negative::class.java)
            val change =
                state.items[0].change as MostViewedChange.Negative
            assertThat(change.value).isEqualTo(50)
            assertThat(change.percentage).isEqualTo(50.0)
        }

    @Test
    fun `when item has no change, then NoChange is returned`() = test {
        val items = listOf(
            ClickItemData(
                name = "Link 1",
                clicks = 100,
                previousClicks = 100
            )
        )
        whenever(statsRepository.fetchClicks(any(), any()))
            .thenReturn(
                ClicksResult.Success(
                    items = items,
                    totalClicks = 100,
                    totalClicksChange = 0,
                    totalClicksChangePercent = 0.0
                )
            )

        initViewModel()
        advanceUntilIdle()

        val state =
            viewModel.uiState.value as MostViewedCardUiState.Loaded
        assertThat(state.items[0].change)
            .isEqualTo(MostViewedChange.NoChange)
    }
    // endregion

    // region Helper functions
    private fun createSuccessResult() = ClicksResult.Success(
        items = listOf(
            ClickItemData(
                name = TEST_ITEM_NAME_1,
                clicks = TEST_ITEM_CLICKS_1,
                previousClicks = TEST_ITEM_PREV_CLICKS_1
            ),
            ClickItemData(
                name = TEST_ITEM_NAME_2,
                clicks = TEST_ITEM_CLICKS_2,
                previousClicks = TEST_ITEM_PREV_CLICKS_2
            )
        ),
        totalClicks = TEST_TOTAL_CLICKS,
        totalClicksChange = TEST_TOTAL_CLICKS_CHANGE,
        totalClicksChangePercent = TEST_TOTAL_CLICKS_CHANGE_PERCENT
    )
    // endregion

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"

        private const val TEST_ITEM_NAME_1 = "example.com"
        private const val TEST_ITEM_NAME_2 = "wordpress.org"
        private const val TEST_ITEM_CLICKS_1 = 500L
        private const val TEST_ITEM_CLICKS_2 = 300L
        private const val TEST_ITEM_PREV_CLICKS_1 = 400L
        private const val TEST_ITEM_PREV_CLICKS_2 = 250L

        private const val TEST_TOTAL_CLICKS = 800L
        private const val TEST_TOTAL_CLICKS_CHANGE = 150L
        private const val TEST_TOTAL_CLICKS_CHANGE_PERCENT = 23.1
    }
}
