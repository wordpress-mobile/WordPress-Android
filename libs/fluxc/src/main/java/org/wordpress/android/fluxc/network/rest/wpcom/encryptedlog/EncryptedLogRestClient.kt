package org.wordpress.android.fluxc.network.rest.wpcom.encryptedlog

import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.network.EncryptedLogUploadRequest
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import org.wordpress.android.fluxc.network.rest.wpcom.encryptedlog.UploadEncryptedLogResult.LogUploadFailed
import org.wordpress.android.fluxc.network.rest.wpcom.encryptedlog.UploadEncryptedLogResult.LogUploaded
import org.wordpress.android.fluxc.store.EncryptedLogStore.UploadEncryptedLogError
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class EncryptedLogRestClient
constructor(
    private val requestQueue: RequestQueue,
    private val appSecrets: AppSecrets
) {
    suspend fun uploadLog(logUuid: String, contents: String): UploadEncryptedLogResult {
        return suspendCancellableCoroutine { cont ->
            val request = EncryptedLogUploadRequest(logUuid, contents, appSecrets.appSecret, Response.Listener {
                cont.resume(LogUploaded)
            }, Response.ErrorListener { error ->
                cont.resume(LogUploadFailed(mapError(error)))
            })
            cont.invokeOnCancellation { request.cancel() }
            requestQueue.add(request)
        }
    }

    // {"error":"invalid-request","message":"Invalid UUID: uuids must only contain letters, numbers, dashes, and curly brackets"}
    private fun mapError(error: VolleyError): UploadEncryptedLogError {
        val errorMessageFromData = String(error.networkResponse.data)
        return UploadEncryptedLogError.Unknown
    }
}

sealed class UploadEncryptedLogResult {
    object LogUploaded : UploadEncryptedLogResult()
    class LogUploadFailed(val error: UploadEncryptedLogError) : UploadEncryptedLogResult()
}
