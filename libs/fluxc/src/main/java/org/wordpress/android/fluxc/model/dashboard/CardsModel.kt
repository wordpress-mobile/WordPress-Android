package org.wordpress.android.fluxc.model.dashboard

import java.util.Date

data class CardsModel(
    val posts: PostsModel
) {
    data class PostsModel(
        val hasPublished: Boolean,
        val draft: List<PostModel>,
        val scheduled: List<PostModel>
    ) {
        data class PostModel(
            val id: Int,
            val title: String?,
            val content: String?,
            val date: Date,
            val featuredImage: String?
        )
    }
}
