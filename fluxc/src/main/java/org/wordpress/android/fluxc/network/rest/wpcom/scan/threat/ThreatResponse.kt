package org.wordpress.android.fluxc.network.rest.wpcom.scan.threat

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

data class ThreatResponse(
    @SerializedName("success") val success: Boolean?,
    @SerializedName("threat") val threat: Threat? = null
) : Response
