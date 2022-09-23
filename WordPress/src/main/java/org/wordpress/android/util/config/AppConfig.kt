package org.wordpress.android.util.config

import kotlinx.coroutines.CoroutineScope
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.AppConfig.FeatureState.BuildConfigValue
import org.wordpress.android.util.config.AppConfig.FeatureState.ManuallyOverriden
import org.wordpress.android.util.config.ExperimentConfig.Variant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfig
@Inject constructor(
    private val remoteConfig: RemoteConfig,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val manualFeatureConfig: ManualFeatureConfig,
) {
    /**
     * We need to keep the value of an already loaded feature flag to make sure the value is not changed
     * while using the app. We should only reload the flags when the application is created.
     */
    private val experimentValues = mutableMapOf<String, String>()
    private val remoteConfigCheck = RemoteConfigCheck(this)

    /**
     * This method initialized the config
     */
    fun init(appScope: CoroutineScope) {
        remoteConfig.init(appScope)
        remoteConfigCheck.checkRemoteFields()
    }

    /**
     * This method triggers refresh of remote configuration.
     */
    fun refresh(appScope: CoroutineScope, forced: Boolean = false) {
        remoteConfig.refresh(appScope, forced)
    }

    /**
     * Get the enabled state of a feature flag. If the flag is enabled in the BuildConfig file, it overrides the remote
     * value. The correct approach is to disable a feature flag for a release version and only enable it remotely.
     * Once the feature is ready to be fully released, we can enable the BuildConfig value.
     * @param feature feature which we're checking remotely
     */
    fun isEnabled(feature: FeatureConfig): Boolean {
        return featureState(feature).isEnabled
    }

    /**
     * Get the enabled flag and the source where it came from.
     * @param feature feature we're checking remotely
     */
    fun featureState(feature: FeatureConfig): FeatureState {
        return buildFeatureState(feature)
    }

    private fun buildFeatureState(feature: FeatureConfig): FeatureState {
        return when {
            manualFeatureConfig.hasManualSetup(feature) -> {
                ManuallyOverriden(manualFeatureConfig.isManuallyEnabled(feature))
            }
            feature.remoteField == null -> {
                BuildConfigValue(feature.buildConfigValue)
            }
            feature.buildConfigValue -> {
                BuildConfigValue(feature.buildConfigValue)
            }
            else -> {
                remoteConfig.getFeatureState(feature.remoteField, feature.buildConfigValue)
            }
        }
    }

    /**
     * Get the currently selected variant for a given experiment. This function returns null if there is no variant
     * for the current user (and the user is in the control group).
     */
    fun getCurrentVariant(experiment: ExperimentConfig): Variant {
        val value = experimentValues.getOrPut(experiment.remoteField) {
            val remoteValue = remoteConfig.getString(experiment.remoteField)
            analyticsTracker.track(
                    Stat.EXPERIMENT_VARIANT_SET,
                    mapOf(experiment.remoteField to remoteValue)
            )
            remoteValue
        }
        return experiment.variants.find { it.value == value }
                ?: throw IllegalArgumentException("Remote variant does not match local value: $value")
    }

    /**
     * This method clears the remote config values from the database
     */
    fun clear() {
        remoteConfig.clear()
    }

    sealed class FeatureState(open val isEnabled: Boolean, val name: String) {
        data class ManuallyOverriden(
            override val isEnabled: Boolean
        ) : FeatureState(isEnabled, "manually_overriden")

        data class BuildConfigValue(
            override val isEnabled: Boolean
        ) : FeatureState(isEnabled, "build_config_value")

        data class RemoteValue(
            override val isEnabled: Boolean
        ) : FeatureState(isEnabled, "remote_source_value")

        data class StaticValue(
            override val isEnabled: Boolean
        ) : FeatureState(isEnabled, "static_source_value")

        data class DefaultValue(
            override val isEnabled: Boolean
        ) : FeatureState(isEnabled, "default_source_value")
    }
}
