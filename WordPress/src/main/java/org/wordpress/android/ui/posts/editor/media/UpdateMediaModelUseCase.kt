package org.wordpress.android.ui.posts.editor.media

import dagger.Reusable
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.MediaActionBuilder
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState
import org.wordpress.android.fluxc.model.PostImmutableModel
import javax.inject.Inject

/**
 * Updates posts localId, remoteId and upload status.
 *
 */
@Reusable
class UpdateMediaModelUseCase @Inject constructor(private val dispatcher: Dispatcher) {
    fun updateMediaModel(
        media: MediaModel,
        postData: PostImmutableModel,
        initialUploadState: MediaUploadState
    ) {
        media.postId = postData.remotePostId
        media.localPostId = postData.id
        media.setUploadState(initialUploadState)
        dispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(media))
    }
}
