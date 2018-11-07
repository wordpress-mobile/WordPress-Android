package org.wordpress.android.fluxc.model.stats

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.FollowersModel.FollowerModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.CommentsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowerType
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowerType.EMAIL
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowerType.WP_COM
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.FollowersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.MostPopularResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostStatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.PostsResponse.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.InsightsRestClient.VisitResponse
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
                stats.visitors,
                stats.views,
                stats.posts,
                stats.viewsBestDay,
                stats.viewsBestDayTotal
        )
    }

    fun map(response: MostPopularResponse, site: SiteModel): InsightsMostPopularModel {
        return InsightsMostPopularModel(
                site.siteId,
                response.highestDayOfWeek,
                response.highestHour,
                response.highestDayPercent,
                response.highestHourPercent
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
                postResponse.title,
                postResponse.url,
                postResponse.date,
                postResponse.id,
                viewsCount,
                commentCount,
                postResponse.likeCount,
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

    fun map(response: FollowersResponse, followerType: FollowerType): FollowersModel {
        val followers = response.subscribers.map {
            FollowerModel(
                    it.avatar,
                    it.label,
                    it.url,
                    it.dateSubscribed
            )
        }
        val total = when (followerType) {
            WP_COM -> response.totalWpCom
            EMAIL -> response.totalEmail
        }
        return FollowersModel(total, followers)
    }

    fun map(response: CommentsResponse): CommentsModel {
        val authors = response.authors.map { CommentsModel.Author(it.name,it.comments, it.link, it.gravatar) }
        val posts = response.posts.map { CommentsModel.Post(it.id, it.name, it.comments, it.link) }
        return CommentsModel(posts, authors)
    }
}
