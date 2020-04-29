package org.wordpress.android.ui.posts.prepublishing.visibility

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PRIVATE
import javax.inject.Inject

class UpdatePostStatusUseCase @Inject constructor() {
    fun updatePostStatus(
        visibility: Visibility,
        editPostRepository: EditPostRepository,
        onPostStatusUpdated: () -> Unit
    ) {
        val postStatus = if (visibility == PRIVATE) {
            PostStatus.PRIVATE
        } else {
            PostStatus.PUBLISHED
        }

        editPostRepository.updateAsync({ postModel: PostModel ->
            postModel.setStatus(postStatus.toString())
            true
        }, { _, result ->
            if (result == UpdatePostResult.Updated) {
                onPostStatusUpdated.invoke()
            }
        })
    }
}
