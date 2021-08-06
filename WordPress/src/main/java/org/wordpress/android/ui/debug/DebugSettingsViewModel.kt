package org.wordpress.android.ui.debug

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.config.FeaturesInDevelopment
import org.wordpress.android.util.config.ManualFeatureConfig
import org.wordpress.android.util.config.RemoteConfig
import org.wordpress.android.util.config.RemoteConfigDefaults
import org.wordpress.android.ui.debug.DebugSettingsViewModel.FeatureUiItem.Button
import org.wordpress.android.ui.debug.DebugSettingsViewModel.FeatureUiItem.Feature
import org.wordpress.android.ui.debug.DebugSettingsViewModel.FeatureUiItem.Feature.State.DISABLED
import org.wordpress.android.ui.debug.DebugSettingsViewModel.FeatureUiItem.Feature.State.ENABLED
import org.wordpress.android.ui.debug.DebugSettingsViewModel.FeatureUiItem.Feature.State.UNKNOWN
import org.wordpress.android.ui.debug.DebugSettingsViewModel.FeatureUiItem.Header
import org.wordpress.android.ui.debug.DebugSettingsViewModel.FeatureUiItem.ToggleAction
import org.wordpress.android.ui.debug.DebugSettingsViewModel.FeatureUiItem.Type.BUTTON
import org.wordpress.android.ui.debug.DebugSettingsViewModel.FeatureUiItem.Type.FEATURE
import org.wordpress.android.ui.debug.DebugSettingsViewModel.FeatureUiItem.Type.HEADER
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class DebugSettingsViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val manualFeatureConfig: ManualFeatureConfig,
    private val remoteConfig: RemoteConfig
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState
    private val _restartAction = MutableLiveData<Event<Unit>>()
    val restartAction: LiveData<Event<Unit>> = _restartAction
    private var hasChange: Boolean = false

    fun start() {
        launch {
            refresh()
        }
    }

    private fun refresh() {
        val uiItems = mutableListOf<FeatureUiItem>()
        val remoteFeatures = buildRemoteFeatures()
        if (remoteFeatures.isNotEmpty()) {
            uiItems.add(Header(R.string.manual_config_remote_features))
            uiItems.addAll(remoteFeatures)
        }
        val developedFeatures = buildDevelopedFeatures()
        if (remoteFeatures.isNotEmpty()) {
            uiItems.add(Header(R.string.manual_config_features_in_development))
            uiItems.addAll(developedFeatures)
        }
        uiItems.add(Header(R.string.missing_developed_feature))
        if (hasChange) {
            uiItems.add(Button(R.string.manual_config_restart_app, this::restart))
        }
        _uiState.value = UiState(uiItems)
    }

    private fun restart() {
        _restartAction.value = Event(Unit)
    }

    private fun buildDevelopedFeatures(): List<Feature> {
        return FeaturesInDevelopment.featuresInDevelopment.map { name ->
            val value = if (manualFeatureConfig.hasManualSetup(name)) {
                manualFeatureConfig.isManuallyEnabled(name)
            } else {
                null
            }
            Feature(name, value, ToggleAction(name, value?.not() ?: true, this::toggleFeature))
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
                Feature(key, value, ToggleAction(key, !value, this::toggleFeature))
            } else {
                null
            }
        }
    }

    private fun toggleFeature(remoteKey: String, value: Boolean) {
        launch {
            hasChange = true
            manualFeatureConfig.setManuallyEnabled(remoteKey, value)
            refresh()
        }
    }

    data class UiState(val uiItems: List<FeatureUiItem>)
    sealed class FeatureUiItem(val type: Type) {
        data class Header(val header: Int) : FeatureUiItem(HEADER)
        data class Button(val text: Int, val clickAction: () -> Unit) : FeatureUiItem(BUTTON)
        data class Feature(val title: String, val state: State, val toggleAction: ToggleAction) : FeatureUiItem(
                FEATURE
        ) {
            constructor(title: String, enabled: Boolean?, toggleAction: ToggleAction) : this(
                    title,
                    when (enabled) {
                        true -> ENABLED
                        false -> DISABLED
                        null -> UNKNOWN
                    },
                    toggleAction
            )

            enum class State { ENABLED, DISABLED, UNKNOWN }
        }

        data class ToggleAction(
            val key: String,
            val value: Boolean,
            val toggleAction: (key: String, value: Boolean) -> Unit
        ) {
            fun toggle() = toggleAction(key, value)
        }

        enum class Type {
            HEADER, FEATURE, BUTTON
        }
    }
}
