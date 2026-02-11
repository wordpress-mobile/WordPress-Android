package org.wordpress.android.ui.newstats

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.StatsCardsConfigurationRepository
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner.Silent::class)
class NewStatsViewModelTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var cardConfigurationRepository: StatsCardsConfigurationRepository

    @Mock
    private lateinit var networkUtilsWrapper: NetworkUtilsWrapper

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
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    private suspend fun initViewModel(config: StatsCardsConfiguration = StatsCardsConfiguration()) {
        whenever(cardConfigurationRepository.getConfiguration(TEST_SITE_ID)).thenReturn(config)
        viewModel = NewStatsViewModel(
            selectedSiteRepository,
            cardConfigurationRepository,
            networkUtilsWrapper
        )
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
    fun `when hiddenCards is calculated, then it excludes visible cards`() = test {
        val config = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.TODAYS_STATS, StatsCardType.MOST_VIEWED_POSTS_AND_PAGES)
        )
        initViewModel(config)
        advanceUntilIdle()

        val hiddenCards = viewModel.hiddenCards.value

        assertThat(hiddenCards).contains(
            StatsCardType.VIEWS_STATS,
            StatsCardType.MOST_VIEWED_REFERRERS,
            StatsCardType.COUNTRIES
        )
        assertThat(hiddenCards).doesNotContain(
            StatsCardType.TODAYS_STATS,
            StatsCardType.MOST_VIEWED_POSTS_AND_PAGES
        )
    }

    @Test
    fun `when no site selected, then siteId defaults to 0`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)
        whenever(cardConfigurationRepository.getConfiguration(0L)).thenReturn(StatsCardsConfiguration())

        viewModel = NewStatsViewModel(
            selectedSiteRepository,
            cardConfigurationRepository,
            networkUtilsWrapper
        )
        advanceUntilIdle()

        verify(cardConfigurationRepository).getConfiguration(0L)
    }

    // region Move card tests
    @Test
    fun `when moveCardUp is called, then repository moveCardUp is invoked`() = test {
        initViewModel()
        advanceUntilIdle()

        viewModel.moveCardUp(StatsCardType.VIEWS_STATS)
        advanceUntilIdle()

        verify(cardConfigurationRepository).moveCardUp(TEST_SITE_ID, StatsCardType.VIEWS_STATS)
    }

    @Test
    fun `when moveCardToTop is called, then repository moveCardToTop is invoked`() = test {
        initViewModel()
        advanceUntilIdle()

        viewModel.moveCardToTop(StatsCardType.COUNTRIES)
        advanceUntilIdle()

        verify(cardConfigurationRepository).moveCardToTop(TEST_SITE_ID, StatsCardType.COUNTRIES)
    }

    @Test
    fun `when moveCardDown is called, then repository moveCardDown is invoked`() = test {
        initViewModel()
        advanceUntilIdle()

        viewModel.moveCardDown(StatsCardType.TODAYS_STATS)
        advanceUntilIdle()

        verify(cardConfigurationRepository).moveCardDown(TEST_SITE_ID, StatsCardType.TODAYS_STATS)
    }

    @Test
    fun `when moveCardToBottom is called, then repository moveCardToBottom is invoked`() = test {
        initViewModel()
        advanceUntilIdle()

        viewModel.moveCardToBottom(StatsCardType.TODAYS_STATS)
        advanceUntilIdle()

        verify(cardConfigurationRepository).moveCardToBottom(TEST_SITE_ID, StatsCardType.TODAYS_STATS)
    }
    // endregion

    // region cardsToLoad tests
    @Test
    fun `when ViewModel is created, then cardsToLoad starts empty`() = test {
        whenever(cardConfigurationRepository.getConfiguration(TEST_SITE_ID))
            .thenReturn(StatsCardsConfiguration())

        viewModel = NewStatsViewModel(
            selectedSiteRepository,
            cardConfigurationRepository,
            networkUtilsWrapper
        )

        // Before advanceUntilIdle(), config hasn't loaded yet
        assertThat(viewModel.cardsToLoad.value).isEmpty()
    }

    @Test
    fun `when config loads, then cardsToLoad matches visible cards`() = test {
        val config = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.AUTHORS)
        )
        initViewModel(config)
        advanceUntilIdle()

        assertThat(viewModel.cardsToLoad.value).containsExactly(StatsCardType.AUTHORS)
    }

    @Test
    fun `when config loads with default, then cardsToLoad matches default cards`() = test {
        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.cardsToLoad.value).isEqualTo(StatsCardType.defaultCards())
    }

    @Test
    fun `when configuration changes via flow, then cardsToLoad is updated`() = test {
        initViewModel()
        advanceUntilIdle()

        val newConfig = StatsCardsConfiguration(
            visibleCards = listOf(StatsCardType.COUNTRIES)
        )
        configurationFlow.value = TEST_SITE_ID to newConfig
        advanceUntilIdle()

        assertThat(viewModel.cardsToLoad.value).containsExactly(StatsCardType.COUNTRIES)
    }
    // endregion

    // region Network availability tests
    @Test
    fun `when initialized with network available, then isNetworkAvailable is true`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.isNetworkAvailable.value).isTrue()
    }

    @Test
    fun `when initialized without network, then isNetworkAvailable is false`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.isNetworkAvailable.value).isFalse()
    }

    @Test
    fun `when checkNetworkStatus is called, then network status is updated`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.isNetworkAvailable.value).isFalse()

        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        viewModel.checkNetworkStatus()

        assertThat(viewModel.isNetworkAvailable.value).isTrue()
    }
    // endregion

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val OTHER_SITE_ID = 456L
    }
}
