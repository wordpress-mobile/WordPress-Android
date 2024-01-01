package org.wordpress.android.ui.mysite.cards.jpfullplugininstall

import androidx.lifecycle.MutableLiveData
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackInstallFullPluginCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.JetpackInstallFullPluginCardBuilderParams
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
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    private val _refresh = MutableLiveData<Event<Boolean>>()
    val refresh = _refresh

    private val _onOpenJetpackInstallFullPluginOnboarding = SingleLiveEvent<Event<Unit>>()
    val onOpenJetpackInstallFullPluginOnboarding = _onOpenJetpackInstallFullPluginOnboarding

    private val _uiModel = MutableLiveData<JetpackInstallFullPluginCard>()
    val uiModel = _uiModel

    private fun onJetpackInstallFullPluginHideMenuItemClick() {
        selectedSiteRepository.getSelectedSite()?.localId()?.value?.let {
            analyticsTrackerWrapper.track(AnalyticsTracker.Stat.JETPACK_INSTALL_FULL_PLUGIN_CARD_DISMISSED)
            appPrefsWrapper.setShouldHideJetpackInstallFullPluginCard(it, true)
            _refresh.postValue(Event(true))
        }
    }

    private fun onJetpackInstallFullPluginLearnMoreClick() {
        analyticsTrackerWrapper.track(AnalyticsTracker.Stat.JETPACK_INSTALL_FULL_PLUGIN_CARD_TAPPED)
        _onOpenJetpackInstallFullPluginOnboarding.postValue(Event(Unit))
    }

    fun buildCard(site: SiteModel) {
        build(
            JetpackInstallFullPluginCardBuilderParams(
                site = site,
                onLearnMoreClick = this::onJetpackInstallFullPluginLearnMoreClick,
                onHideMenuItemClick = this::onJetpackInstallFullPluginHideMenuItemClick
            )
        )
    }

    fun build(
        params: JetpackInstallFullPluginCardBuilderParams
    ) {
        if (shouldShowCard(params.site)) {
            uiModel.postValue(
                JetpackInstallFullPluginCard(
                    siteName = params.site.name,
                    pluginNames = params.site.activeIndividualJetpackPluginNames().orEmpty(),
                    onLearnMoreClick = ListItemInteraction.create(params.onLearnMoreClick),
                    onHideMenuItemClick = ListItemInteraction.create(params.onHideMenuItemClick),
                )
            )
        }
    }

    private fun shouldShowCard(site: SiteModel): Boolean {
        return site.id != 0 && !appPrefsWrapper.getShouldHideJetpackInstallFullPluginCard(site.id) &&
                site.isJetpackIndividualPluginConnectedWithoutFullPlugin()
    }
}
