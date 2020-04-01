package org.wordpress.android.util.encryptedlogging

import com.android.volley.Cache
import com.android.volley.Network
import com.android.volley.NetworkResponse
import com.android.volley.ParseError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.Response.ErrorListener
import com.android.volley.VolleyError
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonRequest
import org.json.JSONObject
import java.io.File

interface LogFileProvider {
    fun getLogFiles(): List<File>
}

typealias LogUploadCallback = () -> Unit

class EncryptedLogUploadEventNotifier(
    val uploadStartedCallback: LogUploadCallback?,
    val uploadFailedCallback: LogUploadCallback?,
    val uploadFinishedCallback: LogUploadCallback?
)

private const val UPLOAD_URL = "https://public-api.wordpress.com/rest/v1.1/encrypted-logging"

class EncryptedLogUploader(
    private val notifier: EncryptedLogUploadEventNotifier?,
    private val uploadURL: String = UPLOAD_URL
) {
    /**
     * Upload an encrypted log file
     */
    fun upload(log: EncryptedLog, attempt: Int = 1) {

        val req = EncryptedLogUploadRequest(log, uploadURL, Response.Listener {

        }, Response.ErrorListener {
            
        })
    }
}

class EncryptedLog(val uuid: String, val file: File)


class EncryptedLogUploadRequest(
    private val log: EncryptedLog,
    uploadURL: String,
    private val successListener: Response.Listener<NetworkResponse>,
    errorListener: Response.ErrorListener
) : Request<NetworkResponse>(Request.Method.POST, uploadURL, errorListener) {
    override fun getHeaders(): MutableMap<String, String> {
        return mutableMapOf(
            "Content-Type" to "application/json",
            "log-uuid"     to log.uuid
        )
    }

    override fun getBody(): ByteArray {
        return log.file.readBytes()
    }

    override fun parseNetworkResponse(response: NetworkResponse?): Response<NetworkResponse> {
        return try {
            Response.success(response, HttpHeaderParser.parseCacheHeaders(response))
        } catch (e: Exception) {
            try {
                val json = JSONObject(response.toString())
                val errorMessage = json.getString("message")
                Response.error(VolleyError(errorMessage))
            } catch (jsonParsingError: Throwable) {
                Response.error(ParseError(jsonParsingError))
            }
        }
    }

    override fun deliverResponse(response: NetworkResponse) = successListener.onResponse(response)
}
