package org.wordpress.android.ui.accounts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginHelper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class LoginEpilogueViewModel @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val siteStore: SiteStore,
    private val wpJetpackIndividualPluginHelper: WPJetpackIndividualPluginHelper,
) : ViewModel() {
    private val _navigationEvents = MediatorLiveData<Event<LoginNavigationEvents>>()
    val navigationEvents: LiveData<Event<LoginNavigationEvents>> = _navigationEvents

    fun onSiteClick(localId: Int) {
        _navigationEvents.postValue(Event(LoginNavigationEvents.SelectSite(localId)))
    }

    fun onCreateNewSite() {
        _navigationEvents.postValue(Event(LoginNavigationEvents.CreateNewSite))
    }

    fun onContinue() {
        if (!siteStore.hasSite()) handleNoSitesFound() else handleSitesFound()
    }

    private fun handleNoSitesFound() {
        if (buildConfigWrapper.isJetpackApp && !buildConfigWrapper.isSiteCreationEnabled) {
            _navigationEvents.postValue(Event(LoginNavigationEvents.ShowNoJetpackSites))
        } else {
            if (appPrefsWrapper.shouldShowPostSignupInterstitial && buildConfigWrapper.isSignupEnabled) {
                _navigationEvents.postValue(Event(LoginNavigationEvents.ShowPostSignupInterstitialScreen))
            }
            _navigationEvents.postValue(Event(LoginNavigationEvents.CloseWithResultOk))
        }
    }

    private fun handleSitesFound() {
        _navigationEvents.postValue(Event(LoginNavigationEvents.CloseWithResultOk))
    }

    fun onLoginEpilogueResume(doLoginUpdate: Boolean) {
        if (!doLoginUpdate && !siteStore.hasSite()) handleNoSitesFound()
    }

    fun onLoginFinished(doLoginUpdate: Boolean) {
        if (doLoginUpdate && !siteStore.hasSite()) handleNoSitesFound()
    }

    fun onSiteListLoaded() {
        // don't check if already shown
        if (_navigationEvents.value?.peekContent() == LoginNavigationEvents.ShowJetpackIndividualPluginOverlay) return

        viewModelScope.launch {
            val showOverlay = wpJetpackIndividualPluginHelper.shouldShowJetpackIndividualPluginOverlay()
            if (showOverlay) {
                delay(DELAY_BEFORE_SHOWING_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY)
                _navigationEvents.postValue(Event(LoginNavigationEvents.ShowJetpackIndividualPluginOverlay))
            }
        }
    }

    companion object {
        private const val DELAY_BEFORE_SHOWING_JETPACK_INDIVIDUAL_PLUGIN_OVERLAY = 500L
    }
}
