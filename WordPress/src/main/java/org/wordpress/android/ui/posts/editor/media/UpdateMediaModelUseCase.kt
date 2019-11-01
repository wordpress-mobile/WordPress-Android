package org.wordpress.android.ui.posts.editor.media

import dagger.Reusable
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.ui.posts.editor.EditorMediaPostData
import javax.inject.Inject

// TODO Delete this class and move the logic to one of the related classes
@Reusable
class UpdateMediaModelUseCase @Inject constructor(private val dispatcher: Dispatcher) {
    fun updateMediaModel(
        media: MediaModel,
        postData: EditorMediaPostData,
        initialUploadState: MediaUploadState
    ) {
        setPostIds(media, postData)
        media.setUploadState(initialUploadState)
        dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media))
    }

    private fun setPostIds(
        media: MediaModel,
        postData: EditorMediaPostData
    ) {
        if (!postData.isLocalDraft) {
            media.postId = postData.remotePostId
        }
        media.localPostId = postData.localPostId
    }
}
