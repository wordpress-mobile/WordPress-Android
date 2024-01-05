package org.wordpress.android.fluxc.model.dashboard

import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.store.dashboard.CardsStore.ActivityCardError
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
        POSTS(PostsCardModel::class.java, "posts"),
        PAGES(PagesCardModel::class.java, "pages"),
        ACTIVITY(ActivityCardModel::class.java, "activity"),
        DYNAMIC(DynamicCardsModel::class.java, "dynamic"),
    }

    data class ActivityCardModel(
        val activities: List<ActivityLogModel> = emptyList(),
        val error: ActivityCardError? = null
    )  : CardModel(Type.ACTIVITY)

    data class PagesCardModel(
        val pages: List<PageCardModel> = emptyList(),
    ) : CardModel(Type.PAGES) {
        data class PageCardModel(
            val id: Int,
            val title: String,
            val content: String,
            val lastModifiedOrScheduledOn: Date,
            val status: String,
            val date: Date
        )
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

    data class DynamicCardsModel(
        val dynamicCards: List<DynamicCardModel> = emptyList(),
    ) : CardModel(Type.DYNAMIC) {
        data class DynamicCardModel(
            val id: String,
            val title: String?,
            val featuredImage: String?,
            val url: String?,
            val action: String?,
            val order: CardOrder,
            val rows: List<DynamicCardRowModel>,
        )

        data class DynamicCardRowModel(
            val icon: String?,
            val title: String?,
            val description: String?,
        )

        enum class CardOrder(val order: String) {
            TOP("top"),
            BOTTOM("bottom");

            companion object {
                fun fromString(order: String?): CardOrder {
                    return values().firstOrNull { it.order.equals(order, ignoreCase = true) } ?: BOTTOM // default
                }
            }
        }
    }
}
