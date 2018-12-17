package org.wordpress.android.fluxc.model.stats.time

import com.google.gson.Gson
import org.wordpress.android.fluxc.model.stats.time.ClicksModel.Click
import org.wordpress.android.fluxc.model.stats.time.AuthorsModel.Post
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel.Referrer
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ClicksRestClient.ClicksResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.AuthorsRestClient.AuthorsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.VisitAndViewsRestClient.VisitsAndViewsResponse
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.STATS
import javax.inject.Inject

class TimeStatsMapper
@Inject constructor(val gson: Gson) {
    fun map(response: PostAndPageViewsResponse, pageSize: Int): PostAndPageViewsModel {
        val postViews = response.days.entries.first().value.postViews
        val stats = postViews.take(pageSize).mapNotNull { item ->
            val type = when (item.type) {
                "post" -> ViewsType.POST
                "page" -> ViewsType.PAGE
                "homepage" -> ViewsType.HOMEPAGE
                else -> {
                    AppLog.e(STATS, "PostAndPageViewsResponse.type: Unexpected view type: ${item.type}")
                    null
                }
            }
            type?.let {
                if (item.id == null || item.title == null || item.href == null) {
                    AppLog.e(STATS, "PostAndPageViewsResponse.type: Non-nullable fields are null - $item")
                }
                ViewsModel(item.id ?: 0, item.title ?: "", item.views ?: 0, type, item.href ?: "")
            }
        }
        return PostAndPageViewsModel(stats, postViews.size > pageSize)
    }

    fun map(response: ReferrersResponse, pageSize: Int): ReferrersModel {
        val first = response.groups.values.first()
        val groups = first.groups.take(pageSize).map { group ->
            val children = group.referrers?.mapNotNull { result ->
                if (result.name != null && result.views != null) {
                    val firstChildUrl = result.children?.firstOrNull()?.url
                    Referrer(result.name, result.views, result.icon, firstChildUrl ?: result.url)
                } else {
                    AppLog.e(STATS, "ReferrersResponse: Missing fields on a referrer")
                    null
                }
            }
            ReferrersModel.Group(group.groupId, group.name, group.icon, group.url, group.total, children ?: listOf())
        }
        return ReferrersModel(first.otherViews ?: 0, first.totalViews ?: 0, groups, first.groups.size > groups.size)
    }

    fun map(response: ClicksResponse, pageSize: Int): ClicksModel {
        val first = response.groups.values.first()
        val groups = first.clicks.take(pageSize).map { group ->
            val children = group.clicks?.mapNotNull { result ->
                if (result.name != null && result.views != null) {
                    Click(result.name, result.views, result.icon, result.url)
                } else {
                    AppLog.e(STATS, "ClicksResponse.type: Missing fields on a Click object")
                    null
                }
            }
            ClicksModel.Group(group.groupId, group.name, group.icon, group.url, group.views, children ?: listOf())
        }
        return ClicksModel(
                first.otherClicks ?: 0,
                first.totalClicks ?: 0,
                groups,
                first.clicks.size > groups.size
        )
    }

    fun map(response: VisitsAndViewsResponse): VisitsAndViewsModel {
        val periodIndex = response.fields?.indexOf("period")
        val viewsIndex = response.fields?.indexOf("views")
        val visitorsIndex = response.fields?.indexOf("visitors")
        val likesIndex = response.fields?.indexOf("likes")
        val reblogsIndex = response.fields?.indexOf("reblogs")
        val commentsIndex = response.fields?.indexOf("comments")
        val postsIndex = response.fields?.indexOf("posts")
        val dataPerPeriod = response.data?.mapNotNull { periodData ->
            periodData?.let {
                val period = periodIndex?.let { periodData[it] }
                if (period != null) {
                    PeriodData(
                            period,
                            periodData.getLongOrZero(viewsIndex),
                            periodData.getLongOrZero(visitorsIndex),
                            periodData.getLongOrZero(likesIndex),
                            periodData.getLongOrZero(reblogsIndex),
                            periodData.getLongOrZero(commentsIndex),
                            periodData.getLongOrZero(postsIndex)
                    )
                } else {
                    null
                }
            }
        }
        if (response.data == null) {
            AppLog.e(STATS, "VisitsAndViewsResponse: Date field should never be null")
        }
        return VisitsAndViewsModel(response.date ?: "", dataPerPeriod ?: listOf())
    }

    private fun List<String?>.getLongOrZero(itemIndex: Int?): Long {
        return itemIndex?.let {
            val stringValue = this[it]
            stringValue?.toLong()
        } ?: 0
    }

    fun map(response: AuthorsResponse, pageSize: Int): AuthorsModel {
        val first = response.groups.values.first()
        val authors = first.authors.take(pageSize).map { author ->
            val posts = author.mappedPosts?.mapNotNull { result ->
                if (result.postId != null && result.title != null) {
                    Post(result.postId, result.title, result.views ?: 0, result.url)
                } else {
                    AppLog.e(STATS, "AuthorsResponse: Missing fields on a post")
                    null
                }
            }
            if (author.name == null || author.views == null || author.avatar == null) {
                AppLog.e(STATS, "AuthorsResponse: Missing fields on an author")
            }
            AuthorsModel.Author(author.name ?: "", author.views ?: 0, author.avatar, posts ?: listOf())
        }
        return AuthorsModel(first.otherViews ?: 0, authors, first.authors.size > authors.size)
    }
}
