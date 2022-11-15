package org.wordpress.android.ui.debug

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.debug.DebugSettingsViewModel.NavigationAction.DebugCookies
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Button
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature.State.DISABLED
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature.State.ENABLED
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature.State.UNKNOWN
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Header
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Row
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.ToggleAction
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type.BUTTON
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type.FEATURE
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type.HEADER
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type.ROW
import org.wordpress.android.ui.notifications.NotificationManagerWrapper
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.ListItemInteraction.Companion.create
import org.wordpress.android.util.DebugUtils
import org.wordpress.android.util.config.FeaturesInDevelopment
import org.wordpress.android.util.config.ManualFeatureConfig
import org.wordpress.android.util.config.FeatureFlagConfig
import org.wordpress.android.util.config.RemoteConfigDefaults
import org.wordpress.android.viewmodel.ContextProvider
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.workers.weeklyroundup.WeeklyRoundupNotifier
import javax.inject.Inject
import javax.inject.Named

class DebugSettingsViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val manualFeatureConfig: ManualFeatureConfig,
    private val remoteConfig: FeatureFlagConfig,
    private val debugUtils: DebugUtils,
    private val weeklyRoundupNotifier: WeeklyRoundupNotifier,
    private val notificationManager: NotificationManagerWrapper,
    private val contextProvider: ContextProvider
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState
    private val _onNavigation = MutableLiveData<Event<NavigationAction>>()
    val onNavigation: LiveData<Event<NavigationAction>> = _onNavigation
    private var hasChange: Boolean = false

    fun start() {
        launch {
            refresh()
        }
    }

    private fun refresh() {
        val uiItems = mutableListOf<UiItem>()
        val remoteFeatures = buildRemoteFeatures()
        if (remoteFeatures.isNotEmpty()) {
            uiItems.add(Header(R.string.debug_settings_remote_features))
            uiItems.addAll(remoteFeatures)
        }
        val developedFeatures = buildDevelopedFeatures()
        if (remoteFeatures.isNotEmpty()) {
            uiItems.add(Header(R.string.debug_settings_features_in_development))
            uiItems.addAll(developedFeatures)
        }
        uiItems.add(Header(R.string.debug_settings_missing_developed_feature))
        if (hasChange) {
            uiItems.add(Button(R.string.debug_settings_restart_app, debugUtils::restartApp))
        }
        uiItems.add(Header(R.string.debug_settings_tools))
        uiItems.add(Row(R.string.debug_cookies_title, create(this::onDebugCookiesClick)))
        uiItems.add(Row(R.string.debug_settings_force_show_weekly_roundup, create(this::onForceShowWeeklyRoundupClick)))
        _uiState.value = UiState(uiItems)
    }

    private fun onDebugCookiesClick() {
        _onNavigation.value = Event(DebugCookies)
    }

    private fun onForceShowWeeklyRoundupClick() = launch(bgDispatcher) {
        weeklyRoundupNotifier.buildNotifications().forEach {
            notificationManager.notify(it.id, it.asNotificationCompatBuilder(contextProvider.getContext()).build())
        }
    }

    private fun buildDevelopedFeatures(): List<Feature> {
        return FeaturesInDevelopment.featuresInDevelopment.map { name ->
            val value = if (manualFeatureConfig.hasManualSetup(name)) {
                manualFeatureConfig.isManuallyEnabled(name)
            } else {
                null
            }
            Feature(name, value, ToggleAction(name, value?.not() ?: true, this::toggleFeature))
        }.sortedBy { it.title}
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
        }.sortedBy { it.title }
    }

    private fun toggleFeature(remoteKey: String, value: Boolean) {
        launch {
            hasChange = true
            manualFeatureConfig.setManuallyEnabled(remoteKey, value)
            refresh()
        }
    }

    data class UiState(val uiItems: List<UiItem>)
    sealed class UiItem(val type: Type) {
        data class Header(val header: Int) : UiItem(HEADER)
        data class Button(val text: Int, val clickAction: () -> Unit) : UiItem(BUTTON)
        data class Feature(val title: String, val state: State, val toggleAction: ToggleAction) : UiItem(FEATURE) {
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

        data class Row(val title: Int, val onClick: ListItemInteraction) : UiItem(ROW)

        data class ToggleAction(
            val key: String,
            val value: Boolean,
            val toggleAction: (key: String, value: Boolean) -> Unit
        ) {
            fun toggle() = toggleAction(key, value)
        }

        enum class Type {
            HEADER, FEATURE, BUTTON, ROW
        }
    }

    sealed class NavigationAction {
        object DebugCookies : NavigationAction()
    }
}
