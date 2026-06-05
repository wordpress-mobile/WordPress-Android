package org.wordpress.android.support.unified.util

import android.annotation.SuppressLint
import android.app.Application
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.support.unified.model.AttachmentState
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Named

/**
 * Validates attachment selections against the total size limit and keeps the resulting
 * [AttachmentState] consistent. Shared by the HE and unified support flows.
 */
class AttachmentStateValidator @Inject constructor(
    private val application: Application,
    private val appLogWrapper: AppLogWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) {
    @Suppress("LoopWithTooManyJumpStatements")
    suspend fun addAttachments(
        currentAttachmentState: AttachmentState,
        uris: List<Uri>
    ): AttachmentState = withContext(ioDispatcher) {
        if (uris.isEmpty()) {
            return@withContext currentAttachmentState
        }

        val validUris = mutableListOf<Uri>()
        val skippedUris = mutableListOf<Uri>()

        // Calculate current total size
        var currentTotalSize = calculateTotalSize(currentAttachmentState.acceptedUris)

        // Validate each new attachment
        for (uri in uris) {
            val fileSize = getFileSize(uri)

            // Skip if we can't determine file size we just allow it to be added
            if (fileSize != null) {
                // Check if adding this file would exceed total size limit
                if (currentTotalSize + fileSize > MAX_TOTAL_SIZE_BYTES) {
                    skippedUris.add(uri)
                    continue
                }
            }

            // File is valid, add it
            validUris.add(uri)
            currentTotalSize += fileSize ?: 0
        }

        // Build the new attachment state
        val currentAccepted = currentAttachmentState.acceptedUris
        val newAccepted = currentAccepted + validUris

        // Calculate rejected total size
        val rejectedTotalSize = calculateTotalSize(skippedUris)

        AttachmentState(
            acceptedUris = newAccepted,
            rejectedUris = skippedUris,
            currentTotalSizeBytes = currentTotalSize,
            rejectedTotalSizeBytes = rejectedTotalSize
        )
    }

    fun removeAttachment(currentState: AttachmentState, uri: Uri): AttachmentState {
        val newAcceptedUris = currentState.acceptedUris.filter { it != uri }
        return currentState.copy(acceptedUris = newAcceptedUris)
    }

    @SuppressLint("Recycle") // False positive: descriptor is closed via .use {}
    @Suppress("TooGenericExceptionCaught")
    private suspend fun getFileSize(uri: Uri): Long? = withContext(ioDispatcher) {
        try {
            application.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
                descriptor.length
            }
        } catch (e: Exception) {
            appLogWrapper.d(AppLog.T.SUPPORT, "Could not determine file size for URI: $uri - ${e.message}")
            // Silently return null if we can't get the file size
            // This will be handled by the validation logic
            null
        }
    }

    /**
     * Calculates the total size of all files in the list
     * @param uris List of URIs to calculate size for
     * @return Total size in bytes
     */
    private suspend fun calculateTotalSize(uris: List<Uri>): Long {
        var totalSize = 0L
        for (uri in uris) {
            totalSize += getFileSize(uri) ?: 0L
        }
        return totalSize
    }

    companion object {
        const val MAX_TOTAL_SIZE_BYTES = 20L * 1024 * 1024 // 20MB total
    }
}
