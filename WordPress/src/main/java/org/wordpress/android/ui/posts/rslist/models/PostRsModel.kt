package org.wordpress.android.ui.posts.rslist.models

/**
 * Domain model for a post fetched via wordpress-rs.
 * Maps from uniffi's AnyPostWithEditContext.
 */
data class PostRsModel(
    val remotePostId: Long,
    val title: String,
    val excerpt: String,
    val date: String,
    val modifiedDate: String,
    val status: String,
    val authorId: Long,
    val featuredMediaId: Long,
    val link: String
)
