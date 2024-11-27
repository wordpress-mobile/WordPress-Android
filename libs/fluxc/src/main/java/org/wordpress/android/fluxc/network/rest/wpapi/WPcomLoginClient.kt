package org.wordpress.android.fluxc.network.rest.wpapi

import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WPcomAuthorizationCodeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class WPcomLoginClient @Inject constructor(
    private val context: CoroutineContext,
    private val appSecrets: AppSecrets
) {
    private val client = OkHttpClient()

    fun loginUri(redirectUri: String): Uri {
        return Uri.Builder().scheme("https")
            .authority("public-api.wordpress.com")
            .path("/oauth2/authorize")
            .appendQueryParameter("client_id", appSecrets.appId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", "global")
            .build()
    }

    suspend fun exchangeAuthCodeForToken(code: String): Result<String> {
        val tokenUrl = Uri.Builder()
            .scheme("https")
            .authority("public-api.wordpress.com")
            .path("oauth2/token")
            .build()
            .toString()

        val formBody = FormBody.Builder()

        mutableMapOf(
            "client_id" to appSecrets.appId,
            "redirect_uri" to appSecrets.redirectUri,
            "client_secret" to appSecrets.appSecret,
            "code" to code,
            "grant_type" to "authorization_code",
        ).forEach { (t, u) -> formBody.add(t, u) }

        val request = Request.Builder()
            .url(tokenUrl)
            .post(formBody.build())
            .build()

        return withContext(context) {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                response.body?.let { Log.e("WPCOM_LOGIN", it.string()) }
                Result.failure(WPcomLoginError.AccessDenied)
            } else {
                val json = response.body?.string() ?: return@withContext Result.failure(WPcomLoginError.InvalidResponse)
                val gson = Gson().fromJson(json, WPcomAuthorizationCodeResponse::class.java)
                Result.success(gson.accessToken)
            }
        }
    }
}

sealed class WPcomLoginError(val code: Int): Throwable() {
    data object AccessDenied: WPcomLoginError(1)
    data object InvalidResponse: WPcomLoginError(2)
}
