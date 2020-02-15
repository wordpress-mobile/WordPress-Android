package org.wordpress.android.viewmodel.pages

import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.ui.posts.PostUtils
import javax.inject.Inject

class PageConflictResolver @Inject constructor() {
    private var postIdForFetchingRemoteVersionOfConflictedPost: LocalId? = null

    fun hasUnhandledAutoSave(post: PostModel): Boolean {
        return PostUtils.hasAutoSave(post)
    }

    fun doesPostHaveUnhandledConflict(post: PostModel): Boolean {
        // If we are fetching the remote version of a conflicted post, it means it's already being handled
        val isFetchingConflictedPost = postIdForFetchingRemoteVersionOfConflictedPost != null &&
                postIdForFetchingRemoteVersionOfConflictedPost == LocalId(post.id)
        return !isFetchingConflictedPost && PostUtils.isPostInConflictWithRemote(post)
    }
}
