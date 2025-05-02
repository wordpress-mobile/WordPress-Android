package org.wordpress.android.login.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import rs.wordpress.api.kotlin.WpLoginClient
import javax.inject.Inject
import androidx.core.net.toUri

@Suppress("TooGenericExceptionCaught")
class LoginSiteAddressViewModel @Inject constructor(
    private val wpLoginClient: WpLoginClient,
    private val appSecrets: AppSecrets
) : ViewModel() {
    private val mutableAuthorizationUrlFlow = MutableStateFlow("")
    val authorizationUrlFlow get() = mutableAuthorizationUrlFlow.asStateFlow()

    fun runApiDiscovery(url: String) {
        viewModelScope.launch {
            try {
                val urlDiscovery = wpLoginClient.apiDiscovery(url)
                val authorizationUrl = urlDiscovery.apiDetails.findApplicationPasswordsAuthenticationUrl()
                val authorizationUrlComplete = if (authorizationUrl.isNullOrEmpty()) {
                    authorizationUrl
                } else {
                    authorizationUrl.toUri().buildUpon().apply {
                        appendQueryParameter("app_name", "android-jetpack-client")
                        appendQueryParameter("success_url", appSecrets.redirectUri)
                    }.build().toString()
                }
                mutableAuthorizationUrlFlow.update { authorizationUrlComplete.orEmpty() }
                Log.d("WP_RS", "Found authorization for $url URL: $authorizationUrlComplete")
            } catch (throwable: Throwable) {
                Log.e("WP_RS", "VM: Error during API discovery for $url", throwable)
            }
        }
    }

    fun consumeAuthorizationUrl() {
        mutableAuthorizationUrlFlow.update { "" }
    }
}
