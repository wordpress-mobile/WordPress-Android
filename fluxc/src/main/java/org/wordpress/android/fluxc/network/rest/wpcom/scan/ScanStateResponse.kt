package org.wordpress.android.fluxc.network.rest.wpcom.scan

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.network.Response

data class ScanStateResponse(
    val state: String,
    val threats: List<Threat>?,
    val credentials: List<Credentials>?,
    @SerializedName("has_cloud") val hasCloud: Boolean,
    @SerializedName(value = "most_recent", alternate = ["current"]) val scanProgressStatus: ScanProgressStatus?
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
        val timestamp: String,
        val duration: Int,
        val progress: Int,
        val error: Boolean,
        @SerializedName("is_initial") val isInitial: Boolean
    )
}
