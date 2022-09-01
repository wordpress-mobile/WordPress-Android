package org.wordpress.android.util.config

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.persistence.RemoteConfigDao
import org.wordpress.android.fluxc.store.mobile.FeatureFlagsStore
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.UTILS
import org.wordpress.android.util.config.AppConfig.FeatureState
import javax.inject.Inject
import javax.inject.Named

/**
 * Do not use this class outside of this package. Use [AppConfig] instead
 */
class RemoteConfig
@Inject constructor(
    private val featureFlagStore: FeatureFlagsStore,
    @Named(APPLICATION_SCOPE) private val appScope: CoroutineScope
) {
    lateinit var flags: List<RemoteConfigDao.RemoteConfig>

    fun init(appScope: CoroutineScope) {
        Log.e("Fetching remote flags", "initiated")
        appScope.launch {
            flags = featureFlagStore.getFeatureFlags()
        }
    }

    private suspend fun fetchRemoteFlags() {
        Log.e("Refreshing remote flags", " ")
        val response = featureFlagStore.fetchFeatureFlags(
                deviceId = "12345",
                platform = "android",
                buildNumber = "570",
                marketingVersion = "15.1.1",
                identifier = "com.jetpack.android"
        )
        Log.e("response", response.toString())
        response.featureFlags?.let { configValues ->
            Log.e("Remote config values", configValues.toString())
            AnalyticsTracker.track(
                    Stat.FEATURE_FLAGS_SYNCED_STATE,
                    configValues
            )
        }
        if (response.isError) {
            AppLog.e(
                    UTILS,
                    "Remote config sync failed"
            )
        }
    }

    fun refresh(appScope: CoroutineScope) {
        appScope.launch {
            fetchRemoteFlags()
            flags = featureFlagStore.getFeatureFlags()
        }
    }

    fun isEnabled(field: String): Boolean = FirebaseRemoteConfig.getInstance().getBoolean(field)
    fun getString(field: String): String = FirebaseRemoteConfig.getInstance().getString(field)
    fun getFeatureState(remoteField: String, buildConfigValue: Boolean): FeatureState {
        val remoteConfig = flags.find { it.key == remoteField }
        return if (remoteConfig == null) {
            appScope.launch { featureFlagStore.insertRemoteConfigValue(remoteField, buildConfigValue) }
            FeatureState.BuildConfigValue(buildConfigValue)
        } else {
            FeatureState.RemoteValue(remoteConfig.value)
        }
    }
}

