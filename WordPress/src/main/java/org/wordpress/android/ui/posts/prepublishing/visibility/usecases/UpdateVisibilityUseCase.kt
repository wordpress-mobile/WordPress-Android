package org.wordpress.android.ui.posts.prepublishing.visibility.usecases

import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.DRAFT
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PASSWORD_PROTECTED
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PENDING_REVIEW
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PRIVATE
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PUBLISH
import javax.inject.Inject

class UpdateVisibilityUseCase @Inject constructor(private val updatePostStatusUseCase: UpdatePostStatusUseCase) {
    fun updatePostVisibility(
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

        updatePostStatusUseCase.updatePostStatus(postStatus, editPostRepository) {
            onPostStatusUpdated.invoke()
        }
    }
}
