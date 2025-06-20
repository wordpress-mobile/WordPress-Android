package org.wordpress.android.ui.accounts.applicationpassword

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper

import javax.inject.Inject
import javax.inject.Named

private const val TAG = "ApplicationPasswordLoginViewModel"

class ApplicationPasswordLoginViewModel @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
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
            if (credentialsStored) {
                _onFinishedEvent.emit(urlLogin.siteUrl)
            } else {
                val siteFetched = fetchSites(urlLogin)
                _onFinishedEvent.emit(if (siteFetched) urlLogin.siteUrl else null)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchSites(
        urlLogin: ApplicationPasswordLoginHelper.UriLogin
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
                siteStore.onAction(
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
}
