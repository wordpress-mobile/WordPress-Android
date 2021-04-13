package org.wordpress.android.util.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig.VALUE_SOURCE_DEFAULT
import com.google.firebase.remoteconfig.FirebaseRemoteConfig.VALUE_SOURCE_REMOTE
import com.google.firebase.remoteconfig.FirebaseRemoteConfig.VALUE_SOURCE_STATIC
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder
import org.wordpress.android.BuildConfig
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.UTILS
import org.wordpress.android.util.config.AppConfig.FeatureState
import javax.inject.Inject

/**
 * Do not use this class outside of this package. Use [AppConfig] instead
 */
class RemoteConfig
@Inject constructor() {
    fun init() {
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = Builder()
                .setMinimumFetchIntervalInSeconds(BuildConfig.REMOTE_CONFIG_FETCH_INTERVAL)
                .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        firebaseRemoteConfig.setDefaultsAsync(RemoteConfigDefaults.remoteConfigDefaults)
        firebaseRemoteConfig.activate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                AppLog.d(
                        UTILS,
                        "Remote config activated: ${task.result}"
                )
            } else {
                AppLog.e(
                        UTILS,
                        "Remote config activate failed"
                )
            }
        }
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                AppLog.d(
                        UTILS,
                        "Remote config fetched and activated: ${task.result}"
                )
            } else {
                AppLog.e(
                        UTILS,
                        "Remote config fetchAndActivate failed"
                )
            }
        }
    }

    fun refresh() {
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        firebaseRemoteConfig.fetch()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        AppLog.d(
                                UTILS,
                                "Remote config fetched: ${task.result}"
                        )
                    } else {
                        AppLog.e(
                                UTILS,
                                "Remote config fetch failed"
                        )
                    }
                }
    }

    fun isEnabled(field: String): Boolean = FirebaseRemoteConfig.getInstance().getBoolean(field)
    fun getString(field: String): String = FirebaseRemoteConfig.getInstance().getString(field)
    fun getFeatureState(remoteField: String): FeatureState {
        val value = FirebaseRemoteConfig.getInstance().getValue(remoteField)
        return when (value.source) {
            VALUE_SOURCE_DEFAULT -> FeatureState.DefaultValue(value.asBoolean())
            VALUE_SOURCE_REMOTE -> FeatureState.RemoteValue(value.asBoolean())
            VALUE_SOURCE_STATIC -> FeatureState.StaticValue(value.asBoolean())
            else -> FeatureState.StaticValue(value.asBoolean())
        }
    }
}
