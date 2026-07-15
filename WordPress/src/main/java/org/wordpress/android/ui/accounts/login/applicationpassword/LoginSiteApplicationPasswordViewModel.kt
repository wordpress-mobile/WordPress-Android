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
import kotlinx.coroutines.withTimeoutOrNull
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import javax.inject.Inject

@HiltViewModel
class LoginSiteApplicationPasswordViewModel @Inject constructor(
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val fetchConnectSiteInfoUseCase: FetchConnectSiteInfoUseCase,
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
                is ApplicationPasswordLoginHelper.DiscoveryResult.Failed ->
                    handleDiscoveryFailed(siteUrl, result.userFacingMessage)
            }
            _loadingStateFlow.value = false
        }
    }

    /**
     * When Application Password discovery fails, the site may be hosted on WordPress.com with a
     * custom domain (which doesn't support application passwords). Check the connect-site-info and,
     * if so, guide the user to the WordPress.com login flow instead of the generic error.
     */
    private suspend fun handleDiscoveryFailed(siteUrl: String, userFacingMessage: String) {
        val info = withTimeoutOrNull(CONNECT_SITE_INFO_TIMEOUT_MS) {
            fetchConnectSiteInfoUseCase.fetchConnectSiteInfo(siteUrl)
        }
        if (info != null && !info.isError && info.isWPCom) {
            _wpComDetected.send(siteUrl)
        } else {
            _errorMessage.value = userFacingMessage
            _discoveryURL.send("")
        }
    }

    fun cancelDiscovery() {
        discoveryJob?.cancel()
        discoveryJob = null
        _loadingStateFlow.value = false
    }

    fun setError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }

    companion object {
        private const val CONNECT_SITE_INFO_TIMEOUT_MS = 15_000L
    }
}
