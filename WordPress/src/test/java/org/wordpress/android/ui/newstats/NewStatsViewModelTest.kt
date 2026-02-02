package org.wordpress.android.ui.newstats

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.mostviewed.MostViewedDataSource
import org.wordpress.android.ui.newstats.repository.StatsCardsConfigurationRepository

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner.Silent::class)
class NewStatsViewModelTest : BaseUnitTest() {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var cardConfigurationRepository: StatsCardsConfigurationRepository

    private lateinit var viewModel: NewStatsViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    private val configurationFlow = MutableStateFlow<Pair<Long, StatsCardsConfiguration>?>(null)

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(cardConfigurationRepository.configurationFlow).thenReturn(configurationFlow)
    }

    private suspend fun initViewModel(config: StatsCardsConfiguration = StatsCardsConfiguration()) {
        whenever(cardConfigurationRepository.getConfiguration(TEST_SITE_ID)).thenReturn(config)
        viewModel = NewStatsViewModel(selectedSiteRepository, cardConfigurationRepository)
    }

    @Test
    fun `when initialized with default config, then default cards are visible`() = test {
        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.visibleCards.value).isEqualTo(StatsCardType.defaultCards())
    }

    @Test
    fun `when initialized with custom config, then custom cards are visible`() = test {
        val customConfig = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.TODAYS_STATS, StatsCardType.VIEWS_STATS)
        )
        initViewModel(customConfig)
        advanceUntilIdle()

        assertThat(viewModel.visibleCards.value).containsExactly(
            StatsCardType.TODAYS_STATS,
            StatsCardType.VIEWS_STATS
        )
    }

    @Test
    fun `when removeCard is called, then repository removeCard is invoked`() = test {
        initViewModel()
        advanceUntilIdle()

        viewModel.removeCard(StatsCardType.TODAYS_STATS)
        advanceUntilIdle()

        verify(cardConfigurationRepository).removeCard(TEST_SITE_ID, StatsCardType.TODAYS_STATS)
    }

    @Test
    fun `when addCard is called, then repository addCard is invoked`() = test {
        val config = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.TODAYS_STATS)
        )
        initViewModel(config)
        advanceUntilIdle()

        viewModel.addCard(StatsCardType.COUNTRIES)
        advanceUntilIdle()

        verify(cardConfigurationRepository).addCard(TEST_SITE_ID, StatsCardType.COUNTRIES)
    }

    @Test
    fun `when removeMostViewedCard is called, then repository removeMostViewedCard is invoked`() = test {
        initViewModel()
        advanceUntilIdle()

        viewModel.removeMostViewedCard(0)
        advanceUntilIdle()

        verify(cardConfigurationRepository).removeMostViewedCard(TEST_SITE_ID, 0)
    }

    @Test
    fun `when addMostViewedCard is called, then repository addMostViewedCard is invoked`() = test {
        initViewModel()
        advanceUntilIdle()

        viewModel.addMostViewedCard(MostViewedDataSource.REFERRERS)
        advanceUntilIdle()

        verify(cardConfigurationRepository).addMostViewedCard(TEST_SITE_ID, MostViewedDataSource.REFERRERS.name)
    }

    @Test
    fun `when updateMostViewedDataSource is called, then state is updated immediately`() = test {
        val config = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.MOST_VIEWED, StatsCardType.MOST_VIEWED),
            mostViewedDataSources = listOf(
                MostViewedDataSource.POSTS_AND_PAGES.name,
                MostViewedDataSource.REFERRERS.name
            )
        )
        initViewModel(config)
        advanceUntilIdle()

        viewModel.updateMostViewedDataSource(0, MostViewedDataSource.REFERRERS)

        assertThat(viewModel.mostViewedDataSources.value[0]).isEqualTo(MostViewedDataSource.REFERRERS)
    }

    @Test
    fun `when updateMostViewedDataSource is called, then repository is updated`() = test {
        val config = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.MOST_VIEWED),
            mostViewedDataSources = listOf(MostViewedDataSource.POSTS_AND_PAGES.name)
        )
        initViewModel(config)
        advanceUntilIdle()

        viewModel.updateMostViewedDataSource(0, MostViewedDataSource.REFERRERS)
        advanceUntilIdle()

        verify(cardConfigurationRepository).updateMostViewedDataSource(
            eq(TEST_SITE_ID),
            eq(0),
            eq(MostViewedDataSource.REFERRERS.name)
        )
    }

    @Test
    fun `when configuration changes via flow, then state is updated`() = test {
        initViewModel()
        advanceUntilIdle()

        val newConfig = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.COUNTRIES)
        )
        configurationFlow.value = TEST_SITE_ID to newConfig
        advanceUntilIdle()

        assertThat(viewModel.visibleCards.value).containsExactly(StatsCardType.COUNTRIES)
    }

    @Test
    fun `when configuration changes for different site, then state is not updated`() = test {
        initViewModel()
        advanceUntilIdle()
        val initialCards = viewModel.visibleCards.value

        val newConfig = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.COUNTRIES)
        )
        configurationFlow.value = OTHER_SITE_ID to newConfig
        advanceUntilIdle()

        assertThat(viewModel.visibleCards.value).isEqualTo(initialCards)
    }

    @Test
    fun `when hiddenCards is calculated, then it excludes visible cards except MOST_VIEWED`() = test {
        val config = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.TODAYS_STATS, StatsCardType.MOST_VIEWED)
        )
        initViewModel(config)
        advanceUntilIdle()

        val hiddenCards = viewModel.hiddenCards.value

        assertThat(hiddenCards).contains(StatsCardType.VIEWS_STATS, StatsCardType.COUNTRIES)
        assertThat(hiddenCards).doesNotContain(StatsCardType.TODAYS_STATS, StatsCardType.MOST_VIEWED)
    }

    @Test
    fun `when no site selected, then siteId defaults to 0`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)
        whenever(cardConfigurationRepository.getConfiguration(0L)).thenReturn(StatsCardsConfiguration())

        viewModel = NewStatsViewModel(selectedSiteRepository, cardConfigurationRepository)
        advanceUntilIdle()

        verify(cardConfigurationRepository).getConfiguration(0L)
    }

    @Test
    fun `when mostViewedDataSources has valid values, then they are parsed correctly`() = test {
        val config = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.MOST_VIEWED, StatsCardType.MOST_VIEWED),
            mostViewedDataSources = listOf(
                MostViewedDataSource.POSTS_AND_PAGES.name,
                MostViewedDataSource.REFERRERS.name
            )
        )
        initViewModel(config)
        advanceUntilIdle()

        assertThat(viewModel.mostViewedDataSources.value).containsExactly(
            MostViewedDataSource.POSTS_AND_PAGES,
            MostViewedDataSource.REFERRERS
        )
    }

    @Test
    fun `when hiddenMostViewedDataSources is calculated, then it excludes visible data sources`() = test {
        val config = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.MOST_VIEWED),
            mostViewedDataSources = listOf(MostViewedDataSource.POSTS_AND_PAGES.name)
        )
        initViewModel(config)
        advanceUntilIdle()

        val hiddenDataSources = viewModel.hiddenMostViewedDataSources.value

        assertThat(hiddenDataSources).containsExactly(MostViewedDataSource.REFERRERS)
    }

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val OTHER_SITE_ID = 456L
    }
}
