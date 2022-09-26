package org.wordpress.android.util.config

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.BuildConfig
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.persistence.RemoteConfigDao
import org.wordpress.android.fluxc.store.NotificationStore.Companion.WPCOM_PUSH_DEVICE_UUID
import org.wordpress.android.fluxc.store.mobile.FeatureFlagsStore
import org.wordpress.android.fluxc.utils.PreferenceUtils
import org.wordpress.android.modules.APPLICATION_SCOPE
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.UTILS
import org.wordpress.android.util.config.AppConfig.FeatureState
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

const val REMOTE_REFRESH_INTERVAL_IN_HOURS = 12
const val REMOTE_FLAG_PLATFORM_PARAMETER = "android"
const val ONE_HOUR = (60 * 60 * 1000) % 24

/**
 * Do not use this class outside of this package. Use [AppConfig] instead
 */
class RemoteConfig
@Inject constructor(
    private val featureFlagStore: FeatureFlagsStore,
    private val context: Context,
    @Named(APPLICATION_SCOPE) private val appScope: CoroutineScope
) {
    private val preferences by lazy { PreferenceUtils.getFluxCPreferences(context) }

    lateinit var flags: List<RemoteConfigDao.RemoteConfig>

    fun init(appScope: CoroutineScope) {
        appScope.launch {
            flags = featureFlagStore.getFeatureFlags()
        }
    }

    fun refresh(appScope: CoroutineScope, forced: Boolean) {
        appScope.launch {
            if (isRefreshNeeded() || forced) {
                fetchRemoteFlags()
                flags = featureFlagStore.getFeatureFlags()
            }
        }
    }

    private fun isRefreshNeeded(): Boolean {
        val lastModifiedFlag = featureFlagStore.getTheLastSyncedRemoteConfig()
        val timeDifferenceInMilliSeconds = System.currentTimeMillis() - lastModifiedFlag
        val differenceInHours = (timeDifferenceInMilliSeconds / ONE_HOUR)
        if (differenceInHours >= REMOTE_REFRESH_INTERVAL_IN_HOURS) return true
        return false
    }

    private suspend fun fetchRemoteFlags() {
        val response = featureFlagStore.fetchFeatureFlags(
                buildNumber = BuildConfig.VERSION_CODE.toString(),
                deviceId = preferences.getString(WPCOM_PUSH_DEVICE_UUID, null) ?: generateAndStoreUUID(),
                identifier = BuildConfig.APPLICATION_ID,
                marketingVersion = BuildConfig.VERSION_NAME,
                platform = REMOTE_FLAG_PLATFORM_PARAMETER
        )
        response.featureFlags?.let { configValues ->
            AppLog.e(UTILS, "Remote config values synced")
            AnalyticsTracker.track(
                    Stat.FEATURE_FLAGS_SYNCED_STATE,
                    configValues
            )
        }
        if (response.isError) {
            AppLog.e(UTILS, "Remote config sync failed")
        }
    }

    private fun generateAndStoreUUID(): String {
        return UUID.randomUUID().toString()
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

    fun clear() {
        AppLog.e(UTILS, "Remote config values cleared")
        flags = emptyList()
        featureFlagStore.clearAllValues()
    }
}

