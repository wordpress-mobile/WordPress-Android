package org.wordpress.android.ui.mysite.items.jetpackSwitchmenu

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.jetpackfeature.JetpackFeatureCardHelper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class JetpackSwitchMenuViewModelSlice @Inject constructor(
    private val jetpackFeatureCardHelper: JetpackFeatureCardHelper,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    private val _uiModel = MutableLiveData<MySiteCardAndItem.Card.JetpackSwitchMenu?>()
    val uiModel = _uiModel.distinctUntilChanged()

    suspend fun buildJetpackSwitchMenu() {
        if (!jetpackFeatureCardHelper.shouldShowSwitchToJetpackMenuCard()) {
            _uiModel.postValue(null)
            return
        }
        _uiModel.postValue(
            MySiteCardAndItem.Card.JetpackSwitchMenu(
                onClick = ListItemInteraction.create(this::onJetpackFeatureCardClick),
                onRemindMeLaterItemClick = ListItemInteraction.create(
                    this::onSwitchToJetpackMenuCardRemindMeLaterClick
                ),
                onHideMenuItemClick = ListItemInteraction.create(this::onSwitchToJetpackMenuCardHideMenuItemClick),
                onMoreMenuClick = ListItemInteraction.create(this::onJetpackFeatureCardMoreMenuClick)
            )
        )
    }

    private fun onJetpackFeatureCardClick() {
        jetpackFeatureCardHelper.track(AnalyticsTracker.Stat.REMOVE_FEATURE_CARD_TAPPED)
        _onNavigation.value = Event(
            SiteNavigationAction.OpenJetpackFeatureOverlay(
                source = JetpackFeatureRemovalOverlayUtil.JetpackFeatureCollectionOverlaySource.FEATURE_CARD
            )
        )
    }

    private fun onSwitchToJetpackMenuCardRemindMeLaterClick() {
        jetpackFeatureCardHelper.track(AnalyticsTracker.Stat.REMOVE_FEATURE_CARD_REMIND_LATER_TAPPED)
        appPrefsWrapper.setSwitchToJetpackMenuCardLastShownTimestamp(System.currentTimeMillis())
        _uiModel.postValue(null)
    }

    private fun onSwitchToJetpackMenuCardHideMenuItemClick() {
        jetpackFeatureCardHelper.hideSwitchToJetpackMenuCard()
        _uiModel.postValue(null)
    }

    private fun onJetpackFeatureCardMoreMenuClick() {
        jetpackFeatureCardHelper.track(AnalyticsTracker.Stat.REMOVE_FEATURE_CARD_MENU_ACCESSED)
    }

    fun clearValue() {
        _uiModel.postValue(null)
    }
}
