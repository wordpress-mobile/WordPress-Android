package org.wordpress.android.ui.newstats.latestpost

sealed class LatestPostCardUiState {
    data object Loading : LatestPostCardUiState()

    /** The site has no published posts yet. */
    data object NoData : LatestPostCardUiState()

    data class Loaded(
        val postId: Long,
        val postTitle: String,
        val postDate: String,
        val views: Long,
        val likes: Long,
        val comments: Long,
        /** Daily views for the trailing week, oldest first. */
        val recentViews: List<Long>
    ) : LatestPostCardUiState()

    /**
     * The card shows a fixed error message and a retry action, so the underlying failure isn't
     * carried here -- it's already logged at the point it happens.
     */
    data object Error : LatestPostCardUiState()
}
