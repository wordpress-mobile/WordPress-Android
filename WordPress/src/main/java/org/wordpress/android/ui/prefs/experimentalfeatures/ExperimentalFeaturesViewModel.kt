package org.wordpress.android.ui.prefs.experimentalfeatures

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.BuildConfig
import org.wordpress.android.util.config.GutenbergKitFeature
import javax.inject.Inject

@HiltViewModel
internal class ExperimentalFeaturesViewModel @Inject constructor(
    private val experimentalFeatures: ExperimentalFeatures,
    private val gutenbergKitFeature: GutenbergKitFeature
) : ViewModel() {
    private val _switchStates = MutableStateFlow<Map<ExperimentalFeatures.Feature, Boolean>>(emptyMap())
    val switchStates: StateFlow<Map<ExperimentalFeatures.Feature, Boolean>> = _switchStates.asStateFlow()

    init {
        val initialStates = ExperimentalFeatures.Feature.entries
            .filter { feature ->
                shouldShowFeature(feature)
            }.associateWith { feature ->
                experimentalFeatures.isEnabled(feature)
            }
        _switchStates.value = initialStates
    }

    private fun shouldShowFeature(feature: ExperimentalFeatures.Feature): Boolean {
        // only show subscribers in debug builds
        return if (BuildConfig.DEBUG.not() && feature == ExperimentalFeatures.Feature.EXPERIMENTAL_SUBSCRIBERS_FEATURE) {
            false
        } else if (gutenbergKitFeature.isEnabled()) {
            feature != ExperimentalFeatures.Feature.EXPERIMENTAL_BLOCK_EDITOR
        } else {
            feature != ExperimentalFeatures.Feature.DISABLE_EXPERIMENTAL_BLOCK_EDITOR
        }
    }

    fun onFeatureToggled(feature: ExperimentalFeatures.Feature, enabled: Boolean) {
        _switchStates.update { currentStates ->
            currentStates.toMutableMap().apply {
                this[feature] = enabled
                experimentalFeatures.setEnabled(feature, enabled)
            }
        }
    }
}
