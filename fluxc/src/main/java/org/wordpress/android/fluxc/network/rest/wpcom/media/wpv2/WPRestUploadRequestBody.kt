package org.wordpress.android.fluxc.network.rest.wpcom.media.wpv2

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okio.BufferedSink
import okio.buffer
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.network.BaseUploadRequestBody
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import java.io.File
import java.io.IOException
import java.net.URLEncoder

private const val FILE_DATA_KEY = "file"

class WPRestUploadRequestBody(
    media: MediaModel,
    progressListener: ProgressListener
) : BaseUploadRequestBody(media, progressListener) {
    private val multipartBody: MultipartBody

    init {
        multipartBody = buildMultipartBody()
    }

    private fun buildMultipartBody(): MultipartBody {
        val mediaFile = File(media.filePath)
        val body = mediaFile.asRequestBody(media.mimeType.toMediaType())
        val fileName = URLEncoder.encode(media.fileName, "UTF-8")

        val builder: MultipartBody.Builder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(FILE_DATA_KEY, fileName, body)

        return builder.build()
    }

    override fun contentLength(): Long {
        return try {
            multipartBody.contentLength()
        } catch (e: IOException) {
            AppLog.w(MEDIA, "Error determining mMultipartBody content length: $e")
            -1L
        }
    }

    override fun contentType(): MediaType = multipartBody.contentType()

    override fun writeTo(sink: BufferedSink) {
        val countingSink = CountingSink(sink)
        val bufferedSink = countingSink.buffer()
        multipartBody.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    override fun getProgress(bytesWritten: Long): Float = bytesWritten.toFloat() / contentLength()
}
