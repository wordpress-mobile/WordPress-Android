package org.wordpress.android.login.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import rs.wordpress.api.kotlin.WpLoginClient
import javax.inject.Inject

class LoginSiteApplicationPasswordViewModel @Inject constructor(
    private val wpLoginClient: WpLoginClient
) : ViewModel() {
    fun runApiDiscovery(url: String) {
        viewModelScope.launch {
        }
    }
}
