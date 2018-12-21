package org.wordpress.android.fluxc.model.stats.time

data class AuthorsModel(val otherViews: Int, val authors: List<Author>, val hasMore: Boolean) {
    data class Author(
        val name: String,
        val views: Int,
        val avatarUrl: String?,
        val posts: List<Post>
    )
    data class Post(val id: String, val title: String, val views: Int, val url: String?)
}
