package org.wordpress.android.fluxc.store.mobile

import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsError
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.FeatureFlagsRestClient
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao.FeatureFlag
import org.wordpress.android.fluxc.persistence.FeatureFlagConfigDao.FeatureFlagValueSource
import org.wordpress.android.fluxc.store.Store
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeatureFlagsStore @Inject constructor(
    private val featureFlagsRestClient: FeatureFlagsRestClient,
    private val featureFlagConfigDao: FeatureFlagConfigDao,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchFeatureFlags(
        buildNumber: String,
        deviceId: String,
        identifier: String,
        marketingVersion: String,
        platform: String
    ) = coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetch feature-flags") {
        val payload = featureFlagsRestClient.fetchFeatureFlags(
                buildNumber,
                deviceId,
                identifier,
                marketingVersion,
                platform
        )
        return@withDefaultContext when {
            payload.isError -> FeatureFlagsResult(payload.error)
            payload.featureFlags != null -> {
                featureFlagConfigDao.insert(payload.featureFlags)
                FeatureFlagsResult(payload.featureFlags)
            }
            else -> FeatureFlagsResult(FeatureFlagsError(GENERIC_ERROR))
        }
    }

    fun getFeatureFlags(): List<FeatureFlag> {
        return featureFlagConfigDao.getFeatureFlagList()
    }

    fun insertFeatureFlagValue(key: String, value: Boolean) {
        featureFlagConfigDao.insert(
                FeatureFlag(
                        key = key,
                        value = value,
                        createdAt = System.currentTimeMillis(),
                        modifiedAt = System.currentTimeMillis(),
                        source = FeatureFlagValueSource.BUILD_CONFIG
                )
        )
    }

    fun clearAllValues() {
        featureFlagConfigDao.clear()
    }

    data class FeatureFlagsResult(
        val featureFlags: Map<String, Boolean>? = null
    ) : Store.OnChanged<FeatureFlagsError>() {
        constructor(error: FeatureFlagsError) : this() {
            this.error = error
        }
    }
}
