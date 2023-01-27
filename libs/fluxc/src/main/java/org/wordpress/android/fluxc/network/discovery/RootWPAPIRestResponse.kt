package org.wordpress.android.fluxc.network.discovery

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.rest.JsonObjectOrEmptyArray

class RootWPAPIRestResponse(
    val name: String? = null,
    val url: String? = null,
    @SerializedName("gmt_offset") val gmtOffset: Double? = null,
    @SerializedName("timezone_string") val timeZoneString: String? = null,
    val namespaces: List<String>? = null,
    val authentication: Authentication? = null
) : Response {
    class Authentication : JsonObjectOrEmptyArray() {
        class Oauth1 {
            var request: String? = null
            var authorize: String? = null
            var access: String? = null
            var version: String? = null
        }

        var oauth1: Oauth1? = null
    }
}
