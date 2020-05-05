package org.wordpress.android.ui.posts.prepublishing.home.usecases

import android.text.TextUtils
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.posts.EditPostRepository
import org.wordpress.android.ui.posts.PostUtilsWrapper
import javax.inject.Inject

typealias StringResourceId = Int

class GetPublishButtonLabelUseCase @Inject constructor(private val postUtilsWrapper: PostUtilsWrapper) {
    fun getLabel(editPostRepository: EditPostRepository): StringResourceId {
        val status = editPostRepository.status
        return when {
            !TextUtils.isEmpty(editPostRepository.dateCreated) -> {
                when {
                    status == PostStatus.SCHEDULED -> R.string.prepublishing_nudges_home_schedule_button
                    status == PostStatus.PUBLISHED || status == PostStatus.PRIVATE ->
                        R.string.prepublishing_nudges_home_publish_button
                    editPostRepository.isLocalDraft -> R.string.prepublishing_nudges_home_publish_button
                    postUtilsWrapper.isPublishDateInTheFuture(editPostRepository.dateCreated) ->
                        R.string.prepublishing_nudges_home_schedule_button
                    else -> R.string.prepublishing_nudges_home_publish_button
                }
            }
            else -> R.string.prepublishing_nudges_home_publish_button
        }
    }
}
