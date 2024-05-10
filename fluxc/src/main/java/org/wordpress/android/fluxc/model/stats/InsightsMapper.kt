package org.wordpress.android.fluxc.model.stats

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.FollowersModel.FollowerModel
import org.wordpress.android.fluxc.model.stats.LimitMode.Top
import org.wordpress.android.fluxc.model.stats.TagsModel.TagModel
import org.wordpress.android.fluxc.model.stats.YearsInsightsModel.YearInsights
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel.Day
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel.Month
import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel.StreakModel
import org.wordpress.android.fluxc.model.stats.subscribers.PostsModel
import org.wordpress.android.fluxc.model.stats.subscribers.PostsModel.PostModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.AllTimeInsightsRestClient.AllTimeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.CommentsRestClient.CommentsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowerType
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowerType.ALL
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowerType.EMAIL
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowerType.WP_COM
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.FollowersRestClient.FollowersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient.PostStatsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.LatestPostInsightsRestClient.PostsResponse.PostResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.MostPopularRestClient.MostPopularResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PostingActivityRestClient.PostingActivityResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.PublicizeRestClient.PublicizeResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.SummaryRestClient.SummaryResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TagsRestClient.TagsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TagsRestClient.TagsResponse.TagsGroup.TagResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.insights.TodayInsightsRestClient.VisitResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers.EmailsRestClient.EmailsSummaryResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.subscribers.EmailsRestClient.SortField
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.StatsUtils
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.STATS
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

private const val PERIOD = "period"
private const val VIEWS = "views"
private const val VISITORS = "visitors"
private const val LIKES = "likes"
private const val REBLOGS = "reblogs"
private const val COMMENTS = "comments"
private const val POSTS = "posts"

private const val MILLIS = 1000

class InsightsMapper @Inject constructor(val statsUtils: StatsUtils) {
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

    fun map(response: MostPopularResponse): YearsInsightsModel? {
        val yearInsightsResponse = response.yearInsightResponses?.map { YearInsights(
                it.avgComments,
                it.avgImages,
                it.avgLikes,
                it.avgWords,
                it.totalComments,
                it.totalImages,
                it.totalLikes,
                it.totalPosts,
                it.totalWords,
                it.year
        ) }
        return yearInsightsResponse?.let { YearsInsightsModel(it) }
    }

    @Suppress("ComplexCondition")
    fun map(
        postResponse: PostResponse,
        postStatsResponse: PostStatsResponse,
        site: SiteModel
    ): InsightsLatestPostModel {
        val daysViews = if (
            postStatsResponse.fields != null &&
            postStatsResponse.data != null &&
            postStatsResponse.fields.size > 1 &&
            postStatsResponse.fields[0] == "period" &&
            postStatsResponse.fields[1] == "views"
        ) {
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
            postResponse.id,
            viewsCount ?: 0,
            commentCount,
            postResponse.likeCount ?: 0,
            daysViews,
            postResponse.featuredImage ?: ""
        )
    }

    fun map(response: VisitResponse): VisitsModel {
        val result: Map<String, String> = response.fields.mapIndexedNotNull { index, value ->
            if (response.data.isNotEmpty() && response.data[0].size > index) {
                value to response.data[0][index]
            } else {
                null
            }
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
        }
        val total = when (followerType) {
            ALL -> response.total
            WP_COM -> response.totalWpCom
            EMAIL -> response.totalEmail
        }
        val hasMore = if (response.page != null && response.pages != null) {
            response.page < response.pages
        } else {
            false
        }
        return FollowersModel(total ?: 0, followers, hasMore)
    }

    fun mapAndMergeFollowersModels(
        followerResponses: List<FollowersResponse>,
        followerType: FollowerType,
        cacheMode: LimitMode
    ): FollowersModel {
        return followerResponses.fold(FollowersModel(0, emptyList(), false)) { accumulator, next ->
                val nextModel = map(next, followerType)
                accumulator.copy(
                        totalCount = nextModel.totalCount,
                        followers = accumulator.followers + nextModel.followers,
                        hasMore = nextModel.hasMore
                )
            }
            .let {
                if (cacheMode is LimitMode.Top) {
                    return@let it.copy(followers = it.followers.take(cacheMode.limit))
                } else {
                    return@let it
                }
            }
    }

    @Suppress("ComplexMethod", "ComplexCondition")
    fun map(response: CommentsResponse, cacheMode: LimitMode): CommentsModel {
        val authors = response.authors?.let {
            if (cacheMode is LimitMode.Top) {
                return@let it.take(cacheMode.limit)
            } else {
                return@let it
            }
        }
        ?.mapNotNull {
            if (
                it.name != null &&
                it.comments != null &&
                it.link != null &&
                it.gravatar != null
            ) {
                CommentsModel.Author(it.name, it.comments, it.link, it.gravatar)
            } else {
                AppLog.e(STATS, "CommentsResponse.authors: Non-null field is coming as null from API")
                null
            }
        }
        val posts = response.posts?.let {
            if (cacheMode is LimitMode.Top) {
                return@let it.take(cacheMode.limit)
            } else {
                return@let it
            }
        }
        ?.mapNotNull {
            if (
                it.id != null &&
                it.name != null &&
                it.comments != null &&
                it.link != null
            ) {
                CommentsModel.Post(it.id, it.name, it.comments, it.link)
            } else {
                AppLog.e(STATS, "CommentsResponse.posts: Non-null field is coming as null from API")
                null
            }
        }
        val hasMoreAuthors = (response.authors != null && cacheMode is Top && response.authors.size > cacheMode.limit)
        val hasMorePosts = (response.posts != null && cacheMode is Top && response.posts.size > cacheMode.limit)
        return CommentsModel(posts ?: listOf(), authors ?: listOf(), hasMorePosts, hasMoreAuthors)
    }

