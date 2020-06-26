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
 * Within the app, [EncryptedLog] should be used, for DB interactions [EncryptedLogModel] should be used.
 */
data class EncryptedLog(
    val dateCreated: Date,
    val uuid: String,
    val file: File,
    val uploadState: EncryptedLogUploadState
) {
    companion object {
        fun fromEncryptedLogModel(encryptedLogModel: EncryptedLogModel) = EncryptedLog(
                dateCreated = DateTimeUtils.dateUTCFromIso8601(encryptedLogModel.dateCreated),
                // Crash if values are missing which shouldn't happen if there are no logic errors
                uuid = encryptedLogModel.uuid!!,
                file = File(encryptedLogModel.filePath),
                uploadState = encryptedLogModel.uploadState
        )
    }
}

@Table
@RawConstraints("UNIQUE(UUID) ON CONFLICT REPLACE")
class EncryptedLogModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var dateCreated: String? = null // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    @Column var uuid: String? = null
    @Column var filePath: String? = null
    @Column var uploadStateDbValue: Int = EncryptedLogUploadState.DEFAULT.value

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
            it.dateCreated = DateTimeUtils.iso8601UTCFromDate(encryptedLog.dateCreated)
            it.uuid = encryptedLog.uuid
            it.filePath = encryptedLog.file.path
            it.uploadStateDbValue = encryptedLog.uploadState.value
        }
    }
}

enum class EncryptedLogUploadState(val value: Int) {
    DEFAULT(0),
    NEEDS_UPLOAD(1),
    UPLOAD_FAILED(2)
}
