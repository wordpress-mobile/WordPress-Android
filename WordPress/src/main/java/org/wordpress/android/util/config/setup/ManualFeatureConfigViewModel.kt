package org.wordpress.android.util.config.setup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.config.ConsolidatedMediaPickerFeatureConfig
import org.wordpress.android.util.config.FeatureConfig
import org.wordpress.android.util.config.RemoteConfig
import org.wordpress.android.util.config.RemoteConfigDefaults
import org.wordpress.android.util.config.setup.ManualFeatureConfigViewModel.FeatureUiItem.Feature
import org.wordpress.android.util.config.setup.ManualFeatureConfigViewModel.FeatureUiItem.Header
import org.wordpress.android.util.config.setup.ManualFeatureConfigViewModel.FeatureUiItem.Type.FEATURE
import org.wordpress.android.util.config.setup.ManualFeatureConfigViewModel.FeatureUiItem.Type.HEADER
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class ManualFeatureConfigViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val manualFeatureConfig: ManualFeatureConfig,
    private val remoteConfig: RemoteConfig,
    consolidatedMediaPickerFeatureConfig: ConsolidatedMediaPickerFeatureConfig
) : ScopedViewModel(mainDispatcher) {
    // Local features
    private val localFeature = mapOf<String, FeatureConfig>(
            "Consolidated media picker" to consolidatedMediaPickerFeatureConfig
    )
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState

    fun start() {
        launch {
            refresh()
        }
    }

    private fun refresh() {
        val uiItems = mutableListOf<FeatureUiItem>()
        val remoteFeatures = buildRemoteFeatures()
        if (remoteFeatures.isNotEmpty()) {
            uiItems.add(Header(R.string.remote_features))
            uiItems.addAll(remoteFeatures)
        }
        val developedFeatures = buildDevelopedFeatures()
        if (remoteFeatures.isNotEmpty()) {
            uiItems.add(Header(R.string.features_in_development))
            uiItems.addAll(developedFeatures)
        }
        _uiState.value = UiState(uiItems)
    }

    private fun buildDevelopedFeatures(): List<Feature> {
        return localFeature.map { (key, featureConfig) ->
            val value = if (manualFeatureConfig.hasManualSetup(featureConfig)) {
                manualFeatureConfig.isManuallyEnabled(featureConfig)
            } else {
                featureConfig.buildConfigValue
            }
            Feature(key, value) {
                toggleFeature(featureConfig, !value)
            }
        }
    }

    private fun buildRemoteFeatures(): List<Feature> {
        return RemoteConfigDefaults.remoteConfigDefaults.mapNotNull { (key, defaultValue) ->
            val value = if (manualFeatureConfig.hasManualSetup(key)) {
                manualFeatureConfig.isManuallyEnabled(key)
            } else {
                when (defaultValue.toString()) {
                    "true", "false" -> remoteConfig.isEnabled(key)
                    else -> null
                }
            }
            if (value != null) {
                Feature(key, value) {
                    toggleFeature(key, !value)
                }
            } else {
                null
            }
        }
    }

    private fun toggleFeature(remoteKey: String, value: Boolean) {
        launch {
            manualFeatureConfig.setManuallyEnabled(remoteKey, value)
            refresh()
        }
    }

    private fun toggleFeature(feature: FeatureConfig, value: Boolean) {
        launch {
            manualFeatureConfig.setManuallyEnabled(feature, value)
            refresh()
        }
    }

    data class UiState(val uiItems: List<FeatureUiItem>)
    sealed class FeatureUiItem(val type: Type) {
        data class Header(val header: Int) : FeatureUiItem(HEADER)
        data class Feature(val title: String, val enabled: Boolean, val toggleAction: () -> Unit) : FeatureUiItem(FEATURE)
        enum class Type {
            HEADER, FEATURE
        }
    }
}
