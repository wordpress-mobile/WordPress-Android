package org.wordpress.android.fluxc.model.stats.subscribers

data class PostsModel(val posts: List<PostModel>) {
    data class PostModel(val id: Long, val href: String, val title: String, val opens: Int, val clicks: Int)
}
