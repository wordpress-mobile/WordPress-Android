package org.wordpress.android.ui.media

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.utils.AuthenticationUtils
import java.io.IOException
import java.net.URL
import javax.inject.Inject
import javax.inject.Named

class VideoLoader
@Inject constructor(
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @param:Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val authenticationUtils: AuthenticationUtils
) {
    fun runIfMediaNotTooBig(scope: CoroutineScope, filePath: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        scope.launch {
            var length = 0
            withContext(bgDispatcher) {
                try {
                    val url = URL(filePath)

                    val urlConnection = url.openConnection()
                    for ((key, value) in authenticationUtils.getAuthHeaders(filePath).entries) {
                        urlConnection.addRequestProperty(key, value)
                    }

                    length = urlConnection.contentLength
                    if (length <= 0) {
                        length = urlConnection.getHeaderFieldInt("Content-Length", 0)
                    }
                } catch (ioe: IOException) {
                    ioe.printStackTrace()
                }
            }
            withContext(mainDispatcher) {
                if (length in 1 until SIZE_LIMIT_10_MB) {
                    onSuccess()
                } else {
                    onFailure()
                }
            }
        }
    }

    companion object {
        private const val SIZE_LIMIT_10_MB = 10485760
    }
}
