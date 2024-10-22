package org.wordpress.android.fluxc.network.rest.wpcom.scan.threat

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

data class FixThreatsStatusResponse(
    @SerializedName("ok") val ok: Boolean?,
    @JsonAdapter(FixThreatsStatusDeserializer::class)
    @SerializedName("threats") val fixThreatsStatus: List<FixThreatStatus>?
) : Response {
    data class FixThreatStatus(
        @SerializedName("id") val id: Long?,
        @SerializedName("status") val status: String?,
        @SerializedName("error") val error: String?
    )
}
