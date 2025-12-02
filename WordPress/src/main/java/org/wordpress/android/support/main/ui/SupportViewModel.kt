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
import org.wordpress.android.BuildConfig
import org.wordpress.android.fluxc.network.NetworkRequestsRetentionPeriod
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.support.common.model.UserInfo
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures
import org.wordpress.android.util.AppLog
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val accountStore: AccountStore,
    private val appLogWrapper: AppLogWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val experimentalFeatures: ExperimentalFeatures,
) : ViewModel() {
    sealed class NavigationEvent {
        data object NavigateToAskTheBots : NavigationEvent()
        data object NavigateToLogin : NavigationEvent()
        data object NavigateToHelpCenter : NavigationEvent()
        data object NavigateToApplicationLogs : NavigationEvent()
        data object NavigateToAskHappinessEngineers : NavigationEvent()
        data object NavigateToNetworkRequests : NavigationEvent()
    }

    sealed class DialogState {
        data object Hidden : DialogState()
        data class EnableTracking(
            val selectedPeriod: NetworkRequestsRetentionPeriod
        ) : DialogState()

        data object DisableTracking : DialogState()
    }

    data class SupportOptionsVisibility(
        val showAskTheBots: Boolean = true,
        val showAskHappinessEngineers: Boolean = true
    )

    data class NetworkTrackingState(
        val showNetworkDebugging: Boolean = false,
        val isTrackingEnabled: Boolean = false,
        val retentionPeriod: NetworkRequestsRetentionPeriod = NetworkRequestsRetentionPeriod.ONE_HOUR
    )

    private val _userInfo = MutableStateFlow(UserInfo("", "", null))
    val userInfo: StateFlow<UserInfo> = _userInfo.asStateFlow()

    private val _optionsVisibility = MutableStateFlow(SupportOptionsVisibility())
    val optionsVisibility: StateFlow<SupportOptionsVisibility> = _optionsVisibility.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> = _navigationEvents.asSharedFlow()

    private val _dialogState = MutableStateFlow<DialogState>(DialogState.Hidden)
    val dialogState: StateFlow<DialogState> = _dialogState.asStateFlow()

    private val _networkTrackingState = MutableStateFlow(NetworkTrackingState())
    val networkTrackingState: StateFlow<NetworkTrackingState> = _networkTrackingState.asStateFlow()

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
            showAskTheBots = hasAccessToken && BuildConfig.IS_JETPACK_APP,
            showAskHappinessEngineers = hasAccessToken && BuildConfig.IS_JETPACK_APP
        )

        initNetworkTrackingState()
    }

    private fun initNetworkTrackingState() {
        val isFeatureEnabled = experimentalFeatures.isEnabled(ExperimentalFeatures.Feature.NETWORK_DEBUGGING)
        val isTrackingEnabled = appPrefsWrapper.isTrackNetworkRequestsEnabled
        val retentionPeriod = NetworkRequestsRetentionPeriod.fromInt(
            appPrefsWrapper.trackNetworkRequestsRetentionPeriod
        )

        _networkTrackingState.value = NetworkTrackingState(
            showNetworkDebugging = isFeatureEnabled,
            isTrackingEnabled = isTrackingEnabled,
            retentionPeriod = retentionPeriod
        )
    }

    fun onHelpCenterClick() {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateToHelpCenter)
        }
    }

    fun onAskTheBotsClick() {
        viewModelScope.launch {
            // hasAccessToken() checks if it exists and it's not empty, not only the nullability.
            // So, if it's true, then we are sure the token is not null
            if (!accountStore.hasAccessToken()) {
                appLogWrapper.d(AppLog.T.SUPPORT, "Trying to open a bot conversation without access token")
            } else {
                _navigationEvents.emit(NavigationEvent.NavigateToAskTheBots)
            }
        }
    }

    fun onAskHappinessEngineersClick() {
        viewModelScope.launch {
            // hasAccessToken() checks if it exists and it's not empty, not only the nullability.
            // So, if it's true, then we are sure the token is not null
            if (!accountStore.hasAccessToken()) {
                appLogWrapper.d(AppLog.T.SUPPORT, "Trying to open a HE conversation without access token")
            } else {
                _navigationEvents.emit(NavigationEvent.NavigateToAskHappinessEngineers)
            }
        }
    }

    fun onApplicationLogsClick() {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateToApplicationLogs)
        }
    }

    fun onLoginClick() {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateToLogin)
        }
    }

    fun onNetworkTrackingToggle(enabled: Boolean) {
        if (enabled) {
            val currentPeriod = NetworkRequestsRetentionPeriod.fromInt(
                appPrefsWrapper.trackNetworkRequestsRetentionPeriod
            )
            _dialogState.value = DialogState.EnableTracking(currentPeriod)
        } else {
            _dialogState.value = DialogState.DisableTracking
        }
    }

    fun onEnableTrackingConfirmed(period: NetworkRequestsRetentionPeriod) {
        appPrefsWrapper.trackNetworkRequestsRetentionPeriod = period.value
        appPrefsWrapper.isTrackNetworkRequestsEnabled = true

        _networkTrackingState.value = _networkTrackingState.value.copy(
            isTrackingEnabled = true,
            retentionPeriod = period
        )
        _dialogState.value = DialogState.Hidden
        appLogWrapper.d(AppLog.T.API, "Track network requests enabled with retention: $period")
    }

    fun onDisableTrackingConfirmed() {
        appPrefsWrapper.isTrackNetworkRequestsEnabled = false

        _networkTrackingState.value = _networkTrackingState.value.copy(
            isTrackingEnabled = false
        )
        _dialogState.value = DialogState.Hidden
        appLogWrapper.d(AppLog.T.API, "Track network requests disabled")
    }

    fun onDialogDismissed() {
        _dialogState.value = DialogState.Hidden
    }

    fun onRetentionPeriodSelected(period: NetworkRequestsRetentionPeriod) {
        val currentState = _dialogState.value
        if (currentState is DialogState.EnableTracking) {
            _dialogState.value = currentState.copy(selectedPeriod = period)
        }
    }

    fun onViewNetworkRequestsClick() {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.NavigateToNetworkRequests)
        }
    }
}
