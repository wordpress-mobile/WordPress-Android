package org.wordpress.android.fluxc.network.discovery

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

class RootWPAPIRestResponse(
    val name: String? = null,
    val description: String? = null,
    val url: String? = null,
    @SerializedName("gmt_offset") val gmtOffset: String? = null,
    val namespaces: List<String>? = null,
    val authentication: Authentication? = null
) : Response {
    class Authentication(
        @SerializedName("application-passwords") val applicationPasswords: ApplicationPasswords?
    ) {
        class ApplicationPasswords(
            val endpoints: Endpoints?
        ) {
            class Endpoints(
                val authorization: String?
            )
        }
    }
}
