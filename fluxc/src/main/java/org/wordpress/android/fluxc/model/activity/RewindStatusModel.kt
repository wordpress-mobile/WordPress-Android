package org.wordpress.android.fluxc.model.activity

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table

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
            FAIL("fail"),
            UNKNOWN("unknown");

            companion object {
                fun fromValue(value: String): Status? {
                    return Status.values().firstOrNull { it.value == value }
                }
            }
        }

        companion object {
            fun build(restoreId: String?,
                      restoreState: String?,
                      restoreProgress: Int?,
                      restoreMessage: String?,
                      restoreErrorCode: String?,
                      restoreFailureReason: String?): RestoreStatus? {
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

    @Table(name = "RewindStatusModel")
    data class Builder(@PrimaryKey
                       @Column private var mId: Int = -1,
                       @Column var localSiteId: Int,
                       @Column var remoteSiteId: Long,
                       @Column var rewindState: String? = null,
                       @Column var reason: String? = null,
                       @Column var restoreId: String? = null,
                       @Column var restoreState: String? = null,
                       @Column var restoreProgress: Int? = null,
                       @Column var restoreMessage: String? = null,
                       @Column var restoreErrorCode: String? = null,
                       @Column var restoreFailureReason: String? = null) : Identifiable {
        override fun setId(id: Int) {
            this.mId = id
        }

        override fun getId() = mId

        constructor() : this(-1, 0, 0)

        fun build(): RewindStatusModel {
            val restoreStatus = RestoreStatus.build(restoreId,
                    restoreState,
                    restoreProgress,
                    restoreMessage,
                    restoreErrorCode,
                    restoreFailureReason)
            return RewindStatusModel(rewindState?.let { State.fromValue(it) } ?: State.UNKNOWN, reason, restoreStatus)
        }
    }
}
