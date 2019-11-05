package org.wordpress.android.ui.posts

import dagger.Reusable
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus
import javax.inject.Inject

/**
 * Injectable wrapper around PostUtils.
 *
 * PostUtils interface is consisted of static methods, which make the client code difficult to test/mock. Main purpose
 * of this wrapper is to make testing easier.
 *
 */
@Reusable
class PostUtilsWrapper @Inject constructor() {
    fun isPublishable(post: PostModel) = PostUtils.isPublishable(post)

    fun isPostInConflictWithRemote(post: PostModel) = PostUtils.isPostInConflictWithRemote(post)

    fun isPostCurrentlyBeingEdited(post: PostModel) = PostUtils.isPostCurrentlyBeingEdited(post)

    fun shouldPublishImmediately(postStatus: PostStatus, dateCreated: String) =
            PostUtils.shouldPublishImmediately(postStatus, dateCreated)

    fun postHasEdits(oldPost: PostModel?, newPost: PostModel?) =
            PostUtils.postHasEdits(oldPost, newPost)
}
