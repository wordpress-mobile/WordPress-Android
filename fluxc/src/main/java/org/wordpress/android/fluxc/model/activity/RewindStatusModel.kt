package org.wordpress.android.fluxc.model.activity

import java.util.Date

data class RewindStatusModel(
    val state: State,
    val reason: String?,
    val lastUpdated: Date,
    val canAutoconfigure: Boolean?,
    val credentials: List<Credentials>?,
    val rewind: Rewind?
) {
    enum class State(val value: String) {
        ACTIVE("active"),
        INACTIVE("inactive"),
        UNAVAILABLE("unavailable"),
        AWAITING_CREDENTIALS("awaitingCredentials"),
        PROVISIONING("provisioning"),
        UNKNOWN("unknown");

        companion object {
            fun fromValue(value: String): State? {
                return State.values().firstOrNull { it.value == value }
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
        val status: Status,
        val startedAt: Date,
        val progress: Int,
        val reason: String?
    ) {
        enum class Status(val value: String) {
            RUNNING("running"), FINISHED("finished"), FAILED("failed");

            companion object {
                fun fromValue(value: String): Status? {
                    return Status.values().firstOrNull { it.value == value }
                }
            }
        }

        companion object {
            fun build(
                rewindId: String?,
                stringStatus: String?,
                longStartedAt: Long?,
                progress: Int?,
                reason: String?
            ): Rewind? {
                val status = stringStatus?.let { Status.fromValue(it) }
                val startedAt = longStartedAt?.let { Date(it) }
                if (status != null && startedAt != null && progress != null) {
                    return Rewind(rewindId, status, startedAt, progress, reason)
                }
                return null
            }
        }
    }
}
