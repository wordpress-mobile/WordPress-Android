package org.wordpress.android.support

import android.content.Context
import android.net.Uri
import com.zendesk.service.ErrorResponse
import com.zendesk.service.ZendeskCallback
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.extensions.fileName
import org.wordpress.android.util.extensions.mimeType
import zendesk.support.Support
import zendesk.support.UploadResponse
import java.io.File

/**
 * https://zendesk.github.io/mobile_sdk_javadocs/supportv2/v301/index.html?zendesk/support/UploadProvider.html
 */
class ZendeskUploadHelper {
    fun uploadFile(
        context: Context,
        uri: Uri,
    ): zendesk.support.Attachment? {
        val uploadProvider = Support.INSTANCE.provider()?.uploadProvider()
        if (uploadProvider == null) {
            AppLog.e(T.SUPPORT, "Upload provider is null")
            return null
        }

        val file = uri.fileName(context)?.let { File(it) }
        if (file == null) {
            AppLog.e(T.SUPPORT, "Upload file is null")
            return null
        }

        uploadProvider.uploadAttachment(
            file.name,
            file,
            uri.mimeType(context),
            object : ZendeskCallback<UploadResponse>() {
                override fun onSuccess(result: UploadResponse) {
                    // return result.attachment
                }

                override fun onError(errorResponse: ErrorResponse?) {
                    AppLog.v(
                        T.SUPPORT, "Uploading to Zendesk failed with" +
                                " error: ${errorResponse?.reason}"
                    )
                }
            })

        return null
    }

    fun uploadFiles(
        context: Context,
        uris: List<Uri>,
    ): zendesk.support.Attachment? {
        uris.forEach {
            uploadFile(context, it)
        }
        return null
    }
}
