package org.wordpress.android.fluxc.model.scan.threat

data class FixThreatStatusModel(
    val id: Long,
    val status: FixStatus,
    val error: String? = null
) {
    enum class FixStatus(val value: String) {
        NOT_STARTED("not_started"),
        IN_PROGRESS("in_progress"),
        NOT_FIXED("not_fixed"),
        FIXED("fixed"),
        UNKNOWN("unknown");

        companion object {
            fun fromValue(value: String?): FixStatus {
                return values().firstOrNull { it.value == value } ?: UNKNOWN
            }
        }
    }
}
