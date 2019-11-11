package org.wordpress.android.fluxc.store

import com.google.gson.JsonElement
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.reactnative.ReactNativeWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.reactnative.ReactNativeWPComRestClient
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

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
    private val coroutineContext: CoroutineContext
) {
    suspend fun performWPComRequest(url: String, params: Map<String, String>): ReactNativeFetchResponse =
            withContext(coroutineContext) {
                return@withContext wpComRestClient.fetch(url, params, ::Success, ::Error)
            }

    suspend fun performWPAPIRequest(url: String, params: Map<String, String>): ReactNativeFetchResponse =
            withContext(coroutineContext) {
                return@withContext wpAPIRestClient.fetch(url, params, ::Success, ::Error)
            }
}

sealed class ReactNativeFetchResponse {
    class Success(val result: JsonElement) : ReactNativeFetchResponse()
    class Error(networkError: BaseNetworkError) : ReactNativeFetchResponse() {
        val error = networkError.volleyError?.message
                ?: (networkError as? WPComGsonNetworkError)?.apiError
                ?: networkError.message
                ?: "Unknown ${networkError.javaClass.simpleName}"
    }
}
