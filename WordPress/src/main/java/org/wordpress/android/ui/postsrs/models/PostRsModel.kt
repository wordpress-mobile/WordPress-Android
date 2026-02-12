package org.wordpress.android.ui.postsrs.models

import uniffi.wp_api.PostStatus

/**
 * Domain model for a post fetched via wordpress-rs.
 * Maps from uniffi's AnyPostWithViewContext.
 */
data class PostRsModel(
    val remotePostId: Long,
    val authorId: Long,
    val title: String,
    val excerpt: String,
    val date: String,
    val status: PostStatus
)
