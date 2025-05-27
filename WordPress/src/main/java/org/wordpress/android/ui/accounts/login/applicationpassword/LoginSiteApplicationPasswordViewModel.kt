package org.wordpress.android.ui.accounts.login.applicationpassword

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.ApiDiscoveryResult
import rs.wordpress.api.kotlin.WpLoginClient
import javax.inject.Inject

class LoginSiteApplicationPasswordViewModel @Inject constructor(
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
) : ViewModel() {
    fun runApiDiscovery(siteUrl: String) {
        viewModelScope.launch {
            getAuthorizationUrlComplete(siteUrl)
        }
    }

    private suspend fun getAuthorizationUrlComplete(siteUrl: String): String =
        try {
            applicationPasswordLoginHelper.getAuthorizationUrlComplete(siteUrl)
        } catch (throwable: Throwable) {
            applicationPasswordLoginHelper.handleAuthenticationDiscoveryError(siteUrl, throwable)
        }
}
