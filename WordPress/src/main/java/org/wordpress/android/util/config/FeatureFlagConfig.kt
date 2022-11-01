package org.wordpress.android.util.config

import android.content.Context
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.BuildConfig
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao.FeatureFlag
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

const val FEATURE_FLAG_PLATFORM_PARAMETER = "android"

/**
 * Do not use this class outside of this package. Use [AppConfig] instead
 */
class FeatureFlagConfig
@Inject constructor(
    private val featureFlagStore: FeatureFlagsStore,
    private val context: Context,
    @Named(APPLICATION_SCOPE) private val appScope: CoroutineScope
) {
    private val preferences by lazy { PreferenceUtils.getFluxCPreferences(context) }

    lateinit var flags: List<FeatureFlag>

    fun init(appScope: CoroutineScope) {
        appScope.launch {
            flags = featureFlagStore.getFeatureFlags()
            if (flags.isEmpty()) {
                refresh(appScope)
            }
        }
    }

    fun refresh(appScope: CoroutineScope) {
        appScope.launch {
            fetchRemoteFlags()
            flags = featureFlagStore.getFeatureFlags()
        }
    }

    private suspend fun fetchRemoteFlags() {
        val response = featureFlagStore.fetchFeatureFlags(
                buildNumber = BuildConfig.VERSION_CODE.toString(),
                deviceId = preferences.getString(WPCOM_PUSH_DEVICE_UUID, null) ?: generateAndStoreUUID(),
                identifier = BuildConfig.APPLICATION_ID,
                marketingVersion = BuildConfig.VERSION_NAME,
                platform = FEATURE_FLAG_PLATFORM_PARAMETER
        )
        response.featureFlags?.let { configValues ->
            AppLog.e(UTILS, "Feature flag values synced")
            AnalyticsTracker.track(
                    Stat.FEATURE_FLAGS_SYNCED_STATE,
                    configValues
            )
        }
        if (response.isError) {
            AppLog.e(UTILS, "Feature flag values sync failed")
        }
    }

    private fun generateAndStoreUUID(): String {
        return UUID.randomUUID().toString()
    }

    fun isEnabled(field: String): Boolean = flags.find { it.key == field }!!.value
    fun getString(field: String): String = FirebaseRemoteConfig.getInstance().getString(field)
    fun getFeatureState(remoteField: String, buildConfigValue: Boolean): FeatureState {
        val remoteFeatureFlag = flags.find { it.key == remoteField }
        return if (remoteFeatureFlag == null) {
            appScope.launch { featureFlagStore.insertFeatureFlagValue(remoteField, buildConfigValue) }
            FeatureState.BuildConfigValue(buildConfigValue)
        } else {
            FeatureState.RemoteValue(remoteFeatureFlag.value)
        }
    }

    fun clear() {
        AppLog.e(UTILS, "Feature flag values cleared")
        flags = emptyList()
        featureFlagStore.clearAllValues()
    }
}

