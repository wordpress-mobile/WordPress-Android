package org.wordpress.android.fluxc.model.stats.time

data class VideoPlaysModel(
    val otherPlays: Int,
    val totalPlays: Int,
    val plays: List<VideoPlays>,
    val hasMore: Boolean
) {
    data class VideoPlays(
        val postId: String,
        val title: String,
        val url: String?,
        val plays: Int
    )
}
