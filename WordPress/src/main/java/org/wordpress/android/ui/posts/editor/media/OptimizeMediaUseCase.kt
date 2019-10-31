package org.wordpress.android.ui.posts.editor.media

import android.net.Uri
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.posts.editor.EditorTracker
import org.wordpress.android.util.MediaUtilsWrapper
import javax.inject.Inject
import javax.inject.Named

class OptimizeMediaUseCase @Inject constructor(
    private val editorTracker: EditorTracker,
    private val mediaUtilsWrapper: MediaUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    suspend fun optimizeMediaAsync(
        site: SiteModel,
        isNew: Boolean,
        uriList: List<Uri>
    ): OptimizeMediaResult {
        return withContext(bgDispatcher) {
            uriList
                    .map { async { optimizeMediaAndTrackEvent(it, isNew, site) } }
                    .map { it.await() }
                    .toList()
                    .let {
                        OptimizeMediaResult(
                                optimizedMediaUris = it.filterNotNull(),
                                someMediaCouldNotBeRetrieved = it.contains(null)
                        )
                    }
        }
    }

    private fun optimizeMediaAndTrackEvent(mediaUri: Uri, isNew: Boolean, site: SiteModel): Uri? {
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

        editorTracker.trackAddMediaFromDevice(site, isNew, isVideo, updatedMediaUri)

        return updatedMediaUri
    }

    data class OptimizeMediaResult(
        val optimizedMediaUris: List<Uri>,
        val someMediaCouldNotBeRetrieved: Boolean
    )
}
