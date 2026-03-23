package org.wordpress.android.ui.newstats

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.datasource.StatsInsightsData
import org.wordpress.android.ui.newstats.datasource.StatsSummaryData
import org.wordpress.android.ui.newstats.repository.InsightsCardsConfigurationRepository
import org.wordpress.android.ui.newstats.repository.InsightsResult
import org.wordpress.android.ui.newstats.repository.StatsSummaryResult
import org.wordpress.android.ui.newstats.repository.StatsSummaryUseCase
import org.wordpress.android.ui.newstats.repository.StatsInsightsUseCase
import org.wordpress.android.ui.newstats.repository.StatsTagsUseCase
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@Suppress("LargeClass")
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner.Silent::class)
class InsightsViewModelTest :
    BaseUnitTest(StandardTestDispatcher()) {
    @Mock
    private lateinit var selectedSiteRepository:
        SelectedSiteRepository

    @Mock
    private lateinit var cardConfigurationRepository:
        InsightsCardsConfigurationRepository

    @Mock
    private lateinit var networkUtilsWrapper:
        NetworkUtilsWrapper

    @Mock
    private lateinit var statsSummaryUseCase:
        StatsSummaryUseCase

    @Mock
    private lateinit var statsInsightsUseCase:
        StatsInsightsUseCase

    @Mock
    private lateinit var statsTagsUseCase:
        StatsTagsUseCase

    @Mock
    private lateinit var resourceProvider:
        ResourceProvider

    private lateinit var viewModel: InsightsViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    private val configurationFlow =
        MutableStateFlow<
            Pair<Long, InsightsCardsConfiguration>?
        >(null)

    @Before
    fun setUp() {
        whenever(
            selectedSiteRepository.getSelectedSite()
        ).thenReturn(testSite)
        whenever(
            cardConfigurationRepository.configurationFlow
        ).thenReturn(configurationFlow)
        whenever(
            resourceProvider.getString(
                R.string.stats_error_unknown
            )
        ).thenReturn("An unexpected error occurred")
        whenever(
            networkUtilsWrapper.isNetworkAvailable()
        ).thenReturn(true)
    }

    private suspend fun initViewModel(
        config: InsightsCardsConfiguration =
            InsightsCardsConfiguration()
    ) {
        whenever(
            cardConfigurationRepository
                .getConfiguration(TEST_SITE_ID)
        ).thenReturn(config)
        viewModel = InsightsViewModel(
            selectedSiteRepository,
            cardConfigurationRepository,
            networkUtilsWrapper,
            statsSummaryUseCase,
            statsInsightsUseCase,
            statsTagsUseCase,
            resourceProvider
        )
    }

    @Test
    fun `when initialized with default config, then default cards are visible`() =
        test {
            initViewModel()
            advanceUntilIdle()

            assertThat(viewModel.visibleCards.value)
                .isEqualTo(
                    InsightsCardType.defaultCards()
                )
        }

    @Test
    fun `when initialized with custom config, then custom cards are visible`() =
        test {
            val customConfig =
                InsightsCardsConfiguration(
                    visibleCards = listOf(
                        InsightsCardType.YEAR_IN_REVIEW
                    )
                )
            initViewModel(customConfig)
            advanceUntilIdle()

            assertThat(viewModel.visibleCards.value)
                .containsExactly(
                    InsightsCardType.YEAR_IN_REVIEW
                )
        }

    @Test
    fun `when removeCard is called, then repository removeCard is invoked`() =
        test {
            initViewModel()
            advanceUntilIdle()

            viewModel.removeCard(
                InsightsCardType.YEAR_IN_REVIEW
            )
            advanceUntilIdle()

            verify(cardConfigurationRepository)
                .removeCard(
                    TEST_SITE_ID,
                    InsightsCardType.YEAR_IN_REVIEW
                )
        }

    @Test
    fun `when addCard is called, then repository addCard is invoked`() =
        test {
            val config = InsightsCardsConfiguration(
                visibleCards = emptyList()
            )
            initViewModel(config)
            advanceUntilIdle()

            viewModel.addCard(
                InsightsCardType.YEAR_IN_REVIEW
            )
            advanceUntilIdle()

            verify(cardConfigurationRepository)
                .addCard(
                    TEST_SITE_ID,
                    InsightsCardType.YEAR_IN_REVIEW
                )
        }

    @Test
    fun `when configuration changes via flow, then state is updated`() =
        test {
            initViewModel(
                InsightsCardsConfiguration(
                    visibleCards = emptyList()
                )
            )
            advanceUntilIdle()

            val newConfig = InsightsCardsConfiguration(
                visibleCards = listOf(
                    InsightsCardType.YEAR_IN_REVIEW
                )
            )
            configurationFlow.value =
                TEST_SITE_ID to newConfig
            advanceUntilIdle()

            assertThat(viewModel.visibleCards.value)
                .containsExactly(
                    InsightsCardType.YEAR_IN_REVIEW
                )
        }

    @Test
    fun `when configuration changes for different site, then state is not updated`() =
        test {
            initViewModel()
            advanceUntilIdle()
            val initialCards =
                viewModel.visibleCards.value

            val newConfig = InsightsCardsConfiguration(
                visibleCards = emptyList()
            )
            configurationFlow.value =
                OTHER_SITE_ID to newConfig
            advanceUntilIdle()

            assertThat(viewModel.visibleCards.value)
                .isEqualTo(initialCards)
        }

    @Test
    fun `when hiddenCards is calculated, then it excludes visible cards`() =
        test {
            val config = InsightsCardsConfiguration(
                visibleCards = listOf(
                    InsightsCardType.YEAR_IN_REVIEW
                )
            )
            initViewModel(config)
            advanceUntilIdle()

            val hiddenCards =
                viewModel.hiddenCards.value

            assertThat(hiddenCards).doesNotContain(
                InsightsCardType.YEAR_IN_REVIEW
            )
        }

    @Test
    fun `when no site selected, then config is not loaded`() =
        test {
            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(null)

            viewModel = InsightsViewModel(
                selectedSiteRepository,
                cardConfigurationRepository,
                networkUtilsWrapper,
                statsSummaryUseCase,
                statsInsightsUseCase,
                statsTagsUseCase,
                resourceProvider
            )
            advanceUntilIdle()

            verify(
                cardConfigurationRepository,
                never()
            ).getConfiguration(any())
        }

    @Test
    fun `when no site selected, then removeCard is no-op`() =
        test {
            initViewModel()
            advanceUntilIdle()

            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(null)
            viewModel.removeCard(
                InsightsCardType.YEAR_IN_REVIEW
            )
            advanceUntilIdle()

            verify(
                cardConfigurationRepository,
                never()
            ).removeCard(any(), any())
        }

    @Test
    fun `when moveCardUp is called, then repository moveCardUp is invoked`() =
        test {
            initViewModel()
            advanceUntilIdle()

            viewModel.moveCardUp(
                InsightsCardType.YEAR_IN_REVIEW
            )
            advanceUntilIdle()

            verify(cardConfigurationRepository)
                .moveCardUp(
                    TEST_SITE_ID,
                    InsightsCardType.YEAR_IN_REVIEW
                )
        }

    @Test
    fun `when moveCardToTop is called, then repository moveCardToTop is invoked`() =
        test {
            initViewModel()
            advanceUntilIdle()

            viewModel.moveCardToTop(
                InsightsCardType.YEAR_IN_REVIEW
            )
            advanceUntilIdle()

            verify(cardConfigurationRepository)
                .moveCardToTop(
                    TEST_SITE_ID,
                    InsightsCardType.YEAR_IN_REVIEW
                )
        }

    @Test
    fun `when moveCardDown is called, then repository moveCardDown is invoked`() =
        test {
            initViewModel()
            advanceUntilIdle()

            viewModel.moveCardDown(
                InsightsCardType.YEAR_IN_REVIEW
            )
            advanceUntilIdle()

            verify(cardConfigurationRepository)
                .moveCardDown(
                    TEST_SITE_ID,
                    InsightsCardType.YEAR_IN_REVIEW
                )
        }

    @Test
    fun `when moveCardToBottom is called, then repository moveCardToBottom is invoked`() =
        test {
            initViewModel()
            advanceUntilIdle()

            viewModel.moveCardToBottom(
                InsightsCardType.YEAR_IN_REVIEW
            )
            advanceUntilIdle()

            verify(cardConfigurationRepository)
                .moveCardToBottom(
                    TEST_SITE_ID,
                    InsightsCardType.YEAR_IN_REVIEW
                )
        }

    @Test
    fun `when ViewModel is created, then cardsToLoad starts empty`() =
        test {
            whenever(
                cardConfigurationRepository
                    .getConfiguration(TEST_SITE_ID)
            ).thenReturn(InsightsCardsConfiguration())

            viewModel = InsightsViewModel(
                selectedSiteRepository,
                cardConfigurationRepository,
                networkUtilsWrapper,
                statsSummaryUseCase,
                statsInsightsUseCase,
                statsTagsUseCase,
                resourceProvider
            )

            assertThat(viewModel.cardsToLoad.value)
                .isEmpty()
        }

    @Test
    fun `when config loads, then cardsToLoad matches visible cards`() =
        test {
            val config = InsightsCardsConfiguration(
                visibleCards = listOf(
                    InsightsCardType.YEAR_IN_REVIEW
                )
            )
            initViewModel(config)
            advanceUntilIdle()

            assertThat(viewModel.cardsToLoad.value)
                .containsExactly(
                    InsightsCardType.YEAR_IN_REVIEW
                )
        }

    @Test
    fun `when initialized with network available, then isNetworkAvailable is true`() =
        test {
            whenever(
                networkUtilsWrapper.isNetworkAvailable()
            ).thenReturn(true)

            initViewModel()
            advanceUntilIdle()

            assertThat(
                viewModel.isNetworkAvailable.value
            ).isTrue()
        }

    @Test
    fun `when initialized without network, then isNetworkAvailable is false`() =
        test {
            whenever(
                networkUtilsWrapper.isNetworkAvailable()
            ).thenReturn(false)

            initViewModel()
            advanceUntilIdle()

            assertThat(
                viewModel.isNetworkAvailable.value
            ).isFalse()
        }

    @Test
    fun `when checkNetworkStatus is called, then network status is updated`() =
        test {
            whenever(
                networkUtilsWrapper.isNetworkAvailable()
            ).thenReturn(false)
            initViewModel()
            advanceUntilIdle()

            assertThat(
                viewModel.isNetworkAvailable.value
            ).isFalse()

            whenever(
                networkUtilsWrapper.isNetworkAvailable()
            ).thenReturn(true)
            viewModel.checkNetworkStatus()

            assertThat(
                viewModel.isNetworkAvailable.value
            ).isTrue()
        }

    // region Data fetching tests

    @Test
    fun `when loadDataIfNeeded called, then both use cases are invoked`() =
        test {
            whenever(
                statsSummaryUseCase(any(), any())
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestSummaryData()
                )
            )
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            initViewModel()
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            verify(statsSummaryUseCase)
                .invoke(eq(TEST_SITE_ID), eq(false))
            verify(statsInsightsUseCase)
                .invoke(eq(TEST_SITE_ID), eq(false))
        }

    @Test
    fun `when loadDataIfNeeded called twice, then use cases invoked only once`() =
        test {
            whenever(
                statsSummaryUseCase(any(), any())
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestSummaryData()
                )
            )
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            initViewModel()
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            verify(statsSummaryUseCase,
                times(1))
                .invoke(any(), any())
            verify(statsInsightsUseCase,
                times(1))
                .invoke(any(), any())
        }

    @Test
    fun `when fetchData called, then results are emitted`() =
        test {
            val testSummary = createTestSummaryData()
            val testInsights = createTestInsightsData()
            whenever(
                statsSummaryUseCase(any(), any())
            ).thenReturn(
                StatsSummaryResult.Success(testSummary)
            )
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Success(testInsights)
            )

            initViewModel()
            advanceUntilIdle()

            viewModel.summaryResult.test {
                viewModel.fetchData()
                advanceUntilIdle()
                val result = awaitItem()
                assertThat(result).isInstanceOf(
                    StatsSummaryResult
                        .Success::class.java
                )
                assertThat(
                    (result as StatsSummaryResult.Success)
                        .data.views
                ).isEqualTo(TEST_VIEWS)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `when refreshData called, then forceRefresh is true`() =
        test {
            whenever(
                statsSummaryUseCase(any(), any())
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestSummaryData()
                )
            )
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            initViewModel()
            advanceUntilIdle()

            viewModel.refreshData()
            advanceUntilIdle()

            verify(statsSummaryUseCase)
                .invoke(eq(TEST_SITE_ID), eq(true))
            verify(statsInsightsUseCase)
                .invoke(eq(TEST_SITE_ID), eq(true))
        }

    @Test
    fun `when refreshData called, then isDataRefreshing resets to false`() =
        test {
            whenever(
                statsSummaryUseCase(any(), any())
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestSummaryData()
                )
            )
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            initViewModel()
            advanceUntilIdle()

            viewModel.refreshData()
            advanceUntilIdle()

            assertThat(viewModel.isDataRefreshing.value)
                .isFalse()
        }

    @Suppress("TooGenericExceptionThrown")
    @Test
    fun `when summary use case throws, then error result is emitted`() =
        test {
            whenever(
                statsSummaryUseCase(any(), any())
            ).thenAnswer {
                throw RuntimeException("Test error")
            }
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            initViewModel()
            advanceUntilIdle()

            viewModel.summaryResult.test {
                viewModel.fetchData()
                advanceUntilIdle()
                val result = awaitItem()
                assertThat(result).isInstanceOf(
                    StatsSummaryResult
                        .Error::class.java
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `when no site selected, then fetchData is no-op`() =
        test {
            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(null)

            val config = InsightsCardsConfiguration(
                visibleCards = emptyList()
            )
            initViewModel(config)
            advanceUntilIdle()

            viewModel.fetchData()
            advanceUntilIdle()

            verify(statsSummaryUseCase, never())
                .invoke(any(), any())
        }

    @Test
    fun `when all cards hidden, then no endpoints are called`() =
        test {
            val config = InsightsCardsConfiguration(
                visibleCards = emptyList()
            )
            initViewModel(config)
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            verify(statsSummaryUseCase, never())
                .invoke(any(), any())
            verify(statsInsightsUseCase, never())
                .invoke(any(), any())
        }

    @Test
    fun `when only summary cards visible, then only summary is fetched`() =
        test {
            whenever(
                statsSummaryUseCase(any(), any())
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestSummaryData()
                )
            )

            val config = InsightsCardsConfiguration(
                visibleCards = listOf(
                    InsightsCardType.ALL_TIME_STATS,
                    InsightsCardType.MOST_POPULAR_DAY
                )
            )
            initViewModel(config)
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            verify(statsSummaryUseCase)
                .invoke(eq(TEST_SITE_ID), eq(false))
            verify(statsInsightsUseCase, never())
                .invoke(any(), any())
        }

    @Test
    fun `when only insights cards visible, then only insights is fetched`() =
        test {
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            val config = InsightsCardsConfiguration(
                visibleCards = listOf(
                    InsightsCardType.YEAR_IN_REVIEW,
                    InsightsCardType.MOST_POPULAR_TIME
                )
            )
            initViewModel(config)
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            verify(statsInsightsUseCase)
                .invoke(eq(TEST_SITE_ID), eq(false))
            verify(statsSummaryUseCase, never())
                .invoke(any(), any())
        }

    @Test
    fun `when hidden card re-added, then its endpoint is fetched`() =
        test {
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )
            whenever(
                statsSummaryUseCase(any(), any())
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestSummaryData()
                )
            )

            // Start with only insights cards
            val config = InsightsCardsConfiguration(
                visibleCards = listOf(
                    InsightsCardType.YEAR_IN_REVIEW
                )
            )
            initViewModel(config)
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            verify(statsInsightsUseCase,
                times(1))
                .invoke(any(), any())
            verify(statsSummaryUseCase, never())
                .invoke(any(), any())

            // Now add a summary card via config change
            val newConfig = InsightsCardsConfiguration(
                visibleCards = listOf(
                    InsightsCardType.YEAR_IN_REVIEW,
                    InsightsCardType.ALL_TIME_STATS
                )
            )
            configurationFlow.value =
                TEST_SITE_ID to newConfig
            advanceUntilIdle()

            // loadDataIfNeeded should now fetch
            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            verify(statsSummaryUseCase,
                times(1))
                .invoke(eq(TEST_SITE_ID), eq(false))
        }

    @Test
    fun `when refresh called, then all visible endpoints are re-fetched`() =
        test {
            whenever(
                statsSummaryUseCase(any(), any())
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestSummaryData()
                )
            )
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            initViewModel()
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            viewModel.refreshData()
            advanceUntilIdle()

            verify(statsSummaryUseCase,
                times(1))
                .invoke(eq(TEST_SITE_ID), eq(false))
            verify(statsSummaryUseCase,
                times(1))
                .invoke(eq(TEST_SITE_ID), eq(true))
            verify(statsInsightsUseCase,
                times(1))
                .invoke(eq(TEST_SITE_ID), eq(false))
            verify(statsInsightsUseCase,
                times(1))
                .invoke(eq(TEST_SITE_ID), eq(true))
        }

    @Test
    fun `when refresh called twice rapidly, then second refresh replaces first`() =
        test {
            whenever(
                statsSummaryUseCase(any(), any())
            ).thenReturn(
                StatsSummaryResult.Success(
                    createTestSummaryData()
                )
            )
            whenever(
                statsInsightsUseCase(any(), any())
            ).thenReturn(
                InsightsResult.Success(
                    createTestInsightsData()
                )
            )

            initViewModel()
            advanceUntilIdle()

            viewModel.loadDataIfNeeded()
            advanceUntilIdle()

            // Two rapid refreshes — second should
            // cancel the first via fetchJob?.cancel().
            viewModel.refreshData()
            viewModel.refreshData()
            advanceUntilIdle()

            // The second refreshData() cancels the
            // first job before it executes, so only
            // one forceRefresh=true call completes.
            assertThat(
                viewModel.isDataRefreshing.value
            ).isFalse()

            verify(statsSummaryUseCase, times(1))
                .invoke(eq(TEST_SITE_ID), eq(true))
        }

    @Test
    fun `when initialized, then all caches are cleared`() =
        test {
            initViewModel()
            advanceUntilIdle()

            verify(statsSummaryUseCase).clearCache()
            verify(statsInsightsUseCase).clearCache()
            verify(statsTagsUseCase).clearCache()
        }

    // endregion

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val OTHER_SITE_ID = 456L
        private const val TEST_VIEWS = 6782856L

        private fun createTestSummaryData() =
            StatsSummaryData(
                views = TEST_VIEWS,
                visitors = 154791L,
                posts = 42L,
                comments = 85L,
                viewsBestDay = "2022-02-22",
                viewsBestDayTotal = 4600L
            )

        private fun createTestInsightsData() =
            StatsInsightsData(
                highestHour = 14,
                highestHourPercent = 15.5,
                highestDayOfWeek = 3,
                highestDayPercent = 25.0,
                years = emptyList()
            )
    }
}
