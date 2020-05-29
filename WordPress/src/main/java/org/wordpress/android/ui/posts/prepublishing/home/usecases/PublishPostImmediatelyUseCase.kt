package org.wordpress.android.ui.posts.prepublishing.home.usecases

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult
import org.wordpress.android.ui.posts.PostUtilsWrapper
import javax.inject.Inject

class PublishPostImmediatelyUseCase @Inject constructor(private val postUtilsWrapper: PostUtilsWrapper) {
    fun updatePostToPublishImmediately(
        editPostRepository: EditPostRepository,
        isNewPost: Boolean,
        onPostUpdated: () -> Unit
    ) {
        editPostRepository.updateAsync({ postModel: PostModel ->
            postUtilsWrapper.updatePublishDateIfShouldBePublishedImmediately(postModel)

            if (isNewPost) {
                postModel.setStatus(PostStatus.DRAFT.toString())
            } else {
                postModel.setStatus(PostStatus.PUBLISHED.toString())
            }
            true
        }, { _, result ->
            if (result == UpdatePostResult.Updated) {
                onPostUpdated.invoke()
            }
        })
    }
}
