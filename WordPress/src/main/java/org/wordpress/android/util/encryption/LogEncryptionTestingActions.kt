package org.wordpress.android.util.encryption

import com.android.volley.Response.ErrorListener
import com.android.volley.Response.Listener
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject
import org.wordpress.android.util.AppLog

class LogEncryptionTestingActions {

    /**
     * Sends the encrypted logs in JSON format
     */
    fun sendEncryptedLogsFile(encryptedLogsJson: JSONObject, logsUUID: String) {
        encryptedLogsJson.put("logs_id", logsUUID)
        val request = JsonObjectRequest("https://log-encryption-testing.herokuapp.com", encryptedLogsJson,
                Listener<JSONObject> { AppLog.i(AppLog.T.API, "Encrypted logs sent to Wordpress API server") },
                ErrorListener { AppLog.i(AppLog.T.API, "An error occourted when sending logs") }
        )
        // SEND REQUEST
    }
}