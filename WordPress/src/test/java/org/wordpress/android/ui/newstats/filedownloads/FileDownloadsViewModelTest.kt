package org.wordpress.android.ui.newstats.filedownloads

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
import org.wordpress.android.ui.newstats.repository.FileDownloadItemData
import org.wordpress.android.ui.newstats.repository.FileDownloadsResult
import org.wordpress.android.ui.newstats.repository.StatsRepository
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class FileDownloadsViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var accountStore: AccountStore

    @Mock
    private lateinit var statsRepository: StatsRepository

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    private lateinit var viewModel: FileDownloadsViewModel

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
        viewModel = FileDownloadsViewModel(
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
        whenever(
            resourceProvider.getString(R.string.stats_error_api)
        ).thenReturn("API error")
        whenever(statsRepository.fetchFileDownloads(any(), any()))
            .thenReturn(
                FileDownloadsResult.Error(R.string.stats_error_api)
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
            whenever(statsRepository.fetchFileDownloads(any(), any()))
                .thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertThat(state)
                .isInstanceOf(MostViewedCardUiState.Loaded::class.java)
        }

    @Test
    fun `when data loads, then items contain correct values`() = test {
        whenever(statsRepository.fetchFileDownloads(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        val state =
            viewModel.uiState.value as MostViewedCardUiState.Loaded
        assertThat(state.items).hasSize(2)
        assertThat(state.items[0].title).isEqualTo(TEST_ITEM_NAME_1)
        assertThat(state.items[0].views)
            .isEqualTo(TEST_ITEM_DOWNLOADS_1)
        assertThat(state.items[1].title).isEqualTo(TEST_ITEM_NAME_2)
        assertThat(state.items[1].views)
            .isEqualTo(TEST_ITEM_DOWNLOADS_2)
    }

    @Test
    fun `when data loads with more than 10 items, then only 10 are shown`() =
        test {
            val manyItems = (1..15).map { index ->
                FileDownloadItemData(
                    name = "File $index",
                    downloads = (100 - index).toLong(),
                    previousDownloads = (90 - index).toLong()
                )
            }
            whenever(statsRepository.fetchFileDownloads(any(), any()))
                .thenReturn(
                    FileDownloadsResult.Success(
                        items = manyItems,
                        totalDownloads = 1000,
                        totalDownloadsChange = 100,
                        totalDownloadsChangePercent = 10.0
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
            whenever(statsRepository.fetchFileDownloads(any(), any()))
                .thenReturn(
                    FileDownloadsResult.Success(
                        items = emptyList(),
                        totalDownloads = 0,
                        totalDownloadsChange = 0,
                        totalDownloadsChangePercent = 0.0
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
        whenever(statsRepository.fetchFileDownloads(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.onPeriodChanged(StatsPeriod.Last30Days)
        advanceUntilIdle()

        verify(statsRepository, times(2))
            .fetchFileDownloads(any(), any())
        verify(statsRepository).fetchFileDownloads(
            eq(TEST_SITE_ID), eq(StatsPeriod.Last30Days)
        )
    }

    @Test
    fun `when same period is selected, then data is not reloaded`() =
        test {
            whenever(statsRepository.fetchFileDownloads(any(), any()))
                .thenReturn(createSuccessResult())

            initViewModel()
            advanceUntilIdle()

            viewModel.onPeriodChanged(StatsPeriod.Last7Days)
            advanceUntilIdle()

            verify(statsRepository, times(1))
                .fetchFileDownloads(any(), any())
        }
    // endregion

    // region Refresh
    @Test
    fun `when refresh is called, then data is fetched`() = test {
        whenever(statsRepository.fetchFileDownloads(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.refresh()
        advanceUntilIdle()

        verify(statsRepository, times(2))
            .fetchFileDownloads(eq(TEST_SITE_ID), any())
    }
    // endregion

    // region Retry
    @Test
    fun `when onRetry is called, then data is reloaded`() = test {
        whenever(statsRepository.fetchFileDownloads(any(), any()))
            .thenReturn(createSuccessResult())

        initViewModel()
        advanceUntilIdle()

        viewModel.onRetry()
        advanceUntilIdle()

        verify(statsRepository, times(2))
            .fetchFileDownloads(any(), any())
    }
    // endregion

    // region getDetailData
    @Test
    fun `when getDetailData is called, then returns cached data`() =
        test {
            whenever(statsRepository.fetchFileDownloads(any(), any()))
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
                .isEqualTo(StatsCardType.FILE_DOWNLOADS)
            assertThat(detailData.items).hasSize(2)
            assertThat(detailData.totalViews)
                .isEqualTo(TEST_TOTAL_DOWNLOADS)
            assertThat(detailData.totalViewsChange)
                .isEqualTo(TEST_TOTAL_DOWNLOADS_CHANGE)
        }
    // endregion

    // region Change calculations
    @Test
    fun `when item has positive change, then Positive is returned`() =
        test {
            val items = listOf(
                FileDownloadItemData(
                    name = "File 1",
                    downloads = 150,
                    previousDownloads = 100
                )
            )
            whenever(statsRepository.fetchFileDownloads(any(), any()))
                .thenReturn(
                    FileDownloadsResult.Success(
                        items = items,
                        totalDownloads = 150,
                        totalDownloadsChange = 50,
                        totalDownloadsChangePercent = 50.0
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
                FileDownloadItemData(
                    name = "File 1",
                    downloads = 50,
                    previousDownloads = 100
                )
            )
            whenever(statsRepository.fetchFileDownloads(any(), any()))
                .thenReturn(
                    FileDownloadsResult.Success(
                        items = items,
                        totalDownloads = 50,
                        totalDownloadsChange = -50,
                        totalDownloadsChangePercent = -50.0
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
            FileDownloadItemData(
                name = "File 1",
                downloads = 100,
                previousDownloads = 100
            )
        )
        whenever(statsRepository.fetchFileDownloads(any(), any()))
            .thenReturn(
                FileDownloadsResult.Success(
                    items = items,
                    totalDownloads = 100,
                    totalDownloadsChange = 0,
                    totalDownloadsChangePercent = 0.0
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
    private fun createSuccessResult() = FileDownloadsResult.Success(
        items = listOf(
            FileDownloadItemData(
                name = TEST_ITEM_NAME_1,
                downloads = TEST_ITEM_DOWNLOADS_1,
                previousDownloads = TEST_ITEM_PREV_DOWNLOADS_1
            ),
            FileDownloadItemData(
                name = TEST_ITEM_NAME_2,
                downloads = TEST_ITEM_DOWNLOADS_2,
                previousDownloads = TEST_ITEM_PREV_DOWNLOADS_2
            )
        ),
        totalDownloads = TEST_TOTAL_DOWNLOADS,
        totalDownloadsChange = TEST_TOTAL_DOWNLOADS_CHANGE,
        totalDownloadsChangePercent = TEST_TOTAL_DOWNLOADS_CHANGE_PERCENT
    )
    // endregion

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val TEST_ACCESS_TOKEN = "test_access_token"

        private const val TEST_ITEM_NAME_1 = "report.pdf"
        private const val TEST_ITEM_NAME_2 = "guide.zip"
        private const val TEST_ITEM_DOWNLOADS_1 = 500L
        private const val TEST_ITEM_DOWNLOADS_2 = 300L
        private const val TEST_ITEM_PREV_DOWNLOADS_1 = 400L
        private const val TEST_ITEM_PREV_DOWNLOADS_2 = 250L

        private const val TEST_TOTAL_DOWNLOADS = 800L
        private const val TEST_TOTAL_DOWNLOADS_CHANGE = 150L
        private const val TEST_TOTAL_DOWNLOADS_CHANGE_PERCENT = 23.1
    }
}
