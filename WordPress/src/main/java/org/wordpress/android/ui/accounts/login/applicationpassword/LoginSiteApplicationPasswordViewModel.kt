package org.wordpress.android.ui.accounts.login.applicationpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class LoginSiteApplicationPasswordViewModel @Inject constructor(
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val resourceProvider: ResourceProvider,
) : ViewModel() {
    private val _discoveryURL = Channel<String>(Channel.BUFFERED)
    val discoveryURL = _discoveryURL.receiveAsFlow()

    private val _wpComDetected = Channel<String>(Channel.BUFFERED)
    val wpComDetected = _wpComDetected.receiveAsFlow()

    private val _loadingStateFlow = MutableStateFlow(false)
    val loadingStateFlow = _loadingStateFlow.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var discoveryJob: Job? = null

    fun runApiDiscovery(siteUrl: String) {
        _errorMessage.value = null
        _loadingStateFlow.value = true
        discoveryJob = viewModelScope.launch {
            when (val result = applicationPasswordLoginHelper.getAuthorizationUrlComplete(siteUrl)) {
                is ApplicationPasswordLoginHelper.DiscoveryResult.Authorized ->
                    _discoveryURL.send(result.authorizationUrl)
                is ApplicationPasswordLoginHelper.DiscoveryResult.WpComSite ->
                    _wpComDetected.send(siteUrl)
                // Report why discovery failed. Previously this also sent an empty discoveryURL, which
                // the fragment read as "unsupported" and used to overwrite this message with a generic
                // one — so the real reason never reached the user. Failure is signalled by
                // [errorMessage] alone; discoveryURL only ever carries a URL to navigate to.
                is ApplicationPasswordLoginHelper.DiscoveryResult.Failed ->
                    _errorMessage.value = messageFor(result)
            }
            _loadingStateFlow.value = false
        }
    }

    /**
     * Prefer an explanation the user can act on. The library's message for a private site is
     * "failed to read its API configuration", which describes the symptom rather than the cause and
     * reads as though the site is broken.
     */
    private fun messageFor(result: ApplicationPasswordLoginHelper.DiscoveryResult.Failed): String =
        when (result.reason) {
            ApplicationPasswordLoginHelper.DiscoveryResult.FailureReason.PrivateSite ->
                resourceProvider.getString(R.string.application_password_private_site_error)
            ApplicationPasswordLoginHelper.DiscoveryResult.FailureReason.NotSupported ->
                resourceProvider.getString(R.string.application_password_not_supported_error)
            ApplicationPasswordLoginHelper.DiscoveryResult.FailureReason.Unknown ->
                result.userFacingMessage
        }

    fun cancelDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        _loadingStateFlow.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
