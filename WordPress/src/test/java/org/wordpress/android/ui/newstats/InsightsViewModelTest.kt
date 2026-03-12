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
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.newstats.repository.InsightsCardsConfigurationRepository
import org.wordpress.android.util.NetworkUtilsWrapper

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
            networkUtilsWrapper
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
                networkUtilsWrapper
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
                networkUtilsWrapper
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

    companion object {
        private const val TEST_SITE_ID = 123L
        private const val OTHER_SITE_ID = 456L
    }
}
