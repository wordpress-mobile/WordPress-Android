package org.wordpress.android.ui.posts.editor.media

import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.ui.posts.EditPostActivity
import org.wordpress.android.ui.posts.editor.EditorMediaListener
import org.wordpress.android.ui.uploads.UploadServiceFacade
import java.util.ArrayList
import javax.inject.Inject

class UploadMediaUseCase @Inject constructor(
    private val uploadServiceFacade: UploadServiceFacade
) {
    /**
     *
     * Only [MediaModel] objects that have `MediaUploadState.QUEUED` statuses will be uploaded. .
     */
    fun savePostAndStartUpload(editorMediaListener: EditorMediaListener, mediaModels: List<MediaModel>) {
        mediaModels.filter { media ->
            MediaUploadState.fromString(media.uploadState) == MediaUploadState.QUEUED
        }.let { queuedMediaModels ->
            // before starting the service, we need to update the posts' contents so we are sure the service
            // can retrieve it from there on
            editorMediaListener.savePostAsyncFromEditorMedia(EditPostActivity.AfterSavePostListener {
                uploadServiceFacade.uploadMediaFromEditor(ArrayList(queuedMediaModels))
            })
        }
    }
}
