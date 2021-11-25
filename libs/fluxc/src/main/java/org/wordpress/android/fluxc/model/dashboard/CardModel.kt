package org.wordpress.android.fluxc.model.dashboard

import java.util.Date

sealed class CardModel(
    val type: Type
) {
    enum class Type(
        val classOf: Class<*>
    ) {
        POSTS(PostsCardModel::class.java)
    }

    data class PostsCardModel(
        val hasPublished: Boolean,
        val draft: List<PostCardModel>,
        val scheduled: List<PostCardModel>
    ) : CardModel(Type.POSTS) {
        data class PostCardModel(
            val id: Int,
            val title: String?,
            val content: String?,
            val featuredImage: String?,
            val date: Date
        )
    }
}
