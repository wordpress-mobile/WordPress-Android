package org.wordpress.android.ui.accounts.login.applicationpassword

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
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.util.AppLog
import javax.inject.Inject

@HiltViewModel
class ApplicationPasswordReauthenticateViewModel @Inject constructor(
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val appLogWrapper: AppLogWrapper,
) : ViewModel() {
    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    @Suppress("TooGenericExceptionCaught")
    fun onDialogConfirmed(authenticationUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true

            if (authenticationUrl.isEmpty()) {
                appLogWrapper.w(AppLog.T.MAIN, "Authentication URL is empty, cannot proceed with reauthentication")
                _navigationEvent.emit(NavigationEvent.ShowError)
                return@launch
            }

            try {
                val completeAuthUrl = applicationPasswordLoginHelper.getAuthorizationUrlComplete(authenticationUrl)

                if (completeAuthUrl.isNotEmpty()) {
                    _navigationEvent.emit(NavigationEvent.NavigateToLogin(completeAuthUrl))
                } else {
                    appLogWrapper.e(AppLog.T.MAIN, "Failed to process authentication URL")
                    _navigationEvent.emit(NavigationEvent.ShowError)
                }
            } catch (e: Throwable) {
                appLogWrapper.e(AppLog.T.MAIN, "Error processing authentication URL - ${e.stackTraceToString()}")
                _navigationEvent.emit(NavigationEvent.ShowError)
            } finally {
                _isLoading.value = false
            }
        }
    }

    sealed class NavigationEvent {
        data class NavigateToLogin(val authenticationUrl: String) : NavigationEvent()
        object ShowError : NavigationEvent()
    }
}

