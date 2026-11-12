package org.wordpress.android.ui.reader.repository

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.wordpress.rest.RestRequest
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import org.wordpress.android.WordPress
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.VolleyUtils
import kotlin.coroutines.resume

/**
 * The result of a REST request: the response, or the error (null if the request was cancelled)
 */
internal sealed class RestResult {
    data class Success(val json: JSONObject?) : RestResult()
    data class Failure(val error: VolleyError?) : RestResult() {
        val statusCode: Int get() = VolleyUtils.statusCodeFromVolleyError(error)
        val errorString: String get() = VolleyUtils.errStringFromVolleyError(error)
    }

    val response: JSONObject? get() = (this as? Success)?.json

    /**
     * True if Volley cancelled the request, as it does for every request at sign-out. Callers shouldn't revert
     * local changes then, since the Reader's data is being cleared.
     */
    val isCancelled: Boolean get() = this is Failure && error == null
}

/**
 * Runs a Volley request and returns its result. Cancelling the caller doesn't cancel the request.
 */
internal suspend fun awaitRestRequest(
    start: (RestRequest.Listener, RestRequest.ErrorListener) -> Request<*>
): RestResult {
    val queue = WordPress.requestQueue
    var cancelListener: RequestQueue.RequestEventListener? = null
    try {
        return suspendCancellableCoroutine { continuation ->
            val request = start(
                { json -> if (continuation.isActive) continuation.resume(RestResult.Success(json)) },
                { volleyError ->
                    AppLog.e(AppLog.T.READER, volleyError)
                    if (continuation.isActive) continuation.resume(RestResult.Failure(volleyError))
                }
            )
            // Volley calls neither listener for a cancelled request (every request is cancelled at sign-out), so
            // this watches for it finishing instead. Otherwise the caller would never resume.
            cancelListener = RequestQueue.RequestEventListener { finished, event ->
                val isCancelled = finished === request && event == RequestQueue.RequestEvent.REQUEST_FINISHED &&
                    finished.isCanceled
                if (isCancelled && continuation.isActive) continuation.resume(RestResult.Failure(null))
            }.also { queue?.addRequestEventListener(it) }
            if (request.isCanceled && continuation.isActive) continuation.resume(RestResult.Failure(null))
        }
    } finally {
        cancelListener?.let { queue?.removeRequestEventListener(it) }
    }
}
