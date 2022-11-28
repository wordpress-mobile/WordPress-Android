package org.wordpress.android.fluxc.store.mobile

import org.wordpress.android.fluxc.network.rest.wpcom.mobile.RemoteConfigErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.RemoteConfigClient
import org.wordpress.android.fluxc.network.rest.wpcom.mobile.RemoteConfigError
import org.wordpress.android.fluxc.store.Store
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigStore @Inject constructor(
    private val remoteConfigClient: RemoteConfigClient,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchRemoteConfig() = coroutineEngine.withDefaultContext(AppLog.T.API, this,
            "fetch remote-config") {
        val payload = remoteConfigClient.fetchRemoteConfig()
        return@withDefaultContext when {
            payload.isError -> RemoteConfigResult(payload.error)
            payload.remoteConfig != null -> {
                RemoteConfigResult(payload.remoteConfig)
            }
            else -> RemoteConfigResult(RemoteConfigError(GENERIC_ERROR))
        }
    }

    data class RemoteConfigResult(
        val remoteConfig: Map<String, Boolean>? = null
    ) : Store.OnChanged<RemoteConfigError>() {
        constructor(error: RemoteConfigError) : this() {
            this.error = error
        }
    }
}
