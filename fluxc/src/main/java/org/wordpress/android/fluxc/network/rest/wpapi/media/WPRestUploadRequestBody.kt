package org.wordpress.android.fluxc.network.rest.wpapi.media

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

private const val FILE_FORM_KEY = "file"
private const val TITLE_FORM_KEY = "title"
private const val DESCRIPTION_FORM_KEY = "description"
private const val CAPTION_FORM_KEY = "caption"
private const val ALT_FORM_KEY = "alt_text"
private const val POST_ID_FORM_KEY = "post"

class WPRestUploadRequestBody(
    media: MediaModel,
    progressListener: ProgressListener
) : BaseUploadRequestBody(media, progressListener) {
    private val multipartBody: MultipartBody

    init {
        multipartBody = buildMultipartBody()
    }

    private fun buildMultipartBody(): MultipartBody {
        fun MultipartBody.Builder.addParamIfNotEmpty(key: String, attribute: String?): MultipartBody.Builder {
            return apply {
                attribute?.takeIf { it.isNotEmpty() }?.let {
                    addFormDataPart(key, it)
                }
            }
        }

        val mediaFile = File(media.filePath)
        val body = mediaFile.asRequestBody(media.mimeType.toMediaType())
        val fileName = URLEncoder.encode(media.fileName, "UTF-8")

        val builder: MultipartBody.Builder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(FILE_FORM_KEY, fileName, body)
                .addParamIfNotEmpty(TITLE_FORM_KEY, media.title)
                .addParamIfNotEmpty(DESCRIPTION_FORM_KEY, media.description)
                .addParamIfNotEmpty(CAPTION_FORM_KEY, media.caption)
                .addParamIfNotEmpty(ALT_FORM_KEY, media.alt)
                .addParamIfNotEmpty(POST_ID_FORM_KEY, media.postId.takeIf { it > 0L }?.toString())

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
