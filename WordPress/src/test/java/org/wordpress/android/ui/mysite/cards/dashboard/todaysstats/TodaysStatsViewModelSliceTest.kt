package org.wordpress.android.ui.mysite.cards.dashboard.todaysstats

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.ui.prefs.AppPrefsWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class TodaysStatsViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var cardsTracker: CardsTracker

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var todaysStatsCardBuilder: TodaysStatsCardBuilder

    private lateinit var todaysStatsViewModelSlice: TodaysStatsViewModelSlice

    private lateinit var navigationActions: MutableList<SiteNavigationAction>

    private lateinit var uiModels : MutableList<MySiteCardAndItem.Card.TodaysStatsCard?>

    private val site = mock<SiteModel>()

    @Before
    fun setUp() {
        todaysStatsViewModelSlice = TodaysStatsViewModelSlice(
            cardsTracker,
            selectedSiteRepository,
            jetpackFeatureRemovalPhaseHelper,
            appPrefsWrapper,
            todaysStatsCardBuilder
        )
        navigationActions = mutableListOf()
        todaysStatsViewModelSlice.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }

        uiModels = mutableListOf()
        todaysStatsViewModelSlice.uiModel.observeForever { uiModels.add(it) }

        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
    }

    @Test
    fun `given todays stat card, when card item is clicked, then stats page is opened`() =
        test {
            val params = todaysStatsViewModelSlice.getTodaysStatsBuilderParams(mock())

            params.onTodaysStatsCardClick()

            assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenStatsByDay(site))
            verify(cardsTracker).trackCardItemClicked(
                CardsTracker.Type.STATS.label,
                CardsTracker.StatsSubtype.TODAYS_STATS.label
            )
        }

    @Test
    fun `given todays stat card, when get more views url is clicked, then external link is opened`() =
        test {
            val params = todaysStatsViewModelSlice.getTodaysStatsBuilderParams(mock())

            params.onGetMoreViewsClick()

            assertThat(navigationActions)
                .containsOnly(
                    SiteNavigationAction.OpenExternalUrl(
                        TodaysStatsCardBuilder.URL_GET_MORE_VIEWS_AND_TRAFFIC
                    )
                )
            verify(cardsTracker).trackCardItemClicked(
                CardsTracker.Type.STATS.label,
                CardsTracker.StatsSubtype.TODAYS_STATS_NUDGE.label
            )
        }


    @Test
    fun `given todays stats card, when more menu is accessed, then event is tracked`() = test {
        val params = todaysStatsViewModelSlice.getTodaysStatsBuilderParams(mock())

        params.moreMenuClickParams.onMoreMenuClick.invoke()

        verify(cardsTracker).trackCardMoreMenuClicked(CardsTracker.Type.STATS.label)
    }

    @Test
    fun `given todays stats card, when more menu item view stats is accessed, then event is tracked`() = test {
        val params = todaysStatsViewModelSlice.getTodaysStatsBuilderParams(mock())

        params.moreMenuClickParams.onViewStatsMenuItemClick.invoke()

        verify(cardsTracker).trackCardMoreMenuItemClicked(
            CardsTracker.Type.STATS.label,
            TodaysStatsMenuItemType.VIEW_STATS.label
        )
        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenStatsByDay(site))
    }


    @Test
    fun `given todays stats card, when more menu item hide this is accessed, then event is tracked`() = test {
        val siteId = 1L
        whenever(selectedSiteRepository.getSelectedSite()?.siteId).thenReturn(siteId)

        val params = todaysStatsViewModelSlice.getTodaysStatsBuilderParams(mock())

        params.moreMenuClickParams.onHideThisMenuItemClick.invoke()

        verify(appPrefsWrapper).setShouldHideTodaysStatsDashboardCard(siteId,true)
        verify(cardsTracker).trackCardMoreMenuItemClicked(
            CardsTracker.Type.STATS.label,
            TodaysStatsMenuItemType.HIDE_THIS.label
        )
        assertThat(uiModels.last()).isNull()
    }
}
