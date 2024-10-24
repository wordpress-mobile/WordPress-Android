package org.wordpress.android.fluxc.network

import com.android.volley.Request.Method as VolleyMethod

enum class HttpMethod {
    GET, POST, DELETE, PUT, HEAD, OPTIONS, TRACE, PATCH
}

fun HttpMethod.toVolleyMethod(): Int = when (this) {
    HttpMethod.GET -> VolleyMethod.GET
    HttpMethod.POST -> VolleyMethod.POST
    HttpMethod.DELETE -> VolleyMethod.DELETE
    HttpMethod.PUT -> VolleyMethod.PUT
    HttpMethod.HEAD -> VolleyMethod.HEAD
    HttpMethod.OPTIONS -> VolleyMethod.OPTIONS
    HttpMethod.TRACE -> VolleyMethod.TRACE
    HttpMethod.PATCH -> VolleyMethod.PATCH
}
