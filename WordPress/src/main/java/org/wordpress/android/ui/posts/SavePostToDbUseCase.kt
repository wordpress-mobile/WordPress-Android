package org.wordpress.android.ui.posts

import android.content.Context
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtilsWrapper
import org.wordpress.android.ui.uploads.UploadUtilsWrapper
import org.wordpress.android.util.DateTimeUtilsWrapper
import javax.inject.Inject

class SavePostToDbUseCase
@Inject constructor(
    private val uploadUtils: UploadUtilsWrapper,
    private val dateTimeUtils: DateTimeUtilsWrapper,
    private val dispatcher: Dispatcher,
    private val pendingDraftsNotificationsUtils: PendingDraftsNotificationsUtilsWrapper,
    private val context: Context
) {
    fun savePostToDb(
        postRepository: EditPostRepository,
        site: SiteModel
    ) {
        if (postRepository.postHasChanges()) {
            val post = checkNotNull(postRepository.getEditablePost())
            // mark as pending if the user doesn't have publishing rights
            if (!uploadUtils.userCanPublish(site)) {
                when (postRepository.status) {
                    PostStatus.UNKNOWN,
                    PostStatus.PUBLISHED,
                    PostStatus.SCHEDULED,
                    PostStatus.PRIVATE ->
                        // TODO Add toast
                        post.setStatus(PostStatus.PENDING.toString())
                    PostStatus.DRAFT,
                    PostStatus.PENDING,
                    PostStatus.TRASHED -> {
                    }
                }
            }
            if (!post.isLocalDraft) {
                post.setIsLocallyChanged(true)
            }
            post.setDateLocallyChanged(dateTimeUtils.currentTimeInIso8601())
            handlePendingDraftNotifications(postRepository)
            postRepository.savePostSnapshot()
            dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post))
        }
    }

    private fun handlePendingDraftNotifications(
        editPostRepository: EditPostRepository
    ) {
        if (editPostRepository.status == PostStatus.DRAFT) {
            // now set the pending notification alarm to be triggered in the next day, week, and month
            pendingDraftsNotificationsUtils
                    .scheduleNextNotifications(
                            context,
                            editPostRepository.id,
                            editPostRepository.dateLocallyChanged
                    )
        } else {
            pendingDraftsNotificationsUtils.cancelPendingDraftAlarms(
                    context,
                    editPostRepository.id
            )
        }
    }
}
