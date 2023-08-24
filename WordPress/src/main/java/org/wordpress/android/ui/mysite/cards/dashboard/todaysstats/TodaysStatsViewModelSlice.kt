package org.wordpress.android.ui.mysite.cards.dashboard.todaysstats

import android.util.Log
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.TodaysStatsCardBuilderParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class TodaysStatsViewModelSlice @Inject constructor(
    private val cardsTracker: CardsTracker,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper
) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    fun getTodaysStatsBuilderParams(todaysStatsCardModel: TodaysStatsCardModel?) : TodaysStatsCardBuilderParams {
        return TodaysStatsCardBuilderParams(
            todaysStatsCard = todaysStatsCardModel,
            onTodaysStatsCardClick = this::onTodaysStatsCardClick,
            onGetMoreViewsClick = this::onGetMoreViewsClick,
            moreMenuClickParams = TodaysStatsCardBuilderParams.MoreMenuParams(
                onMoreMenuClick = this::onMoreMenuClick,
                onHideThisMenuItemClick = this::onHideThisMenuItemClick,
                onViewStatsMenuItemClick = this::onViewStatsMenuItemClick
            )
        )
    }

    private fun onTodaysStatsCardClick() {
        cardsTracker.trackTodaysStatsCardClicked()
        navigateToTodaysStats()
    }

    @Suppress("EmptyFunctionBlock")
    private fun onGetMoreViewsClick() {
        cardsTracker.trackTodaysStatsCardGetMoreViewsNudgeClicked()
        if (jetpackFeatureRemovalPhaseHelper.shouldShowStaticPage()) {
            _onNavigation.value = Event(SiteNavigationAction.ShowJetpackRemovalStaticPostersView)
        } else {
            _onNavigation.value = Event(
                SiteNavigationAction.OpenTodaysStatsGetMoreViewsExternalUrl(
                    TodaysStatsCardBuilder.URL_GET_MORE_VIEWS_AND_TRAFFIC
                )
            )
        }
    }

    private fun onMoreMenuClick() {
        // todo: track click cardsTracker.trackCardMoreMenuClicked(CardsTracker.Type.TODAYS_STATS.label)
        Log.i(javaClass.simpleName, "***=> onMoreMenuClick")
    }

    private fun onHideThisMenuItemClick() {
        // todo: track click cardsTracker.trackCardMoreMenuItemClicked
        // todo implement the logic to hide the card and add tracking logic
        Log.i(javaClass.simpleName, "***=> onHideThisMenuItemClick")
    }

    private fun onViewStatsMenuItemClick() {
        // todo: track click
        Log.i(javaClass.simpleName, "***=> onViewStatsMenuItemClick")
        // cardsTracker.trackCardMoreMenuItemClicked(
        navigateToTodaysStats()
    }

    private fun navigateToTodaysStats() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        if (jetpackFeatureRemovalPhaseHelper.shouldShowStaticPage()) {
            _onNavigation.value = Event(SiteNavigationAction.ShowJetpackRemovalStaticPostersView)
        } else {
            _onNavigation.value = Event(SiteNavigationAction.OpenStatsInsights(selectedSite))
        }
    }
}
