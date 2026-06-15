package org.wordpress.android.support.unified.util

import android.app.Application
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.support.unified.model.AttachmentState
import org.wordpress.android.util.extensions.fileSize
import javax.inject.Inject
import javax.inject.Named

/**
 * Validates attachment selections against the total size limit and keeps the resulting
 * [AttachmentState] consistent.
 */
class AttachmentStateValidator @Inject constructor(
    private val application: Application,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) {
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

        // Validate each new attachment. An unknown size is reported as 0, which is always accepted.
        for (uri in uris) {
            val fileSize = uri.fileSize(application)
            if (currentTotalSize + fileSize > MAX_TOTAL_SIZE_BYTES) {
                skippedUris.add(uri)
            } else {
                validUris.add(uri)
                currentTotalSize += fileSize
            }
        }

        AttachmentState(
            acceptedUris = currentAttachmentState.acceptedUris + validUris,
            // rejectedUris reflects only the URIs skipped in this call; it is not accumulated
            // across calls. The reply form re-validates these against freed space on removal.
            rejectedUris = skippedUris,
            currentTotalSizeBytes = currentTotalSize,
            rejectedTotalSizeBytes = calculateTotalSize(skippedUris)
        )
    }

    suspend fun removeAttachment(currentState: AttachmentState, uri: Uri): AttachmentState =
        withContext(ioDispatcher) {
            val newAcceptedUris = currentState.acceptedUris.filter { it != uri }
            currentState.copy(
                acceptedUris = newAcceptedUris,
                currentTotalSizeBytes = calculateTotalSize(newAcceptedUris)
            )
        }

    private fun calculateTotalSize(uris: List<Uri>): Long = uris.sumOf { it.fileSize(application) }

    companion object {
        const val MAX_TOTAL_SIZE_BYTES = 20L * 1024 * 1024 // 20MB total
    }
}
