package org.wordpress.android.ui.posts.reactnative

import androidx.core.util.Consumer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
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
) {
    fun performGetRequest(
        pathWithParams: String,
        mSite: SiteModel,
        onSuccess: Consumer<String>,
        onError: Consumer<String>
    ) {
        GlobalScope.launch(bgDispatcher) {
            if (mSite.isUsingWpComRestApi) {
                performGetRequestForWPComSite(pathWithParams, mSite.siteId, onSuccess::accept, onError::accept)
            } else {
                performGetRequestForSelfHostedSite(pathWithParams, mSite.url, onSuccess::accept, onError::accept)
            }
        }
    }

    private suspend fun performGetRequestForWPComSite(
        pathWithParams: String,
        wpComSiteId: Long,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        parseUrlAndParamsForWPCom(pathWithParams, wpComSiteId)?.let { (url, params) ->
            val response = reactNativeStore.performWPComRequest(url, params)
            handleResponse(response, onSuccess, onError)
        }
    }

    private suspend fun performGetRequestForSelfHostedSite(
        pathWithParams: String,
        siteUrl: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        parseUrlAndParamsForWPOrg(pathWithParams, siteUrl)?.let { (url, params) ->
            val response = reactNativeStore.performWPAPIRequest(url, params)
            handleResponse(response, onSuccess, onError)
        }
    }

    private fun handleResponse(
        response: ReactNativeFetchResponse,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        when (response) {
            is Success -> onSuccess(response.result.toString())
            is Error -> onError(response.error)
        }
    }
}
