package org.wordpress.android.ui.posts.reactnative

import androidx.core.util.Consumer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.store.ReactNativeStore
import org.wordpress.android.modules.BG_THREAD
import javax.inject.Inject
import javax.inject.Named

class ReactNativeRequestHandler @Inject constructor(
    private val reactNativeStore: ReactNativeStore,
    private val urlUtil: ReactNativeUrlUtil,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : CoroutineScope {
    override val coroutineContext = bgDispatcher + Job()

    fun performGetRequest(
        pathWithParams: String,
        mSite: SiteModel,
        onSuccess: Consumer<String>,
        onError: Consumer<Map<String, Any?>>
    ) {
        launch {
            if (mSite.isUsingWpComRestApi) {
                performGetRequestForWPComSite(pathWithParams, mSite.siteId, onSuccess::accept, onError::accept)
            } else {
                performGetRequestForSelfHostedSite(pathWithParams, mSite.url, onSuccess::accept, onError::accept)
            }
        }
    }

    /**
     * A given instance of this class may not be used after [destroy] is called because:
     *     (1) this class's coroutineContext has a single job instance that is created on initialization;
     *     (2) calling `destroy()` cancels that job; and
     *     (3) jobs cannot be reused once cancelled.
     */
    fun destroy() {
        coroutineContext[Job]!!.cancel()
    }

    private suspend fun performGetRequestForWPComSite(
        pathWithParams: String,
        wpComSiteId: Long,
        onSuccess: (String) -> Unit,
        onError: (Map<String, Any?>) -> Unit
    ) {
        urlUtil.parseUrlAndParamsForWPCom(pathWithParams, wpComSiteId)?.let { (url, params) ->
            val response = reactNativeStore.performWPComRequest(url, params)
            handleResponse(response, onSuccess, onError)
        }
    }

    private suspend fun performGetRequestForSelfHostedSite(
        pathWithParams: String,
        siteUrl: String,
        onSuccess: (String) -> Unit,
        onError: (Map<String, Any?>) -> Unit
    ) {
        urlUtil.parseUrlAndParamsForWPOrg(pathWithParams, siteUrl)?.let { (url, params) ->
            val response = reactNativeStore.performWPAPIRequest(url, params)
            handleResponse(response, onSuccess, onError)
        }
    }

    private fun handleResponse(
        response: ReactNativeFetchResponse,
        onSuccess: (String) -> Unit,
        onError: (Map<String, Any?>) -> Unit
    ) {
        when (response) {
            is Success -> onSuccess(response.result.toString())
            is Error -> onError(mapOf(
                    "code" to response.error.volleyError?.networkResponse?.statusCode,
                    "message" to extractErrorMessage(response.error)
            ))
        }
    }

    private fun extractErrorMessage(networkError: BaseRequest.BaseNetworkError): String? {
        val volleyError = networkError.volleyError?.message
        val wpComError = (networkError as? WPComGsonRequest.WPComGsonNetworkError)?.apiError
        val baseError = networkError.message
        val errorType = networkError.type?.toString()
        return when {
            volleyError?.isNotBlank() == true -> volleyError
            wpComError?.isNotBlank() == true -> wpComError
            baseError?.isNotBlank() == true -> baseError
            errorType?.isNotBlank() == true -> errorType
            else -> "Unknown ${networkError.javaClass.simpleName} Error"
        }
    }
}
