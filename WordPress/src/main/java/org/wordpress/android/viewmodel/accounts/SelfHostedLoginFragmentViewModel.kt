package org.wordpress.android.viewmodel.accounts

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Request.Builder
import rs.wordpress.api.kotlin.WpLoginClient
import uniffi.wp_api.RequestExecutor
import uniffi.wp_api.WpNetworkRequest
import uniffi.wp_api.WpNetworkResponse

//class WordPressRequestExecutor: RequestExecutor {
//    val client: OkHttpClient = OkHttpClient()
//
//    override suspend fun execute(request: WpNetworkRequest): WpNetworkResponse {
//        val okRequest: Request = Builder()
//            .url(request.url())
//            .build()
//
//        client.newCall(okRequest).executeAsync()
//    }
//
//}

class SelfHostedLoginFragmentViewModel: ViewModel() {
    private val _siteUrl = MutableStateFlow(" ")
    val siteUrl = _siteUrl.asStateFlow()

    fun setSiteUrl(newValue: String) {
        if(newValue == "") {
            _siteUrl.value = " "
        } else {
            _siteUrl.value = newValue.trim()
        }
    }

    fun didTapContinue() {
        runBlocking {
            val authenticationUrl = runBlocking {

                WpLoginClient().apiDiscovery(siteUrl.value)
                    .getOrThrow().apiDetails.findApplicationPasswordsAuthenticationUrl()
            }
            val uriBuilder = Uri.parse(authenticationUrl).buildUpon()

            uriBuilder
                .appendQueryParameter("app_name", "WordPressRsAndroidExample")
                .appendQueryParameter("app_id", "00000000-0000-4000-8000-000000000000")
                .appendQueryParameter("success_url", "wordpressrsexample://authorized")

            Log.d("WP_RS", uriBuilder.build().toString())
        }
    }
}
