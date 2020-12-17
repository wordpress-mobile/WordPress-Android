package org.wordpress.android.fluxc.model.scan.threat

import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.Fixable
import org.wordpress.android.fluxc.model.scan.threat.ThreatModel.ThreatStatus
import java.util.Date

data class BaseThreatModel(
    val id: Long,
    val signature: String,
    val description: String,
    val status: ThreatStatus,
    val firstDetected: Date,
    val fixable: Fixable? = null,
    val fixedOn: Date? = null
)

sealed class ThreatModel {
    abstract val baseThreatModel: BaseThreatModel

    data class GenericThreatModel(
        override val baseThreatModel: BaseThreatModel
    ) : ThreatModel()

    data class CoreFileModificationThreatModel(
        override val baseThreatModel: BaseThreatModel,
        val fileName: String,
        val diff: String
    ) : ThreatModel()

    data class VulnerableExtensionThreatModel(
        override val baseThreatModel: BaseThreatModel,
        val extension: Extension
    ) : ThreatModel() {
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

    data class DatabaseThreatModel(
        override val baseThreatModel: BaseThreatModel,
        val rows: List<Row>? = null
    ) : ThreatModel() {
        data class Row(
            val id: Int,
            val rowNumber: Int,
            val description: String? = null,
            val code: String? = null,
            val url: String? = null
        )
    }

    data class FileThreatModel(
        override val baseThreatModel: BaseThreatModel,
        val fileName: String? = null,
        val context: ThreatContext
    ) : ThreatModel() {
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

    enum class ThreatStatus(val value: String) {
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
}
