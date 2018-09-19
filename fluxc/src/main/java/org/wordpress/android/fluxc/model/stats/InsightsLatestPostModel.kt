package org.wordpress.android.fluxc.model.stats

data class InsightsLatestPostModel(
    val siteId: Long,
    val postTitle: String,
    val postURL: String,
    val postDate: String,
    val postID: Long,
    val postViewsCount : Int = 0,
    val postCommentCount: Int = 0,
    val postLikeCount: Int
)


