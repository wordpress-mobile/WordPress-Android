package org.wordpress.android.support.unified.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.EncryptedLogging
import org.wordpress.android.util.LogFileProviderWrapper
import javax.inject.Inject
import javax.inject.Named

/**
 * Encrypts and uploads the app log files to the encrypted logging service, returning the
 * generated log UUIDs so they can be referenced from support requests.
 */
class EncryptedAppLogsUploader @Inject constructor(
    private val encryptedLogging: EncryptedLogging,
    private val logFileProvider: LogFileProviderWrapper,
    private val appLogWrapper: AppLogWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) {
    @Suppress("NestedBlockDepth", "TooGenericExceptionCaught")
    suspend fun uploadLogs(): List<String> = withContext(ioDispatcher) {
        try {
            val encryptedLogsUuid = mutableListOf<String>()
            logFileProvider.getLogFiles().forEach { logFile ->
                if (logFile.exists()) {
                    encryptedLogging.encryptAndUploadLogFile(
                        logFile = logFile,
                        shouldStartUploadImmediately = true
                    )?.let { uuid ->
                        encryptedLogsUuid.add(uuid)
                    }
                }
            }
            encryptedLogsUuid
        } catch (throwable: Throwable) {
            appLogWrapper.e(AppLog.T.SUPPORT, "Error uploading logs: ${throwable.stackTraceToString()}")
            throw throwable
        }
    }
}
