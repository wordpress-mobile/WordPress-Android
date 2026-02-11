package org.wordpress.android.ui.posts_rs.models

/**
 * Domain model for a post fetched via wordpress-rs.
 * Maps from uniffi's AnyPostWithEditContext.
 */
data class PostRsModel(
    val remotePostId: Long,
    val title: String,
    val excerpt: String,
    val date: String,
    val status: String
)
