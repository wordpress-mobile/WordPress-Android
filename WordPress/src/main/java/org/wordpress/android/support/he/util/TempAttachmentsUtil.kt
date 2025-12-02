package org.wordpress.android.support.he.util

import android.app.Application
import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.util.AppLog
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import org.wordpress.android.fluxc.module.OkHttpClientQualifiers
import javax.inject.Inject
import javax.inject.Named
import kotlin.collections.forEach

class TempAttachmentsUtil @Inject constructor(
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
    @Named(OkHttpClientQualifiers.REGULAR) private val okHttpClient: OkHttpClient,
    private val appLogWrapper: AppLogWrapper,
    private val application: Application,
    private val accountStore: AccountStore
) {
    companion object {
        private const val CONNECTION_TIMEOUT_MS = 30_000 // 30 seconds
        private const val READ_TIMEOUT_MS = 60_000 // 60 seconds
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun createTempFilesFrom(uris: List<Uri>): List<File> = withContext(ioDispatcher) {
        uris.map{ it.toTempFile() }
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun removeTempFiles(files: List<File>) = withContext(ioDispatcher) {
        try {
            var removed = files.isEmpty() // If empty, count them as removed
            files.forEach { file ->
                if (file.exists()) {
                    removed = removed && file.delete()
                }
            }
            removed
        } catch (e: Exception) {
            appLogWrapper.e(AppLog.T.SUPPORT, "Error removing attachment temp files temp files: " +
                    e.stackTraceToString())
        }
    }

    @Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown")
    private suspend fun Uri.toTempFile(): File = withContext(ioDispatcher) {
        try {
            val inputStream = application.contentResolver.openInputStream(this@toTempFile)
                ?: throw Exception("Failed to open input stream for attachment")

            // Get file extension from MIME type or URI
            val extension = getFileExtension()
            val fileName = "support_attachment_${System.currentTimeMillis()}.$extension"
            val tempFile = File(application.cacheDir, fileName)

            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()

            tempFile
        } catch (e: Exception) {
            appLogWrapper.e(AppLog.T.SUPPORT, "Error copying URI to temp file: ${e.stackTraceToString()}")
            throw e
        }
    }

    @Suppress("ReturnCount")
    private fun Uri.getFileExtension(): String {
        // First, try to get extension from MIME type
        val mimeType = application.contentResolver.getType(this)
        mimeType?.let { type ->
            val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
            if (!extension.isNullOrEmpty()) {
                return extension
            }
        }

        // Fallback: try to extract extension from the URI path
        val path = this.path
        path?.let {
            val lastDotIndex = it.lastIndexOf('.')
            if (lastDotIndex > 0 && lastDotIndex < it.length - 1) {
                return it.substring(lastDotIndex + 1)
            }
        }

        // Default to jpg if we can't determine the extension
        return "jpg"
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun createVideoTempFile(videoUrl: String): File? = withContext(ioDispatcher) {
        var tempFile: File? = null

        try {
            tempFile = File.createTempFile("video_", ".mp4", application.cacheDir)

            val request = Request.Builder()
                .url(videoUrl)
                .addHeader("Authorization", "Bearer ${accountStore.accessToken}")
                .build()

            val client = okHttpClient.newBuilder()
                .connectTimeout(CONNECTION_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
                .build()

            client.newCall(request).execute().use { response ->
                val responseCode = response.code
                AppLog.d(AppLog.T.SUPPORT, "Download response code: $responseCode")

                if (response.isSuccessful) {
                    response.body?.byteStream()?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    AppLog.d(AppLog.T.SUPPORT, "Video downloaded: ${tempFile.absolutePath}")
                    tempFile
                } else {
                    val deleted = tempFile?.delete()
                    AppLog.e(
                        AppLog.T.SUPPORT,
                        "Failed to download video. Deleted: $deleted - Response code: $responseCode"
                    )
                    null
                }
            }
        } catch (e: Exception) {
            val deleted = tempFile?.delete()
            AppLog.e(AppLog.T.SUPPORT, "Error downloading video: ${e.message} Deleted: $deleted")
            null
        }
    }
}
