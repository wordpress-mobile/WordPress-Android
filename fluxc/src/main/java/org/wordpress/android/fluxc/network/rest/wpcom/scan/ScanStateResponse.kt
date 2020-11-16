package org.wordpress.android.fluxc.network.rest.wpcom.scan

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response
import java.util.Date

data class ScanStateResponse(
    val state: String,
    val threats: List<Threat>?,
    val credentials: List<Credentials>?,
    val reason: String?,
    @SerializedName("has_cloud") val hasCloud: Boolean,
    @SerializedName("most_recent") val mostRecentStatus: ScanProgressStatus?,
    @SerializedName("current") val currentStatus: ScanProgressStatus?
) : Response {
    data class Threat(
        val id: Int,
        val signature: String,
        val description: String,
        val status: String,
        val fixable: Fixable,
        val extension: Extension,
        @SerializedName("first_detected") val firstDetected: String
    ) {
        data class Fixable(
            val fixer: String,
            val target: String
        )

        data class Extension(
            val type: String,
            val slug: String,
            val name: String,
            val version: String,
            val isPremium: Boolean
        )
    }

    data class Credentials(
        val type: String,
        val role: String,
        @SerializedName("still_valid") val stillValid: Boolean
    )

    data class ScanProgressStatus(
        val duration: Int,
        val progress: Int,
        val error: Boolean,
        @SerializedName("timestamp") val startDate: Date,
        @SerializedName("is_initial") val isInitial: Boolean
    )
}
