package org.wordpress.android.ui.posts.prepublishing.home.usecases

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.SCHEDULED
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.util.DateTimeUtilsWrapper
import javax.inject.Inject

class PublishPostImmediatelyUseCase @Inject constructor(private val dateTimeUtilsWrapper: DateTimeUtilsWrapper) {
    fun updatePostToPublishImmediately(
        editPostRepository: EditPostRepository,
        publishPost: Boolean
    ) {
        editPostRepository.updateAsync({ postModel: PostModel ->
            if (postModel.status == SCHEDULED.toString()) {
                postModel.setDateCreated(dateTimeUtilsWrapper.currentTimeInIso8601())
            }
            // when the post is a Draft, Publish Now is shown as the Primary Action but if it's already Published then
            // Update Now is shown.
            if (publishPost) {
                postModel.setStatus(PostStatus.PUBLISHED.toString())
            } else {
                postModel.setStatus(PostStatus.DRAFT.toString())
            }
            true
        })
    }
}
