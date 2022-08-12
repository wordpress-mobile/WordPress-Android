package org.wordpress.android.fluxc.model.activity

import java.util.Date

data class RewindStatusModel(
    val state: State,
    val reason: Reason,
    val lastUpdated: Date,
    val canAutoconfigure: Boolean?,
    val credentials: List<Credentials>?,
    val rewind: Rewind?
) {
    @Suppress("unused")
    enum class State(val value: String) {
        ACTIVE("active"),
        INACTIVE("inactive"),
        UNAVAILABLE("unavailable"),
        AWAITING_CREDENTIALS("awaiting_credentials"),
        PROVISIONING("provisioning"),
        UNKNOWN("unknown");

        companion object {
            fun fromValue(value: String): State? {
                return values().firstOrNull { it.value == value }
            }
        }
    }

    enum class Reason(val value: String?) {
        MULTISITE_NOT_SUPPORTED("multisite_not_supported"),
        NO_REASON(null),
        UNKNOWN("unknown");

        companion object {
            fun fromValue(value: String?): Reason {
                return values().firstOrNull { it.value == value } ?: UNKNOWN
            }
        }
    }

    data class Credentials(
        val type: String,
        val role: String,
        val host: String?,
        val port: Int?,
        val stillValid: Boolean
    )

    data class Rewind(
        val rewindId: String?,
        val restoreId: Long,
        val status: Status,
        val progress: Int?,
        val reason: String?,
        val message: String?,
        val currentEntry: String?
    ) {
        enum class Status(val value: String) {
            RUNNING("running"), FINISHED("finished"), FAILED("failed"), QUEUED("queued");

            companion object {
                fun fromValue(value: String?): Status? {
                    return value?.let { values().firstOrNull { it.value == value } }
                }
            }
        }

        companion object {
            @Suppress("LongParameterList")
            fun build(
                rewindId: String?,
                restoreId: Long?,
                stringStatus: String?,
                progress: Int?,
                reason: String?,
                message: String?,
                currentEntry: String?
            ): Rewind? {
                val status = stringStatus?.let { Status.fromValue(it) }
                if (status != null && restoreId != null) {
                    return Rewind(rewindId, restoreId, status, progress, reason, message, currentEntry)
                }
                return null
            }
        }
    }
}
