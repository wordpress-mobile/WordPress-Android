package org.wordpress.android.ui.posts

import dagger.Reusable
import org.wordpress.android.fluxc.model.PostModel
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
}
