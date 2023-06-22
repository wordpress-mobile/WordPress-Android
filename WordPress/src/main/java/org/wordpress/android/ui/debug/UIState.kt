package org.wordpress.android.ui.debug

data class UiState(val uiItems: List<UiItem>)
sealed class UiItem(open val type: DebugSettingsType) {
    sealed class FeatureFlag(
        val title: String,
        val state: State,
        val toggleAction: ToggleAction,
        override val type: DebugSettingsType
    ) : UiItem(type) {
        data class RemoteFeatureFlag(
            val remoteKey: String,
            val enabled: Boolean?,
            val toggle: ToggleAction,
            val source: String
        ) : FeatureFlag(remoteKey, enabled, toggle, DebugSettingsType.REMOTE_FEATURES)

        data class LocalFeatureFlag(
            val featureName: String,
            val enabled: Boolean?,
            val toggle: ToggleAction
        ) : FeatureFlag(featureName, enabled, toggle, DebugSettingsType.FEATURES_IN_DEVELOPMENT)

        constructor(
            title: String,
            enabled: Boolean?,
            toggleAction: ToggleAction,
            debugSettingsType: DebugSettingsType
        ) : this(
            title,
            when (enabled) {
                true -> State.ENABLED
                false -> State.DISABLED
                null -> State.UNKNOWN
            },
            toggleAction,
            debugSettingsType
        )

        enum class State { ENABLED, DISABLED, UNKNOWN }

        @Suppress("DataClassShouldBeImmutable") // We're not in prod code or diffing here, the rule is moot
        var preview: (() -> Unit)? = null
    }

    data class Field(val remoteFieldKey: String, val remoteFieldValue: String, val remoteFieldSource: String) :
        UiItem(DebugSettingsType.REMOTE_FIELD_CONFIGS)

    data class ToggleAction(
        val key: String,
        val value: Boolean,
        val toggleAction: (key: String, value: Boolean) -> Unit
    ) {
        fun toggle() = toggleAction(key, value)
    }
}
