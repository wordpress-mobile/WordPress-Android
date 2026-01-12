package org.wordpress.android.ui.accounts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

class LoginEpilogueViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteStore: SiteStore,
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

    fun onSiteListLoaded(firstSiteLocalId: Int = -1) {
        // Skip the site picker and auto-select the default site
        val siteToSelect = getDefaultSiteLocalId()
            ?: if (firstSiteLocalId != -1) firstSiteLocalId else null

        if (siteToSelect != null) {
            onSiteClick(siteToSelect)
        } else {
            // No sites found - close and let main activity handle it
            _navigationEvents.postValue(Event(LoginNavigationEvents.CloseWithResultOk))
        }
    }

    /**
     * Returns the default site local ID using the same logic as WPMainActivity.initSelectedSite():
     * 1. Previously selected site from preferences
     * 2. Primary WordPress.com site
     * 3. First site in the list
     */
    fun getDefaultSiteLocalId(): Int? {
        // Try previously selected site from preferences
        val selectedSiteLocalId = selectedSiteRepository.getSelectedSiteLocalId(fromPrefs = true)
        if (selectedSiteLocalId != SelectedSiteRepository.UNAVAILABLE) {
            val site = siteStore.getSiteByLocalId(selectedSiteLocalId)
            if (site != null) {
                return site.id
            }
        }

        // Try primary WordPress.com site
        val primarySiteId = accountStore.account.primarySiteId
        val primarySite = siteStore.getSiteBySiteId(primarySiteId)
        if (primarySite != null) {
            return primarySite.id
        }

        // Fall back to first site in the list
        val sites = siteStore.sites
        if (sites.isNotEmpty()) {
            return sites[0].id
        }

        return null
    }

}
