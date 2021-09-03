package org.wordpress.android.ui.media

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.utils.AuthenticationUtils
import org.wordpress.android.util.AppLog.T
import java.io.IOException
import java.net.URL
import javax.inject.Inject
import javax.inject.Named

class VideoLoader
@Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @param:Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val authenticationUtils: AuthenticationUtils,
    private val appLogWrapper: AppLogWrapper
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
                    val url = URL(filePath)

                    val urlConnection = url.openConnection()
                    for ((key, value) in authenticationUtils.getAuthHeaders(filePath).entries) {
                        urlConnection.addRequestProperty(key, value)
                    }

                    length = urlConnection.contentLength
                    if (length <= MIN_SIZE) {
                        length = urlConnection.getHeaderFieldInt("Content-Length", MIN_SIZE)
                    }
                } catch (ioe: IOException) {
                    appLogWrapper.e(T.MEDIA, "Failed to load video thumbnail: ${ioe.stackTrace}")
                }
            }
            withContext(mainDispatcher) {
                if (length > MIN_SIZE && length < SIZE_LIMIT_10_MB) {
                    loadAction()
                } else {
                    fallbackAction()
                }
            }
        }
    }

    companion object {
        private const val MIN_SIZE = 0
        private const val SIZE_LIMIT_10_MB = 10485760
    }
}
