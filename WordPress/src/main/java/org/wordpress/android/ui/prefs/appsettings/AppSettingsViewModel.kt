package org.wordpress.android.ui.prefs.appsettings

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AccountErrorType
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.ui.deeplinks.DeepLinkOpenWebLinksWithJetpackHelper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.AppSettingsActivity.Companion.EXTRA_SHOW_PRIVACY_SETTINGS
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementProvider
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtils
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val sharedPreferences: SharedPreferences,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val buildConfigWrapper: BuildConfigWrapper,
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val featureAnnouncementProvider: FeatureAnnouncementProvider,
    private val openWebLinksWithJetpackHelper: DeepLinkOpenWebLinksWithJetpackHelper,
    private val resourceProvider: ResourceProvider,
    private val contextProvider: ContextProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<AppSettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<AppSettingsEvent>(Channel.BUFFERED)
    val events: Flow<AppSettingsEvent> = _events.receiveAsFlow()

    init {
        // Only track on first launch, not when the saved state is restored after process death
        if (savedStateHandle.get<Boolean>(KEY_PRIVACY_SETTINGS_SHOWN) == null) {
            if (_uiState.value.isPrivacySettingsShown) {
                analyticsTrackerWrapper.track(Stat.PRIVACY_SETTINGS_OPENED)
            }
            savedStateHandle[KEY_PRIVACY_SETTINGS_SHOWN] = _uiState.value.isPrivacySettingsShown
        }
        if (buildConfigWrapper.isWhatsNewFeatureEnabled) {
            viewModelScope.launch { loadWhatsNew() }
        }
    }

    private fun createInitialState() = AppSettingsUiState(
        themeValue = sharedPreferences.getString(
            resourceProvider.getString(R.string.pref_key_app_theme),
            null
        ) ?: resourceProvider.getString(R.string.app_theme_entry_value_default),
        showOpenLinksInJetpack = openWebLinksWithJetpackHelper.shouldShowAppSetting(),
        isOpenLinksInJetpack = appPrefsWrapper.getIsOpenWebLinksWithJetpack(),
        isImageOptimize = appPrefsWrapper.isImageOptimize,
        isStripImageLocation = appPrefsWrapper.isStripImageLocation,
        isVideoOptimize = appPrefsWrapper.isVideoOptimize,
        mediaValues = mapOf(
            MediaChoice.IMAGE_MAX_SIZE to appPrefsWrapper.imageOptimizeMaxSize,
            MediaChoice.IMAGE_QUALITY to appPrefsWrapper.imageOptimizeQuality,
            MediaChoice.VIDEO_WIDTH to appPrefsWrapper.videoOptimizeWidth,
            MediaChoice.VIDEO_QUALITY to appPrefsWrapper.videoOptimizeQuality,
        ),
        showDebugSettings = buildConfigWrapper.isDebug() && buildConfigWrapper.isDebugSettingsEnabled(),
        isSendUsageStats = if (accountStore.hasAccessToken()) {
            !accountStore.account.tracksOptOut
        } else {
            sharedPreferences.getBoolean(resourceProvider.getString(R.string.pref_key_send_usage), true)
        },
        isSendCrashReports = sharedPreferences.getBoolean(
            resourceProvider.getString(R.string.pref_key_send_crash),
            true
        ),
        isPrivacySettingsShown = savedStateHandle.get<Boolean>(KEY_PRIVACY_SETTINGS_SHOWN)
            ?: (savedStateHandle.get<Boolean>(EXTRA_SHOW_PRIVACY_SETTINGS) == true),
    )

    private suspend fun loadWhatsNew() {
        // Show the cached announcement right away, then refresh it from the network
        listOf(true, false).forEach { fromCache ->
            featureAnnouncementProvider.getLatestFeatureAnnouncement(fromCache)?.let { announcement ->
                _uiState.update { it.copy(whatsNewVersion = announcement.appVersionName) }
            }
        }
    }

    /**
     * Account events are only handled while the screen is visible, so errors caused elsewhere don't queue up and
     * show as stale toasts when the user returns
     */
    fun onStart() {
        dispatcher.register(this)
        if (accountStore.hasAccessToken() && networkUtilsWrapper.isNetworkAvailable()) {
            dispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction())
        }
    }

    fun onStop() {
        dispatcher.unregister(this)
    }

    fun onThemeSelected(themeValue: String) {
        if (themeValue == _uiState.value.themeValue) return
        sharedPreferences.edit {
            putString(resourceProvider.getString(R.string.pref_key_app_theme), themeValue)
        }
        _uiState.update { it.copy(themeValue = themeValue) }
        analyticsTrackerWrapper.track(Stat.APP_SETTINGS_APPEARANCE_CHANGED, mapOf(TRACK_STYLE to themeValue))
        sendEvent(AppSettingsEvent.ApplyTheme(themeValue))
    }

    @Suppress("TooGenericExceptionCaught")
    fun onOpenLinksInJetpackChanged(enabled: Boolean) {
        try {
            if (enabled) {
                openWebLinksWithJetpackHelper.disableDeepLinks()
            } else {
                openWebLinksWithJetpackHelper.enableDeepLinks()
            }
            appPrefsWrapper.setIsOpenWebLinksWithJetpack(enabled)
            _uiState.update { it.copy(isOpenLinksInJetpack = enabled) }
            analyticsTrackerWrapper.track(
                Stat.APP_SETTINGS_OPEN_WEB_LINKS_WITH_JETPACK_CHANGED,
                mapOf(TRACK_ENABLED to enabled)
            )
        } catch (e: Exception) {
            AppLog.e(AppLog.T.UTILS, "Unable to enable or disable open with Jetpack components ", e)
            sendEvent(
                AppSettingsEvent.ShowToast(
                    if (enabled) {
                        R.string.preference_open_links_in_jetpack_setting_change_enable_error
                    } else {
                        R.string.preference_open_links_in_jetpack_setting_change_disable_error
                    }
                )
            )
        }
    }

    fun onDeviceSettingsClick() {
        analyticsTrackerWrapper.track(Stat.APP_SETTINGS_OPEN_DEVICE_SETTINGS_TAPPED)
        sendEvent(AppSettingsEvent.OpenDeviceSettings)
    }

    fun onPrivacySettingsClick() {
        analyticsTrackerWrapper.track(Stat.APP_SETTINGS_PRIVACY_SETTINGS_TAPPED)
        analyticsTrackerWrapper.track(Stat.PRIVACY_SETTINGS_OPENED)
        setPrivacySettingsShown(true)
    }

    fun onPrivacySettingsClosed() = setPrivacySettingsShown(false)

    private fun setPrivacySettingsShown(shown: Boolean) {
        savedStateHandle[KEY_PRIVACY_SETTINGS_SHOWN] = shown
        _uiState.update { it.copy(isPrivacySettingsShown = shown) }
    }

    fun onImageOptimizeChanged(enabled: Boolean) {
        appPrefsWrapper.isImageOptimize = enabled
        _uiState.update { it.copy(isImageOptimize = enabled) }
        analyticsTrackerWrapper.track(Stat.SITE_SETTINGS_OPTIMIZE_IMAGES_CHANGED, mapOf(TRACK_ENABLED to enabled))
    }

    fun onStripImageLocationChanged(enabled: Boolean) {
        appPrefsWrapper.isStripImageLocation = enabled
        _uiState.update { it.copy(isStripImageLocation = enabled) }
        analyticsTrackerWrapper.track(
            Stat.APP_SETTINGS_REMOVE_LOCATION_FROM_MEDIA_CHANGED,
            mapOf(TRACK_ENABLED to enabled)
        )
    }

    fun onVideoOptimizeChanged(enabled: Boolean) {
        appPrefsWrapper.isVideoOptimize = enabled
        _uiState.update { it.copy(isVideoOptimize = enabled) }
        analyticsTrackerWrapper.track(Stat.APP_SETTINGS_VIDEO_OPTIMIZATION_CHANGED, mapOf(TRACK_ENABLED to enabled))
    }

    fun onMediaChoiceSelected(choice: MediaChoice, value: Int) {
        val stat = when (choice) {
            MediaChoice.IMAGE_MAX_SIZE -> {
                appPrefsWrapper.imageOptimizeMaxSize = value
                Stat.APP_SETTINGS_MAX_IMAGE_SIZE_CHANGED
            }
            MediaChoice.IMAGE_QUALITY -> {
                appPrefsWrapper.imageOptimizeQuality = value
                Stat.APP_SETTINGS_IMAGE_QUALITY_CHANGED
            }
            MediaChoice.VIDEO_WIDTH -> {
                appPrefsWrapper.videoOptimizeWidth = value
                Stat.APP_SETTINGS_MAX_VIDEO_SIZE_CHANGED
            }
            MediaChoice.VIDEO_QUALITY -> {
                appPrefsWrapper.videoOptimizeQuality = value
                Stat.APP_SETTINGS_VIDEO_QUALITY_CHANGED
            }
        }
        _uiState.update { it.copy(mediaValues = it.mediaValues + (choice to value)) }
        analyticsTrackerWrapper.track(stat)
    }

    fun onWhatsNewClick() {
        analyticsTrackerWrapper.track(Stat.FEATURE_ANNOUNCEMENT_SHOWN_FROM_APP_SETTINGS)
        sendEvent(AppSettingsEvent.ShowWhatsNew)
    }

    fun onSendUsageStatsChanged(enabled: Boolean) {
        // Stores the pref and syncs the opt-out with WordPress.com
        AnalyticsUtils.updateAnalyticsPreference(contextProvider.getContext(), dispatcher, accountStore, !enabled)
        _uiState.update { it.copy(isSendUsageStats = enabled) }
    }

    fun onSendCrashReportsChanged(enabled: Boolean) {
        sharedPreferences.edit {
            putBoolean(resourceProvider.getString(R.string.pref_key_send_crash), enabled)
        }
        _uiState.update { it.copy(isSendCrashReports = enabled) }
        analyticsTrackerWrapper.track(Stat.PRIVACY_SETTINGS_REPORT_CRASHES_TOGGLED, mapOf(TRACK_ENABLED to enabled))
    }

    fun onLearnMoreClick(url: String) {
        analyticsTrackerWrapper.track(Stat.SITE_SETTINGS_LEARN_MORE_CLICKED)
        sendEvent(AppSettingsEvent.OpenUrl(url))
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.isError) {
            errorMessageFor(event.error.type)?.let { sendEvent(AppSettingsEvent.ShowToast(it)) }
        } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS && accountStore.hasAccessToken()) {
            // Keep the usage switch in sync with the value stored on WordPress.com
            _uiState.update { it.copy(isSendUsageStats = !accountStore.account.tracksOptOut) }
        }
    }

    private fun errorMessageFor(type: AccountErrorType) = when (type) {
        AccountErrorType.SETTINGS_FETCH_GENERIC_ERROR,
        AccountErrorType.ACCOUNT_FETCH_ERROR -> R.string.error_fetch_account_settings
        AccountErrorType.SETTINGS_FETCH_REAUTHORIZATION_REQUIRED_ERROR -> R.string.error_disabled_apis
        AccountErrorType.SETTINGS_POST_ERROR -> R.string.error_post_account_settings
        else -> null
    }

    private fun sendEvent(event: AppSettingsEvent) {
        viewModelScope.launch { _events.send(event) }
    }

    companion object {
        private const val TRACK_STYLE = "style"
        private const val TRACK_ENABLED = "enabled"
        private const val KEY_PRIVACY_SETTINGS_SHOWN = "privacy_settings_shown"
    }
}
