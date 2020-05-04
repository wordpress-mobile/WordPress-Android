package org.wordpress.android.ui.posts.prepublishing.home.usecases

import android.text.TextUtils
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.PostUtils
import javax.inject.Inject

typealias StringResourceId = Int

class GetPublishButtonLabelUseCase @Inject constructor() {
    fun getLabel(editPostRepository: EditPostRepository): StringResourceId {
        val postModel = editPostRepository.getPost()
        requireNotNull(postModel) { "PostModel can't be null. " }

        val dateCreated = postModel.dateCreated
        val status = PostStatus.fromPost(postModel)

        return when {
            !TextUtils.isEmpty(dateCreated) -> {
                when {
                    status == PostStatus.SCHEDULED -> R.string.prepublishing_nudges_home_schedule_button
                    status == PostStatus.PUBLISHED || status == PostStatus.PRIVATE -> R.string.prepublishing_nudges_home_publish_button
                    postModel.isLocalDraft -> R.string.prepublishing_nudges_home_publish_button
                    PostUtils.isPublishDateInTheFuture(postModel.dateCreated) -> R.string.prepublishing_nudges_home_schedule_button
                    else -> R.string.prepublishing_nudges_home_publish_button
                }
            }
            else -> R.string.prepublishing_nudges_home_publish_button
        }
    }
}

