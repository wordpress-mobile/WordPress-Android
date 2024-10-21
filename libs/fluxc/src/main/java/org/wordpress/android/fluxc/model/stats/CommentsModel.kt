package org.wordpress.android.fluxc.model.stats

data class CommentsModel(
    val posts: List<Post>,
    val authors: List<Author>,
    val hasMorePosts: Boolean,
    val hasMoreAuthors: Boolean
) {
    data class Post(val id: Long, val name: String, val comments: Int, val link: String)
    data class Author(val name: String, val comments: Int, val link: String, val gravatar: String)
}
