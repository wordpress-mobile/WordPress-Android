package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.EncryptedLogAction
import org.wordpress.android.fluxc.action.EncryptedLogAction.UPLOAD_LOG
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLog
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLogUploadState.FAILED
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLogUploadState.UPLOADING
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptionUtils
import org.wordpress.android.fluxc.model.encryptedlogging.LogEncrypter
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.encryptedlog.EncryptedLogRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.encryptedlog.UploadEncryptedLogResult.LogUploadFailed
import org.wordpress.android.fluxc.network.rest.wpcom.encryptedlog.UploadEncryptedLogResult.LogUploaded
import org.wordpress.android.fluxc.persistence.EncryptedLogSqlUtils
import org.wordpress.android.fluxc.store.EncryptedLogStore.UploadEncryptedLogError.InvalidRequest
import org.wordpress.android.fluxc.store.EncryptedLogStore.UploadEncryptedLogError.TooManyRequests
import org.wordpress.android.fluxc.store.EncryptedLogStore.UploadEncryptedLogError.Unknown
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.API
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// TODO: Increase the retry count
private const val MAX_FAIL_COUNT = 1

// TODO: Add EncryptedLogModel DB migration

@Singleton
class EncryptedLogStore @Inject constructor(
    private val encryptedLogRestClient: EncryptedLogRestClient,
    private val encryptedLogSqlUtils: EncryptedLogSqlUtils,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    private val keyPair = EncryptionUtils.sodium.cryptoBoxKeypair()

    override fun onRegister() {
        AppLog.d(API, this.javaClass.name + ": onRegister")
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? EncryptedLogAction ?: return
        when (actionType) {
            UPLOAD_LOG -> {
                coroutineEngine.launch(API, this, "EncryptedLogStore: On UPLOAD_LOG") {
                    queueLogForUpload(action.payload as UploadEncryptedLogPayload)
                }
            }
        }
    }

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
    private suspend fun queueLogForUpload(payload: UploadEncryptedLogPayload) {
        // If the log file doesn't exist, there is nothing we can do
        if (!payload.file.exists()) {
            return
        }
        val encryptedLog = EncryptedLog(
                uuid = payload.uuid,
                file = payload.file
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
        ).encrypt()
        when (val result = encryptedLogRestClient.uploadLog(encryptedLog.uuid, contents)) {
            is LogUploaded -> handleSuccessfulUpload(encryptedLog)
            is LogUploadFailed -> handleFailedUpload(encryptedLog, result.error)
        }
    }

    private suspend fun handleSuccessfulUpload(encryptedLog: EncryptedLog) {
        deleteEncryptedLog(encryptedLog)
        emitChange(OnEncryptedLogUploaded(uuid = encryptedLog.uuid, file = encryptedLog.file))
        uploadNext()
    }

    private suspend fun handleFailedUpload(encryptedLog: EncryptedLog, error: UploadEncryptedLogError) {
        when (error) {
            is TooManyRequests -> {
                encryptedLogSqlUtils.insertOrUpdateEncryptedLog(encryptedLog.copy(uploadState = FAILED))
            }
            is InvalidRequest -> {
                handleFinalUploadFailure(encryptedLog, error)
            }
            Unknown -> {
                if (encryptedLog.failedCount + 1 >= MAX_FAIL_COUNT) {
                    handleFinalUploadFailure(encryptedLog, error)
                } else {
                    encryptedLogSqlUtils.insertOrUpdateEncryptedLog(encryptedLog.copy(
                            failedCount = encryptedLog.failedCount + 1,
                            uploadState = FAILED
                    ))
                }
            }
        }
        uploadNextWithBackOffTiming()
    }

    /**
     * If a log has failed to upload too many times, or it's failing for a reason we know retrying won't help,
     * this method should be called to clean up and notify the client.
     */
    private fun handleFinalUploadFailure(encryptedLog: EncryptedLog, error: UploadEncryptedLogError) {
        deleteEncryptedLog(encryptedLog)

        // Since we have a retry mechanism we should only notify that we failed to upload when we give up
        emitChange(OnEncryptedLogUploaded(uuid = encryptedLog.uuid, file = encryptedLog.file, error = error))
    }

    private fun deleteEncryptedLog(encryptedLog: EncryptedLog) {
        // TODO: Do we want to delete the unencrypted log file?
        encryptedLogSqlUtils.deleteEncryptedLogs(listOf(encryptedLog))
    }

    class UploadEncryptedLogPayload(
        val uuid: String,
        val file: File
    ) : Payload<BaseNetworkError>()

    class OnEncryptedLogUploaded(
        val uuid: String,
        val file: File,
        error: UploadEncryptedLogError? = null
    ) : Store.OnChanged<UploadEncryptedLogError>() {
        init {
            this.error = error
        }
    }

    // TODO: Any other known upload errors we should handle?
    sealed class UploadEncryptedLogError(val message: String? = null) : OnChangedError {
        object Unknown : UploadEncryptedLogError()
        class InvalidRequest(message: String?) : UploadEncryptedLogError(message)
        class TooManyRequests(message: String?) : UploadEncryptedLogError(message)
    }
}
