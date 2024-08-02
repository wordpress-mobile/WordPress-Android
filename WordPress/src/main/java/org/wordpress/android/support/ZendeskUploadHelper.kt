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

class ZendeskUploadHelper {
    suspend fun uploadFile(
        context: Context,
        uri: Uri,
    ): zendesk.support.Attachment? {
        val uploadProvider = Support.INSTANCE.provider()?.uploadProvider()
        if (uploadProvider == null) {
            AppLog.e(AppLog.T.SUPPORT, "Upload provider is null")
            return null
        }

        val file = File(uri.fileName(context))
        val mimeType = uri.mimeType(context)

        uploadProvider.uploadAttachment(
            file.name,
            file,
            mimeType,
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

    suspend fun uploadFiles(
        context: Context,
        uris: List<Uri>,
    ): zendesk.support.Attachment? {
        uris.forEach {
            uploadFile(context, it)
        }
        return null
    }
}
