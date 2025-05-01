package org.wordpress.android.login.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rs.wordpress.api.kotlin.WpLoginClient
import javax.inject.Inject

@Suppress("TooGenericExceptionCaught")
class LoginSiteAddressViewModel @Inject constructor(
    private val wpLoginClient: WpLoginClient
) : ViewModel() {
    fun runApiDiscovery(url: String) {
        viewModelScope.launch {
            try {
                val urlDiscovery = wpLoginClient.apiDiscovery(url)
                val authorizationUrl = urlDiscovery.apiDetails.findApplicationPasswordsAuthenticationUrl()
                Log.d("WP_RS", "VM: Found authorization URL: $authorizationUrl")
            } catch (throwable: Throwable) {
                Log.e("WP_RS", "VM: Error during API discovery", throwable)
            }
        }
    }
}
