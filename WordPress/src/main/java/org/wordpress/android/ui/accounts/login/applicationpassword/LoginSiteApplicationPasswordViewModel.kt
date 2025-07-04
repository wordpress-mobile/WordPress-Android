package org.wordpress.android.ui.accounts.login.applicationpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import javax.inject.Inject

class LoginSiteApplicationPasswordViewModel @Inject constructor(
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
) : ViewModel() {
    // replay = 0: New subscribers will not receive previously emitted values
    private val _discoveryURL = MutableSharedFlow<String>(replay = 0)
    val discoveryURL = _discoveryURL.asSharedFlow()

    private val _loadingStateFlow = MutableStateFlow(false)
    val loadingStateFlow get() = _loadingStateFlow.asStateFlow()

    fun runApiDiscovery(siteUrl: String) {
        _loadingStateFlow.value = true
        viewModelScope.launch {
            val discoveryUrl = applicationPasswordLoginHelper.getAuthorizationUrlComplete(siteUrl)
            _discoveryURL.emit(discoveryUrl)
            _loadingStateFlow.value = false
        }
    }
}
