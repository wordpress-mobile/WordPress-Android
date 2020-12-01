package org.wordpress.android.fluxc.network.rest.wpcom.scan.threat

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.DatabaseThreatModel.Row
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.FileThreatModel.ThreatContext
import java.util.Date

data class Threat(
    @SerializedName("id") val id: Long?,
    @SerializedName("signature") val signature: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("fixable") @JsonAdapter(FixableDeserializer::class) val fixable: Fixable?,
    @SerializedName("extension") val extension: Extension?,
    @SerializedName("first_detected") val firstDetected: Date?,
    @SerializedName("fixed_on") val fixedOn: Date?,
    @SerializedName("context") @JsonAdapter(ThreatContextDeserializer::class) val context: ThreatContext?,
    @SerializedName("filename") val fileName: String?,
    @SerializedName("diff") val diff: String?,
    @SerializedName("rows") @JsonAdapter(RowsDeserializer::class) val rows: List<Row>?
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
