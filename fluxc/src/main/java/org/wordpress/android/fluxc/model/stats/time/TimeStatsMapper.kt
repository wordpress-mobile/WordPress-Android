package org.wordpress.android.fluxc.model.stats.time

import com.google.gson.Gson
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.stats.LimitMode
import org.wordpress.android.fluxc.model.stats.time.AuthorsModel.Post
import org.wordpress.android.fluxc.model.stats.time.ClicksModel.Click
import org.wordpress.android.fluxc.model.stats.time.FileDownloadsModel.FileDownloads
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel.Referrer
import org.wordpress.android.fluxc.model.stats.time.VisitsAndViewsModel.PeriodData
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.AuthorsRestClient.AuthorsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ClicksRestClient.ClicksResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.CountryViewsRestClient.CountryViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.FileDownloadsRestClient.FileDownloadsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.SearchTermsRestClient.SearchTermsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.VideoPlaysRestClient.VideoPlaysResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.VisitAndViewsRestClient.VisitsAndViewsResponse
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.STATS
import javax.inject.Inject

class TimeStatsMapper
@Inject constructor(val gson: Gson) {
    fun map(response: PostAndPageViewsResponse, cacheMode: LimitMode): PostAndPageViewsModel {
        val postViews = response.days.entries.firstOrNull()?.value?.postViews ?: listOf()
        val stats = postViews.let {
            if (cacheMode is LimitMode.Top) {
                return@let it.take(cacheMode.limit)
            } else {
                return@let it
            }
        }.map { item ->
            val type = when (item.type) {
                "post" -> ViewsType.POST
                "page" -> ViewsType.PAGE
                "homepage" -> ViewsType.HOMEPAGE
                else -> {
                    ViewsType.OTHER
                }
            }
            type.let {
                if (item.id == null || item.title == null || item.href == null) {
                    AppLog.e(STATS, "PostAndPageViewsResponse.type: Non-nullable fields are null - $item")
                }
                ViewsModel(item.id ?: 0, item.title ?: "", item.views ?: 0, type, item.href ?: "")
            }
        }
        return PostAndPageViewsModel(stats, cacheMode is LimitMode.Top && postViews.size > cacheMode.limit)
    }

    fun map(response: ReferrersResponse, cacheMode: LimitMode): ReferrersModel {
        val first = response.groups.values.firstOrNull()
        val groups = first?.let {
            first.groups.let {
                if (cacheMode is LimitMode.Top) {
                    it.take(cacheMode.limit)
                } else {
                    it
                }
            }.map { group ->
                val children = group.referrers?.mapNotNull { result ->
                    if (result.name != null && result.views != null) {
                        val firstChildUrl = result.children?.firstOrNull()?.url
                        Referrer(result.name, result.views, result.icon, firstChildUrl ?: result.url)
                    } else {
                        AppLog.e(STATS, "ReferrersResponse: Missing fields on a referrer")
                        null
                    }
                }
                ReferrersModel.Group(
                        group.groupId,
                        group.name,
                        group.icon,
                        group.url,
                        group.total,
                        children ?: listOf()
                )
            }
        }
        val hasMore = if (first != null && groups != null) first.groups.size > groups.size else false
        return ReferrersModel(first?.otherViews ?: 0, first?.totalViews ?: 0, groups ?: listOf(), hasMore)
    }

    fun map(response: ClicksResponse, cacheMode: LimitMode): ClicksModel {
        val first = response.groups.values.firstOrNull()
        val groups = first?.let {
            first.clicks.let {
                if (cacheMode is LimitMode.Top) {
                    it.take(cacheMode.limit)
                } else {
                    it
                }
            }.map { group ->
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
        }
        val hasMore = if (first != null && groups != null) first.clicks.size > groups.size else false
        return ClicksModel(
                first?.otherClicks ?: 0,
                first?.totalClicks ?: 0,
                groups ?: listOf(),
                hasMore
        )
    }

    fun map(response: VisitsAndViewsResponse, cacheMode: LimitMode): VisitsAndViewsModel {
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
                if (period != null && period.isNotBlank()) {
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
        }?.let {
            if (cacheMode is LimitMode.Top) {
                it.take(cacheMode.limit)
            } else {
                it
            }
        }
        if (response.data == null || response.date == null || dataPerPeriod == null) {
            AppLog.e(STATS, "VisitsAndViewsResponse: data, date & dataPerPeriod fields should never be null")
        }
        return VisitsAndViewsModel(response.date ?: "", dataPerPeriod ?: listOf())
    }

    private fun List<String?>.getLongOrZero(itemIndex: Int?): Long {
        return itemIndex?.let {
            val stringValue = this[it]
            stringValue?.toLong()
        } ?: 0
    }

    fun map(response: CountryViewsResponse, cacheMode: LimitMode): CountryViewsModel {
        val first = response.days.values.firstOrNull()
        val countriesInfo = response.countryInfo
        val groups = first?.let {
            first.views.let {
                if (cacheMode is LimitMode.Top) {
                    it.take(cacheMode.limit)
                } else {
                    it
                }
            }.mapNotNull { countryViews ->
                val countryInfo = countriesInfo[countryViews.countryCode]
                if (countryViews.countryCode != null && countryInfo != null && countryInfo.countryFull != null) {
                    CountryViewsModel.Country(
                            countryViews.countryCode,
                            countryInfo.countryFull,
                            countryViews.views ?: 0,
                            countryInfo.flagIcon,
                            countryInfo.flatFlagIcon
                    )
                } else {
                    AppLog.e(STATS, "CountryViewsResponse: Missing fields on a CountryViews object")
                    null
                }
            }
        }
        val hasMore = if (first != null && groups != null) first.views.size > groups.size else false
        return CountryViewsModel(
                first?.otherViews ?: 0,
                first?.totalViews ?: 0,
                groups ?: listOf(),
                hasMore
        )
    }

    fun map(response: AuthorsResponse, cacheMode: LimitMode): AuthorsModel {
        val first = response.groups.values.firstOrNull()
        val authors = first?.let {
            first.authors.let {
                if (cacheMode is LimitMode.Top) {
                    it.take(cacheMode.limit)
                } else {
                    it
                }
            }.map { author ->
                val posts = author.mappedPosts?.mapNotNull { result ->
                    if (result.postId != null && result.title != null) {
                        Post(result.postId, result.title, result.views ?: 0, result.url)
                    } else {
                        AppLog.e(STATS, "AuthorsResponse: Missing fields on a post")
                        null
                    }
                }
                if (author.name == null || author.views == null || author.avatarUrl == null) {
                    AppLog.e(STATS, "AuthorsResponse: Missing fields on an author")
                }
                AuthorsModel.Author(
                        StringEscapeUtils.unescapeHtml4(author.name) ?: "",
                        author.views ?: 0, author.avatarUrl, posts ?: listOf()
                )
            }
        }
        val hasMore = if (first != null && authors != null) first.authors.size > authors.size else false
        return AuthorsModel(first?.otherViews ?: 0, authors ?: listOf(), hasMore)
    }

    fun map(response: SearchTermsResponse, cacheMode: LimitMode): SearchTermsModel {
        val first = response.days.values.firstOrNull()
        val groups = first?.let {
            first.searchTerms.let {
                if (cacheMode is LimitMode.Top) {
                    it.take(cacheMode.limit)
                } else {
                    it
                }
            }.mapNotNull { searchTerm ->
                if (searchTerm.term != null) {
                    SearchTermsModel.SearchTerm(searchTerm.term, searchTerm.views ?: 0)
                } else {
                    AppLog.e(STATS, "SearchTermsResponse: Missing term field on a Search terms object")
                    null
                }
            }
        }
        val hasMore = if (first != null && groups != null) first.searchTerms.size > groups.size else false
        return SearchTermsModel(
                first?.otherSearchTerms ?: 0,
                first?.totalSearchTimes ?: 0,
                first?.encryptedSearchTerms ?: 0,
                groups ?: listOf(),
                hasMore
        )
    }

    fun map(response: VideoPlaysResponse, cacheMode: LimitMode): VideoPlaysModel {
        val first = response.days.values.firstOrNull()
        val groups = first?.let {
            first.plays.let {
                if (cacheMode is LimitMode.Top) {
                    it.take(cacheMode.limit)
                } else {
                    it
                }
            }.mapNotNull { result ->
                if (result.postId != null && result.title != null) {
                    VideoPlaysModel.VideoPlays(result.postId, result.title, result.url, result.plays ?: 0)
                } else {
                    AppLog.e(STATS, "VideoPlaysResponse: Missing fields on a Video plays object")
                    null
                }
            }
        }
        val hasMore = if (first != null && groups != null) first.plays.size > groups.size else false
        return VideoPlaysModel(
                first?.otherPlays ?: 0,
                first?.totalPlays ?: 0,
                groups ?: listOf(),
                hasMore
        )
    }

    fun map(response: FileDownloadsResponse, cacheMode: LimitMode): FileDownloadsModel {
        val first = response.groups.values.firstOrNull()
        val downloads = first?.files?.let {
            if (cacheMode is LimitMode.Top) {
                it.take(cacheMode.limit)
            } else {
                it
            }
        }?.mapNotNull {
            if (it.filename != null) {
                FileDownloads(it.filename, it.downloads ?: 0)
            } else {
                null
            }
        } ?: listOf()
        return FileDownloadsModel(downloads, downloads.size < first?.files?.size ?: 0)
    }
}
