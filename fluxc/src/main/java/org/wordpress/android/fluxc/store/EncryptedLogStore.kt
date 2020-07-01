package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.EncryptedLog
import org.wordpress.android.fluxc.model.EncryptedLogUploadState
import org.wordpress.android.fluxc.model.EncryptedLogUploadState.NEEDS_UPLOAD
import org.wordpress.android.fluxc.model.EncryptedLogUploadState.UPLOAD_FAILED
import org.wordpress.android.fluxc.network.rest.wpcom.encryptedlog.EncryptedLogRestClient
import org.wordpress.android.fluxc.persistence.EncryptedLogSqlUtils
import java.io.File
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_FAIL_COUNT = 3

//TODO: Add EncryptedLogModel DB migration

@Singleton
class EncryptedLogStore @Inject constructor(
    private val encryptedLogRestClient: EncryptedLogRestClient,
    private val encryptedLogSqlUtils: EncryptedLogSqlUtils
) {
    /**
     * Document the logic for when uploads will happen:
     *
     * Uploads should be checked:
     * 1. After [queueLogForUpload]
     * 2. After [handleSuccessfulUpload]
     * 3. Sometimes after [handleFailedUpload]
     * 4. At application start
     * 5. After a timer - maybe due to [handleFailedUpload]
     */
    fun newLogFile(): EncryptedLog {
        val encryptedLog = EncryptedLog(
                uuid = UUID.randomUUID().toString(),
                file = File("") // TODO: Give proper path
        )
        encryptedLogSqlUtils.insertOrUpdateEncryptedLog(encryptedLog)
        return encryptedLog
    }

    private suspend fun queueLogForUpload(uuid: String) {
        // TODO: Delete the file if we can't find a record in the DB
        encryptedLogSqlUtils.getEncryptedLog(uuid)?.copy(uploadState = NEEDS_UPLOAD)?.let { encryptedLog ->
            encryptedLogSqlUtils.insertOrUpdateEncryptedLog(encryptedLog)
            uploadNext()
        }
    }

    private suspend fun uploadNextWithBackOffTiming() {
        // TODO: Add a backoff timer
        uploadNext()
    }

    private suspend fun uploadNext() {
        // TODO: Don't upload if we are already uploading something
        val list = encryptedLogSqlUtils.getEncryptedLogsForUploadState(uploadState = NEEDS_UPLOAD)
        list.forEach { encryptedLog ->
            encryptedLogRestClient.uploadLog(encryptedLog.uuid, encryptedLog.file)
        }
    }

    private suspend fun handleSuccessfulUpload(encryptedLog: EncryptedLog) {
        deleteEncryptedLog(encryptedLog)
        uploadNext()
    }

    private suspend fun handleFailedUpload(encryptedLog: EncryptedLog) {
        // TODO: Handle known server errors. If the upload failed for a temporary reason, don't increase failed count
        // TODO: and just queue it again with a back off timer
        val updatedEncryptedLog = encryptedLog.copy(
                failedCount = encryptedLog.failedCount + 1,
                uploadState = UPLOAD_FAILED
        )
        if (updatedEncryptedLog.failedCount >= MAX_FAIL_COUNT) {
            // If a log failed to upload too many times, assume we can't upload it
            deleteEncryptedLog(updatedEncryptedLog)
        }
        uploadNextWithBackOffTiming()
    }

    private suspend fun deleteEncryptedLog(encryptedLog: EncryptedLog) {
        // TODO: Delete log file
        encryptedLogSqlUtils.deleteEncryptedLog(encryptedLog.uuid)
    }
}
