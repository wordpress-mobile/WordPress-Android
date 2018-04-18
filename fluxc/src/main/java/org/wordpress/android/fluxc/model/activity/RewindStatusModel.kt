package org.wordpress.android.fluxc.model.activity

data class RewindStatusModel(val state: State, val reason: String?, val restore: RestoreStatus?) {
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

    data class RestoreStatus(
        val id: String,
        val status: Status,
        val progress: Int,
        val message: String?,
        val errorCode: String?,
        val failureReason: String?
    ) {
        enum class Status(val value: String) {
            QUEUED("queued"),
            FINISHED("finished"),
            RUNNING("running"),
            FAIL("fail"),
            UNKNOWN("unknown");

            companion object {
                fun fromValue(value: String): Status? {
                    return Status.values().firstOrNull { it.value == value }
                }
            }
        }

        companion object {
            fun build(
                restoreId: String?,
                restoreState: String?,
                restoreProgress: Int?,
                restoreMessage: String?,
                restoreErrorCode: String?,
                restoreFailureReason: String?
            ): RestoreStatus? {
                return if (restoreId != null && restoreState != null && restoreProgress != null) {
                    RestoreStatus(restoreId,
                            Status.fromValue(restoreState) ?: Status.UNKNOWN,
                            restoreProgress,
                            restoreMessage,
                            restoreErrorCode,
                            restoreFailureReason)
                } else {
                    null
                }
            }
        }
    }
}
