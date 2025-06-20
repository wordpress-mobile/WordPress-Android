package org.wordpress.android.ui.accounts.applicationpassword

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper.UriLogin
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "ApplicationPasswordLoginViewModel"

class ApplicationPasswordLoginViewModel @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    // Dispatcher is the way to dispatch actions in the Flux architecture. It will call siteStore.onAction()
    private val dispatcher: Dispatcher,
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val selfHostedEndpointFinder: SelfHostedEndpointFinder,
    private val siteStore: SiteStore,
) : ViewModel() {
    private val _onFinishedEvent = MutableSharedFlow<String?>()
    /**
     * A shared flow that emits the site URL when the setup is finished.
     * It can emit null if the site could not be set up.
     */
    val onFinishedEvent = _onFinishedEvent.asSharedFlow()

    private var currentUrlLogin: UriLogin? = null

    fun onStart() {
        dispatcher.register(this)
    }

    fun onStop() {
        dispatcher.unregister(this)
    }

    /**
     * This method is called to set up the site with the provided raw data.
     *
     * @param rawData The raw data containing the callback data from the application password login.
     */
    fun setupSite(rawData: String) {
        viewModelScope.launch {
            val urlLogin = applicationPasswordLoginHelper.getSiteUrlLoginFromRawData(rawData)
            // Store credentials if the site already exists
            val credentialsStored = storeCredentials(rawData)
            // If the site already exists, we can skip fetching it again
            if (credentialsStored) {
                _onFinishedEvent.emit(urlLogin.siteUrl)
            } else {
                fetchSites(urlLogin)
                currentUrlLogin = urlLogin
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchSites(
        urlLogin: UriLogin
    ): Boolean = withContext(ioDispatcher) {
        try {
            if (urlLogin.user.isNullOrEmpty() ||
                urlLogin.password.isNullOrEmpty() ||
                urlLogin.siteUrl.isNullOrEmpty()) {
                Log.e(TAG, "Cannot store credentials: rawData is empty")
                false
            } else {
                val xmlRpcEndpoint =
                    selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl)
                dispatcher.dispatch(
                    SiteActionBuilder.newFetchSitesXmlRpcFromApplicationPasswordAction(
                        RefreshSitesXMLRPCPayload(
                            username = urlLogin.user,
                            password = urlLogin.password,
                            url = xmlRpcEndpoint,
                        )
                    )
                )
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error storing credentials", e)
            false
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun storeCredentials(rawData: String): Boolean = withContext(ioDispatcher) {
        try {
            if (rawData.isEmpty()) {
                Log.e(TAG, "Cannot store credentials: rawData is empty")
                false
            } else {
                val credentialsStored = applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(rawData)
                credentialsStored
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error storing credentials", e)
            false
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onSiteChanged(event: SiteStore.OnSiteChanged) {
        Log.e(TAG, "Site changed: ${event.rowsAffected}")
        val site = siteStore.sites.firstOrNull { it.url == currentUrlLogin?.siteUrl }
        if (site == null) {
            Log.e(TAG, "Site not found for URL: ${currentUrlLogin?.siteUrl}")
            viewModelScope.launch {
                _onFinishedEvent.emit(null)
            }
        } else {
            dispatcher.dispatch(SiteActionBuilder.newFetchProfileXmlRpcAction(site))
        };
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onProfileFetched(event: SiteStore.OnProfileFetched) {
        viewModelScope.launch {
            _onFinishedEvent.emit(event.site.url)
        }
    }
}
