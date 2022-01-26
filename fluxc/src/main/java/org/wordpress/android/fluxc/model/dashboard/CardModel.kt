package org.wordpress.android.fluxc.model.dashboard

import java.util.Date

sealed class CardModel(
    val type: Type
) {
    enum class Type(
        val classOf: Class<*>
    ) {
        TODAYS_STATS(TodaysStatsCardModel::class.java),
        POSTS(PostsCardModel::class.java)
    }

    data class TodaysStatsCardModel(
        val views: Int,
        val visitors: Int,
        val likes: Int,
        val comments: Int
    ) : CardModel(Type.TODAYS_STATS)

    data class PostsCardModel(
        val hasPublished: Boolean,
        val draft: List<PostCardModel>,
        val scheduled: List<PostCardModel>
    ) : CardModel(Type.POSTS) {
        data class PostCardModel(
            val id: Int,
            val title: String,
            val content: String,
            val featuredImage: String?,
            val date: Date
        )
    }
}
