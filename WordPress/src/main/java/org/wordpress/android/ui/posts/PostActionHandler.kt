package org.wordpress.android.ui.posts

import android.content.Intent
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.ui.notifications.utils.PendingDraftsNotificationsUtils
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.CriticalPostActionTracker.CriticalPostAction.DELETING_POST
import org.wordpress.android.ui.posts.CriticalPostActionTracker.CriticalPostAction.MOVING_POST_TO_DRAFT
import org.wordpress.android.ui.posts.CriticalPostActionTracker.CriticalPostAction.RESTORING_POST
import org.wordpress.android.ui.posts.CriticalPostActionTracker.CriticalPostAction.TRASHING_POST
import org.wordpress.android.ui.posts.CriticalPostActionTracker.CriticalPostAction.TRASHING_POST_WITH_LOCAL_CHANGES
import org.wordpress.android.ui.posts.PostListAction.DismissPendingNotification
import org.wordpress.android.ui.posts.PostListAction.PreviewPost
import org.wordpress.android.ui.posts.PostListAction.RetryUpload
import org.wordpress.android.ui.posts.PostListAction.ViewPost
import org.wordpress.android.ui.posts.PostListAction.ViewStats
import org.wordpress.android.ui.posts.PostUploadAction.CancelPostAndMediaUpload
import org.wordpress.android.ui.posts.PostUploadAction.EditPostResult
import org.wordpress.android.ui.posts.PostUploadAction.PublishPost
import org.wordpress.android.ui.posts.RemotePreviewLogicHelper.RemotePreviewType
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.ui.uploads.UploadUtils
import org.wordpress.android.util.ToastUtils
import org.wordpress.android.util.ToastUtils.Duration
import org.wordpress.android.viewmodel.helpers.ToastMessageHolder
import org.wordpress.android.widgets.PostListButtonType
import org.wordpress.android.widgets.PostListButtonType.BUTTON_CANCEL_PENDING_AUTO_UPLOAD
import org.wordpress.android.widgets.PostListButtonType.BUTTON_DELETE
import org.wordpress.android.widgets.PostListButtonType.BUTTON_DELETE_PERMANENTLY
import org.wordpress.android.widgets.PostListButtonType.BUTTON_EDIT
import org.wordpress.android.widgets.PostListButtonType.BUTTON_MORE
import org.wordpress.android.widgets.PostListButtonType.BUTTON_MOVE_TO_DRAFT
import org.wordpress.android.widgets.PostListButtonType.BUTTON_PREVIEW
import org.wordpress.android.widgets.PostListButtonType.BUTTON_PUBLISH
import org.wordpress.android.widgets.PostListButtonType.BUTTON_RETRY
import org.wordpress.android.widgets.PostListButtonType.BUTTON_SHOW_MOVE_TRASHED_POST_TO_DRAFT_DIALOG
import org.wordpress.android.widgets.PostListButtonType.BUTTON_STATS
import org.wordpress.android.widgets.PostListButtonType.BUTTON_SUBMIT
import org.wordpress.android.widgets.PostListButtonType.BUTTON_SYNC
import org.wordpress.android.widgets.PostListButtonType.BUTTON_TRASH
import org.wordpress.android.widgets.PostListButtonType.BUTTON_VIEW

/**
 * This is a temporary class to make the PostListViewModel more manageable. Please feel free to refactor it any way
 * you see fit.
 */
