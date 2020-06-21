package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.UTILS
import org.wordpress.android.util.MediaUtilsWrapper
import javax.inject.Inject
import javax.inject.Named

@Reusable
class CopyMediaToAppStorageUseCase @Inject constructor(
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) {
    /*
   * Some media providers (eg. Google Photos) give us a limited access to media files just so we can copy them and then
   * they revoke the access. Copying these files must be performed on the UI thread, otherwise the access might be
   * revoked before the action completes. See https://github.com/wordpress-mobile/WordPress-Android/issues/5818
   */
    suspend fun copyFilesToAppStorageIfNecessary(uriList: List<Uri>): CopyMediaResult {
        return withContext(mainDispatcher) {
            uriList
                    .map { mediaUri ->
                        if (!mediaUtilsWrapper.isInMediaStore(mediaUri)) {
                            copyToAppStorage(mediaUri)
                        } else {
                            mediaUri
                        }
                    }
                    .toList()
                    .let {
                        CopyMediaResult(
                                permanentlyAccessibleUris = it.filterNotNull(),
                                copyingSomeMediaFailed = it.contains(null)
                        )
                    }
        }
    }

    private fun copyToAppStorage(mediaUri: Uri): Uri? {
        return try {
            mediaUtilsWrapper.copyFileToAppStorage(mediaUri)
        } catch (e: IllegalStateException) {
            // Ref: https://github.com/wordpress-mobile/WordPress-Android/issues/5823
            val errorMessage = "Can't download the image at: $mediaUri See issue #5823"
            AppLog.e(UTILS, errorMessage, e)
            null
        }
    }

    data class CopyMediaResult(
        val permanentlyAccessibleUris: List<Uri>,
        val copyingSomeMediaFailed: Boolean
    )
}
