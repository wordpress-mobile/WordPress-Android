package org.wordpress.android.fluxc.model.stats

data class CommentsModel(
    var posts: List<Post>,
    var authors: List<Author>
) {
    data class Post(val id: Long, val name: String, val comments: Int, val link: String)
    data class Author(val name: String, val comments: Int, val link: String, val gravatar: String)
}
