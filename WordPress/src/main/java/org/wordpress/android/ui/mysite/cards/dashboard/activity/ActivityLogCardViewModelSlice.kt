package org.wordpress.android.ui.mysite.cards.dashboard.activity

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.wordpress.android.fluxc.model.dashboard.CardModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.ActivityCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.ActivityCardBuilderParams.ActivityCardItemClickParams
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityLogCardViewModelSlice @Inject constructor(
    private val cardsTracker: CardsTracker,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val activityCardBuilder: ActivityCardBuilder
) {
    private val _uiModel = MutableLiveData<MySiteCardAndItem.Card.ActivityCard?>()
    val uiModel = _uiModel as LiveData<MySiteCardAndItem.Card.ActivityCard?>

    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    fun buildCard(activityCardModel: CardModel.ActivityCardModel?) {
        _uiModel.postValue(activityCardBuilder.build(getActivityLogCardBuilderParams(activityCardModel)))
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getActivityLogCardBuilderParams(activityCardModel: CardModel.ActivityCardModel?) =
        ActivityCardBuilderParams(
            activityCardModel = activityCardModel,
            onActivityItemClick = this::onActivityCardItemClick,
            onMoreMenuClick = this::onActivityCardMoreMenuClick,
            onAllActivityMenuItemClick = this::onActivityCardAllActivityItemClick,
            onHideMenuItemClick = this::onActivityCardHideMenuItemClick
        )

    private fun onActivityCardItemClick(activityCardItemClickParams: ActivityCardItemClickParams) {
        cardsTracker.trackCardItemClicked(
            CardsTracker.Type.ACTIVITY.label,
            CardsTracker.ActivityLogSubtype.ACTIVITY_LOG.label
        )
        _onNavigation.value =
            Event(
                SiteNavigationAction.OpenActivityLogDetail(
                    requireNotNull(selectedSiteRepository.getSelectedSite()),
                    activityCardItemClickParams.activityId,
                    activityCardItemClickParams.isRewindable
                )
            )
    }

    private fun onActivityCardHideMenuItemClick() {
        cardsTracker.trackCardMoreMenuItemClicked(
            CardsTracker.Type.ACTIVITY.label,
            MenuItemType.HIDE_THIS.label
        )
        appPrefsWrapper.setShouldHideActivityDashboardCard(
            requireNotNull(selectedSiteRepository.getSelectedSite()).siteId, true
        )
        _uiModel.value = null
    }

    private fun onActivityCardAllActivityItemClick() {
        cardsTracker.trackCardMoreMenuItemClicked(
            CardsTracker.Type.ACTIVITY.label,
            MenuItemType.ALL_ACTIVITY.label
        )
        _onNavigation.value =
            Event(SiteNavigationAction.OpenActivityLog(requireNotNull(selectedSiteRepository.getSelectedSite())))
    }

    private fun onActivityCardMoreMenuClick() = cardsTracker.trackCardMoreMenuClicked(CardsTracker.Type.ACTIVITY.label)

    fun clearValue() {
        _uiModel.postValue(null)
    }

    enum class MenuItemType(val label: String) {
        ALL_ACTIVITY("all_activity"),
        HIDE_THIS("hide_this")
    }
}


