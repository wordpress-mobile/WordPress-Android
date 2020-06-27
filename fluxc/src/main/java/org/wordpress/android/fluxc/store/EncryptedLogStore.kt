package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.EncryptedLog
import org.wordpress.android.fluxc.model.EncryptedLogUploadState
import org.wordpress.android.fluxc.model.EncryptedLogUploadState.NEEDS_UPLOAD
import org.wordpress.android.fluxc.network.rest.wpcom.encryptedlog.EncryptedLogRestClient
import org.wordpress.android.fluxc.persistence.EncryptedLogSqlUtils
import java.io.File
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

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
                dateCreated = Date(),
                uuid = UUID.randomUUID().toString(),
                file = File(""), // TODO: Give proper path
                uploadState = EncryptedLogUploadState.CREATED
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

    private suspend fun uploadNext(): EncryptedLog {
        // TODO: Don't upload if we are already uploading something
        val list = encryptedLogSqlUtils.getEncryptedLogsForUploadState(uploadState = NEEDS_UPLOAD)
        list.forEach { encryptedLog ->
            encryptedLogRestClient.uploadLog(encryptedLog.uuid, encryptedLog.file)
        }
    }

    private suspend fun handleSuccessfulUpload(encryptedLog: EncryptedLog) {
        deleteFile(encryptedLog.file)
        encryptedLogSqlUtils.deleteEncryptedLog(encryptedLog.uuid)
        uploadNext()
    }

    private suspend fun handleFailedUpload(encryptedLog: EncryptedLog) {
        // TODO: increase failed count
        // TODO: Delete the file and db record if the failed count is more than the limit
        // TODO: Add a backoff timer and consider skipping the upload for the specific log for a bit
        // TODO: Handle known server errors (might change above todos ^)
        uploadNext()
    }

    private suspend fun deleteFile(file: File) {
        // TODO
    }
}
