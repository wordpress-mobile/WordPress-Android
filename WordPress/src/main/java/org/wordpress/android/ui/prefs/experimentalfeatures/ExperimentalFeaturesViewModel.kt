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
    private val gutenbergKitFeature: GutenbergKitFeature
) : ViewModel() {
    private val _switchStates = MutableStateFlow<Map<ExperimentalFeature, Boolean>>(emptyMap())
    val switchStates: StateFlow<Map<ExperimentalFeature, Boolean>> = _switchStates.asStateFlow()

    init {
        val initialStates = ExperimentalFeature.entries
            .filter { feature ->
                shouldShowFeature(feature)
            }.associateWith { feature ->
                feature.isEnabled()
            }
        _switchStates.value = initialStates
    }

    private fun shouldShowFeature(feature: ExperimentalFeature): Boolean {
        // only show subscribers in debug builds
        return if (BuildConfig.DEBUG.not() && feature == ExperimentalFeature.EXPERIMENTAL_SUBSCRIBERS_FEATURE) {
            false
        } else if (gutenbergKitFeature.isEnabled()) {
            feature != ExperimentalFeature.EXPERIMENTAL_BLOCK_EDITOR
        } else {
            feature != ExperimentalFeature.DISABLE_EXPERIMENTAL_BLOCK_EDITOR
        }
    }

    fun onFeatureToggled(feature: ExperimentalFeature, enabled: Boolean) {
        _switchStates.update { currentStates ->
            currentStates.toMutableMap().apply {
                this[feature] = enabled
                feature.setEnabled(enabled)
            }
        }
    }
}
