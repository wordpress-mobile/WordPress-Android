package org.wordpress.android.ui.prefs.privacy.banner

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.PrivacySettingsRepository
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class PrivacyBannerViewModel @Inject constructor(
    @Named(UI_THREAD) val mainDispatcher: CoroutineDispatcher,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val repository: PrivacySettingsRepository,
) : ScopedViewModel(mainDispatcher) {
    private val initialUserPreference: Boolean = !analyticsTrackerWrapper.hasUserOptedOut
    private val isAnalyticsEnabled: Boolean
        get() = !analyticsTrackerWrapper.hasUserOptedOut

    private val _actionEvent = MutableSharedFlow<ActionEvent>()
    val actionEvent: Flow<ActionEvent> = _actionEvent

    private val _uiState = MutableStateFlow(
        UiState(
            analyticsSwitchEnabled = isAnalyticsEnabled,
            loading = false,
            showError = false,
        )
    )
    val uiState: StateFlow<UiState> = _uiState

    var loading: Boolean
        get() = uiState.value.loading
        set(value) {
            _uiState.value = uiState.value.copy(loading = value)
        }

    init {
        analyticsTrackerWrapper.track(Stat.PRIVACY_CHOICES_BANNER_PRESENTED)
    }

    fun onAnalyticsEnabledChanged(isEnabled: Boolean) {
        _uiState.value = uiState.value.copy(analyticsSwitchEnabled = isEnabled)
    }

    fun onSettingsPressed() {
        analyticsTrackerWrapper.track(Stat.PRIVACY_CHOICES_BANNER_SETTINGS_BUTTON_TAPPED)

        val analyticsEnabled = uiState.value.analyticsSwitchEnabled

        if (analyticsEnabled == initialUserPreference) {
            launch {
                appPrefsWrapper.savedPrivacyBannerSettings = true
                _actionEvent.emit(ActionEvent.ShowSettings)
            }
            return
        } else {
            launch {
                if (repository.isUserWPCOM()) {
                    loading = true
                    repository.updateTracksSetting(analyticsEnabled)
                        .onSuccess {
                            appPrefsWrapper.savedPrivacyBannerSettings = true
                            analyticsTrackerWrapper.hasUserOptedOut = !analyticsEnabled
                            _actionEvent.emit(ActionEvent.ShowSettings)
                        }
                        .onFailure {
                            _actionEvent.emit(
                                ActionEvent.ShowErrorOnSettings(requestedAnalyticsValue = analyticsEnabled)
                            )
                        }
                    loading = false
                } else {
                    appPrefsWrapper.savedPrivacyBannerSettings = true
                    analyticsTrackerWrapper.hasUserOptedOut = !analyticsEnabled
                    _actionEvent.emit(ActionEvent.ShowSettings)
                }
            }
        }
    }

    fun onSavePressed() {
        analyticsTrackerWrapper.track(Stat.PRIVACY_CHOICES_BANNER_SAVE_BUTTON_TAPPED)
        val analyticsEnabled = uiState.value.analyticsSwitchEnabled

        launch {
            if (repository.isUserWPCOM()) {
                loading = true
                repository.updateTracksSetting(analyticsEnabled)
                    .onSuccess {
                        appPrefsWrapper.savedPrivacyBannerSettings = true
                        analyticsTrackerWrapper.hasUserOptedOut = !analyticsEnabled
                        _actionEvent.emit(ActionEvent.Dismiss)
                    }
                    .onFailure {
                        _uiState.value = uiState.value.copy(showError = true)
                    }
                loading = false
            } else {
                appPrefsWrapper.savedPrivacyBannerSettings = true
                analyticsTrackerWrapper.hasUserOptedOut = !analyticsEnabled
                _actionEvent.emit(ActionEvent.Dismiss)
            }
        }
    }

    data class UiState(
        val analyticsSwitchEnabled: Boolean,
        val loading: Boolean,
        val showError: Boolean = false,
    )

    sealed class ActionEvent {
        object Dismiss: ActionEvent()
        object ShowSettings : ActionEvent()
        data class ShowErrorOnSettings(val requestedAnalyticsValue: Boolean?) : ActionEvent()
    }
}
