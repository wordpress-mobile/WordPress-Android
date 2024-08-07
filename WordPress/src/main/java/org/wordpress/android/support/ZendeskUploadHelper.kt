package org.wordpress.android.support

import com.zendesk.service.ErrorResponse
import com.zendesk.service.ZendeskCallback
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.extensions.mimeType
import zendesk.support.Support
import zendesk.support.UploadResponse
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * https://zendesk.github.io/mobile_sdk_javadocs/supportv2/v301/index.html?zendesk/support/UploadProvider.html
 */
class ZendeskUploadHelper @Inject constructor() {

    /**
     * Uploads multiple attachments to Zendesk and returns a list of their tokens when completed
     */
    suspend fun uploadFileAttachments(
        files: List<File>,
    ) = suspendCoroutine { continuation ->
        val uploadProvider = Support.INSTANCE.provider()?.uploadProvider()
        if (uploadProvider == null) {
            AppLog.e(T.SUPPORT, "Upload provider is null")
            continuation.resume(null)
            return@suspendCoroutine
        }

        val tokens = ArrayList<String>()
        var numAttachments = files.size

        fun decAttachments() {
            numAttachments--
            if (numAttachments <= 0) {
                continuation.resume(tokens)
            }
        }

        val callback = object : ZendeskCallback<UploadResponse>() {
            override fun onSuccess(result: UploadResponse) {
                result.token?.let {
                    tokens.add(it)
                }
                decAttachments()
            }

            override fun onError(errorResponse: ErrorResponse?) {
                AppLog.e(
                    T.SUPPORT, "Uploading to Zendesk failed with ${errorResponse?.reason}"
                )
                decAttachments()
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

            override fun onError(error: ErrorResponse?) {
                AppLog.e(T.SUPPORT, "Unable to delete Zendesk attachment")
            }
        })
    }
}
