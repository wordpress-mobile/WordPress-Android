package org.wordpress.android.fluxc.model.stats

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.FollowersModel.FollowerModel
import org.wordpress.android.fluxc.model.stats.TagsModel.TagModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.CommentsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowerType
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowerType.EMAIL
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowerType.WP_COM
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.MostPopularResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostStatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.TagsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.TagsResponse.TagsGroup.TagResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.VisitResponse
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.STATS
import java.util.Date
import javax.inject.Inject

private const val PERIOD = "period"
private const val VIEWS = "views"
private const val VISITORS = "visitors"
private const val LIKES = "likes"
private const val REBLOGS = "reblogs"
private const val COMMENTS = "comments"
private const val POSTS = "posts"

class InsightsMapper
@Inject constructor() {
    fun map(response: AllTimeResponse, site: SiteModel): InsightsAllTimeModel {
        val stats = response.stats
        return InsightsAllTimeModel(
                site.siteId,
                response.date,
                stats?.visitors ?: 0,
                stats?.views ?: 0,
                stats?.posts ?: 0,
                stats?.viewsBestDay ?: "",
                stats?.viewsBestDayTotal ?: 0
        )
    }

    fun map(response: MostPopularResponse, site: SiteModel): InsightsMostPopularModel {
        return InsightsMostPopularModel(
                site.siteId,
                response.highestDayOfWeek ?: 0,
                response.highestHour ?: 0,
                response.highestDayPercent ?: 0.0,
                response.highestHourPercent ?: 0.0
        )
    }

    fun map(
        postResponse: PostResponse,
        postStatsResponse: PostStatsResponse,
        site: SiteModel
    ): InsightsLatestPostModel {
        val daysViews = if (postStatsResponse.fields != null &&
                postStatsResponse.data != null &&
                postStatsResponse.fields.size > 1 &&
                postStatsResponse.fields[0] == "period" &&
                postStatsResponse.fields[1] == "views") {
            postStatsResponse.data.map { list -> list[0] to list[1].toInt() }
        } else {
            listOf()
        }
        val viewsCount = postStatsResponse.views
        val commentCount = postResponse.discussion?.commentCount ?: 0
        return InsightsLatestPostModel(
                site.siteId,
                postResponse.title ?: "",
                postResponse.url ?: "",
                postResponse.date ?: Date(0),
                postResponse.id ?: 0,
                viewsCount ?: 0,
                commentCount,
                postResponse.likeCount ?: 0,
                daysViews
        )
    }

    fun map(response: VisitResponse): VisitsModel {
        val result: Map<String, String> = response.fields.mapIndexed { index, value ->
            value to response.data[0][index]
        }.toMap()
        return VisitsModel(
                result[PERIOD] ?: "",
                result[VIEWS]?.toInt() ?: 0,
                result[VISITORS]?.toInt() ?: 0,
                result[LIKES]?.toInt() ?: 0,
                result[REBLOGS]?.toInt() ?: 0,
                result[COMMENTS]?.toInt() ?: 0,
                result[POSTS]?.toInt() ?: 0
        )
    }

    fun map(response: FollowersResponse, followerType: FollowerType, pageSize: Int): FollowersModel {
        val followers = response.subscribers.mapNotNull {
            if (it.avatar != null && it.label != null && it.dateSubscribed != null) {
                FollowerModel(
                        it.avatar,
                        it.label,
                        it.url,
                        it.dateSubscribed
                )
            } else {
                AppLog.e(STATS, "CommentsResponse.posts: Non-null field is coming as null from API")
                null
            }
        }.take(pageSize)
        val total = when (followerType) {
            WP_COM -> response.totalWpCom
            EMAIL -> response.totalEmail
        }
        return FollowersModel(total ?: 0, followers, response.subscribers.size > pageSize)
    }

    fun map(response: CommentsResponse, pageSize: Int): CommentsModel {
        val authors = response.authors?.take(pageSize)?.mapNotNull {
            if (it.name != null && it.comments != null && it.link != null && it.gravatar != null) {
                CommentsModel.Author(it.name, it.comments, it.link, it.gravatar)
            } else {
                AppLog.e(STATS, "CommentsResponse.authors: Non-null field is coming as null from API")
                null
            }
        }
        val posts = response.posts?.take(pageSize)?.mapNotNull {
            if (it.id != null && it.name != null && it.comments != null && it.link != null) {
                CommentsModel.Post(it.id, it.name, it.comments, it.link)
            } else {
                AppLog.e(STATS, "CommentsResponse.posts: Non-null field is coming as null from API")
                null
            }
        }
        val hasMoreAuthors = (response.authors != null && response.authors.size > pageSize)
        val hasMorePosts = (response.posts != null && response.posts.size > pageSize)
        return CommentsModel(posts ?: listOf(), authors ?: listOf(), hasMorePosts, hasMoreAuthors)
    }

    fun map(response: TagsResponse, pageSize: Int): TagsModel {
        return TagsModel(response.tags.take(pageSize).map { tag ->
            TagModel(tag.tags.mapNotNull { it.toItem() }.take(pageSize), tag.views ?: 0)
        }, response.tags.size > pageSize)
    }

    private fun TagResponse.toItem(): TagModel.Item? {
        return if (this.name != null && this.type != null && this.link != null) {
            TagModel.Item(this.name, this.type, this.link)
        } else {
            AppLog.e(STATS, "TagResponse: Mandatory fields are null so the Tag can't be mapped to Model")
            null
        }
    }
}
