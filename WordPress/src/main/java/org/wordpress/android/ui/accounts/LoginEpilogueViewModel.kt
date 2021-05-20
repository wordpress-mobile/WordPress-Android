package org.wordpress.android.ui.accounts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.viewmodel.Event

import javax.inject.Inject

class LoginEpilogueViewModel @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val siteStore: SiteStore
) : ViewModel() {
    private val _navigationEvents = MediatorLiveData<Event<LoginNavigationEvents>>()
    val navigationEvents: LiveData<Event<LoginNavigationEvents>> = _navigationEvents

    fun onContinue() {
        if (!siteStore.hasSite()) handleNoSitesFound() else handleSitesFound()
    }

    private fun handleNoSitesFound() {
        if (buildConfigWrapper.isJetpackApp) {
            _navigationEvents.postValue(Event(LoginNavigationEvents.ShowNoJetpackSites))
        } else {
            if (appPrefsWrapper.shouldShowPostSignupInterstitial) {
                _navigationEvents.postValue(Event(LoginNavigationEvents.ShowPostSignupInterstitialScreen))
            }
            _navigationEvents.postValue(Event(LoginNavigationEvents.CloseWithResultOk))
        }
    }

    private fun handleSitesFound() {
        _navigationEvents.postValue(Event(LoginNavigationEvents.CloseWithResultOk))
    }
}
