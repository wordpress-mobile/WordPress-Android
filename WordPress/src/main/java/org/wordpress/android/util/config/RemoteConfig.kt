package org.wordpress.android.util.config

import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder
import org.wordpress.android.BuildConfig
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.UTILS
import javax.inject.Inject

/**
 * Do not use this class outside of this package. Use [AppConfig] instead
 */
class RemoteConfig
@Inject constructor() {
    fun refresh() {
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = Builder()
                .setMinimumFetchIntervalInSeconds(BuildConfig.REMOTE_CONFIG_FETCH_INTERVAL)
                .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        firebaseRemoteConfig.setDefaultsAsync(RemoteConfigDefaults.remoteConfigDefaults)
        firebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener { task: Task<Boolean?> ->
                    if (task.isSuccessful) {
                        AppLog.d(
                                UTILS,
                                "Remote config updated: ${task.result}"
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
}
