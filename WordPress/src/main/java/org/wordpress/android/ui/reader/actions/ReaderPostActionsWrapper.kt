package org.wordpress.android.ui.reader.actions

import dagger.Reusable
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.models.ReaderPost
import javax.inject.Inject

@Reusable
class ReaderPostActionsWrapper @Inject constructor(private val siteStore: SiteStore) {
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
        requestListener: ReaderActions.OnRequestListener<String>?
    ) = ReaderPostActions.requestBlogPost(blogId, postId, requestListener)
}
