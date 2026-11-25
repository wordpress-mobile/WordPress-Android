package org.wordpress.android.util.config

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.BuildConfig
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsRestClient
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao.FeatureFlag
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao.FeatureFlagValueSource
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
import javax.inject.Singleton

const val FEATURE_FLAG_PLATFORM_PARAMETER = "android"

/**
 * Do not use this class outside of this package. Use [AppConfig] instead
 */

@Singleton
class FeatureFlagConfig
@Inject constructor(
    private val featureFlagStore: FeatureFlagsStore,
    private val context: Context,
    @Named(APPLICATION_SCOPE) private val appScope: CoroutineScope
) {
    private val preferences by lazy { PreferenceUtils.getFluxCPreferences(context) }

    var flags: List<FeatureFlag> = arrayListOf()

    private val storedFlagsLoaded = CompletableDeferred<Unit>()

    private val _updates = MutableStateFlow(0)

    /** Changes each time [flags] is reloaded from the database, after the initial load and every refresh. */
    val updates: StateFlow<Int> = _updates

    fun hasLoadedStoredFlags() = storedFlagsLoaded.isCompleted

    suspend fun awaitStoredFlags() = storedFlagsLoaded.await()

    /** Whether the stored value of [remoteField] came from the server rather than a default written locally. */
    fun isFetchedValue(remoteField: String) =
        flags.find { it.key == remoteField }?.source == FeatureFlagValueSource.REMOTE

    fun init(appScope: CoroutineScope) {
        appScope.launch {
            flags = featureFlagStore.getFeatureFlags()
            onFlagsLoaded()
            // If the flags are empty, then this means that the
            if (flags.isEmpty()) {
                insertRemoteConfigDefaultsInDatabase()
                refresh(appScope)
            }
        }
    }

    private fun insertRemoteConfigDefaultsInDatabase() {
        RemoteFeatureConfigDefaults.remoteFeatureConfigDefaults.mapNotNull { remoteField ->
            val defaultValue = when (remoteField.value.toString()) {
                "true" -> true
                "false" -> false
                else -> null
            }
            defaultValue?.let {
                featureFlagStore.insertFeatureFlagValue(
                    remoteField.key,
                    defaultValue
                )
            }
        }
    }

    fun refresh(appScope: CoroutineScope): Job =
        appScope.launch {
            fetchRemoteFlags()
            flags = featureFlagStore.getFeatureFlags()
            onFlagsLoaded()
        }

    private fun onFlagsLoaded() {
        storedFlagsLoaded.complete(Unit)
        _updates.update { it + 1 }
    }

    private suspend fun fetchRemoteFlags() {
        val response = featureFlagStore.fetchFeatureFlags(
            FeatureFlagsRestClient.FeatureFlagsPayload(
                buildNumber = BuildConfig.VERSION_CODE.toString(),
                deviceId = preferences.getString(WPCOM_PUSH_DEVICE_UUID, null)
                    ?: generateAndStoreUUID(),
                identifier = BuildConfig.APPLICATION_ID,
                marketingVersion = BuildConfig.VERSION_NAME,
                platform = FEATURE_FLAG_PLATFORM_PARAMETER,
                osVersion = android.os.Build.VERSION.RELEASE
            )
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

    fun isEnabled(field: String): Boolean {
        return flags.find { it.key == field }?.value ?: false
    }

    fun getString(field: String): String {
        return flags.find { it.key == field }?.key ?: ""
    }

    fun getFeatureState(remoteField: String, buildConfigValue: Boolean): FeatureState {
        val remoteFeatureFlag = flags.find { it.key == remoteField }
        return if (remoteFeatureFlag == null) {
            val defaultValue = getRemoteConfigDefaultValue(remoteField)
            storeMissingFlag(remoteField, defaultValue ?: buildConfigValue)
            if (defaultValue != null) {
                FeatureState.DefaultValue(defaultValue)
            } else {
                FeatureState.BuildConfigValue(buildConfigValue)
            }
        } else {
            FeatureState.RemoteValue(remoteFeatureFlag.value)
        }
    }

    private fun storeMissingFlag(remoteField: String, value: Boolean) {
        // Until init() has loaded the stored flags, a flag can look missing only because it isn't loaded yet,
        // and storing the default would overwrite its stored remote value
        if (!hasLoadedStoredFlags()) return
        appScope.launch {
            featureFlagStore.insertFeatureFlagValue(remoteField, value)
            flags = featureFlagStore.getFeatureFlags()
        }
    }

    private fun getRemoteConfigDefaultValue(remoteField: String): Boolean? {
        val defaultValue = RemoteFeatureConfigDefaults.remoteFeatureConfigDefaults[remoteField]
        return when (defaultValue.toString()) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    fun clear() {
        AppLog.e(UTILS, "Feature flag values cleared")
        flags = emptyList()
        featureFlagStore.clearAllValues()
    }
}

