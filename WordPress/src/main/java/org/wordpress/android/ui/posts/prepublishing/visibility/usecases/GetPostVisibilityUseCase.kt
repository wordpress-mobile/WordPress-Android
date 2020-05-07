package org.wordpress.android.ui.posts.prepublishing.visibility.usecases

import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PASSWORD_PROTECTED
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PRIVATE
import org.wordpress.android.ui.posts.prepublishing.visibility.PrepublishingVisibilityItemUiState.Visibility.PUBLIC
import javax.inject.Inject

class GetPostVisibilityUseCase @Inject constructor() {
    fun getVisibility(editPostRepository: EditPostRepository) = when {
        editPostRepository.password.isNotEmpty() -> PASSWORD_PROTECTED
        editPostRepository.status == PostStatus.PRIVATE -> PRIVATE
        else -> PUBLIC
    }
}
