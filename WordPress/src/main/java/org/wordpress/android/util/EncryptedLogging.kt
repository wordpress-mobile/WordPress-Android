package org.wordpress.android.util

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.ASYNC
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.EncryptedLogActionBuilder
import org.wordpress.android.fluxc.store.EncryptedLogStore
import org.wordpress.android.fluxc.store.EncryptedLogStore.OnEncryptedLogUploaded
import org.wordpress.android.fluxc.store.EncryptedLogStore.UploadEncryptedLogPayload
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.AVAILABLE
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.UNAVAILABLE
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class EncryptedLogging @Inject constructor(
    private val dispatcher: Dispatcher,
    private val encryptedLogStore: EncryptedLogStore,
    private val connectionStatusLiveData: LiveData<ConnectionStatus>,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val coroutineScope = CoroutineScope(bgDispatcher)
    private var lastConnectionStatus = AVAILABLE

    fun start() {
        dispatcher.dispatch(EncryptedLogActionBuilder.newResetUploadStatesAction())
        uploadQueuedEncryptedLogsInBgThread()

        connectionStatusLiveData.observeForever { newStatus ->
            val connectionBecameAvailable = newStatus == AVAILABLE && lastConnectionStatus == UNAVAILABLE
            lastConnectionStatus = newStatus
            if (connectionBecameAvailable) {
                uploadQueuedEncryptedLogsInBgThread()
            }
        }
    }

    private fun uploadQueuedEncryptedLogsInBgThread() {
        coroutineScope.launch {
            dispatcher.dispatch(EncryptedLogActionBuilder.newResetUploadStatesAction())
            encryptedLogStore.uploadQueuedEncryptedLogs()
        }
    }

    /**
     * Dispatches a FluxC action that will queue the given log to be uploaded as soon as possible.
     *
     * @param logFile Log file to be uploaded
     * @param shouldStartUploadImmediately This parameter will decide whether we should try to upload the log file
     * immediately. We are unlikely to have enough time to complete the upload, so we can use this parameter to avoid
     * the unnecessary upload failure.
     */
    fun encryptAndUploadLogFile(logFile: File, shouldStartUploadImmediately: Boolean): String? {
        // TODO: Check how long log files are kept for and increase the duration if necessary
        if (logFile.exists()) {
            val uuid = UUID.randomUUID().toString()
            val payload = UploadEncryptedLogPayload(
                    uuid = uuid,
                    file = logFile,
                    // If the connection is not available, we shouldn't try to upload immediately
                    shouldStartUploadImmediately = shouldStartUploadImmediately && lastConnectionStatus == AVAILABLE
            )
            dispatcher.dispatch(EncryptedLogActionBuilder.newUploadLogAction(payload))
            return uuid
        }
        return null
    }

    @Suppress("unused")
    @Subscribe(threadMode = ASYNC)
    fun onEncryptedLogUploaded(event: OnEncryptedLogUploaded) {
        if (event.isError) {
            AppLog.e(T.MAIN, "Encrypted log with uuid: ${event.uuid} failed to upload with error: ${event.error}")
        } else {
            AppLog.e(T.MAIN, "Encrypted log with uuid: ${event.uuid} uploaded successfully!")
        }
    }
}
