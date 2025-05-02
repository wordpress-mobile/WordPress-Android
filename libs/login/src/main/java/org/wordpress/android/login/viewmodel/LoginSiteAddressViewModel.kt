package org.wordpress.android.login.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rs.wordpress.api.kotlin.WpLoginClient
import javax.inject.Inject

@Suppress("TooGenericExceptionCaught")
class LoginSiteAddressViewModel @Inject constructor(
    private val wpLoginClient: WpLoginClient
) : ViewModel() {
    private val mutableAuthorizationUrlFlow = MutableStateFlow("")
    val authorizationUrlFlow get() = mutableAuthorizationUrlFlow.asStateFlow()

    fun runApiDiscovery(url: String) {
        viewModelScope.launch {
            try {
                val urlDiscovery = wpLoginClient.apiDiscovery(url)
                val authorizationUrl = urlDiscovery.apiDetails.findApplicationPasswordsAuthenticationUrl()
                mutableAuthorizationUrlFlow.update { authorizationUrl.orEmpty() }
                Log.d("WP_RS", "Found authorization for $url URL: $authorizationUrl")
            } catch (throwable: Throwable) {
                Log.e("WP_RS", "VM: Error during API discovery for $url", throwable)
            }
        }
    }

    fun consumeAuthorizationUrl() {
        mutableAuthorizationUrlFlow.update { "" }
    }
}
