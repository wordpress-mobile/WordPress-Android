@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.comments

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.datasets.wrappers.NotificationsTableWrapper
import org.wordpress.android.fluxc.generated.CommentActionBuilder
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentStore.RemoteLikeCommentPayload
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.models.Note
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.comments.unified.CommentsStoreAdapter
import org.wordpress.android.ui.notifications.NotificationEvents.OnNoteCommentLikeChanged
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named


@HiltViewModel
class CommentDetailViewModel @Inject constructor(
    @Named(BG_THREAD) bgDispatcher: CoroutineDispatcher,
    private val commentStore: CommentsStore,
    private val commentsStoreAdapter: CommentsStoreAdapter,
    private val eventBusWrapper: EventBusWrapper,
    private val notificationsTableWrapper: NotificationsTableWrapper,
) : ScopedViewModel(bgDispatcher) {
    private val _updatedComment = MutableLiveData<CommentModel>()
    val updatedComment: LiveData<CommentModel> = _updatedComment

    /**
     * Like or unlike a comment
     * @param comment the comment to like or unlike
     * @param site the site the comment belongs to
     * @param note the note the comment belongs to, non-null if the comment is from a notification
     */
    fun likeComment(comment: CommentModel, site: SiteModel, note: Note? = null) = launch {
        val liked = comment.iLike.not()
        comment.apply { iLike = liked }
            .let { _updatedComment.postValue(it) }

        commentsStoreAdapter.dispatch(
            CommentActionBuilder.newLikeCommentAction(
                RemoteLikeCommentPayload(site, comment, liked)
            )
        )

        note?.let {
            eventBusWrapper.postSticky(OnNoteCommentLikeChanged(note, liked))
        }
    }
}