class PostActionHandler(
    private val dispatcher: Dispatcher,
    private val site: SiteModel,
    private val postStore: PostStore,
    private val postListDialogHelper: PostListDialogHelper,
    private val doesPostHaveUnhandledConflict: (PostModel) -> Boolean,
    private val hasUnhandledAutoSave: (PostModel) -> Boolean,
    private val triggerPostListAction: (PostListAction) -> Unit,
    private val triggerPostUploadAction: (PostUploadAction) -> Unit,
    private val invalidateList: () -> Unit,
    private val checkNetworkConnection: () -> Boolean,
    private val showSnackbar: (SnackbarMessageHolder) -> Unit,
    private val showToast: (ToastMessageHolder) -> Unit,
    private val triggerPreviewStateUpdate: (PostListRemotePreviewState, PostInfoType) -> Unit
) {
    private val criticalPostActionTracker = CriticalPostActionTracker(onStateChanged = {
        invalidateList.invoke()
    })

    fun handlePostButton(buttonType: PostListButtonType, post: PostModel) {
        when (buttonType) {
            BUTTON_EDIT -> editPostButtonAction(site, post)
            BUTTON_RETRY -> triggerPostListAction.invoke(RetryUpload(post))
            BUTTON_MOVE_TO_DRAFT -> {
                moveTrashedPostToDraft(post)
            }
            BUTTON_PUBLISH -> {
                postListDialogHelper.showPublishConfirmationDialog(post)
            }
            BUTTON_SYNC -> {
                postListDialogHelper.showSyncScheduledPostConfirmationDialog(post)
            }
            BUTTON_SUBMIT -> publishPost(post.id)
            BUTTON_VIEW -> triggerPostListAction.invoke(ViewPost(site, post))
            BUTTON_PREVIEW -> triggerPostListAction.invoke(
                    PreviewPost(
                            site = site,
                            post = post,
                            triggerPreviewStateUpdate = triggerPreviewStateUpdate,
                            showToast = showToast,
                            messageMediaUploading = ToastMessageHolder(
                                    R.string.editor_toast_uploading_please_wait,
                                    ToastUtils.Duration.SHORT
                            )
                    )
            )
            BUTTON_STATS -> triggerPostListAction.invoke(ViewStats(site, post))
            BUTTON_TRASH -> {
                if (post.isLocallyChanged) {
                    postListDialogHelper.showTrashPostWithLocalChangesConfirmationDialog(post)
                } else {
                    trashPost(post)
                }
            }
            BUTTON_DELETE, BUTTON_DELETE_PERMANENTLY -> {
                postListDialogHelper.showDeletePostConfirmationDialog(post)
            }
            BUTTON_CANCEL_PENDING_AUTO_UPLOAD -> {
                cancelPendingAutoUpload(post)
            }
            BUTTON_SHOW_MOVE_TRASHED_POST_TO_DRAFT_DIALOG -> {
                postListDialogHelper.showMoveTrashedPostToDraftDialog(post)
            }
            BUTTON_MORE -> {
            } // do nothing - ui will show a popup window
        }
    }

    private fun cancelPendingAutoUpload(post: PostModel) {
        val msgRes = UploadUtils.cancelPendingAutoUpload(post, dispatcher)
        showSnackbar.invoke(SnackbarMessageHolder(msgRes))
    }

    fun newPost() {
        triggerPostListAction(PostListAction.NewPost(site))
    }

    fun handleEditPostResult(data: Intent?) {
        val localPostId = data?.getIntExtra(EditPostActivity.EXTRA_POST_LOCAL_ID, 0)
        if (localPostId == null || localPostId == 0) {
            return
        }
        val post = postStore.getPostByLocalPostId(localPostId)
        if (post != null) {
            triggerPostUploadAction(EditPostResult(site, post, data) { publishPost(localPostId) })
        }
    }

    fun handleRemotePreview(localPostId: Int, remotePreviewType: RemotePreviewType) {
        val post = postStore.getPostByLocalPostId(localPostId)
        if (post != null) {
            triggerPostListAction.invoke(PostListAction.RemotePreviewPost(site, post, remotePreviewType))
        }
    }

    fun publishPost(localPostId: Int) {
        val post = postStore.getPostByLocalPostId(localPostId)
        if (post != null) {
            triggerPostUploadAction.invoke(PublishPost(dispatcher, site, post))
        }
    }

    fun moveTrashedPostToDraft(localPostId: Int) {
        val post = postStore.getPostByLocalPostId(localPostId)
        if (post != null) {
            moveTrashedPostToDraft(post)
        }
    }

    private fun moveTrashedPostToDraft(post: PostModel) {
        /*
         * We need network connection to move a post to remote draft. We can technically move it to the local drafts
         * but that'll leave the trashed post in the remote which can be confusing.
         */
        if (!checkNetworkConnection.invoke()) {
            return
        }
        post.setStatus(DRAFT.toString())
        dispatcher.dispatch(PostActionBuilder.newPushPostAction(RemotePostPayload(post, site)))

        val localPostId = LocalId(post.id)
        criticalPostActionTracker.add(localPostId, MOVING_POST_TO_DRAFT)

        val snackBarHolder = SnackbarMessageHolder(
                messageRes = R.string.post_moving_to_draft,
                onDismissAction = {
                    criticalPostActionTracker.remove(localPostId, MOVING_POST_TO_DRAFT)
                }
        )
        showSnackbar.invoke(snackBarHolder)
    }

    private fun editPostButtonAction(site: SiteModel, post: PostModel) {
        // first of all, check whether this post is in Conflicted state with a more recent remote version
        if (doesPostHaveUnhandledConflict.invoke(post)) {
            postListDialogHelper.showConflictedPostResolutionDialog(post)
            return
        }

        // Then check if an autosave revision is available
        if (hasUnhandledAutoSave.invoke(post)) {
            postListDialogHelper.showAutoSaveRevisionDialog(post)
            return
        }

        editPost(site, post)
    }

    private fun editPost(site: SiteModel, post: PostModel) {
        if (UploadService.isPostUploadingOrQueued(post)) {
            // If the post is uploading media, allow the media to continue uploading, but don't upload the
            // post itself when they finish (since we're about to edit it again)
            UploadService.cancelQueuedPostUpload(post)
        }
        triggerPostListAction.invoke(PostListAction.EditPost(site, post, loadAutoSaveRevision = false))
    }

    fun deletePost(localPostId: Int) {
        // If post doesn't exist, nothing else to do
        val post = postStore.getPostByLocalPostId(localPostId) ?: return
        criticalPostActionTracker.add(LocalId(post.id), DELETING_POST)

        when {
            post.isLocalDraft -> {
                val pushId = PendingDraftsNotificationsUtils.makePendingDraftNotificationId(post.id)
                triggerPostListAction(DismissPendingNotification(pushId))
                dispatcher.dispatch(PostActionBuilder.newRemovePostAction(post))
            }
            else -> {
                dispatcher.dispatch(PostActionBuilder.newDeletePostAction(RemotePostPayload(post, site)))
            }
        }
    }

    /**
     * This function handles a post being deleted and removed. Since deleting remote posts will trigger both delete
     * and remove actions we only want to remove the critical action when the post is actually successfully removed.
     *
     * It's possible to separate these into two methods that handles delete and remove. However, the fact that they
     * follow the same approach and the tricky nature of delete action makes combining the actions like so makes our
     * expectations clearer.
     */
    fun handlePostDeletedOrRemoved(localPostId: LocalId, isRemoved: Boolean, isError: Boolean) {
        if (criticalPostActionTracker.get(localPostId) != DELETING_POST) {
            /*
             * This is an unexpected action and either it has already been handled or another critical action has
             * been performed. In either case, safest action is to just ignore it.
             */
            return
        }
        if (isError) {
            showToast.invoke(ToastMessageHolder(R.string.error_deleting_post, Duration.SHORT))
        }
        if (isRemoved) {
            criticalPostActionTracker.remove(localPostId = localPostId, criticalPostAction = DELETING_POST)
        }
    }

    fun trashPostWithLocalChanges(localPostId: Int) {
        // If post doesn't exist, nothing else to do
        val post = postStore.getPostByLocalPostId(localPostId) ?: return
        trashPost(post, true)
    }

    private fun trashPost(post: PostModel, hasLocalChanges: Boolean = false) {
        // We need network connection to trash a post
        if (!checkNetworkConnection()) {
            return
        }
        val criticalPostAction = if (hasLocalChanges) {
            TRASHING_POST_WITH_LOCAL_CHANGES
        } else {
            TRASHING_POST
        }

        showSnackbar.invoke(SnackbarMessageHolder(R.string.post_trashing))
        criticalPostActionTracker.add(localPostId = LocalId(post.id), criticalPostAction = criticalPostAction)

        triggerPostUploadAction.invoke(CancelPostAndMediaUpload(post))
        dispatcher.dispatch(PostActionBuilder.newDeletePostAction(RemotePostPayload(post, site)))
    }

    fun handlePostTrashed(localPostId: LocalId, isError: Boolean) {
        val criticalAction = criticalPostActionTracker.get(localPostId)
        if (criticalAction != TRASHING_POST && criticalAction != TRASHING_POST_WITH_LOCAL_CHANGES) {
            /*
             * This is an unexpected action and either it has already been handled or another critical action has
             * been performed. In either case, safest action is to just ignore it.
             */
            return
        }
        criticalPostActionTracker.remove(localPostId = localPostId, criticalPostAction = criticalAction)
        if (isError) {
            showToast.invoke(ToastMessageHolder(R.string.error_deleting_post, Duration.SHORT))
        } else {
            val snackBarHolder = when (criticalAction) {
                TRASHING_POST -> SnackbarMessageHolder(
                        messageRes = R.string.post_trashed,
                        buttonTitleRes = R.string.undo,
                        buttonAction = {
                            val post = postStore.getPostByLocalPostId(localPostId.value)
                            if (post != null) {
                                restorePost(post)
                            }
                        }
                )
                TRASHING_POST_WITH_LOCAL_CHANGES -> SnackbarMessageHolder(messageRes = R.string.post_trashed)
                else -> throw IllegalStateException("Unexpected action in handlePostTrashed(): $criticalAction")
            }
            showSnackbar.invoke(snackBarHolder)
        }
    }

    private fun restorePost(post: PostModel) {
        // We need network connection to restore a post
        if (!checkNetworkConnection.invoke()) {
            return
        }
        showSnackbar.invoke(SnackbarMessageHolder(messageRes = R.string.post_restoring))
        criticalPostActionTracker.add(localPostId = LocalId(post.id), criticalPostAction = RESTORING_POST)
        dispatcher.dispatch(PostActionBuilder.newRestorePostAction(RemotePostPayload(post, site)))
    }

    fun handlePostRestored(localPostId: LocalId, isError: Boolean) {
        if (criticalPostActionTracker.get(localPostId) != RESTORING_POST) {
            /*
             * This is an unexpected action and either it has already been handled or another critical action has
             * been performed. In either case, safest action is to just ignore it.
             */
            return
        }
        criticalPostActionTracker.remove(localPostId = localPostId, criticalPostAction = RESTORING_POST)
        if (isError) {
            showToast.invoke(ToastMessageHolder(R.string.error_restoring_post, Duration.SHORT))
        } else {
            showSnackbar.invoke(SnackbarMessageHolder(messageRes = R.string.post_restored))
        }
    }

    fun isPerformingCriticalAction(localPostId: LocalId): Boolean {
        return criticalPostActionTracker.contains(localPostId)
    }
}
