package org.wordpress.android.util.config

import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.ExperimentConfig.Variant
import java.lang.IllegalArgumentException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfig
@Inject constructor(private val remoteConfig: RemoteConfig, private val analyticsTracker: AnalyticsTrackerWrapper) {
    /**
     * We need to keep the value of an already loaded feature flag to make sure the value is not changed while using the app.
     * We should only reload the flags when the application is created.
     */
    private val enabledFeatures = mutableMapOf<FeatureConfig, Boolean>()
    private val experimentValues = mutableMapOf<ExperimentConfig, String>()

    /**
     * This method initialized the config and triggers refresh of remote configuration.
     */
    fun refresh() {
        remoteConfig.refresh()
    }

    /**
     * Get the enabled state of a feature flag. If the flag is enabled in the BuildConfig file, it overrides the
     * remote value. The correct approach is to disable a feature flag for a release version and only enable it remotely.
     * Once the feature is ready to be fully released, we can enable the BuildConfig value.
     * @param feature feature which we're checking remotely
     */
    fun isEnabled(feature: FeatureConfig): Boolean {
        if (feature.remoteField == null) {
            return feature.buildConfigValue
        }
        return enabledFeatures.getOrPut(feature) {
            val loadedValue = feature.buildConfigValue || remoteConfig.isEnabled(feature.remoteField)
            enabledFeatures[feature] = loadedValue
            analyticsTracker.track(Stat.FEATURE_FLAG_SET, mapOf(feature.remoteField to loadedValue))
            loadedValue
        }
    }

    /**
     * Get the currently selected variant for a given experiment. This function returns null if there is no variant
     * for the current user (and the user is in the control group).
     */
    fun getCurrentVariant(experiment: ExperimentConfig): Variant {
        val value = experimentValues.getOrPut(experiment) {
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
}
