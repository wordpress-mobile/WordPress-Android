package org.wordpress.android.util

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.ASYNC
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.EncryptedLogActionBuilder
import org.wordpress.android.fluxc.store.EncryptedLogStore
import org.wordpress.android.fluxc.store.EncryptedLogStore.OnEncryptedLogUploaded
import org.wordpress.android.fluxc.store.EncryptedLogStore.OnEncryptedLogUploaded.EncryptedLogFailedToUpload
import org.wordpress.android.fluxc.store.EncryptedLogStore.OnEncryptedLogUploaded.EncryptedLogUploadedSuccessfully
import org.wordpress.android.fluxc.store.EncryptedLogStore.UploadEncryptedLogPayload
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.util.AppLog.T
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class EncryptedLogging @Inject constructor(
    private val dispatcher: Dispatcher,
    private val encryptedLogStore: EncryptedLogStore,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private val coroutineScope = CoroutineScope(bgDispatcher)

    init {
        dispatcher.register(this)
    }

    fun start() {
        dispatcher.dispatch(EncryptedLogActionBuilder.newResetUploadStatesAction())
        if (networkUtilsWrapper.isNetworkAvailable()) {
            coroutineScope.launch {
                encryptedLogStore.uploadQueuedEncryptedLogs()
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
            val payload = UploadEncryptedLogPayload(
                    uuid = uuid,
                    file = logFile,
                    // If the connection is not available, we shouldn't try to upload immediately
                    shouldStartUploadImmediately = shouldStartUploadImmediately &&
                            networkUtilsWrapper.isNetworkAvailable()
            )
            dispatcher.dispatch(EncryptedLogActionBuilder.newUploadLogAction(payload))
            return uuid
        }
        return null
    }

    @Suppress("unused")
    @Subscribe(threadMode = ASYNC)
    fun onEncryptedLogUploaded(event: OnEncryptedLogUploaded) {
        when (event) {
            is EncryptedLogUploadedSuccessfully -> {
                AppLog.i(T.MAIN, "Encrypted log with uuid: ${event.uuid} uploaded successfully!")
            }
            is EncryptedLogFailedToUpload -> {
                AppLog.e(T.MAIN, "Encrypted log with uuid: ${event.uuid} failed to upload with error: ${event.error}")
            }
        }
    }
}
