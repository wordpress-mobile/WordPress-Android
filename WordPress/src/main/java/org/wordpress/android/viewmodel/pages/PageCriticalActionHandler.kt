package org.wordpress.android.viewmodel.pages

import kotlinx.coroutines.Job
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.posts.CriticalPostActionTracker
import org.wordpress.android.ui.posts.CriticalPostActionTracker.CriticalPostAction.DELETING_POST
import org.wordpress.android.ui.posts.CriticalPostActionTracker.CriticalPostAction.MOVING_POST_TO_DRAFT
import org.wordpress.android.ui.posts.CriticalPostActionTracker.CriticalPostAction.RESTORING_POST
import org.wordpress.android.ui.posts.CriticalPostActionTracker.CriticalPostAction.TRASHING_POST
import org.wordpress.android.ui.posts.CriticalPostActionTracker.CriticalPostAction.TRASHING_POST_WITH_LOCAL_CHANGES

class PageCriticalActionHandler(
    private val postStore: PostStore,
    private val invalidateList: () -> Job
) {
    private val criticalPostActionTracker = CriticalPostActionTracker(onStateChanged = {
        invalidateList.invoke()
    })

    private fun moveTrashedPostToDraft(post: PostModel) {
        val localPostId = LocalId(post.id)
        criticalPostActionTracker.add(localPostId, MOVING_POST_TO_DRAFT)
    }

    private fun undoMoveTrashedPostToDraft(post: PostModel) {
        val localPostId = LocalId(post.id)

        criticalPostActionTracker.remove(localPostId, MOVING_POST_TO_DRAFT)
    }

    fun deletePost(localPostId: Int) {
        // If post doesn't exist, nothing else to do
        val post = postStore.getPostByLocalPostId(localPostId) ?: return
        criticalPostActionTracker.add(LocalId(post.id), DELETING_POST)
    }

    /**
     * This function handles a post being deleted and removed. Since deleting remote posts will trigger both delete
     * and remove actions we only want to remove the critical action when the post is actually successfully removed.
     *
     * It's possible to separate these into two methods that handles delete and remove. However, the fact that they
     * follow the same approach and the tricky nature of delete action makes combining the actions like so makes our
     * expectations clearer.
     */
    fun handlePostDeletedOrRemoved(localPostId: LocalId) {
        if (criticalPostActionTracker.get(localPostId) != DELETING_POST) {
            /*
             * This is an unexpected action and either it has already been handled or another critical action has
             * been performed. In either case, safest action is to just ignore it.
             */
            return
        }

        criticalPostActionTracker.remove(
                localPostId = localPostId,
                criticalPostAction = DELETING_POST
        )
    }

    private fun trashPost(post: PostModel, hasLocalChanges: Boolean = false) {
        val criticalPostAction = if (hasLocalChanges) {
            TRASHING_POST_WITH_LOCAL_CHANGES
        } else {
            TRASHING_POST
        }

        criticalPostActionTracker.add(
                localPostId = LocalId(post.id),
                criticalPostAction = criticalPostAction
        )
    }

    fun handlePostTrashedOrNot(localPostId: LocalId) {
        val criticalAction = criticalPostActionTracker.get(localPostId)
        if (criticalAction != TRASHING_POST && criticalAction != TRASHING_POST_WITH_LOCAL_CHANGES) {
            /*
             * This is an unexpected action and either it has already been handled or another critical action has
             * been performed. In either case, safest action is to just ignore it.
             */
            return
        }
        criticalPostActionTracker.remove(
                localPostId = localPostId,
                criticalPostAction = criticalAction
        )
    }

    private fun restorePost(post: PostModel) {
        criticalPostActionTracker.add(
                localPostId = LocalId(post.id),
                criticalPostAction = RESTORING_POST
        )
    }

    fun handlePostRestoredOrNot(localPostId: LocalId) {
        if (criticalPostActionTracker.get(localPostId) != RESTORING_POST) {
            /*
             * This is an unexpected action and either it has already been handled or another critical action has
             * been performed. In either case, safest action is to just ignore it.
             */
            return
        }
        criticalPostActionTracker.remove(
                localPostId = localPostId,
                criticalPostAction = RESTORING_POST
        )
    }

    fun isPerformingCriticalAction(localPostId: LocalId): Boolean {
        return criticalPostActionTracker.contains(localPostId)
    }
}

