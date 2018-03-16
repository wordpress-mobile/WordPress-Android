package org.wordpress.android.fluxc.model.activity

data class RewindStatusModel(val state: State, val reason: String?, val restore: RestoreStatus?) {
    enum class State(val value: String) {
        ACTIVE("active"),
        INACTIVE("inactive"),
        UNAVAILABLE("unavailable"),
        AWAITING_CREDENTIALS("awaitingCredentials"),
        PROVISIONING("provisioning")
    }

    data class RestoreStatus(val id: String,
                             val status: Status,
                             val progress: Int,
                             val message: String?,
                             val errorCode: String?,
                             val failureReason: String?) {
        enum class Status(val value: String) {
            QUEUED("queued"),
            FINISHED("finished"),
            RUNNING("running"),
            FAIL("fail");
            companion object {
                fun safeValueOf(value: String): Status? {
                    return Status.values().firstOrNull { it.value == value }
                }
            }
        }
    }
}
