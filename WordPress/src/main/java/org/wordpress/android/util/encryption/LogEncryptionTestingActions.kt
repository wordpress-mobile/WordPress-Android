package org.wordpress.android.util.encryption

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.Response.ErrorListener
import com.android.volley.Response.Listener
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonRequest
import org.json.JSONObject
import org.wordpress.android.WordPress
import org.wordpress.android.util.AppLog

class LogEncryptionTestingActions {
    /**
     * Sends a POST request with the encrypted logs and useful fields
     * for the server to decrypt them.
     */
    fun sendEncryptedLogsFile(encryptedLogsJson: JSONObject) {
        val jsonRequest = JsonStringRequest(
                Request.Method.POST,
                ENCRYPTION_LOGS_SERVER_URL,
                encryptedLogsJson,
                Listener { AppLog.i(AppLog.T.API, "Encrypted logs response successfully received by Wordpress API") },
                ErrorListener { AppLog.e(AppLog.T.API, "An error occurred when sending logs. " + it.message) }
        )
        WordPress.sRequestQueue.add(jsonRequest)
        AppLog.i(AppLog.T.API, "Sending encrypted logs to Wordpress API")
    }
}

private const val ENCRYPTION_LOGS_SERVER_URL = "https://log-encryption-testing.herokuapp.com"

/**
 * This class represents a Request which response is a String.
 */
class JsonStringRequest(
    method: Int,
    url: String,
    requestBody: JSONObject?,
    listener: Listener<String>,
    errorListener: ErrorListener
) : JsonRequest<String>(method, url, requestBody?.toString(), listener, errorListener) {
    override fun parseNetworkResponse(response: NetworkResponse?): Response<String> {
        response?.data ?: return Response.error(VolleyError("The response is null"))

        return Response.success<String>(String(response.data), HttpHeaderParser.parseCacheHeaders(response))
    }
}
