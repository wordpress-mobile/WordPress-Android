package org.wordpress.android.fluxc.network.rest.wpcom.activity

import org.wordpress.android.fluxc.store.Store

data class RewindStatus(val state: State, val reason: String?, val restore: RestoreStatus?) {
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
            FAIL("fail")
        }
    }

    enum class RewindStatusErrorType {
        GENERIC_ERROR,
        AUTHORIZATION_REQUIRED,
        INVALID_RESPONSE,
        MISSING_STATE,
        INVALID_REWIND_STATE,
        MISSING_RESTORE_ID,
        MISSING_RESTORE_STATUS,
        INVALID_RESTORE_STATUS
    }

    data class RewindStatusError(var type: RewindStatusErrorType, var message: String? = null) : Store.OnChangedError
}
