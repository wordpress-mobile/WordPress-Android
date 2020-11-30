package org.wordpress.android.fluxc.network.rest.wpcom.scan.threat

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatContext
import java.util.Date

data class Threat(
    @SerializedName("id") val id: Long?,
    @SerializedName("signature") val signature: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("fixable") val fixable: Fixable?,
    @SerializedName("extension") val extension: Extension?,
    @SerializedName("first_detected") val firstDetected: Date?,
    @SerializedName("fixed_on") val fixedOn: Date?,
    @SerializedName("context") var context: ThreatContext?,
    @SerializedName("filename") val fileName: String?,
    @SerializedName("diff") val diff: String?,
    @SerializedName("rows") val rows: String?
) {
    data class Fixable(
        @SerializedName("file") val file: String?,
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
