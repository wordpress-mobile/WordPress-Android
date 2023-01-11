package org.wordpress.android.ui.reader.actions

import dagger.Reusable
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import javax.inject.Inject

@Reusable
class ReaderPostActionsWrapper @Inject constructor(private val siteStore: SiteStore) {
    fun addToBookmarked(post: ReaderPost) = ReaderPostActions.addToBookmarked(post)
    fun removeFromBookmarked(post: ReaderPost) = ReaderPostActions.removeFromBookmarked(post)
    fun performLikeActionLocal(
        post: ReaderPost?,
        isAskingToLike: Boolean,
        wpComUserId: Long
    ): Boolean = ReaderPostActions.performLikeActionLocal(post, isAskingToLike, wpComUserId)

    fun performLikeActionRemote(
        post: ReaderPost?,
        isAskingToLike: Boolean,
        wpComUserId: Long,
        actionListener: ActionListener
    ) = ReaderPostActions.performLikeActionRemote(post, isAskingToLike, wpComUserId, actionListener)

    fun bumpPageViewForPost(post: ReaderPost) = ReaderPostActions.bumpPageViewForPost(siteStore, post)

    fun requestRelatedPosts(sourcePost: ReaderPost) = ReaderPostActions.requestRelatedPosts(sourcePost)

    fun requestFeedPost(
        feedId: Long,
        postId: Long,
        requestListener: ReaderActions.OnRequestListener<String>
    ) = ReaderPostActions.requestFeedPost(feedId, postId, requestListener)

    fun requestBlogPost(
        blogId: Long,
        postId: Long,
        requestListener: ReaderActions.OnRequestListener<String>
    ) = ReaderPostActions.requestBlogPost(blogId, postId, requestListener)
}
