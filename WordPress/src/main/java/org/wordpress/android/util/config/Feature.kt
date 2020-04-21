package org.wordpress.android.util.config

import org.wordpress.android.BuildConfig

/**
 * A class that represents a feature behind a feature flag that can be remotely turned on or off.
 * To add a feature don't forget to update the remote_config_defaults.xml file.
 * @param buildConfigValue is the field in the BuildConfig file
 * @param remoteField is the key of the feature flag in the remote config file
 * @param initializeOnStart marks whether this flag needs to be available from the start of the app.
 * If true, the feature flag will be available on the next start of the app after the remote config is loaded
 */
enum class Feature(val buildConfigValue: Boolean, val remoteField: String, val initializeOnStart: Boolean) {
    TENOR(BuildConfig.TENOR_AVAILABLE, "tenor_available", true)
}
