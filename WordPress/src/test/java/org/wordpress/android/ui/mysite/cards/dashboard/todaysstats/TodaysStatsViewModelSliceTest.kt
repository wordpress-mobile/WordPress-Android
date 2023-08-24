package org.wordpress.android.ui.mysite.cards.dashboard.todaysstats

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class TodaysStatsViewModelSliceTest  : BaseUnitTest() {
    @Mock
    lateinit var cardsTracker: CardsTracker

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    private lateinit var todaysStatsViewModelSlice: TodaysStatsViewModelSlice

    private lateinit var navigationActions: MutableList<SiteNavigationAction>

    private val site = mock<SiteModel>()

    @Before
    fun setUp() {
        todaysStatsViewModelSlice = TodaysStatsViewModelSlice(
            cardsTracker,
            selectedSiteRepository,
            jetpackFeatureRemovalPhaseHelper
        )
        navigationActions = mutableListOf()
        todaysStatsViewModelSlice.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
    }

    @Test
    fun `given todays stat card, when card item is clicked, then stats page is opened`() =
        test {
            val params = todaysStatsViewModelSlice.getTodaysStatsBuilderParams(mock())

            params.onTodaysStatsCardClick()

            assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenStatsInsights(site))
            verify(cardsTracker).trackTodaysStatsCardClicked()
        }

    @Test
    fun `given todays stat card, when get more views url is clicked, then external link is opened`() =
        test {
            val params = todaysStatsViewModelSlice.getTodaysStatsBuilderParams(mock())

            params.onGetMoreViewsClick()

            assertThat(navigationActions)
                .containsOnly(
                    SiteNavigationAction.OpenTodaysStatsGetMoreViewsExternalUrl(
                        TodaysStatsCardBuilder.URL_GET_MORE_VIEWS_AND_TRAFFIC
                    )
                )
            verify(cardsTracker).trackTodaysStatsCardGetMoreViewsNudgeClicked()
        }
}
