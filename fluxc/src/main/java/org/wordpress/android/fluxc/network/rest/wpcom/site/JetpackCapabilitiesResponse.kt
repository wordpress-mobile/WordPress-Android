package org.wordpress.android.fluxc.network.rest.wpcom.site

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

data class JetpackCapabilitiesResponse(
    @SerializedName("capabilities") val capabilities: List<String>?
) : Response
