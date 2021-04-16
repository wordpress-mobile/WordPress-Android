package org.wordpress.android.ui.accounts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowNoJetpackSitesError
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSiteAddressError
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject
import kotlin.text.RegexOption.IGNORE_CASE

class LoginViewModel @Inject constructor(
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    private val _navigationEvents = MediatorLiveData<Event<LoginNavigationEvents>>()
    val navigationEvents: LiveData<Event<LoginNavigationEvents>> = _navigationEvents

    fun onHandleSiteAddressError(siteInfo: ConnectSiteInfoPayload) {
        val protocolRegex = Regex("^(http[s]?://)", IGNORE_CASE)
        val siteAddressClean = siteInfo.url.replaceFirst(protocolRegex.toString().toRegex(), "")
        val errorMessage: String = resourceProvider.getString(R.string.login_not_a_jetpack_site, siteAddressClean)
        _navigationEvents.postValue(Event(ShowSiteAddressError(siteAddressClean, errorMessage)))
    }

    fun onHandleNoJetpackSites() {
        val errorMessage: String = resourceProvider.getString(R.string.login_no_jetpack_sites)
        _navigationEvents.postValue(Event(ShowNoJetpackSitesError(errorMessage)))
    }
}
