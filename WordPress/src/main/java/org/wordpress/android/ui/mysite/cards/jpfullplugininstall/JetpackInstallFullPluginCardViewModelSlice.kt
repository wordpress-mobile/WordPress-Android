package org.wordpress.android.ui.mysite.cards.jpfullplugininstall

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.distinctUntilChanged
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackInstallFullPluginCard
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.extensions.activeIndividualJetpackPluginNames
import org.wordpress.android.util.extensions.isJetpackIndividualPluginConnectedWithoutFullPlugin
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject

class JetpackInstallFullPluginCardViewModelSlice @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val jetpackInstallFullPluginShownTracker: JetpackInstallFullPluginShownTracker
) {
    private val _onOpenJetpackInstallFullPluginOnboarding = SingleLiveEvent<Event<Unit>>()
    val onOpenJetpackInstallFullPluginOnboarding = _onOpenJetpackInstallFullPluginOnboarding

    private val _uiModel = MutableLiveData<JetpackInstallFullPluginCard?>()
    val uiModel = _uiModel.distinctUntilChanged()

    private fun onJetpackInstallFullPluginHideMenuItemClick() {
        selectedSiteRepository.getSelectedSite()?.localId()?.value?.let {
            analyticsTrackerWrapper.track(AnalyticsTracker.Stat.JETPACK_INSTALL_FULL_PLUGIN_CARD_DISMISSED)
            appPrefsWrapper.setShouldHideJetpackInstallFullPluginCard(it, true)
            _uiModel.postValue(null)
        }
    }

    private fun onJetpackInstallFullPluginLearnMoreClick() {
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.JETPACK_INSTALL_FULL_PLUGIN_CARD_TAPPED)
        _onOpenJetpackInstallFullPluginOnboarding.postValue(Event(Unit))
    }

    fun buildCard(
        site: SiteModel
    ) {
        if (shouldShowCard(site)) {
            _uiModel.postValue(
                JetpackInstallFullPluginCard(
                    siteName = site.name,
                    pluginNames = site.activeIndividualJetpackPluginNames().orEmpty(),
                    onLearnMoreClick = ListItemInteraction.create(this::onJetpackInstallFullPluginLearnMoreClick),
                    onHideMenuItemClick = ListItemInteraction.create(this::onJetpackInstallFullPluginHideMenuItemClick),
                )
            )
        } else _uiModel.postValue(null)
    }

    private fun shouldShowCard(site: SiteModel): Boolean {
        return site.id != 0 && !appPrefsWrapper.getShouldHideJetpackInstallFullPluginCard(site.id) &&
                site.isJetpackIndividualPluginConnectedWithoutFullPlugin()
    }

    fun clearValue() {
        _uiModel.postValue(null)
    }

    fun trackShown(card: JetpackInstallFullPluginCard) {
        jetpackInstallFullPluginShownTracker.trackShown(card.type)
    }

    fun resetShownTracker() {
        jetpackInstallFullPluginShownTracker.resetShown()
    }
}
