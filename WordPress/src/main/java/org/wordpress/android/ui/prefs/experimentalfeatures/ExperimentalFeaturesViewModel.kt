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
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.accounts.login.ApplicationPasswordLoginHelper
import org.wordpress.android.util.config.GutenbergKitFeature
import javax.inject.Inject
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.AppLog

@HiltViewModel
internal class ExperimentalFeaturesViewModel @Inject constructor(
    private val experimentalFeatures: ExperimentalFeatures,
    private val gutenbergKitFeature: GutenbergKitFeature,
    private val applicationPasswordLoginHelper: ApplicationPasswordLoginHelper,
    private val appLogWrapper: AppLogWrapper,
) : ViewModel() {
    private val _switchStates = MutableStateFlow<Map<Feature, Boolean>>(emptyMap())
    val switchStates: StateFlow<Map<Feature, Boolean>> = _switchStates.asStateFlow()

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
        // only show subscribers in debug builds
        return if (BuildConfig.DEBUG.not() && feature == Feature.EXPERIMENTAL_SUBSCRIBERS_FEATURE) {
            false
        } else if (BuildConfig.DEBUG.not() && feature == Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE) {
            // only show application password in debug builds
            false
        } else if (gutenbergKitFeature.isEnabled()) {
            feature != Feature.EXPERIMENTAL_BLOCK_EDITOR
        } else {
            feature != Feature.DISABLE_EXPERIMENTAL_BLOCK_EDITOR
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun onFeatureToggled(feature: Feature, enabled: Boolean) {
        _switchStates.update { currentStates ->
            currentStates.toMutableMap().apply {
                this[feature] = enabled
                experimentalFeatures.setEnabled(feature, enabled)
            }
        }
        viewModelScope.launch {
            try {
                // Since FluxC has not way to access the experimental features, this is a workaround to remove the
                // Application Password credentials when the feature is disabled to avoid FluxC to use them.
                // See the logic in [SiteModelExtensions.kt] and how it can not access to the feature flag
                if (feature == Feature.EXPERIMENTAL_APPLICATION_PASSWORD_FEATURE && enabled.not()) {
                    applicationPasswordLoginHelper.removeAllApplicationPasswordCredentials()
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
