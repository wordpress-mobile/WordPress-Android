package org.wordpress.android.fluxc.model.scan.threat

import com.google.gson.JsonArray
import java.util.Date

data class ThreatModel(
    val id: Long,
    val signature: String,
    val description: String,
    val status: String?, // TODO: ashiagr - possible values: fixed / ignored / current
    val fixable: Fixable? = null,
    val extension: Extension? = null,
    val firstDetected: Date,
    val fixedOn: Date? = null,
    var context: ThreatContext? = null,
    val fileName: String? = null,
    val diff: String? = null,
    val rows: String? = null
) {
    data class ThreatContext(
        val lines: List<ContextLine>
    ) {
        data class ContextLine(
            val lineNumber: Int,
            val contents: String,
            val highlights: JsonArray?
        )
    }

    data class Fixable(
        val file: String?,
        val fixer: String?, // TODO: ashiagr - possible values: replace, delete, update, edit
        val target: String?
    )

    data class Extension(
        val type: String?, // TODO: ashiagr - possible values: plugin, theme
        val slug: String?,
        val name: String?,
        val version: String?,
        val isPremium: Boolean
    )
}
