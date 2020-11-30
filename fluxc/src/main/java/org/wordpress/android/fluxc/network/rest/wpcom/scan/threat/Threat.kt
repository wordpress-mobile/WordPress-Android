package org.wordpress.android.fluxc.network.rest.wpcom.scan.threat

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Threat(
    @SerializedName("id") val id: Int,
    @SerializedName("signature") val signature: String,
    @SerializedName("description") val description: String,
    @SerializedName("status") val status: String,
    @SerializedName("fixable") val fixable: Fixable?,
    @SerializedName("extension") val extension: Extension?,
    @SerializedName("first_detected") val firstDetected: Date,
    @SerializedName("fixed_on") val fixedOn: Date?
) {
    data class Fixable(
        @SerializedName("fixer") val fixer: String?,
        @SerializedName("target") val target: String?
    )

    data class Extension(
        @SerializedName("type") val type: String?,
        @SerializedName("slug") val slug: String?,
        @SerializedName("name") val name: String?,
        @SerializedName("version") val version: String?,
        @SerializedName("isPremium") val isPremium: Boolean?
    )
}
