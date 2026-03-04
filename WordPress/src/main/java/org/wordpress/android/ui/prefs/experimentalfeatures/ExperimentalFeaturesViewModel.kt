package org.wordpress.android.ui.prefs.experimentalfeatures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.BuildConfig
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.config.GutenbergKitFeature
import javax.inject.Inject

private const val AFFECTED_SITES = "affected_sites"

@HiltViewModel
internal class ExperimentalFeaturesViewModel @Inject constructor(
    private val experimentalFeatures: ExperimentalFeatures,
    private val gutenbergKitFeature: GutenbergKitFeature,
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val appLogWrapper: AppLogWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
) : ViewModel() {
    private val _switchStates = MutableStateFlow<Map<Feature, Boolean>>(emptyMap())
    val switchStates: StateFlow<Map<Feature, Boolean>> = _switchStates.asStateFlow()

    private val _applicationPasswordDialogState =
        MutableStateFlow<ApplicationPasswordDialogState>(ApplicationPasswordDialogState.None)
    val applicationPasswordDialogState: StateFlow<ApplicationPasswordDialogState> =
        _applicationPasswordDialogState.asStateFlow()

    private val _showNetworkDebuggingError = MutableStateFlow(false)
    val showNetworkDebuggingError: StateFlow<Boolean> = _showNetworkDebuggingError.asStateFlow()

    init {
        val initialStates = Feature.entries
            .filter { feature ->
                shouldShowFeature(feature)
            }.associateWith { feature ->
                experimentalFeatures.isEnabled(feature)
            }
        _switchStates.value = initialStates
    }

    private fun shouldShowFeature(feature: Feature): Boolean {
        // Only show Post Types feature in debug builds
        return if (feature == Feature.EXPERIMENTAL_POST_TYPES) {
            BuildConfig.DEBUG
        } else if (gutenbergKitFeature.isEnabled()) {
            feature != Feature.EXPERIMENTAL_BLOCK_EDITOR
        } else {
            feature != Feature.DISABLE_EXPERIMENTAL_BLOCK_EDITOR
        }
    }

    fun onFeatureToggled(feature: Feature, enabled: Boolean) {
        when (feature) {
            // Since FluxC has no way to access the experimental features, this is a workaround to
            // remove the Application Password credentials when the feature is disabled to avoid
            // FluxC to use them.
            // See the logic in [SiteModelExtensions.kt] and how it can not access to the feature flag
            Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE -> {
                if (enabled) {
                    _applicationPasswordDialogState.value = ApplicationPasswordDialogState.Info
                } else {
                    val affectedSites = applicationPasswordLoginHelper.getResettableApplicationPasswordSitesCount()
                    if (affectedSites > 0) {
                        _applicationPasswordDialogState.value =
                            ApplicationPasswordDialogState.Disable(affectedSites)
                    } else {
                        confirmDisableApplicationPassword()
                    }
                }
            }
            // Prevent disabling the feature if network tracking is currently enabled
            Feature.NETWORK_DEBUGGING -> {
                if (!enabled && appPrefsWrapper.isTrackNetworkRequestsEnabled) {
                    _showNetworkDebuggingError.value = true
                } else {
                    setFeatureSwitchState(feature, enabled)
                }
            }
            else -> setFeatureSwitchState(feature, enabled)
        }
    }

    fun dismissNetworkDebuggingError() {
        _showNetworkDebuggingError.value = false
    }

    fun dismissDisableApplicationPassword() {
        _applicationPasswordDialogState.value = ApplicationPasswordDialogState.None
    }

    @Suppress("TooGenericExceptionCaught")
    fun confirmDisableApplicationPassword() {
        _applicationPasswordDialogState.value = ApplicationPasswordDialogState.None
        setFeatureSwitchState(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE, false)

        viewModelScope.launch {
            try {
                val removedCredentialSites = applicationPasswordLoginHelper.removeAllApplicationPasswordCredentials()
                if (removedCredentialSites > 0) {
                    val properties: MutableMap<String, String?> = HashMap()
                    properties[AFFECTED_SITES] = removedCredentialSites.toString()
                    AnalyticsTracker.track(Stat.APPLICATION_PASSWORD_SET_OFF, properties)
                }
            } catch (exception: Throwable) {
                appLogWrapper.e(
                    AppLog.T.DB,
                    "Error when trying to remove Application Password credentials: ${exception.stackTrace}"
                )
            }
        }
    }

    fun dismissApplicationPasswordInfo() {
        _applicationPasswordDialogState.value = ApplicationPasswordDialogState.None
        setFeatureSwitchState(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE, false)
    }

    fun confirmApplicationPasswordInfo() {
        _applicationPasswordDialogState.value = ApplicationPasswordDialogState.None
        setFeatureSwitchState(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE, true)
    }

    private fun setFeatureSwitchState(
        feature: Feature,
        enabled: Boolean
    ) {
        _switchStates.update { currentStates ->
            currentStates.toMutableMap().apply {
                this[feature] = enabled
                experimentalFeatures.setEnabled(feature, enabled)
            }
        }
    }

    sealed class ApplicationPasswordDialogState {
        /**
         * No dialog
         */
        data object None : ApplicationPasswordDialogState()

        /**
         * General info dialog
         */
        data object Info : ApplicationPasswordDialogState()

        /**
         * Dialog representing the disable feature state, and the affected sites if any.
         */
        data class Disable(val affectedSites: Int) : ApplicationPasswordDialogState()
    }
}
