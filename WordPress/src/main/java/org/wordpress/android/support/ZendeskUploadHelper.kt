package org.wordpress.android.support

import com.zendesk.service.ErrorResponse
import com.zendesk.service.ZendeskCallback
import kotlinx.coroutines.DefaultExecutor.enqueue
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
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
     * Uploads an attachment to Zendesk. Note that the UploadResponse will contain the attachment token.
     */
    private fun uploadAttachment(
        file: File,
        mimeType: String,
        callback: ZendeskCallback<UploadResponse>,
    ) {
        val uploadProvider = Support.INSTANCE.provider()?.uploadProvider()
        if (uploadProvider == null) {
            AppLog.e(T.SUPPORT, "Upload provider is null")
            return
        }
        uploadProvider.uploadAttachment(
            file.name,
            file,
            mimeType,
            callback
        )
    }

    suspend fun uploadAttachment(
        file: File,
        mimeType: String,
    ) = suspendCoroutine<String?> { continuation ->
        val uploadProvider = Support.INSTANCE.provider()?.uploadProvider()
        if (uploadProvider == null) {
            AppLog.e(T.SUPPORT, "Upload provider is null")
            continuation.resume(null)
        }
        val callback = object : ZendeskCallback<UploadResponse>() {
            override fun onSuccess(result: UploadResponse) {
                continuation.resume(result.token)
            }

            override fun onError(errorResponse: ErrorResponse?) {
                AppLog.e(
                    T.SUPPORT, "Uploading to Zendesk failed with ${errorResponse?.reason}"
                )
                continuation.resume(null)
            }
        }
        uploadProvider!!.uploadAttachment(
            file.name,
            file,
            mimeType,
            callback
        )
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
