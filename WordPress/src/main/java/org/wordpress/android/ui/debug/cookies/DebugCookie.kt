package org.wordpress.android.ui.debug.cookies

import com.google.gson.Gson
import java.net.HttpCookie
import java.net.URI

data class DebugCookie(
    val domain: String,
    val name: String,
    val value: String?,
    val scheme: String = "https",
    val path: String = "/",
    val version: Int = 0
) {
    val key = domain + "_" + name

    fun toURI(): URI = URI(scheme, domain, path, null)

    fun toHttpCookie(): HttpCookie = HttpCookie(name, value).apply {
        version = this@DebugCookie.version
        domain = this@DebugCookie.domain
        path = this@DebugCookie.path
    }

    fun encode(gson: Gson): String = gson.toJson(this)

    companion object {
        fun decode(gson: Gson, encoded: String?): DebugCookie? = gson.fromJson(encoded, DebugCookie::class.java)
    }
}
