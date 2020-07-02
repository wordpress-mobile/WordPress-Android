package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLog
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLogUploadState.FAILED
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLogUploadState.UPLOADING
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptionUtils
import org.wordpress.android.fluxc.model.encryptedlogging.LogEncrypter
import org.wordpress.android.fluxc.network.rest.wpcom.encryptedlog.EncryptedLogRestClient
import org.wordpress.android.fluxc.persistence.EncryptedLogSqlUtils
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_FAIL_COUNT = 3

// TODO: Add EncryptedLogModel DB migration

@Singleton
class EncryptedLogStore @Inject constructor(
    private val encryptedLogRestClient: EncryptedLogRestClient,
    private val encryptedLogSqlUtils: EncryptedLogSqlUtils
) {
    private val keyPair = EncryptionUtils.sodium.cryptoBoxKeypair()

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
    // TODO: Remove `suspend` and move the logic to bg thread and return asap
    private suspend fun queueLogForUpload(uuid: String, file: File) {
        // If the log file doesn't exist, there is nothing we can do
        if (!file.exists()) {
            return
        }
        val encryptedLog = EncryptedLog(
                uuid = uuid,
                file = file
        )
        encryptedLogSqlUtils.insertOrUpdateEncryptedLog(encryptedLog)
        uploadNext()
    }

    private suspend fun uploadNextWithBackOffTiming() {
        // TODO: Add a backoff timer
        uploadNext()
    }

    private suspend fun uploadNext() {
        if (encryptedLogSqlUtils.getNumberOfUploadingEncryptedLogs() > 0) {
            // We are already uploading another log file
            return
        }
        val (logsToUpload, logsToDelete) = encryptedLogSqlUtils.getEncryptedLogsForUpload()
                .partition { it.file.exists() }
        // Delete any queued encrypted log records if the log file no longer exists
        encryptedLogSqlUtils.deleteEncryptedLogs(logsToDelete)
        // We want to upload a single file at a time
        logsToUpload.firstOrNull()?.let {
            uploadEncryptedLog(it)
        }
    }

    private suspend fun uploadEncryptedLog(encryptedLog: EncryptedLog) {
        // Update the upload state of the log
        encryptedLog.copy(uploadState = UPLOADING).let {
            encryptedLogSqlUtils.insertOrUpdateEncryptedLog(it)
        }
        val contents = LogEncrypter(
                sourceFile = encryptedLog.file,
                uuid = encryptedLog.uuid,
                publicKey = keyPair.publicKey
        ).read()
        encryptedLogRestClient.uploadLog(encryptedLog.uuid, contents)
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
                uploadState = FAILED
        )
        if (updatedEncryptedLog.failedCount >= MAX_FAIL_COUNT) {
            // If a log failed to upload too many times, assume we can't upload it
            deleteEncryptedLog(updatedEncryptedLog)
        }
        uploadNextWithBackOffTiming()
    }

    private fun deleteEncryptedLog(encryptedLog: EncryptedLog) {
        // TODO: Do we want to delete the unencrypted log file?
        encryptedLogSqlUtils.deleteEncryptedLogs(listOf(encryptedLog))
    }
}
