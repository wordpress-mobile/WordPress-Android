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
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import javax.inject.Inject

@HiltViewModel
class LoginSiteApplicationPasswordViewModel @Inject constructor(
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
) : ViewModel() {
    private val _discoveryURL = Channel<String>(Channel.BUFFERED)
    val discoveryURL = _discoveryURL.receiveAsFlow()

    private val _loadingStateFlow = MutableStateFlow(false)
    val loadingStateFlow = _loadingStateFlow.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var discoveryJob: Job? = null

    fun runApiDiscovery(siteUrl: String) {
        _errorMessage.value = null
        _loadingStateFlow.value = true
        discoveryJob = viewModelScope.launch {
            val discoveryUrl = applicationPasswordLoginHelper
                .getAuthorizationUrlComplete(siteUrl)
            _discoveryURL.send(discoveryUrl)
            _loadingStateFlow.value = false
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
}