    fun map(response: SummaryResponse) = SummaryModel(
        response.likes ?: 0,
        response.comments ?: 0,
        response.followers ?: 0
    )

    fun map(response: TagsResponse, cacheMode: LimitMode): TagsModel {
        return TagsModel(response.tags.let {
            if (cacheMode is LimitMode.Top) {
                return@let it.take(cacheMode.limit)
            } else {
                return@let it
            }
        }.map { tag ->
            TagModel(tag.tags.mapNotNull { it.toItem() }.let {
                if (cacheMode is LimitMode.Top) {
                    return@let it.take(cacheMode.limit)
                } else {
                    return@let it
                }
            }, tag.views ?: 0)
        }, cacheMode is LimitMode.Top && response.tags.size > cacheMode.limit)
    }

    private fun TagResponse.toItem(): TagModel.Item? {
        return if (this.name != null && this.type != null && this.link != null) {
            TagModel.Item(this.name, this.type, this.link)
        } else {
            AppLog.e(STATS, "TagResponse: Mandatory fields are null so the Tag can't be mapped to Model")
            null
        }
    }

    fun map(response: PublicizeResponse, limitMode: LimitMode): PublicizeModel {
        return PublicizeModel(
                response.services.sortedBy { it.followers }.let {
                    if (limitMode is LimitMode.Top) {
                        return@let it.take(limitMode.limit)
                    } else {
                        return@let it
                    }
                }.map { PublicizeModel.Service(it.service, it.followers) },
                limitMode is LimitMode.Top && response.services.size > limitMode.limit
        )
    }

    @Suppress("LongMethod")
    fun map(response: PostingActivityResponse, startDay: Day, endDay: Day): PostingActivityModel {
        if (response.streak == null) {
            AppLog.e(STATS, "PostingActivityResponse: Mandatory field streak is null")
        }
        val currentStreakStart = response.streak?.currentStreak?.start?.let { statsUtils.fromFormattedDate(it) }
        val currentStreakEnd = response.streak?.currentStreak?.end?.let { statsUtils.fromFormattedDate(it) }
        val currentStreakLength = response.streak?.currentStreak?.length
        val longStreakStart = response.streak?.longStreak?.start?.let { statsUtils.fromFormattedDate(it) }
        val longStreakEnd = response.streak?.longStreak?.end?.let { statsUtils.fromFormattedDate(it) }
        val longStreakLength = response.streak?.longStreak?.length
        val streak = StreakModel(
                currentStreakStart = currentStreakStart,
                currentStreakEnd = currentStreakEnd,
                currentStreakLength = currentStreakLength,
                longestStreakStart = longStreakStart,
                longestStreakEnd = longStreakEnd,
                longestStreakLength = longStreakLength
        )
        val nonNullData = response.data ?: mapOf()
        val days = mutableMapOf<Day, Int>()
        nonNullData.toList().forEach { (timeStamp, value) ->
            val day = toDay(timeStamp)
            days[day] = (days[day] ?: 0) + value
        }
        val startCalendar = Calendar.getInstance()
        startCalendar.set(startDay.year, startDay.month, startDay.day, 0, 0)
        val endCalendar = Calendar.getInstance()
        endCalendar.set(
                endDay.year,
                endDay.month,
                endDay.day,
                endCalendar.getActualMaximum(Calendar.HOUR_OF_DAY),
                endCalendar.getActualMaximum(Calendar.MINUTE)
        )
        var currentYear = startDay.year
        var currentMonth = startDay.month
        var currentMonthDays = mutableMapOf<Int, Int>()
        val result = mutableListOf<Month>()
        var count = 0
        var max = 0
        while (!startCalendar.after(endCalendar)) {
            if (currentYear != startCalendar.get(Calendar.YEAR) || currentMonth != startCalendar.get(Calendar.MONTH)) {
                result.add(Month(currentYear, currentMonth, currentMonthDays))
                currentYear = startCalendar.get(Calendar.YEAR)
                currentMonth = startCalendar.get(Calendar.MONTH)
                currentMonthDays = mutableMapOf()
            }
            val currentDay = days[Day(
                    startCalendar.get(Calendar.YEAR),
                    startCalendar.get(Calendar.MONTH),
                    startCalendar.get(Calendar.DAY_OF_MONTH)
            )]
            val currentDayPostCount = currentDay ?: 0
            if (currentDayPostCount > max) {
                max = currentDayPostCount
            }
            currentMonthDays[startCalendar.get(Calendar.DAY_OF_MONTH)] = currentDayPostCount
            count++
            startCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        result.add(Month(currentYear, currentMonth, currentMonthDays))
        return PostingActivityModel(streak, result, max, count < nonNullData.count())
    }

    fun map(response: EmailsSummaryResponse, cacheMode: LimitMode, sortField: SortField) = PostsModel(
        response.posts.let {
            if (cacheMode is Top) {
                return@let it.take(cacheMode.limit)
            } else {
                return@let it
            }
        }.map { post -> PostModel(post.id ?: 0, post.href ?: "", post.title ?: "", post.opens ?: 0, post.clicks ?: 0) }
            .sortedByDescending {
                when (sortField) {
                    SortField.POST_ID -> it.id
                    SortField.OPENS -> it.opens.toLong()
                }
            }
    )

    private fun toDay(timeStamp: Long): Day {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timeStamp * MILLIS
        return Day(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
    }
}
