package org.wordpress.android.support.main.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val appLogWrapper: AppLogWrapper,
) : ViewModel() {
    sealed class NavigationEvent {
        data class NavigateToAskTheBots(val accessToken: String, val userName: String) : NavigationEvent()
        data object NavigateToLogin : NavigationEvent()
    }

    data class UserInfo(
        val userName: String = "",
        val userEmail: String = "",
        val avatarUrl: String? = null
    )

    data class SupportOptionsVisibility(
        val showAskTheBots: Boolean = true,
        val showAskHappinessEngineers: Boolean = true
    )

    private val _userInfo = MutableStateFlow(UserInfo())
    val userInfo: StateFlow<UserInfo> = _userInfo.asStateFlow()

    private val _optionsVisibility = MutableStateFlow(SupportOptionsVisibility())
    val optionsVisibility: StateFlow<SupportOptionsVisibility> = _optionsVisibility.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    fun init() {
        val hasAccessToken = accountStore.hasAccessToken()
        _isLoggedIn.value = hasAccessToken

        val account = accountStore.account
        _userInfo.value = UserInfo(
            userName = account.displayName.ifEmpty { account.userName },
            userEmail = account.email,
            avatarUrl = account.avatarUrl.takeIf { it.isNotEmpty() }
        )

        _optionsVisibility.value = SupportOptionsVisibility(
            showAskTheBots = hasAccessToken,
            showAskHappinessEngineers = hasAccessToken
        )
    }

    fun onHelpCenterClick() {
        // Navigate to Help Center
    }

    fun onAskTheBotsClick() {
        viewModelScope.launch {
            // hasAccessToken() checks if it exists and it's not empty, not only the nullability.
            // So, if it's true, then we are sure the token is not null
            if (!accountStore.hasAccessToken()) {
                appLogWrapper.d(AppLog.T.SUPPORT, "Trying to open a bot conversation without access token")
            } else {
                val account = accountStore.account
                _navigationEvents.emit(
                    NavigationEvent.NavigateToAskTheBots(
                        accessToken = accountStore.accessToken!!, // access token has been checked before
                        userName = account.displayName.ifEmpty { account.userName }
                    )
                )
            }
        }
    }

    fun onAskHappinessEngineersClick() {
        // Navigate to Happiness Engineers contact
    }

    fun onApplicationLogsClick() {
        // Navigate to Application Logs
    }

    fun onLoginClick() {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateToLogin)
        }
    }
}
