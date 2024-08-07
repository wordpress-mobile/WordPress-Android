package org.wordpress.android.support

import com.zendesk.service.ErrorResponse
import com.zendesk.service.ZendeskCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.extensions.mimeType
import zendesk.support.Support
import zendesk.support.UploadResponse
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * https://zendesk.github.io/mobile_sdk_javadocs/supportv2/v301/index.html?zendesk/support/UploadProvider.html
 */
class ZendeskUploadHelper @Inject constructor() {
    /**
     * Uploads multiple attachments to Zendesk and returns a list of their tokens when completed
     */
    suspend fun uploadFileAttachments(
        files: List<File>,
    ) = suspendCancellableCoroutine { continuation ->
        val uploadProvider = Support.INSTANCE.provider()?.uploadProvider()
        if (uploadProvider == null) {
            AppLog.e(T.SUPPORT, "Upload provider is null")
            continuation.resumeWithException(IOException("Unable to upload attachments"))
            return@suspendCancellableCoroutine
        }

        val tokens = ArrayList<String>()
        var numAttachments = files.size

        val callback = object : ZendeskCallback<UploadResponse>() {
            override fun onSuccess(result: UploadResponse) {
                if (continuation.isActive) {
                    result.token?.let {
                        tokens.add(it)
                    }
                    numAttachments--
                    if (numAttachments <= 0) {
                        continuation.resume(tokens)
                    }
                }
            }

            override fun onError(errorResponse: ErrorResponse?) {
                AppLog.e(T.SUPPORT, "Uploading to Zendesk failed with ${errorResponse?.reason}")
                if (continuation.isActive) {
                    continuation.resumeWithException(IOException("Uploading to Zendesk failed"))
                }
            }
        }
        for (file in files) {
            uploadProvider.uploadAttachment(
                file.name,
                file,
                file.mimeType() ?: "",
                callback
            )
        }
    }

    /**
     * Deletes an attachment from Zendesk. This is currently used only during development.
     */
    @Suppress("unused")
    fun deleteAttachment(token: String) {
        val uploadProvider = Support.INSTANCE.provider()?.uploadProvider()
        if (uploadProvider == null) {
            AppLog.e(T.SUPPORT, "Upload provider is null")
            return
        }
        uploadProvider.deleteAttachment(token, object : ZendeskCallback<Void>() {
            override fun onSuccess(result: Void) {
                AppLog.i(T.SUPPORT, "Successfully deleted Zendesk attachment")
            }

            override fun onError(errorResponse: ErrorResponse?) {
                AppLog.e(T.SUPPORT, "Unable to delete Zendesk attachment: ${errorResponse?.reason}")
            }
        })
    }
}
