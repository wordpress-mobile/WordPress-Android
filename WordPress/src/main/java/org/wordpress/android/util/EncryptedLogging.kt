package org.wordpress.android.util

import com.automattic.encryptedlogging.EncryptedLogging
import com.automattic.encryptedlogging.store.OnEncryptedLogUploaded
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class EncryptedLogging @Inject constructor(
    private val encryptedLogging: EncryptedLogging,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) {
    private val coroutineScope = CoroutineScope(bgDispatcher)

    fun start() {
        encryptedLogging.resetUploadStates()
        if (networkUtilsWrapper.isNetworkAvailable()) {
            encryptedLogging.uploadEncryptedLogs()
            coroutineScope.launch {
                encryptedLogging.observeEncryptedLogsUploadResult().collect {
                    when (it) {
                        is OnEncryptedLogUploaded.EncryptedLogUploadedSuccessfully -> {
                            analyticsTrackerWrapper.track(Stat.ENCRYPTED_LOGGING_UPLOAD_SUCCESSFUL)
                        }
                        is OnEncryptedLogUploaded.EncryptedLogFailedToUpload -> {
                            // Only track final errors
                            if (!it.willRetry) {
                                analyticsTrackerWrapper.track(
                                    Stat.ENCRYPTED_LOGGING_UPLOAD_FAILED,
                                    mapOf("error_type" to it.error.javaClass.simpleName)
                                )
                            }
                        }
                        null -> Unit // no-op
                    }
                }
            }
        }
    }

    /**
     * Dispatches a FluxC action that will queue the given log to be uploaded as soon as possible.
     *
     * @param logFile Log file to be uploaded
     * @param shouldStartUploadImmediately This parameter will decide whether we should try to upload the log file
     * immediately. After a crash, we are unlikely to have enough time to complete the upload, so we can use this
     * parameter to avoid the unnecessary upload failure.
     */
    fun encryptAndUploadLogFile(logFile: File, shouldStartUploadImmediately: Boolean): String? {
        if (logFile.exists()) {
            val uuid = UUID.randomUUID().toString()
            encryptedLogging.enqueueSendingEncryptedLogs(
                uuid = uuid,
                file = logFile,
                // If the connection is not available, we shouldn't try to upload immediately
                shouldUploadImmediately = shouldStartUploadImmediately &&
                        networkUtilsWrapper.isNetworkAvailable()
            )
            return uuid
        }
        return null
    }
}
