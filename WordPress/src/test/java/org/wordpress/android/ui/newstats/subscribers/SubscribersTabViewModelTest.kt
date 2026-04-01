package org.wordpress.android.ui.newstats.subscribers

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
import org.wordpress.android.ui.newstats.repository.SubscribersCardsConfigurationRepository
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner.Silent::class)
class SubscribersTabViewModelTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var cardConfigurationRepository: SubscribersCardsConfigurationRepository

    @Mock
    private lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private lateinit var viewModel: SubscribersTabViewModel

    private val testSite = SiteModel().apply {
        id = 1
        siteId = TEST_SITE_ID
        name = "Test Site"
    }

    private val configurationFlow =
        MutableStateFlow<Pair<Long, SubscribersCardsConfiguration>?>(null)

    @Before
    fun setUp() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(testSite)
        whenever(cardConfigurationRepository.configurationFlow).thenReturn(configurationFlow)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    private suspend fun initViewModel(
        config: SubscribersCardsConfiguration = SubscribersCardsConfiguration()
    ) {
        whenever(cardConfigurationRepository.getConfiguration(TEST_SITE_ID)).thenReturn(config)
        viewModel = SubscribersTabViewModel(
            selectedSiteRepository,
            cardConfigurationRepository,
            networkUtilsWrapper
        )
    }

    @Test
    fun `when initialized with default config, then default cards are visible`() = test {
        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.visibleCards.value).isEqualTo(SubscribersCardType.defaultCards())
    }

    @Test
    fun `when initialized with custom config, then custom cards are visible`() = test {
        val customConfig = SubscribersCardsConfiguration(
            visibleCards = listOf(
                SubscribersCardType.ALL_TIME_SUBSCRIBERS,
                SubscribersCardType.EMAILS
            )
        )
        initViewModel(customConfig)
        advanceUntilIdle()

        assertThat(viewModel.visibleCards.value).containsExactly(
            SubscribersCardType.ALL_TIME_SUBSCRIBERS,
            SubscribersCardType.EMAILS
        )
    }

    @Test
    fun `when removeCard is called, then repository removeCard is invoked`() = test {
        initViewModel()
        advanceUntilIdle()

        viewModel.removeCard(SubscribersCardType.ALL_TIME_SUBSCRIBERS)
        advanceUntilIdle()

        verify(cardConfigurationRepository).removeCard(
            TEST_SITE_ID,
            SubscribersCardType.ALL_TIME_SUBSCRIBERS
        )
    }

    @Test
    fun `when addCard is called, then repository addCard is invoked`() = test {
        val config = SubscribersCardsConfiguration(
            visibleCards = listOf(SubscribersCardType.ALL_TIME_SUBSCRIBERS)
        )
        initViewModel(config)
        advanceUntilIdle()

        viewModel.addCard(SubscribersCardType.EMAILS)
        advanceUntilIdle()

        verify(cardConfigurationRepository).addCard(TEST_SITE_ID, SubscribersCardType.EMAILS)
    }

    @Test
    fun `when configuration changes via flow, then state is updated`() = test {
        initViewModel()
        advanceUntilIdle()

        val newConfig = SubscribersCardsConfiguration(
            visibleCards = listOf(SubscribersCardType.EMAILS)
        )
        configurationFlow.value = TEST_SITE_ID to newConfig
        advanceUntilIdle()

        assertThat(viewModel.visibleCards.value).containsExactly(SubscribersCardType.EMAILS)
    }

    @Test
    fun `when configuration changes for different site, then state is not updated`() = test {
        initViewModel()
        advanceUntilIdle()
        val initialCards = viewModel.visibleCards.value

        val newConfig = SubscribersCardsConfiguration(
            visibleCards = listOf(SubscribersCardType.EMAILS)
        )
        configurationFlow.value = OTHER_SITE_ID to newConfig
        advanceUntilIdle()

        assertThat(viewModel.visibleCards.value).isEqualTo(initialCards)
    }

    @Test
    fun `when hiddenCards is calculated, then it excludes visible cards`() = test {
        val config = SubscribersCardsConfiguration(
            visibleCards = listOf(
                SubscribersCardType.ALL_TIME_SUBSCRIBERS,
                SubscribersCardType.EMAILS
            )
        )
        initViewModel(config)
        advanceUntilIdle()

        val hiddenCards = viewModel.hiddenCards.value

        assertThat(hiddenCards).contains(
            SubscribersCardType.SUBSCRIBERS_GRAPH,
            SubscribersCardType.SUBSCRIBERS_LIST
        )
        assertThat(hiddenCards).doesNotContain(
            SubscribersCardType.ALL_TIME_SUBSCRIBERS,
            SubscribersCardType.EMAILS
        )
    }

    @Test
    fun `when no site selected, then siteId defaults to 0`() = test {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)
        whenever(cardConfigurationRepository.getConfiguration(0L))
            .thenReturn(SubscribersCardsConfiguration())

        viewModel = SubscribersTabViewModel(
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

        viewModel.moveCardUp(SubscribersCardType.SUBSCRIBERS_GRAPH)
        advanceUntilIdle()

        verify(cardConfigurationRepository).moveCardUp(
            TEST_SITE_ID,
            SubscribersCardType.SUBSCRIBERS_GRAPH
        )
    }

    @Test
    fun `when moveCardToTop is called, then repository moveCardToTop is invoked`() = test {
        initViewModel()
        advanceUntilIdle()

        viewModel.moveCardToTop(SubscribersCardType.EMAILS)
        advanceUntilIdle()

        verify(cardConfigurationRepository).moveCardToTop(
            TEST_SITE_ID,
            SubscribersCardType.EMAILS
        )
    }

    @Test
    fun `when moveCardDown is called, then repository moveCardDown is invoked`() = test {
        initViewModel()
        advanceUntilIdle()

        viewModel.moveCardDown(SubscribersCardType.ALL_TIME_SUBSCRIBERS)
        advanceUntilIdle()

        verify(cardConfigurationRepository).moveCardDown(
            TEST_SITE_ID,
            SubscribersCardType.ALL_TIME_SUBSCRIBERS
        )
    }

    @Test
    fun `when moveCardToBottom is called, then repository moveCardToBottom is invoked`() = test {
        initViewModel()
        advanceUntilIdle()

        viewModel.moveCardToBottom(SubscribersCardType.ALL_TIME_SUBSCRIBERS)
        advanceUntilIdle()

        verify(cardConfigurationRepository).moveCardToBottom(
            TEST_SITE_ID,
            SubscribersCardType.ALL_TIME_SUBSCRIBERS
        )
    }
    // endregion

    // region cardsToLoad tests
    @Test
    fun `when ViewModel is created, then cardsToLoad starts empty`() = test {
        whenever(cardConfigurationRepository.getConfiguration(TEST_SITE_ID))
            .thenReturn(SubscribersCardsConfiguration())

        viewModel = SubscribersTabViewModel(
            selectedSiteRepository,
            cardConfigurationRepository,
            networkUtilsWrapper
        )

        assertThat(viewModel.cardsToLoad.value).isEmpty()
    }

    @Test
    fun `when config loads, then cardsToLoad matches visible cards`() = test {
        val config = SubscribersCardsConfiguration(
            visibleCards = listOf(SubscribersCardType.EMAILS)
        )
        initViewModel(config)
        advanceUntilIdle()

        assertThat(viewModel.cardsToLoad.value).containsExactly(SubscribersCardType.EMAILS)
    }

    @Test
    fun `when config loads with default, then cardsToLoad matches default cards`() = test {
        initViewModel()
        advanceUntilIdle()

        assertThat(viewModel.cardsToLoad.value).isEqualTo(SubscribersCardType.defaultCards())
    }

    @Test
    fun `when configuration changes via flow, then cardsToLoad is not updated after initial load`() =
        test {
            initViewModel()
            advanceUntilIdle()

            val initialCardsToLoad = viewModel.cardsToLoad.value

            val newConfig = SubscribersCardsConfiguration(
                visibleCards = listOf(SubscribersCardType.SUBSCRIBERS_LIST)
            )
            configurationFlow.value = TEST_SITE_ID to newConfig
            advanceUntilIdle()

            assertThat(viewModel.cardsToLoad.value)
                .isEqualTo(initialCardsToLoad)
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

    @Test
    fun `when checkNetworkStatus is called, then it returns current status`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        initViewModel()
        advanceUntilIdle()

        val result = viewModel.checkNetworkStatus()

        assertThat(result).isTrue()
    }
    // endregion

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val OTHER_SITE_ID = 456L
    }
}
