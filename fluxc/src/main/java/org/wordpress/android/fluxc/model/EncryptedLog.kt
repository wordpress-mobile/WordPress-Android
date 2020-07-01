package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.RawConstraints
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.util.DateTimeUtils
import java.io.File
import java.util.Date

/**
 * [EncryptedLog] and [EncryptedLogModel] are tied to each other, any change in one should be reflected in the other.
 * [EncryptedLog] should be used within the app, [EncryptedLogModel] should be used for DB interactions.
 */
data class EncryptedLog(
    val uuid: String,
    val file: File,
    val dateCreated: Date = Date(),
    val uploadState: EncryptedLogUploadState = EncryptedLogUploadState.QUEUED,
    val failedCount: Int = 0
) {
    companion object {
        fun fromEncryptedLogModel(encryptedLogModel: EncryptedLogModel) = EncryptedLog(
                dateCreated = DateTimeUtils.dateUTCFromIso8601(encryptedLogModel.dateCreated),
                // Crash if values are missing which shouldn't happen if there are no logic errors
                uuid = encryptedLogModel.uuid!!,
                file = File(encryptedLogModel.filePath),
                uploadState = encryptedLogModel.uploadState,
                failedCount = encryptedLogModel.failedCount
        )
    }
}

@Table
@RawConstraints("UNIQUE(UUID) ON CONFLICT REPLACE")
class EncryptedLogModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var uuid: String? = null
    @Column var filePath: String? = null
    @Column var dateCreated: String? = null // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column var uploadStateDbValue: Int = EncryptedLogUploadState.QUEUED.value
    @Column var failedCount: Int = 0

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }

    val uploadState: EncryptedLogUploadState
        get() =
            requireNotNull(
                    EncryptedLogUploadState.values()
                            .firstOrNull { it.value == uploadStateDbValue }) {
                "The stateDbValue of the EncryptedLogUploadState didn't match any of the `EncryptedLogUploadState`s. " +
                        "This likely happened because the EncryptedLogUploadState values " +
                        "were altered without a DB migration."
            }

    companion object {
        fun fromEncryptedLog(encryptedLog: EncryptedLog) = EncryptedLogModel().also {
            it.uuid = encryptedLog.uuid
            it.filePath = encryptedLog.file.path
            it.dateCreated = DateTimeUtils.iso8601UTCFromDate(encryptedLog.dateCreated)
            it.uploadStateDbValue = encryptedLog.uploadState.value
            it.failedCount = encryptedLog.failedCount
        }
    }
}

enum class EncryptedLogUploadState(val value: Int) {
    QUEUED(1),
    FAILED(2)
}
