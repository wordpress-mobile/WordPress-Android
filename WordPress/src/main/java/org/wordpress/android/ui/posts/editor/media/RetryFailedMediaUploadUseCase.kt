package org.wordpress.android.ui.posts.editor.media

import dagger.Reusable
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.QUEUED
import org.wordpress.android.ui.posts.editor.EditorMediaListener
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
        failedMediaIds: List<String>
    ) {
        failedMediaIds
                .map { Integer.valueOf(it) }
                .let {
                    updateStatusAndInitiateUpload(editorMediaListener, it)
                }
                .also { tracker.track(Stat.EDITOR_UPLOAD_MEDIA_RETRIED) }
    }

    private suspend fun updateStatusAndInitiateUpload(
        editorMediaListener: EditorMediaListener,
        mediaModelLocalIds: List<Int>
    ) {
        getMediaModelUseCase
                .loadMediaModelFromDb(mediaModelLocalIds)
                .map { media ->
                    updateUploadStatus(media, editorMediaListener)
                    media
                }
                .let { mediaModels ->
                    uploadMediaUseCase.saveQueuedPostAndStartUpload(
                            editorMediaListener,
                            mediaModels
                    )
                }
    }

    private fun updateUploadStatus(
        mediaModel: MediaModel,
        editorMediaListener: EditorMediaListener
    ) {
        updateMediaModelUseCase.updateMediaModel(
                mediaModel,
                editorMediaListener.editorMediaPostData(),
                QUEUED
        )
    }
}
