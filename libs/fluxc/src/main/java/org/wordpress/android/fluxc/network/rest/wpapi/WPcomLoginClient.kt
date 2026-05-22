package org.wordpress.android.fluxc.network.rest.wpapi

import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.wordpress.android.fluxc.module.OkHttpClientQualifiers
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.WPcomAuthorizationCodeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class WPcomLoginClient @Inject constructor(
    private val context: CoroutineContext,
    private val appSecrets: AppSecrets,
    @Named(OkHttpClientQualifiers.INTERCEPTORS) interceptors: Set<@JvmSuppressWildcards Interceptor>
) {
    private val client = OkHttpClient.Builder().apply {
        interceptors.forEach { addInterceptor(it) }
    }.build()

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
            try {
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    response.body?.let { Log.e("WPCOM_LOGIN", it.string()) }
                    Result.failure(WPcomLoginException(WPcomLoginError.AccessDenied))
                } else {
                    val json = response.body?.string()
                        ?: return@withContext Result.failure(
                            WPcomLoginException(WPcomLoginError.InvalidResponse)
                        )
                    val gson = Gson().fromJson(json, WPcomAuthorizationCodeResponse::class.java)
                    Result.success(gson.accessToken)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("WPCOM_LOGIN", "Error exchanging auth code for token", e)
                Result.failure(WPcomLoginException(WPcomLoginError.NetworkError(e)))
            }
        }
    }
}

sealed class WPcomLoginError(val code: Int) {
    data object AccessDenied : WPcomLoginError(CODE_ACCESS_DENIED)
    data object InvalidResponse : WPcomLoginError(CODE_INVALID_RESPONSE)
    data class NetworkError(val cause: Throwable) : WPcomLoginError(CODE_NETWORK_ERROR)

    companion object {
        private const val CODE_ACCESS_DENIED = 1
        private const val CODE_INVALID_RESPONSE = 2
        private const val CODE_NETWORK_ERROR = 3
    }
}

class WPcomLoginException(val error: WPcomLoginError) : Exception(
    "WPcom login failed: $error",
    (error as? WPcomLoginError.NetworkError)?.cause
)
