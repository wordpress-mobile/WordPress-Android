package org.wordpress.android.ui.mysite.cards.dashboard.todaysstats

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.model.dashboard.CardModel.TodaysStatsCardModel
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.TodaysStatsCardBuilderParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class TodaysStatsViewModelSlice @Inject constructor(
    private val cardsTracker: CardsTracker,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val todaysStatsCardBuilder: TodaysStatsCardBuilder
) {
    private val _uiModel = MutableLiveData<MySiteCardAndItem.Card.TodaysStatsCard?>()
    val uiModel = _uiModel as LiveData<MySiteCardAndItem.Card.TodaysStatsCard?>

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    fun buildTodaysStatsCard(todaysStatsCardModel: TodaysStatsCardModel?) {
        _uiModel.postValue(todaysStatsCardBuilder.build(getTodaysStatsBuilderParams(todaysStatsCardModel)))
    }

    fun getTodaysStatsBuilderParams(todaysStatsCardModel: TodaysStatsCardModel?): TodaysStatsCardBuilderParams {
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
        cardsTracker.trackCardItemClicked(CardsTracker.Type.STATS.label, CardsTracker.StatsSubtype.TODAYS_STATS.label)
        navigateToTodaysStats()
    }

    private fun onGetMoreViewsClick() {
        cardsTracker.trackCardItemClicked(
            CardsTracker.Type.STATS.label,
            CardsTracker.StatsSubtype.TODAYS_STATS_NUDGE.label
        )
        if (jetpackFeatureRemovalPhaseHelper.shouldShowStaticPage()) {
            _onNavigation.value = Event(SiteNavigationAction.ShowJetpackRemovalStaticPostersView)
        } else {
            _onNavigation.value = Event(
                SiteNavigationAction.OpenExternalUrl(
                    TodaysStatsCardBuilder.URL_GET_MORE_VIEWS_AND_TRAFFIC
                )
            )
        }
    }

    private fun onMoreMenuClick() {
        cardsTracker.trackCardMoreMenuClicked(CardsTracker.Type.STATS.label)
    }

    private fun onHideThisMenuItemClick() {
        cardsTracker.trackCardMoreMenuItemClicked(
            CardsTracker.Type.STATS.label,
            TodaysStatsMenuItemType.HIDE_THIS.label
        )
        appPrefsWrapper.setShouldHideTodaysStatsDashboardCard(selectedSiteRepository.getSelectedSite()!!.siteId, true)
        _uiModel.value = null
    }

    private fun onViewStatsMenuItemClick() {
        cardsTracker.trackCardMoreMenuItemClicked(
            CardsTracker.Type.STATS.label,
            TodaysStatsMenuItemType.VIEW_STATS.label
        )
        navigateToTodaysStats()
    }

    private fun navigateToTodaysStats() {
        val selectedSite = requireNotNull(selectedSiteRepository.getSelectedSite())
        if (jetpackFeatureRemovalPhaseHelper.shouldShowStaticPage()) {
            _onNavigation.value = Event(SiteNavigationAction.ShowJetpackRemovalStaticPostersView)
        } else {
            _onNavigation.value = Event(SiteNavigationAction.OpenStatsByDay(selectedSite))
        }
    }

    fun clearValue() {
        _uiModel.postValue(null)
    }
}

enum class TodaysStatsMenuItemType(val label: String) {
    VIEW_STATS("view_stats"),
    HIDE_THIS("hide_this")
}
