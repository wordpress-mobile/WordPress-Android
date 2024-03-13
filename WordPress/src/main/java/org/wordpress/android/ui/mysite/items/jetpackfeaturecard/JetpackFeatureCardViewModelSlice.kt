package org.wordpress.android.ui.mysite.items.jetpackfeaturecard

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.mysite.cards.jetpackfeature.JetpackFeatureCardHelper
import org.wordpress.android.ui.mysite.cards.jetpackfeature.JetpackFeatureCardShownTracker
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class JetpackFeatureCardViewModelSlice @Inject constructor(
    private val jetpackFeatureCardHelper: JetpackFeatureCardHelper,
    private val jetpackFeatureCardShownTracker: JetpackFeatureCardShownTracker
) {
    private val _onNavigation = MutableLiveData<Event<SiteNavigationAction>>()
    val onNavigation = _onNavigation

    private val _uiModel = MutableLiveData<MySiteCardAndItem.Card.JetpackFeatureCard?>()
    val uiModel = _uiModel.distinctUntilChanged()

    suspend fun buildJetpackFeatureCard() {
        if (!jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()){
            _uiModel.postValue(null)
            return
        }
        _uiModel.postValue(
            MySiteCardAndItem.Card.JetpackFeatureCard(
                content = jetpackFeatureCardHelper.getCardContent(),
                onClick = ListItemInteraction.create(this::onJetpackFeatureCardClick),
                onHideMenuItemClick = ListItemInteraction.create(this::onJetpackFeatureCardHideMenuItemClick),
                onLearnMoreClick = ListItemInteraction.create(this::onJetpackFeatureCardLearnMoreClick),
                onRemindMeLaterItemClick = ListItemInteraction.create(this::onJetpackFeatureCardRemindMeLaterClick),
                onMoreMenuClick = ListItemInteraction.create(this::onJetpackFeatureCardMoreMenuClick),
                learnMoreUrl = jetpackFeatureCardHelper.getLearnMoreUrl()
            )
        )
    }

    private fun onJetpackFeatureCardClick() {
        jetpackFeatureCardHelper.track(AnalyticsTracker.Stat.REMOVE_FEATURE_CARD_TAPPED)
        _onNavigation.value =
            Event(
                SiteNavigationAction.OpenJetpackFeatureOverlay(
                    source = JetpackFeatureRemovalOverlayUtil.JetpackFeatureCollectionOverlaySource.FEATURE_CARD
                )
            )
    }

    private fun onJetpackFeatureCardHideMenuItemClick() {
        jetpackFeatureCardHelper.hideJetpackFeatureCard()
        _uiModel.postValue(null)
    }

    private fun onJetpackFeatureCardLearnMoreClick() {
        jetpackFeatureCardHelper.track(AnalyticsTracker.Stat.REMOVE_FEATURE_CARD_LINK_TAPPED)
        _onNavigation.value =
            Event(
                SiteNavigationAction.OpenJetpackFeatureOverlay
                    (
                    source =
                    JetpackFeatureRemovalOverlayUtil.JetpackFeatureCollectionOverlaySource.FEATURE_CARD
                )
            )
    }

    private fun onJetpackFeatureCardRemindMeLaterClick() {
        jetpackFeatureCardHelper.setJetpackFeatureCardLastShownTimeStamp(System.currentTimeMillis())
        _uiModel.postValue(null)
    }

    private fun onJetpackFeatureCardMoreMenuClick() {
        jetpackFeatureCardHelper.track(AnalyticsTracker.Stat.REMOVE_FEATURE_CARD_MENU_ACCESSED)
    }

    fun clearValue() {
        _uiModel.postValue(null)
    }

    fun trackShown(itemType: MySiteCardAndItem.Type) {
        jetpackFeatureCardShownTracker.trackShown(itemType)
    }

    fun resetShown() {
        jetpackFeatureCardShownTracker.resetShown()
    }
}

