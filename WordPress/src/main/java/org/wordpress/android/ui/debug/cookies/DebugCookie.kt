package org.wordpress.android.ui.debug.cookies

import com.google.gson.Gson
import java.net.HttpCookie
import java.net.URI

// Persisted via Gson, so the derived values are getters rather than fields Gson would need to restore
data class DebugCookie(
    val host: String,
    val name: String,
    val value: String?
) {
    val key get() = host + "_" + name

    val oldRfcDomain get() = ".$host" // Append leading dot to match subdomains under RFC2109 and RFC2965

    val headerValue get() = name + "=" + value.orEmpty()

    fun toURI(): URI = URI(host)

    fun toHttpCookie(): HttpCookie = HttpCookie(name, value).apply {
        version = 0 // Use Netscape specification to maintain compatibility with java.net.CookieManager
        domain = host
    }

    fun encode(gson: Gson): String = gson.toJson(this)

    companion object {
        fun decode(gson: Gson, encoded: String?): DebugCookie? = gson.fromJson(encoded, DebugCookie::class.java)
    }
}
