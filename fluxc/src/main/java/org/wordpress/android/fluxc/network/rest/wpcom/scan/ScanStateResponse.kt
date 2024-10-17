package org.wordpress.android.fluxc.network.rest.wpcom.scan

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response
import org.wordpress.android.fluxc.network.rest.wpcom.scan.threat.Threat
import java.util.Date

data class ScanStateResponse(
    @SerializedName("state") val state: String,
    @SerializedName("threats") val threats: List<Threat>?,
    @SerializedName("credentials") val credentials: List<Credentials>?,
    @SerializedName("reason") val reason: String?,
    @SerializedName("has_cloud") val hasCloud: Boolean?,
    @SerializedName("most_recent") val mostRecentStatus: ScanProgressStatus?,
    @SerializedName("current") val currentStatus: ScanProgressStatus?
) : Response {
    data class Credentials(
        @SerializedName("type") val type: String,
        @SerializedName("role") val role: String,
        @SerializedName("host") val host: String?,
        @SerializedName("port") val port: Int?,
        @SerializedName("user") val user: String?,
        @SerializedName("path") val path: String?,
        @SerializedName("still_valid") val stillValid: Boolean
    )

    data class ScanProgressStatus(
        @SerializedName("duration") val duration: Int?,
        @SerializedName("progress") val progress: Int?,
        @SerializedName("error") val error: Boolean?,
        @SerializedName("timestamp") val startDate: Date?,
        @SerializedName("is_initial") val isInitial: Boolean?
    )
}
