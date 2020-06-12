package org.wordpress.android.ui.posts.editor.media

import dagger.Reusable
import org.wordpress.android.analytics.AnalyticsTracker.Stat.EDITOR_UPLOAD_MEDIA_FAILED
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import javax.inject.Inject

@Reusable
class HandleMediaUploadErrorUseCase @Inject constructor(
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun onMediaUploadError(editorMediaListener: EditorMediaListener, media: MediaModel, error: MediaError) {
        val localMediaId = media.id.toString()
        val properties: Map<String, Any?> = analyticsUtilsWrapper
                .getMediaProperties(media.isVideo, null, media.filePath)
                .also {
                    it["error_type"] = error.type.name
                }
        analyticsTrackerWrapper.track(EDITOR_UPLOAD_MEDIA_FAILED, properties)
        editorMediaListener.onMediaUploadFailed(localMediaId)
    }
}
