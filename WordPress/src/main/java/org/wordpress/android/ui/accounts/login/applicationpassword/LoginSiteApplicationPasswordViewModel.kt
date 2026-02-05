package org.wordpress.android.ui.accounts.login.applicationpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import javax.inject.Inject

@HiltViewModel
class LoginSiteApplicationPasswordViewModel @Inject constructor(
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
) : ViewModel() {
    // replay = 0: New subscribers will not receive previously emitted values
    private val _discoveryURL = MutableSharedFlow<String>(replay = 0)
    val discoveryURL = _discoveryURL.asSharedFlow()

    private val _loadingStateFlow = MutableStateFlow(false)
    val loadingStateFlow get() = _loadingStateFlow.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun runApiDiscovery(siteUrl: String) {
        _errorMessage.value = null
        _loadingStateFlow.value = true
        viewModelScope.launch {
            val discoveryUrl = applicationPasswordLoginHelper.getAuthorizationUrlComplete(siteUrl)
            _discoveryURL.emit(discoveryUrl)
            _loadingStateFlow.value = false
        }
    }

    fun setError(message: String) {
        _errorMessage.value = message
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
