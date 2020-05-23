package org.wordpress.android.ui.posts.prepublishing.visibility.usecases

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.DRAFT
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PASSWORD_PROTECTED
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PENDING_REVIEW
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PRIVATE
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PUBLISH
import org.wordpress.android.util.DateTimeUtilsWrapper
import java.lang.IllegalStateException
import javax.inject.Inject

class UpdatePostStatusUseCase @Inject constructor(private val dateTimeUtilsWrapper: DateTimeUtilsWrapper) {
    fun updatePostStatus(
        visibility: Visibility,
        editPostRepository: EditPostRepository,
        onPostStatusUpdated: () -> Unit
    ) {
        val postStatus = when (visibility) {
            PUBLISH -> PostStatus.PUBLISHED
            DRAFT -> PostStatus.DRAFT
            PENDING_REVIEW -> PostStatus.PENDING
            PRIVATE -> PostStatus.PRIVATE
            PASSWORD_PROTECTED -> {
                throw IllegalStateException("$visibility shouldn't be persisted. It does  not map to a PostStatus.")
            }
        }

        editPostRepository.updateAsync({ postModel: PostModel ->
            postModel.setStatus(postStatus.toString())

            // since PRIVATE posts always have to be immediate then we set the current date when updating to be sure.
            if (visibility == PRIVATE) {
                postModel.setDateCreated(dateTimeUtilsWrapper.currentTimeInIso8601())
            }

            true
        }, { _, result ->
            if (result == UpdatePostResult.Updated) {
                onPostStatusUpdated.invoke()
            }
        })
    }
}
