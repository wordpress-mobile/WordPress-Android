package org.wordpress.android.ui.media

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URL
import javax.inject.Inject
import javax.inject.Named

class VideoLoader
@Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @param:Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @param:Named("regular") private val okHttpClient: OkHttpClient,
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
            val isNotTooBig = checkMediaNotTooBig(filePath) ?: false
            withContext(mainDispatcher) {
                if (isNotTooBig) {
                    loadAction()
                } else {
                    fallbackAction()
                }
            }
        }
    }

    /**
     * Whether the media at [filePath] has a known size under the limit for extracting a video frame, or null when
     * the size couldn't be read, e.g. because the request failed
     */
    suspend fun checkMediaNotTooBig(filePath: String): Boolean? = withContext(bgDispatcher) {
        try {
            val uri = filePath.toUri()
            val length =
                if (mediaUtilsWrapper.isInMediaStore(uri)) getSizeFromContentUri(uri) else getSizeFromURL(URL(filePath))
            length?.let { it in (MIN_SIZE + 1) until SIZE_LIMIT_10_MB }
        } catch (ioe: IOException) {
            appLogWrapper.e(T.MEDIA, "Failed to load video thumbnail: ${ioe.stackTrace}")
            null
        }
    }

    @SuppressLint("Recycle") // False positive: cursor is closed via .use {}
    private fun getSizeFromContentUri(contentUri: Uri) =
        appContext.contentResolver.query(contentUri, null, null, null, null, null).use { cursor ->
            cursor?.moveToFirst()?.takeIf { true }?.let {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (!cursor.isNull(sizeIndex)) cursor.getInt(sizeIndex) else null
            } ?: 0
        }

    /** The size the server reports, or null for a failed request or one without a Content-Length */
    private fun getSizeFromURL(url: URL): Int? {
        val requestBuilder = Request.Builder()
            .url(url)
            .head()

        authenticationUtils.getAuthHeaders(url.toString()).forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        return okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
            response.takeIf { it.isSuccessful }?.header("Content-Length")?.toIntOrNull()
        }
    }

    companion object {
        private const val MIN_SIZE = 0
        private const val SIZE_LIMIT_10_MB = 10485760
    }
}
