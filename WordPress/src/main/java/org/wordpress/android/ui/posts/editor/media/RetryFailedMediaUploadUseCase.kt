package org.wordpress.android.ui.posts.editor.media

import dagger.Reusable
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.QUEUED
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import javax.inject.Inject

@Reusable
class RetryFailedMediaUploadUseCase @Inject constructor(
    private val getMediaModelUseCase: GetMediaModelUseCase,
    private val updateMediaModelUseCase: UpdateMediaModelUseCase,
    private val uploadMediaUseCase: UploadMediaUseCase,
    private val tracker: AnalyticsTrackerWrapper
) {
    suspend fun retryFailedMediaAsync(
        editorMediaListener: EditorMediaListener,
        failedMediaLocalIds: List<Int>
    ) {
        getMediaModelUseCase
            .loadMediaByLocalId(failedMediaLocalIds)
            .map { media ->
                updateMediaModelUseCase.updateMediaModel(
                    media,
                    editorMediaListener.getImmutablePost(),
                    QUEUED
                )
                media
            }
            .let { mediaModels ->
                if (mediaModels.isNotEmpty()) {
                    uploadMediaUseCase.saveQueuedPostAndStartUpload(
                        editorMediaListener,
                        mediaModels
                    )
                    tracker.track(Stat.EDITOR_UPLOAD_MEDIA_RETRIED)
                }
            }
    }
}
