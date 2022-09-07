package org.wordpress.android.fluxc.store.mobile

import android.util.Log
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsError
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsRestClient
import org.wordpress.android.fluxc.persistence.RemoteConfigDao
import org.wordpress.android.fluxc.persistence.RemoteConfigDao.RemoteConfig
import org.wordpress.android.fluxc.persistence.RemoteConfigDao.RemoteConfigValueSource
import org.wordpress.android.fluxc.store.Store
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFlagsStore @Inject constructor(
    private val featureFlagsRestClient: FeatureFlagsRestClient,
    private val remoteConfigDao: RemoteConfigDao,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchFeatureFlags(
        deviceId: String,
        identifier: String
    ) = coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetch feature-flags") {
        val payload = featureFlagsRestClient.fetchFeatureFlags(
                deviceId,
                identifier
        )
        return@withDefaultContext when {
            payload.isError -> FeatureFlagsResult(payload.error)
            payload.featureFlags != null -> {
                clearAllValues()
                remoteConfigDao.insertRemoteConfig(payload.featureFlags)
                FeatureFlagsResult(payload.featureFlags)
            }
            else -> FeatureFlagsResult(FeatureFlagsError(GENERIC_ERROR))
        }
    }

    fun getFeatureFlags(): List<RemoteConfig> {
        return remoteConfigDao.getRemoteConfigs()
    }

    fun getFeature(key: String): List<RemoteConfig> {
        return remoteConfigDao.getRemoteConfig(key)
    }

    fun insertRemoteConfigValue(key: String, value: Boolean) {
        remoteConfigDao.insertRemoteConfig(
                RemoteConfig(
                        key = key,
                        value = value,
                        createdAt = System.currentTimeMillis(),
                        modifiedAt = System.currentTimeMillis(),
                        source = RemoteConfigValueSource.BUILD_CONFIG
                )
        )
    }

    fun getTheLastSyncedRemoteConfig() =
            remoteConfigDao.getTheLastSyncedRemoteConfig(RemoteConfigValueSource.REMOTE)

    fun clearAllValues() {
        remoteConfigDao.clearRemoteConfig()
    }

    data class FeatureFlagsResult(
        val featureFlags: Map<String, Boolean>? = null
    ) : Store.OnChanged<FeatureFlagsError>() {
        constructor(error: FeatureFlagsError) : this() {
            this.error = error
        }
    }
}
