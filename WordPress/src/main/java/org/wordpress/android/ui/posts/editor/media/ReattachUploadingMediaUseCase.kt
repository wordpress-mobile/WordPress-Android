package org.wordpress.android.ui.posts.editor.media

import org.wordpress.android.editor.EditorMediaUploadListener
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.uploads.UploadServiceFacade
import javax.inject.Inject

class ReattachUploadingMediaUseCase @Inject constructor(
    private val uploadServiceFacade: UploadServiceFacade
) {
    fun reattachUploadingMediaForAztec(
        editPostRepository: EditPostRepository,
        editorMediaUploadListener: EditorMediaUploadListener
    ) {
        // UploadService.getPendingMediaForPost will be populated only when the user exits the editor
        // But if the user doesn't exit the editor and sends the app to the background, a reattachment
        // for the media within this Post is needed as soon as the app comes back to foreground,
        // so we get the list of progressing media for this Post from the UploadService
        val uploadingMediaInPost = editPostRepository.getPendingMediaForPost()
        val allUploadingMediaInPost = ArrayList<MediaModel>(uploadingMediaInPost)

        editPostRepository.getPendingOrInProgressMediaUploadsForPost().forEach { media1 ->
            // add them to the array only if they are not in there yet
            if (uploadingMediaInPost.none { media2 -> media1.id == media2.id }) {
                allUploadingMediaInPost.add(media1)
            }
        }

        // now do proper re-attachment of upload progress on each media item
        allUploadingMediaInPost.forEach { mediaModel ->
            editorMediaUploadListener.onMediaUploadReattached(
                    mediaModel.id.toString(),
                    uploadServiceFacade.getUploadProgressForMedia(mediaModel)
            )
        }
    }
}
