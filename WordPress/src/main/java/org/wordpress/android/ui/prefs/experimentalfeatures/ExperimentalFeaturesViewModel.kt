package org.wordpress.android.ui.prefs.experimentalfeatures

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.BuildConfig
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.prefs.experimentalfeatures.ExperimentalFeatures.Feature
import org.wordpress.android.util.config.GutenbergKitFeature
import javax.inject.Inject

@HiltViewModel
internal class ExperimentalFeaturesViewModel @Inject constructor(
    private val experimentalFeatures: ExperimentalFeatures,
    private val gutenbergKitFeature: GutenbergKitFeature,
    private val appPrefsWrapper: AppPrefsWrapper,
) : ViewModel() {
    private val _switchStates = MutableStateFlow<Map<Feature, Boolean>>(emptyMap())
    val switchStates: StateFlow<Map<Feature, Boolean>> = _switchStates.asStateFlow()

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
}
