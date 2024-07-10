package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.PostImmutableModel

class PostFreshnessCheckerImpl(
    private val timeProvider: TimeProvider = SystemTimeProvider()
) : IPostFreshnessChecker {
    override fun shouldRefreshPost(post: PostImmutableModel): Boolean {
        return postNeedsRefresh(post)
    }

    private fun postNeedsRefresh(post: PostImmutableModel) : Boolean {
        return timeProvider.currentTimeMillis() - post.dbTimestamp > CACHE_VALIDITY_MILLIS
    }

    companion object {
        // Todo turn this into a remote config value
        const val CACHE_VALIDITY_MILLIS = 20000
    }
}

class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
}

