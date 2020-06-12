package org.wordpress.android.ui.posts.editor.media

import dagger.Reusable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.analytics.AnalyticsTracker.Stat.EDITOR_UPLOAD_MEDIA_FAILED
import org.wordpress.android.editor.EditorMediaUploadListener
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import javax.inject.Inject
import javax.inject.Named

@Reusable
class TrackMediaUploadErrorUseCase @Inject constructor(
    private val analyticsUtilsWrapper: AnalyticsUtilsWrapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    suspend fun trackMediaUploadError(media: MediaModel, error: MediaError) {
        withContext(bgDispatcher) {
            val properties: Map<String, Any?> = analyticsUtilsWrapper
                    .getMediaProperties(media.isVideo, null, media.filePath)
                    .also {
                        it["error_type"] = error.type.name
                    }
            analyticsTrackerWrapper.track(EDITOR_UPLOAD_MEDIA_FAILED, properties)
        }
    }
}
