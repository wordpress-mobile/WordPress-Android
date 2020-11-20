package org.wordpress.android.fluxc.network.rest.wpcom.scan

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response
import java.util.Date

data class ScanStateResponse(
    @SerializedName("state") val state: String,
    @SerializedName("threats") val threats: List<Threat>?,
    @SerializedName("credentials") val credentials: List<Credentials>?,
    @SerializedName("reason") val reason: String?,
    @SerializedName("has_cloud") val hasCloud: Boolean,
    @SerializedName("most_recent") val mostRecentStatus: ScanProgressStatus?,
    @SerializedName("current") val currentStatus: ScanProgressStatus?
) : Response {
    data class Threat(
        @SerializedName("id") val id: Int,
        @SerializedName("signature") val signature: String,
        @SerializedName("description") val description: String,
        @SerializedName("status") val status: String,
        @SerializedName("fixable") val fixable: Fixable,
        @SerializedName("extension") val extension: Extension,
        @SerializedName("first_detected") val firstDetected: String
    ) {
        data class Fixable(
            @SerializedName("fixer") val fixer: String,
            @SerializedName("target") val target: String
        )

        data class Extension(
            @SerializedName("type") val type: String,
            @SerializedName("slug") val slug: String,
            @SerializedName("name") val name: String,
            @SerializedName("version") val version: String,
            @SerializedName("isPremium") val isPremium: Boolean
        )
    }

    data class Credentials(
        @SerializedName("type") val type: String,
        @SerializedName("role") val role: String,
        @SerializedName("still_valid") val stillValid: Boolean
    )

    data class ScanProgressStatus(
        @SerializedName("duration") val duration: Int,
        @SerializedName("progress") val progress: Int,
        @SerializedName("error") val error: Boolean,
        @SerializedName("timestamp") val startDate: Date,
        @SerializedName("is_initial") val isInitial: Boolean
    )
}
