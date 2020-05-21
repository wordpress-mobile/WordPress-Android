package org.wordpress.android.ui.posts.prepublishing.visibility.usecases

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult
import org.wordpress.android.util.DateTimeUtilsWrapper
import javax.inject.Inject

class PublishPostImmediatelyUseCase @Inject constructor(private val dateTimeUtilsWrapper: DateTimeUtilsWrapper) {
    fun updatePostToPublishImmediately(
        editPostRepository: EditPostRepository,
        onPostUpdated: () -> Unit
    ) {
        editPostRepository.updateAsync({ postModel: PostModel ->
            postModel.setDateCreated(dateTimeUtilsWrapper.currentTimeInIso8601());
            postModel.setStatus(PostStatus.PUBLISHED.toString())
            true
        }, { _, result ->
            if (result == UpdatePostResult.Updated) {
                onPostUpdated.invoke()
            }
        })
    }
}
