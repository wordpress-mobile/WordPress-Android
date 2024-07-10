package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.PostImmutableModel

/**
 * This interface is implemented by a component that determines if a post
 * is "fresh" or we need to refetch it from the backend.
 */
interface IPostFreshnessChecker {
    fun shouldRefreshPost(post: PostImmutableModel): Boolean
}

interface TimeProvider {
    fun currentTimeMillis(): Long
}

