package org.wordpress.android.ui.mysite.cards.dashboard.todaysstats

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
            onFooterLinkClick = this::onTodaysStatsCardFooterLinkClick
        )
    }

    private fun onTodaysStatsCardFooterLinkClick() {
        cardsTracker.trackTodaysStatsCardFooterLinkClicked()
        navigateToTodaysStats()
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

    private fun navigateToTodaysStats() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        if (jetpackFeatureRemovalPhaseHelper.shouldShowStaticPage()) {
            _onNavigation.value = Event(SiteNavigationAction.ShowJetpackRemovalStaticPostersView)
        } else {
            _onNavigation.value = Event(SiteNavigationAction.OpenStatsInsights(selectedSite))
        }
    }
}
