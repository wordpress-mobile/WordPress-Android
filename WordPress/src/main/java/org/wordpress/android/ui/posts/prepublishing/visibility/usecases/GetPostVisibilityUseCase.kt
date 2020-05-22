package org.wordpress.android.ui.posts.prepublishing.visibility.usecases

import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.PENDING
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PASSWORD_PROTECTED
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PENDING_REVIEW
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PUBLISH
import javax.inject.Inject

class GetPostVisibilityUseCase @Inject constructor() {
    fun getVisibility(editPostRepository: EditPostRepository) = when {
        editPostRepository.password.isNotEmpty() -> PASSWORD_PROTECTED
        editPostRepository.status == PUBLISHED -> PUBLISH
        editPostRepository.status == PENDING -> PENDING_REVIEW
        editPostRepository.status == DRAFT -> Visibility.DRAFT
        editPostRepository.status == PostStatus.PRIVATE -> Visibility.PRIVATE
        else -> throw IllegalStateException("${editPostRepository.status} wasn't resolved by any case in this when clause.")
    }
}
