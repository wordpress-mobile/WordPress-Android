package org.wordpress.android.fluxc.network.rest.wpcom.encryptedlog

import com.android.volley.RequestQueue
import com.android.volley.Response
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.fluxc.network.EncryptedLogUploadRequest
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AppSecrets
import org.wordpress.android.fluxc.network.rest.wpcom.encryptedlog.LogUploadResult.LogUploadFailed
import org.wordpress.android.fluxc.network.rest.wpcom.encryptedlog.LogUploadResult.LogUploaded
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class EncryptedLogRestClient
constructor(
    private val requestQueue: RequestQueue,
    private val appSecrets: AppSecrets
) {
    suspend fun uploadLog(logUuid: String, contents: String) = suspendCancellableCoroutine<LogUploadResult> { cont ->
        val request = EncryptedLogUploadRequest(logUuid, contents, appSecrets.appSecret, Response.Listener {
            // TODO: add callbacks
            cont.resume(LogUploaded)
        }, Response.ErrorListener {
            cont.resume(LogUploadFailed)
        })
        cont.invokeOnCancellation { request.cancel() }
        requestQueue.add(request)
    }
}

sealed class LogUploadResult {
    object LogUploaded : LogUploadResult()
    object LogUploadFailed : LogUploadResult()
}
