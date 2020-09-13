package org.wordpress.android.models.discover

data class RecommendedBlog(
    val name: String,
    val url: String,
    val blogId: Long,
    val description: String,
    val iconUrl: String?,
    val feedId: Long?,
    val isFollowed: Boolean
)
