package org.wordpress.android.ui.posts.editor.media

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.analytics.AnalyticsTracker.Stat.EDITOR_UPLOAD_MEDIA_FAILED
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.FluxCUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.util.helpers.MediaFile
import javax.inject.Inject
import javax.inject.Named

@Reusable
class HandleMediaUploadErrorUseCase @Inject constructor(
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val fluxCUtilsWrapper: FluxCUtilsWrapper,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    fun onMediaUploadError(editorMediaListener: EditorMediaListener, media: MediaModel, error: MediaError) {
        val localMediaId = media.id.toString()
        trackMediaUploadError(media.isVideo, media.filePath, error)
        editorMediaListener.onMediaUploadFailed(localMediaId)
    }

    private fun trackMediaUploadError(isVideo: Boolean, mediaPath: String, error: MediaError) {
        val properties: MutableMap<String, Any?> = analyticsUtilsWrapper
                .getMediaProperties(isVideo, null, mediaPath)
        properties["error_type"] = error.type.name
        analyticsTrackerWrapper.track(EDITOR_UPLOAD_MEDIA_FAILED, properties)
    }
}
