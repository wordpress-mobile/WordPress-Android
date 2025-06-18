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
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "ApplicationPasswordLoginViewModel"

class ApplicationPasswordLoginViewModel @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val selfHostedEndpointFinder: SelfHostedEndpointFinder,
) : ViewModel() {
    private val _onFinishedEvent = MutableSharedFlow<Boolean>()
    val onFinishedEvent = _onFinishedEvent.asSharedFlow()

    fun setupSite(rawData: String) {
        viewModelScope.launch {
            getSiteXmlRpcEndpoint(rawData)
            val credentialsStored = storeCredentials(rawData)
            _onFinishedEvent.emit(credentialsStored)
        }
    }

    suspend private fun getSiteXmlRpcEndpoint(rawData: String): String = withContext(ioDispatcher) {
        try {
            if (rawData.isEmpty()) {
                Log.e(TAG, "Cannot store credentials: rawData is empty")
                ""
            } else {
                val siteUrl = applicationPasswordLoginHelper.getSiteUrlFromUrl(rawData)
                val xmlRpcEndpoint = async {
                    selfHostedEndpointFinder.verifyOrDiscoverXMLRPCEndpoint(siteUrl)
                }.await()
                Log.d(TAG, "Endpoint: $xmlRpcEndpoint")
                xmlRpcEndpoint
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting XML-RPC endpoint", e)
            ""
        }
    }

    suspend private fun storeCredentials(rawData: String): Boolean = withContext(ioDispatcher) {
        try {
            if (rawData.isEmpty()) {
                Log.e(TAG, "Cannot store credentials: rawData is empty")
                false
            } else {
                val credentialsStored = async {
                    applicationPasswordLoginHelper.storeApplicationPasswordCredentialsFrom(rawData)
                }.await()
                credentialsStored
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error storing credentials", e)
            false
        }
    }
}
