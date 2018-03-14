package org.wordpress.android.fluxc.network.rest.wpcom.activity

data class RewindStatus(val state: State, val reason: String?, val restore: RestoreStatus?) {
    enum class State(value: String) {
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
        enum class Status(value: String) {
            QUEUED("queued"),
            FINISHED("finished"),
            RUNNING("running"),
            FAIL("fail")
        }
    }
}
