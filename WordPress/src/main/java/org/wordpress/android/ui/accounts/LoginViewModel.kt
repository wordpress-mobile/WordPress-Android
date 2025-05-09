package org.wordpress.android.ui.accounts

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.runBlocking
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.store.AccountStore.AuthEmailPayloadScheme
import org.wordpress.android.fluxc.store.SiteStore.ConnectSiteInfoPayload
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowNoJetpackSites
import org.wordpress.android.ui.accounts.LoginNavigationEvents.ShowSiteAddressError
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.viewmodel.Event
import rs.wordpress.api.kotlin.WpLoginClient
import javax.inject.Inject
import kotlin.text.RegexOption.IGNORE_CASE
import org.wordpress.android.ui.accounts.login.WPcomLoginHelper

class LoginViewModel @Inject constructor(
    private val buildConfigWrapper: BuildConfigWrapper,
    private val wpLoginClient: WpLoginClient,
    private val wpComLoginHelper: WPcomLoginHelper
) : ViewModel() {
    private val _navigationEvents = MediatorLiveData<Event<LoginNavigationEvents>>()
    val navigationEvents: LiveData<Event<LoginNavigationEvents>> = _navigationEvents

    fun onHandleSiteAddressError(siteInfo: ConnectSiteInfoPayload) {
        val protocolRegex = Regex("^(http[s]?://)", IGNORE_CASE)
        val siteAddressClean = siteInfo.url.replaceFirst(protocolRegex.toString().toRegex(), "")
        _navigationEvents.postValue(Event(ShowSiteAddressError(siteAddressClean)))
    }

    fun onHandleNoJetpackSites() {
        _navigationEvents.postValue(Event(ShowNoJetpackSites))
    }

    fun getMagicLinkScheme() = if (buildConfigWrapper.isJetpackApp) {
        AuthEmailPayloadScheme.JETPACK
    } else {
        AuthEmailPayloadScheme.WORDPRESS
    }

    @Suppress("TooGenericExceptionCaught")
    fun runApiDiscovery(url: String): String = runBlocking {
        try {
            val urlDiscovery = wpLoginClient.apiDiscovery(url)
            val authorizationUrl = urlDiscovery.apiDetails.findApplicationPasswordsAuthenticationUrl()
            val authorizationUrlComplete = wpComLoginHelper.appendParamsToRestAuthorizationUrl(authorizationUrl)
            Log.d("WP_RS", "Found authorization for $url URL: $authorizationUrlComplete")
            AnalyticsTracker.track(AnalyticsTracker.Stat.BACKGROUND_REST_AUTODISCOVERY_SUCCESSFUL)
            authorizationUrlComplete
        } catch (throwable: Throwable) {
            Log.e("WP_RS", "VM: Error during API discovery for $url", throwable)
            AnalyticsTracker.track(AnalyticsTracker.Stat.BACKGROUND_REST_AUTODISCOVERY_FAILED)
            ""
        }
    }
}
