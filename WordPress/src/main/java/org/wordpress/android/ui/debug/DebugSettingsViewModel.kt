package org.wordpress.android.ui.debug

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.debug.DebugSettingsViewModel.NavigationAction.DebugCookies
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature.State.DISABLED
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature.State.ENABLED
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Feature.State.UNKNOWN
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Field
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.ToggleAction
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type.BUTTON
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type.FEATURE
import org.wordpress.android.ui.debug.DebugSettingsViewModel.UiItem.Type.ROW
import org.wordpress.android.ui.debug.previews.PREVIEWS
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.notifications.NotificationManagerWrapper
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.DebugUtils
import org.wordpress.android.util.config.FeatureFlagConfig
import org.wordpress.android.util.config.FeaturesInDevelopment
import org.wordpress.android.util.config.ManualFeatureConfig
import org.wordpress.android.util.config.RemoteFeatureConfigDefaults
import org.wordpress.android.util.config.RemoteFieldConfigDefaults
import org.wordpress.android.util.config.RemoteFieldConfigRepository
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
    private val featureFlagConfig: FeatureFlagConfig,
    private val remoteFieldConfigRepository: RemoteFieldConfigRepository,
    private val debugUtils: DebugUtils,
    private val weeklyRoundupNotifier: WeeklyRoundupNotifier,
    private val notificationManager: NotificationManagerWrapper,
    private val contextProvider: ContextProvider,
    private val jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper,
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableLiveData<UiState>()
    val uiState: LiveData<UiState> = _uiState
    private val _onNavigation = MutableLiveData<Event<NavigationAction>>()
    val onNavigation: LiveData<Event<NavigationAction>> = _onNavigation
    private var hasChange: Boolean = false

    private lateinit var debugSettingsType: DebugSettingsType

    fun start(type: DebugSettingsType) {
        this.debugSettingsType = type
        launch {
            refresh(type)
        }
    }

    private fun refresh(debugSettingsType: DebugSettingsType) {
        val uiItems: MutableList<UiItem> = when (debugSettingsType) {
            DebugSettingsType.REMOTE_FEATURES -> buildRemoteFeatures().map {
                it.apply {
                    preview = { onFeaturePreviewClick(title) }.takeIf { state == ENABLED && PREVIEWS.contains(title) }
                }
            }.toMutableList()

            DebugSettingsType.REMOTE_FIELD_CONFIGS -> buildRemoteFieldConfigs().toMutableList()
            DebugSettingsType.FEATURES_IN_DEVELOPMENT -> buildDevelopedFeatures().toMutableList()
        }
        _uiState.value = UiState(uiItems)
    }

    fun onDebugCookiesClick() {
        _onNavigation.value = Event(DebugCookies)
    }

    private fun onFeaturePreviewClick(key: String) {
        _onNavigation.value = Event(NavigationAction.PreviewFragment(key))
    }

    fun onForceShowWeeklyRoundupClick() = launch(bgDispatcher) {
        if (!jetpackFeatureRemovalPhaseHelper.shouldShowNotifications())
            return@launch
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
        }.sortedBy { it.title }
    }

    private fun buildRemoteFeatures(): List<Feature> {
        return RemoteFeatureConfigDefaults.remoteFeatureConfigDefaults.mapNotNull { (key, defaultValue) ->
            val value = if (manualFeatureConfig.hasManualSetup(key)) {
                manualFeatureConfig.isManuallyEnabled(key)
            } else {
                when (defaultValue.toString()) {
                    "true", "false" -> featureFlagConfig.isEnabled(key)
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

    private fun buildRemoteFieldConfigs(): List<Field> {
        val remoteConfigFields = remoteFieldConfigRepository.remoteFields
        return RemoteFieldConfigDefaults.remoteFieldConfigDefaults.mapNotNull { remoteField ->
            val remoteConfig = remoteConfigFields.find { remoteField.key == it.key }
            remoteConfig?.let {
                Field(remoteField.key, remoteConfig.value.toString(), remoteConfig.source.toString())
            }
        }.sortedBy { it.remoteFieldKey }
    }

    private fun toggleFeature(remoteKey: String, value: Boolean) {
        launch {
            hasChange = true
            manualFeatureConfig.setManuallyEnabled(remoteKey, value)
            refresh(debugSettingsType)
        }
    }

    fun onRestartAppClick() {
        debugUtils.restartApp()
    }

    data class UiState(val uiItems: List<UiItem>)
    sealed class UiItem(val type: Type) {
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

            @Suppress("DataClassShouldBeImmutable") // We're not in prod code or diffing here, the rule is moot
            var preview: (() -> Unit)? = null
        }

        data class Field(val remoteFieldKey: String, val remoteFieldValue: String, val remoteFieldSource: String) :
            UiItem(Type.FIELD)

        data class Row(val title: Int, val onClick: ListItemInteraction) : UiItem(ROW)

        data class ToggleAction(
            val key: String,
            val value: Boolean,
            val toggleAction: (key: String, value: Boolean) -> Unit
        ) {
            fun toggle() = toggleAction(key, value)
        }

        enum class Type {
            FEATURE, BUTTON, ROW, FIELD
        }
    }

    sealed class NavigationAction {
        object DebugCookies : NavigationAction()
        data class PreviewFragment(val name: String) : NavigationAction()
    }
}
