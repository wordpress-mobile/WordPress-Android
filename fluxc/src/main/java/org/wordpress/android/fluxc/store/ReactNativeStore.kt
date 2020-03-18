package org.wordpress.android.fluxc.store

import com.google.gson.JsonElement
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.reactnative.ReactNativeWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.reactnative.ReactNativeWPComRestClient
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This store is for use making calls that originate from React Native. It does not use
 * a higher-level api for the requests and responses because of the unique requirements
 * around React Native. Calls originating from native code should not use this class.
 */
@Singleton
class ReactNativeStore
@Inject constructor(
    private val wpComRestClient: ReactNativeWPComRestClient,
    private val wpAPIRestClient: ReactNativeWPAPIRestClient,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun performWPComRequest(url: String, params: Map<String, String>): ReactNativeFetchResponse =
            coroutineEngine.withDefaultContext(AppLog.T.EDITOR, this, "performWPComRequest") {
                return@withDefaultContext wpComRestClient.fetch(url, params, ::Success, ::Error)
            }

    suspend fun performWPAPIRequest(url: String, params: Map<String, String>): ReactNativeFetchResponse =
            coroutineEngine.withDefaultContext(AppLog.T.EDITOR, this, "performWPAPIRequest") {
                return@withDefaultContext wpAPIRestClient.fetch(url, params, ::Success, ::Error)
            }
}

sealed class ReactNativeFetchResponse {
    class Success(val result: JsonElement) : ReactNativeFetchResponse()
    class Error(val error: BaseNetworkError) : ReactNativeFetchResponse()
}
