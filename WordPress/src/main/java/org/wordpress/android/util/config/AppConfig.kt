package org.wordpress.android.util.config

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfig
@Inject constructor(private val remoteConfig: RemoteConfig) {
    /**
     * We need to keep the value of an already loaded feature flag to make sure the value is not changed while using the app.
     * We should only reload the flags when the application is created.
     */
    private val enabledFeatures = mutableMapOf<Feature, Boolean>()

    /**
     * This method initialized the config and triggers refresh of remote configuration.
     */
    fun init() {
        remoteConfig.init()
    }

    /**
     * Get the enabled state of a feature flag. If the flag is enabled in the BuildConfig file, it overrides the
     * remote value. The correct approach is to disable a feature flag for a release version and only enable it remotely.
     * Once the feature is ready to be fully released, we can enable the BuildConfig value.
     */
    fun isEnabled(feature: Feature): Boolean {
        val cachedValue = enabledFeatures[feature]
        return if (cachedValue == null) {
            val loadedValue = feature.buildConfigValue || remoteConfig.isEnabled(feature.remoteField)
            enabledFeatures[feature] = loadedValue
            loadedValue
        } else
            cachedValue
    }
}
