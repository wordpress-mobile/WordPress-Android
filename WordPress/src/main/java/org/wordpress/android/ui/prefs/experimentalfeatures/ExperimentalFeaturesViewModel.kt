package org.wordpress.android.ui.prefs.experimentalfeatures

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.util.config.GutenbergKitFeature
import javax.inject.Inject
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.AppLog

private const val AFFECTED_SITES = "affected_sites"

@HiltViewModel
internal class ExperimentalFeaturesViewModel @Inject constructor(
    private val experimentalFeatures: ExperimentalFeatures,
    private val gutenbergKitFeature: GutenbergKitFeature,
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val appLogWrapper: AppLogWrapper,
) : ViewModel() {
    private val _switchStates = MutableStateFlow<Map<Feature, Boolean>>(emptyMap())
    val switchStates: StateFlow<Map<Feature, Boolean>> = _switchStates.asStateFlow()

    private val _disableApplicationPasswordDialogState = MutableStateFlow(0)
    val disableApplicationPasswordDialogState: StateFlow<Int> = _disableApplicationPasswordDialogState.asStateFlow()

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
        return if (gutenbergKitFeature.isEnabled()) {
            feature != Feature.EXPERIMENTAL_BLOCK_EDITOR
        } else {
            feature != Feature.DISABLE_EXPERIMENTAL_BLOCK_EDITOR
        }
    }

    fun onFeatureToggled(feature: Feature, enabled: Boolean) {
        // Since FluxC has not way to access the experimental features, this is a workaround to remove the
        // Application Password credentials when the feature is disabled to avoid FluxC to use them.
        // See the logic in [SiteModelExtensions.kt] and how it can not access to the feature flag
        if (feature == Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE && enabled.not()) {
            val affectedSites = applicationPasswordLoginHelper.getApplicationPasswordSitesCount()
            if (affectedSites > 0) {
                _disableApplicationPasswordDialogState.value = affectedSites
            } else {
                confirmDisableApplicationPassword()
            }
        } else {
            _switchStates.update { currentStates ->
                currentStates.toMutableMap().apply {
                    this[feature] = enabled
                    experimentalFeatures.setEnabled(feature, enabled)
                }
            }
        }
    }

    fun dismissDisableApplicationPassword() {
        _disableApplicationPasswordDialogState.value = 0
    }

    @Suppress("TooGenericExceptionCaught")
    fun confirmDisableApplicationPassword() {
        _disableApplicationPasswordDialogState.value = 0
        _switchStates.update { currentStates ->
            currentStates.toMutableMap().apply {
                this[Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE] = false
                experimentalFeatures.setEnabled(Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE, false)
            }
        }

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
}
