package org.wordpress.android.fluxc.model

import android.util.Base64
import org.json.JSONObject

@JvmInline
value class JWTToken(
    val value: String
) {
    /**
     * Returns the token if it is still valid, or null if it is expired.
     */
    @Suppress("MagicNumber")
    fun validateExpiryDate(): JWTToken? {
        fun JSONObject.getLongOrNull(name: String) = this.optLong(name, Long.MAX_VALUE).takeIf { it != Long.MAX_VALUE }

        val payloadJson = getPayloadJson()
        val expiration = payloadJson.getLongOrNull("exp")
            ?: payloadJson.getLongOrNull("expires")
            ?: return null

        val now = System.currentTimeMillis() / 1000

        return if (expiration > now) this else null
    }

    fun getPayloadItem(key: String): String? {
        return getPayloadJson().optString(key)
    }

    private fun getPayloadJson(): JSONObject {
        val payloadEncoded = this.value.split(".")[1]
        return JSONObject(String(Base64.decode(payloadEncoded, Base64.DEFAULT)))
    }
}
