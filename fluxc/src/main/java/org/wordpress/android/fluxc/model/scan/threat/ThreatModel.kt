package org.wordpress.android.fluxc.model.scan.threat

import java.util.Date

sealed class ThreatModel(
    open var id: Long,
    open var signature: String,
    open var description: String,
    open var status: ThreatStatus,
    open var firstDetected: Date,

    open var fixable: Fixable? = null,
    open var fixedOn: Date? = null
) {
    data class GenericThreatModel(
        override var id: Long,
        override var signature: String,
        override var description: String,
        override var status: ThreatStatus,
        override var firstDetected: Date,
        override var fixable: Fixable? = null,
        override var fixedOn: Date? = null
    ) : ThreatModel(
        id = id,
        signature = signature,
        description = description,
        status = status,
        firstDetected = firstDetected,
        fixable = fixable,
        fixedOn = fixedOn
    )

    data class CoreFileModificationThreatModel(
        override var id: Long,
        override var signature: String,
        override var description: String,
        override var status: ThreatStatus,
        override var firstDetected: Date,
        override var fixable: Fixable? = null,
        override var fixedOn: Date? = null,
        val fileName: String,
        val diff: String
    ) : ThreatModel(
        id = id,
        signature = signature,
        description = description,
        status = status,
        firstDetected = firstDetected,
        fixable = fixable,
        fixedOn = fixedOn
    )

    data class VulnerableExtensionThreatModel(
        override var id: Long,
        override var signature: String,
        override var description: String,
        override var status: ThreatStatus,
        override var firstDetected: Date,
        override var fixable: Fixable? = null,
        override var fixedOn: Date? = null,
        val extension: Extension
    ) : ThreatModel(
        id = id,
        signature = signature,
        description = description,
        status = status,
        firstDetected = firstDetected,
        fixable = fixable,
        fixedOn = fixedOn
    )

    data class DatabaseThreatModel(
        override var id: Long,
        override var signature: String,
        override var description: String,
        override var status: ThreatStatus,
        override var firstDetected: Date,
        override var fixable: Fixable? = null,
        override var fixedOn: Date? = null,
        val rows: List<Row>? = null
    ) : ThreatModel(
        id = id,
        signature = signature,
        description = description,
        status = status,
        firstDetected = firstDetected,
        fixable = fixable,
        fixedOn = fixedOn
    ) {
        data class Row(
            val id: Int,
            val rowNumber: Int,
            val description: String? = null,
            val code: String? = null,
            val url: String? = null
        )
    }

    data class FileThreatModel(
        override var id: Long,
        override var signature: String,
        override var description: String,
        override var status: ThreatStatus,
        override var firstDetected: Date,
        override var fixable: Fixable? = null,
        override var fixedOn: Date? = null,
        val fileName: String? = null,
        val context: ThreatContext
    ) : ThreatModel(
        id = id,
        signature = signature,
        description = description,
        status = status,
        firstDetected = firstDetected,
        fixable = fixable,
        fixedOn = fixedOn
    ) {
        data class ThreatContext(
            val lines: List<ContextLine>
        ) {
            data class ContextLine(
                val lineNumber: Int,
                val contents: String,
                val highlights: List<Pair<Int, Int>>? = null
            )
        }
    }

    enum class ThreatStatus(val value: String?) {
        FIXED("fixed"),
        IGNORED("ignored"),
        CURRENT("current"),
        UNKNOWN("unknown");

        companion object {
            fun fromValue(value: String?): ThreatStatus {
                return values().firstOrNull { it.value == value } ?: UNKNOWN
            }
        }
    }

    data class Fixable(
        val file: String?,
        val fixer: FixType,
        val target: String?
    ) {
        enum class FixType(val value: String) {
            REPLACE("replace"),
            DELETE("delete"),
            UPDATE("update"),
            EDIT("edit"),
            UNKNOWN("unknown");

            companion object {
                fun fromValue(value: String?): FixType {
                    return values().firstOrNull { it.value == value } ?: UNKNOWN
                }
            }
        }
    }

    data class Extension(
        val type: ExtensionType,
        val slug: String?,
        val name: String?,
        val version: String?,
        val isPremium: Boolean
    ) {
        enum class ExtensionType(val value: String?) {
            PLUGIN("plugin"),
            THEME("theme"),
            UNKNOWN("unknown");

            companion object {
                fun fromValue(value: String?): ExtensionType {
                    return values().firstOrNull { it.value == value } ?: UNKNOWN
                }
            }
        }
    }
}
