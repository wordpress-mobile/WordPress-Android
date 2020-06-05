package org.wordpress.android.ui.posts.prepublishing.visibility.usecases

import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult
import org.wordpress.android.util.DateTimeUtilsWrapper
import javax.inject.Inject

class UpdatePostStatusUseCase @Inject constructor(private val dateTimeUtilsWrapper: DateTimeUtilsWrapper) {
    fun updatePostStatus(
        postStatus: PostStatus,
        editPostRepository: EditPostRepository,
        onPostStatusUpdated: (PostImmutableModel) -> Unit
    ) {
        editPostRepository.updateAsync({ postModel: PostModel ->
            // we set the date to immediately if it's scheduled.
            if (postStatus == PostStatus.PRIVATE) {
                if (postModel.status == PostStatus.SCHEDULED.toString())
                    postModel.setDateCreated(dateTimeUtilsWrapper.currentTimeInIso8601())
            }

            postModel.setStatus(postStatus.toString())

            true
        }, { postModel, result ->
            if (result == UpdatePostResult.Updated) {
                onPostStatusUpdated.invoke(postModel)
            }
        })
    }
}
