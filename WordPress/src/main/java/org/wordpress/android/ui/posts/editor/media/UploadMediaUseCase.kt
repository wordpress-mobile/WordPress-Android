package org.wordpress.android.ui.posts.editor.media

import dagger.Reusable
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.QUEUED
import org.wordpress.android.ui.posts.EditPostActivity
import org.wordpress.android.ui.uploads.UploadServiceFacade
import java.util.ArrayList
import javax.inject.Inject

/**
 * Saves post so the UploadService has access to the most up to date version and initiates an upload.
 */
@Reusable
class UploadMediaUseCase @Inject constructor(
    private val uploadServiceFacade: UploadServiceFacade
) {
    fun saveQueuedPostAndStartUpload(editorMediaListener: EditorMediaListener, mediaModels: List<MediaModel>) {
        require(mediaModels.all { MediaUploadState.fromString(it.uploadState) == QUEUED }) { "Upload can " +
                "be initiated only on MediaModels with UploadStatus set to QUEUED." }

        mediaModels.let { queuedMediaModels ->
            // before starting the service, we need to update the posts' contents so we are sure the service
            // can retrieve it from there on
            editorMediaListener.syncPostObjectWithUiAndSaveIt(EditPostActivity.OnPostUpdatedFromUIListener {
                uploadServiceFacade.uploadMediaFromEditor(ArrayList(queuedMediaModels))
            })
        }
    }
}
