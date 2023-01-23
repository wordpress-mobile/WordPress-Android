package org.wordpress.android.ui.media

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.utils.AuthenticationUtils
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.MediaUtilsWrapper
import java.io.IOException
import java.net.URL
import javax.inject.Inject
import javax.inject.Named

class VideoLoader
@Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @param:Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val authenticationUtils: AuthenticationUtils,
    private val appLogWrapper: AppLogWrapper,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    private val appContext: Context
) {
    fun runIfMediaNotTooBig(
        scope: CoroutineScope,
        filePath: String,
        loadAction: () -> Unit,
        fallbackAction: () -> Unit
    ) {
        scope.launch {
            var length = MIN_SIZE
            withContext(bgDispatcher) {
                try {
                    val uri = Uri.parse(filePath)
                    length = if (mediaUtilsWrapper.isInMediaStore(uri)) {
                        getSizeFromContentUri(uri)
                    } else {
                        getSizeFromURL(URL(filePath))
                    }
                } catch (ioe: IOException) {
                    appLogWrapper.e(T.MEDIA, "Failed to load video thumbnail: ${ioe.stackTrace}")
                }
            }
            withContext(mainDispatcher) {
                if (length in (MIN_SIZE + 1) until SIZE_LIMIT_10_MB) {
                    loadAction()
                } else {
                    fallbackAction()
                }
            }
        }
    }

    private fun getSizeFromContentUri(contentUri: Uri) =
        appContext.contentResolver.query(contentUri, null, null, null, null, null).use { cursor ->
            cursor?.moveToFirst()?.takeIf { true }?.let {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (!cursor.isNull(sizeIndex)) cursor.getInt(sizeIndex) else null
            } ?: 0
        }

    private fun getSizeFromURL(url: URL): Int {
        val urlConnection = url.openConnection()
        for ((key, value) in authenticationUtils.getAuthHeaders(url.toString()).entries) {
            urlConnection.addRequestProperty(key, value)
        }

        var length = urlConnection.contentLength
        if (length <= MIN_SIZE) {
            length = urlConnection.getHeaderFieldInt("Content-Length", MIN_SIZE)
        }
        return length
    }

    companion object {
        private const val MIN_SIZE = 0
        private const val SIZE_LIMIT_10_MB = 10485760
    }
}
