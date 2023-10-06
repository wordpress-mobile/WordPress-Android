package org.wordpress.android.fluxc.model

import android.util.Base64
import org.json.JSONException
import org.json.JSONObject

@JvmInline
value class JWTToken(
    val value: String
) {
    /**
     * Returns the token if it is still valid, or null if it is expired.
     */
    @Suppress("SwallowedException", "MagicNumber")
    fun takeIfValid(): JWTToken? {
        fun JSONObject.getLongOrNull(name: String) = try {
            this.getLong(name)
        } catch (e: JSONException) {
            null
        }

        val payload = this.value.split(".")[1]
        val claimsJson = String(Base64.decode(payload, Base64.DEFAULT))
        val claims = JSONObject(claimsJson)

        val expiration = claims.getLongOrNull("exp") ?: claims.getLongOrNull("expires") ?: return null
        val now = System.currentTimeMillis() / 1000

        return if (expiration > now) this else null
    }
}
