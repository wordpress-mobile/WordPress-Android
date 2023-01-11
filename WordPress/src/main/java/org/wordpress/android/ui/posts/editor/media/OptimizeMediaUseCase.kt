package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.posts.editor.EditorTracker
import org.wordpress.android.util.MediaUtilsWrapper
import javax.inject.Inject
import javax.inject.Named

/**
 * Optimizes images and fixes their rotation.
 *
 * Warning: This use case optimizes images only if the user enabled image optimization (AppPrefs.isImageOptimize()).
 */
@Reusable
class OptimizeMediaUseCase @Inject constructor(
    private val editorTracker: EditorTracker,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    suspend fun optimizeMediaIfSupportedAsync(
        site: SiteModel,
        freshlyTaken: Boolean,
        uriList: List<Uri>,
        trackEvent: Boolean = true
    ): OptimizeMediaResult {
        return withContext(bgDispatcher) {
            uriList
                .map { async { optimizeMedia(it, freshlyTaken, site, trackEvent) } }
                .map { it.await() }
                .let {
                    OptimizeMediaResult(
                        optimizedMediaUris = it.filterNotNull(),
                        loadingSomeMediaFailed = it.contains(null)
                    )
                }
        }
    }

    private fun optimizeMedia(mediaUri: Uri, freshlyTaken: Boolean, site: SiteModel, trackEvent: Boolean): Uri? {
        val path = mediaUtilsWrapper.getRealPathFromURI(mediaUri) ?: return null
        val isVideo = mediaUtilsWrapper.isVideo(mediaUri.toString())

        /**
         * If the user enabled the optimize images feature, the image gets rotated in mediaUtils.getOptimizedMedia.
         * If the user haven't enabled it, WPCom server takes care of rotating the image, however we need to rotate it
         * manually on self-hosted sites. (https://github.com/wordpress-mobile/WordPress-Android/issues/5737)
         */
        val updatedMediaUri: Uri = mediaUtilsWrapper.getOptimizedMedia(path, isVideo)
            ?: if (!site.isWPCom) {
                mediaUtilsWrapper.fixOrientationIssue(path, isVideo) ?: mediaUri
            } else {
                mediaUri
            }

        if (trackEvent) {
            editorTracker.trackAddMediaFromDevice(site, freshlyTaken, isVideo, updatedMediaUri)
        }

        return updatedMediaUri
    }

    data class OptimizeMediaResult(
        val optimizedMediaUris: List<Uri>,
        val loadingSomeMediaFailed: Boolean
    )
}
