package org.wordpress.android.fluxc.model.dashboard

import org.wordpress.android.fluxc.store.dashboard.CardsStore.PostCardError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.TodaysStatsCardError
import java.util.Date

sealed class CardModel(
    val type: Type
) {
    enum class Type(
        val classOf: Class<*>,
        val label: String
    ) {
        TODAYS_STATS(TodaysStatsCardModel::class.java, "todays_stats"),
        POSTS(PostsCardModel::class.java, "posts")
    }

    data class TodaysStatsCardModel(
        val views: Int = 0,
        val visitors: Int = 0,
        val likes: Int = 0,
        val comments: Int = 0,
        val error: TodaysStatsCardError? = null
    ) : CardModel(Type.TODAYS_STATS)

    data class PostsCardModel(
        val hasPublished: Boolean = false,
        val draft: List<PostCardModel> = emptyList(),
        val scheduled: List<PostCardModel> = emptyList(),
        val error: PostCardError? = null
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
