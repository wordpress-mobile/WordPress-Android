package org.wordpress.android.util.config

import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder
import org.wordpress.android.BuildConfig
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.UTILS
import javax.inject.Inject

/**
 * Do not use this class outside of this package. Use [AppConfig] instead
 */
class RemoteConfig
@Inject constructor(private val appPrefsWrapper: AppPrefsWrapper) {
    fun init() {
        val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = Builder()
                .setMinimumFetchIntervalInSeconds(BuildConfig.REMOTE_CONFIG_FETCH_INTERVAL)
                .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        firebaseRemoteConfig.setDefaultsAsync(RemoteConfigDefaults.remoteConfigDefaults)
        firebaseRemoteConfig.activate()
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
        firebaseRemoteConfig.ensureInitialized().addOnSuccessListener {
            appPrefsWrapper.flagsFetchedSuccessfully = true
        }
    }

    fun isEnabled(field: String): Boolean = FirebaseRemoteConfig.getInstance().getBoolean(field)
    fun getString(field: String): String = FirebaseRemoteConfig.getInstance().getString(field)
    fun isInitialized(): Boolean = appPrefsWrapper.flagsFetchedSuccessfully
}
