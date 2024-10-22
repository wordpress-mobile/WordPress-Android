package org.wordpress.android.fluxc.store.mobile

import org.wordpress.android.fluxc.network.rest.wpcom.mobile.RemoteConfigErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.RemoteConfigRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.RemoteConfigError
import org.wordpress.android.fluxc.persistence.RemoteConfigDao
import org.wordpress.android.fluxc.persistence.RemoteConfigDao.RemoteConfig
import org.wordpress.android.fluxc.persistence.RemoteConfigDao.RemoteConfigValueSource.BUILD_CONFIG
import org.wordpress.android.fluxc.store.Store
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigStore @Inject constructor(
    private val remoteConfigRestClient: RemoteConfigRestClient,
    private val remoteConfigDao: RemoteConfigDao,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchRemoteConfig() = coroutineEngine.withDefaultContext(
            AppLog.T.API, this,
            "fetch remote-field-config"
    ) {
        val payload = remoteConfigRestClient.fetchRemoteConfig()
        return@withDefaultContext when {
            payload.isError -> RemoteConfigResult(payload.error)
            payload.remoteConfig != null -> {
                remoteConfigDao.insert(payload.remoteConfig)
                RemoteConfigResult(payload.remoteConfig)
            }
            else -> RemoteConfigResult(RemoteConfigError(GENERIC_ERROR))
        }
    }

    data class RemoteConfigResult(
        val remoteConfig: Map<String, String>? = null
    ) : Store.OnChanged<RemoteConfigError>() {
        constructor(error: RemoteConfigError) : this() {
            this.error = error
        }
    }

    fun getRemoteConfigs(): List<RemoteConfig> {
        return remoteConfigDao.getRemoteConfigList()
    }

    fun insertRemoteConfig(key: String, value: String) {
        remoteConfigDao.insert(
                RemoteConfig(
                        key = key,
                        value = value,
                        createdAt = System.currentTimeMillis(),
                        modifiedAt = System.currentTimeMillis(),
                        source = BUILD_CONFIG
                )
        )
    }
}
