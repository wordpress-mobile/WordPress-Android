package org.wordpress.android.ui.posts.reactnative

import android.os.Bundle
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
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : CoroutineScope {
    override val coroutineContext = bgDispatcher + Job()

    fun performGetRequest(
        pathWithParams: String,
        mSite: SiteModel,
        onSuccess: Consumer<String>,
        onError: Consumer<Bundle>
    ) {
        launch {
            val response = reactNativeStore.executeRequest(mSite, pathWithParams)
            handleResponse(response, onSuccess::accept, onError::accept)
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

    private fun handleResponse(
        response: ReactNativeFetchResponse,
        onSuccess: (String) -> Unit,
        onError: (Bundle) -> Unit
    ) {
        when (response) {
            is Success -> onSuccess(response.result.toString())
            is Error -> {
                val bundle = Bundle().apply {
                    response.error.volleyError?.networkResponse?.statusCode?.let {
                        putInt("code", it)
                    }
                    extractErrorMessage(response.error)?.let {
                        putString("message", it)
                    }
                }
                onError(bundle)
            }
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
