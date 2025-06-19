package org.wordpress.android.ui.accounts.applicationpassword

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient
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
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val selfHostedEndpointFinder: SelfHostedEndpointFinder,
    private val siteStore: SiteStore,
    private val siteXMLRPCClient: SiteXMLRPCClient
) : ViewModel() {
    private val _onFinishedEvent = MutableSharedFlow<Boolean>()
    val onFinishedEvent = _onFinishedEvent.asSharedFlow()

    fun setupSite(rawData: String) {
        viewModelScope.launch {
            val urlLogin = applicationPasswordLoginHelper.getSiteUrlLoginFromRawData(rawData)
            val siteStored = storeSite(urlLogin)
            if (siteStored) {
                val sitesFetched = fetchSites(urlLogin)
                _onFinishedEvent.emit(sitesFetched)
            } else {
                Log.e(TAG, "Failed to store site")
                _onFinishedEvent.emit(false)
            }
            _onFinishedEvent.emit(false)
        }
    }

    suspend private fun storeSite(urlLogin: UriLogin): Boolean = withContext(ioDispatcher) {
        try {
            if (urlLogin.user.isNullOrEmpty() || urlLogin.password.isNullOrEmpty() || urlLogin.siteUrl.isNullOrEmpty()) {
                Log.e(TAG, "Cannot store site: rawData is empty")
                false
            } else {
                val xmlRpcEndpoint =
                    selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(urlLogin.siteUrl)
                Log.d(TAG, "Endpoint: $xmlRpcEndpoint")

                siteStore.onAction(
                    SiteActionBuilder.newFetchSiteAction(
                        SiteModel().apply {
                            xmlRpcUrl = xmlRpcEndpoint
                            url = urlLogin.siteUrl
                            apiRestUsernamePlain = urlLogin.user
                            apiRestPasswordPlain = urlLogin.password
                        }
                    )
                )

                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting XML-RPC endpoint", e)
            false
        }
    }

    suspend private fun fetchSites(urlLogin: UriLogin): Boolean = withContext(ioDispatcher) {
        try {
            if (urlLogin.user.isNullOrEmpty() || urlLogin.password.isNullOrEmpty() || urlLogin.siteUrl.isNullOrEmpty()) {
                Log.e(TAG, "Cannot store credentials: rawData is empty")
                false
            } else {
                siteStore.onAction(
                    SiteActionBuilder.newFetchSitesXmlRpcAction(
                        RefreshSitesXMLRPCPayload(
                            username = urlLogin.user,
                            password = urlLogin.password,
                            url = urlLogin.siteUrl,
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

    suspend private fun storeCredentials(rawData: String): Boolean = withContext(ioDispatcher) {
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
