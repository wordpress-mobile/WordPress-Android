package org.wordpress.android.ui.newstats.datasource

import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.LocaleManagerWrapper
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.AuthorsRestClient
import org.wordpress.android.fluxc.network.utils.StatsGranularity
import uniffi.wp_api.StatsReferrersParams
import uniffi.wp_api.StatsReferrersPeriod
import uniffi.wp_api.StatsTopPostsParams
import uniffi.wp_api.StatsTopPostsPeriod
import uniffi.wp_api.StatsCountryViewsParams
import uniffi.wp_api.StatsCountryViewsPeriod
import uniffi.wp_api.StatsVisitsParams
import uniffi.wp_api.StatsVisitsUnit
import uniffi.wp_api.WpComLanguage
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import rs.wordpress.api.kotlin.fromLocale
import javax.inject.Inject

/**
 * Implementation of [StatsDataSource] that fetches stats data from the WordPress.com API
 * using the wordpress-rs library.
 */
class StatsDataSourceImpl @Inject constructor(
    private val wpComApiClientProvider: WpComApiClientProvider,
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val authorsRestClient: AuthorsRestClient
) : StatsDataSource {
    /**
     * Access token for API authentication.
     * Marked as @Volatile to ensure visibility across threads since this data source is accessed
     * from multiple coroutine contexts.
     */
    @Volatile
    private var accessToken: String? = null

    private val wpComApiClient: WpComApiClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        check(accessToken != null) { "DataSource not initialized" }
        wpComApiClientProvider.getWpComApiClient(accessToken!!)
    }

    override fun init(accessToken: String) {
        this.accessToken = accessToken
    }

    override suspend fun fetchStatsVisits(
        siteId: Long,
        unit: StatsUnit,
        quantity: Int,
        endDate: String
    ): StatsVisitsDataResult {
        val params = StatsVisitsParams(
            unit = unit.toApiUnit(),
            quantity = quantity.toUInt(),
            endDate = endDate,
        )

        val result = wpComApiClient.request { requestBuilder ->
            requestBuilder.statsVisits().getStatsVisits(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }

        AppLog.d(T.STATS, "StatsDataSourceImpl: fetchStatsVisits result type: ${result::class.simpleName}")

        return when (result) {
            is WpRequestResult.Success -> {
                AppLog.d(T.STATS, "StatsDataSourceImpl: fetchStatsVisits success")
                StatsVisitsDataResult.Success(mapToStatsVisitsData(result.response.data))
            }
            is WpRequestResult.WpError -> {
                AppLog.e(T.STATS, "StatsDataSourceImpl: fetchStatsVisits WpError - ${result.errorMessage}")
                StatsVisitsDataResult.Error(result.errorMessage)
            }
            is WpRequestResult.ResponseParsingError<*> -> {
                AppLog.e(T.STATS, "StatsDataSourceImpl: fetchStatsVisits ResponseParsingError - $result")
                StatsVisitsDataResult.Error("Response parsing error: $result")
            }
            else -> {
                AppLog.e(T.STATS, "StatsDataSourceImpl: fetchStatsVisits unexpected result - $result")
                StatsVisitsDataResult.Error("Unknown error: ${result::class.simpleName}")
            }
        }
    }

    private fun mapToStatsVisitsData(response: uniffi.wp_api.StatsVisitsResponse): StatsVisitsData {
        return StatsVisitsData(
            visits = response.visitsData().map { dataPoint ->
                VisitsDataPoint(period = dataPoint.period, visits = dataPoint.visits.toLong())
            },
            visitors = response.visitorsData().map { dataPoint ->
                VisitorsDataPoint(period = dataPoint.period, visitors = dataPoint.visitors.toLong())
            },
            likes = response.likesData().map { dataPoint ->
                LikesDataPoint(period = dataPoint.period, likes = dataPoint.likes.toLong())
            },
            comments = response.commentsData().map { dataPoint ->
                CommentsDataPoint(period = dataPoint.period, comments = dataPoint.comments.toLong())
            },
            posts = response.postsData().map { dataPoint ->
                PostsDataPoint(period = dataPoint.period, posts = dataPoint.posts.toLong())
            }
        )
    }

    private fun StatsUnit.toApiUnit(): StatsVisitsUnit = when (this) {
        StatsUnit.HOUR -> StatsVisitsUnit.HOUR
        StatsUnit.DAY -> StatsVisitsUnit.DAY
        StatsUnit.WEEK -> StatsVisitsUnit.WEEK
        StatsUnit.MONTH -> StatsVisitsUnit.MONTH
    }

    private val wpComLanguage: WpComLanguage?
        get() = WpComLanguage.fromLocale(localeManagerWrapper.getLocale())

    private fun buildTopPostsParams(dateRange: StatsDateRange, max: Int) = when (dateRange) {
        is StatsDateRange.Preset -> StatsTopPostsParams(
            period = StatsTopPostsPeriod.DAY,
            date = dateRange.date,
            num = dateRange.num.toUInt(),
            max = max.coerceAtLeast(1).toUInt(),
            locale = wpComLanguage
        )
        is StatsDateRange.Custom -> StatsTopPostsParams(
            period = StatsTopPostsPeriod.DAY,
            date = dateRange.date,
            startDate = dateRange.startDate,
            max = max.coerceAtLeast(1).toUInt(),
            locale = wpComLanguage
        )
    }

    override suspend fun fetchTopPostsAndPages(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int
    ): TopPostsDataResult {
        val params = buildTopPostsParams(dateRange, max)
        AppLog.d(T.STATS, "fetchTopPostsAndPages - siteId=$siteId, dateRange=$dateRange, max=$max")
        val result = wpComApiClient.request { requestBuilder ->
            requestBuilder.statsTopPosts().getStatsTopPosts(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }

        AppLog.d(T.STATS, "StatsDataSourceImpl: fetchTopPostsAndPages result type: ${result::class.simpleName}")

        return when (result) {
            is WpRequestResult.Success -> {
                val posts = result.response.data.summary?.postviews.orEmpty()
                AppLog.d(T.STATS, "StatsDataSourceImpl: fetchTopPostsAndPages success - ${posts.size} posts")
                TopPostsDataResult.Success(
                    posts.map { post ->
                        TopPostDataItem(
                            id = post.id.toLong(),
                            title = post.title.orEmpty(),
                            views = post.views?.toLong() ?: 0L
                        )
                    }
                )
            }
            is WpRequestResult.WpError -> {
                AppLog.e(
                    T.STATS,
                    "StatsDataSourceImpl: fetchTopPostsAndPages WpError - message=${result.errorMessage}"
                )
                TopPostsDataResult.Error(result.errorMessage)
            }
            is WpRequestResult.ResponseParsingError<*> -> {
                AppLog.e(
                    T.STATS,
                    "StatsDataSourceImpl: fetchTopPostsAndPages ResponseParsingError - $result"
                )
                TopPostsDataResult.Error("Response parsing error: $result")
            }
            else -> {
                AppLog.e(
                    T.STATS,
                    "StatsDataSourceImpl: fetchTopPostsAndPages unexpected result - $result"
                )
                TopPostsDataResult.Error("Unknown error")
            }
        }
    }

    override suspend fun fetchReferrers(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int
    ): ReferrersDataResult {
        val params = when (dateRange) {
            is StatsDateRange.Preset -> StatsReferrersParams(
                period = StatsReferrersPeriod.DAY,
                date = dateRange.date,
                num = dateRange.num.toUInt(),
                max = max.coerceAtLeast(1).toUInt(),
                locale = wpComLanguage
            )
            is StatsDateRange.Custom -> StatsReferrersParams(
                period = StatsReferrersPeriod.DAY,
                date = dateRange.date,
                startDate = dateRange.startDate,
                max = max.coerceAtLeast(1).toUInt(),
                locale = wpComLanguage
            )
        }

        val result = wpComApiClient.request { requestBuilder ->
            requestBuilder.statsReferrers().getStatsReferrers(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }

        AppLog.d(T.STATS, "StatsDataSourceImpl: fetchReferrers result type: ${result::class.simpleName}")

        return when (result) {
            is WpRequestResult.Success -> {
                val groups = result.response.data.summary?.groups.orEmpty()
                AppLog.d(T.STATS, "StatsDataSourceImpl: fetchReferrers success - ${groups.size} groups")
                ReferrersDataResult.Success(
                    groups.map { group ->
                        ReferrerDataItem(
                            name = group.name.orEmpty(),
                            views = group.total?.toLong() ?: 0L
                        )
                    }
                )
            }
            is WpRequestResult.WpError -> {
                AppLog.e(T.STATS, "StatsDataSourceImpl: fetchReferrers WpError - ${result.errorMessage}")
                ReferrersDataResult.Error(result.errorMessage)
            }
            is WpRequestResult.ResponseParsingError<*> -> {
                AppLog.e(T.STATS, "StatsDataSourceImpl: fetchReferrers ResponseParsingError - $result")
                ReferrersDataResult.Error("Response parsing error: $result")
            }
            else -> {
                AppLog.e(T.STATS, "StatsDataSourceImpl: fetchReferrers unexpected result - $result")
                ReferrersDataResult.Error("Unknown error: ${result::class.simpleName}")
            }
        }
    }

    private fun buildCountryViewsParams(dateRange: StatsDateRange, max: Int) = when (dateRange) {
        is StatsDateRange.Preset -> StatsCountryViewsParams(
            period = StatsCountryViewsPeriod.DAY,
            date = dateRange.date,
            num = dateRange.num.toUInt(),
            max = max.coerceAtLeast(1).toUInt(),
            locale = wpComLanguage,
            summarize = true
        )
        is StatsDateRange.Custom -> StatsCountryViewsParams(
            period = StatsCountryViewsPeriod.DAY,
            date = dateRange.date,
            startDate = dateRange.startDate,
            max = max.coerceAtLeast(1).toUInt(),
            locale = wpComLanguage,
            summarize = true
        )
    }

    override suspend fun fetchCountryViews(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int
    ): CountryViewsDataResult {
        val params = buildCountryViewsParams(dateRange, max)
        val result = wpComApiClient.request { requestBuilder ->
            requestBuilder.statsCountryViews().getStatsCountryViews(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }

        AppLog.d(T.STATS, "StatsDataSourceImpl: fetchCountryViews result type: ${result::class.simpleName}")

        return when (result) {
            is WpRequestResult.Success -> {
                val summary = result.response.data.summary
                val countryInfo = result.response.data.countryInfo.orEmpty()

                val countries = summary?.views.orEmpty().map { countryView ->
                    val code = countryView.countryCode.orEmpty()
                    val info = countryInfo[code]
                    CountryViewItem(
                        countryCode = code,
                        countryName = countryView.location ?: info?.countryFull.orEmpty(),
                        views = countryView.views?.toLong() ?: 0L,
                        flagIconUrl = info?.flagIcon
                    )
                }

                AppLog.d(T.STATS, "StatsDataSourceImpl: fetchCountryViews success - ${countries.size} countries")
                CountryViewsDataResult.Success(
                    CountryViewsData(
                        countries = countries,
                        totalViews = summary?.totalViews?.toLong() ?: 0L,
                        otherViews = summary?.otherViews?.toLong() ?: 0L
                    )
                )
            }
            is WpRequestResult.WpError -> {
                AppLog.e(T.STATS, "StatsDataSourceImpl: fetchCountryViews WpError - ${result.errorMessage}")
                CountryViewsDataResult.Error(result.errorMessage)
            }
            is WpRequestResult.ResponseParsingError<*> -> {
                AppLog.e(T.STATS, "StatsDataSourceImpl: fetchCountryViews ResponseParsingError - $result")
                CountryViewsDataResult.Error("Response parsing error: $result")
            }
            else -> {
                AppLog.e(T.STATS, "StatsDataSourceImpl: fetchCountryViews unexpected result - $result")
                CountryViewsDataResult.Error("Unknown error: ${result::class.simpleName}")
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun fetchTopAuthors(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int
    ): TopAuthorsDataResult {
        val date = when (dateRange) {
            is StatsDateRange.Preset -> LocalDate.parse(dateRange.date)
            is StatsDateRange.Custom -> LocalDate.parse(dateRange.date)
        }
        val javaDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())

        // Create a minimal SiteModel with just the siteId
        val site = SiteModel().apply { this.siteId = siteId }

        AppLog.d(T.STATS, "StatsDataSourceImpl: fetchTopAuthors - siteId=$siteId, date=$date, max=$max")

        return try {
            val result = authorsRestClient.fetchAuthors(
                site = site,
                granularity = StatsGranularity.DAYS,
                date = javaDate,
                itemsToLoad = max,
                forced = true
            )

            if (result.isError) {
                val errorMessage = result.error?.message ?: "Unknown error"
                AppLog.e(T.STATS, "StatsDataSourceImpl: fetchTopAuthors error - $errorMessage")
                TopAuthorsDataResult.Error(errorMessage)
            } else {
                val response = result.response
                // Get the first (and typically only) group of authors
                val authorsGroup = response?.groups?.values?.firstOrNull()
                val authorsList = authorsGroup?.authors.orEmpty()

                val authorItems = authorsList.map { author ->
                    TopAuthorItem(
                        name = author.name.orEmpty(),
                        avatarUrl = author.avatarUrl,
                        views = author.views?.toLong() ?: 0L
                    )
                }

                val totalViews = authorItems.sumOf { it.views }

                AppLog.d(
                    T.STATS,
                    "StatsDataSourceImpl: fetchTopAuthors success - ${authorItems.size} authors"
                )
                TopAuthorsDataResult.Success(
                    TopAuthorsData(
                        authors = authorItems,
                        totalViews = totalViews
                    )
                )
            }
        } catch (e: Exception) {
            AppLog.e(T.STATS, "StatsDataSourceImpl: fetchTopAuthors exception - ${e.message}")
            TopAuthorsDataResult.Error(e.message ?: "Unknown error")
        }
    }
}
