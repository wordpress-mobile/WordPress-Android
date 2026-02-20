package org.wordpress.android.ui.newstats.datasource

import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.LocaleManagerWrapper
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.StatsCityViewsParams
import uniffi.wp_api.StatsCityViewsPeriod
import uniffi.wp_api.StatsCountryViewsParams
import uniffi.wp_api.StatsCountryViewsPeriod
import uniffi.wp_api.StatsReferrersParams
import uniffi.wp_api.StatsReferrersPeriod
import uniffi.wp_api.StatsRegionViewsParams
import uniffi.wp_api.StatsRegionViewsPeriod
import uniffi.wp_api.StatsTopAuthorsParams
import uniffi.wp_api.StatsTopAuthorsPeriod
import uniffi.wp_api.StatsTopPostsParams
import uniffi.wp_api.StatsTopPostsPeriod
import uniffi.wp_api.StatsVisitsParams
import uniffi.wp_api.StatsVisitsUnit
import uniffi.wp_api.WpComLanguage
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
    private val localeManagerWrapper: LocaleManagerWrapper
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

    private fun buildRegionViewsParams(
        dateRange: StatsDateRange,
        max: Int
    ) = when (dateRange) {
        is StatsDateRange.Preset -> StatsRegionViewsParams(
            period = StatsRegionViewsPeriod.DAY,
            date = dateRange.date,
            num = dateRange.num.toUInt(),
            max = max.coerceAtLeast(1).toUInt(),
            locale = wpComLanguage,
            summarize = true
        )
        is StatsDateRange.Custom -> StatsRegionViewsParams(
            period = StatsRegionViewsPeriod.DAY,
            date = dateRange.date,
            startDate = dateRange.startDate,
            max = max.coerceAtLeast(1).toUInt(),
            locale = wpComLanguage,
            summarize = true
        )
    }

    override suspend fun fetchRegionViews(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int
    ): RegionViewsDataResult {
        val params = buildRegionViewsParams(dateRange, max)
        val result = wpComApiClient.request { requestBuilder ->
            requestBuilder.statsRegionViews().getStatsRegionViews(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }
        logResultType("fetchRegionViews", result)
        return when (result) {
            is WpRequestResult.Success -> {
                val summary = result.response.data.summary
                val countryInfo =
                    result.response.data.countryInfo.orEmpty()
                val regions =
                    summary?.views.orEmpty().map { regionView ->
                        val code = regionView.countryCode.orEmpty()
                        val info = countryInfo[code]
                        RegionViewItem(
                            location =
                                regionView.location.orEmpty(),
                            countryCode = code,
                            views =
                                regionView.views?.toLong() ?: 0L,
                            flagIconUrl = info?.flagIcon
                        )
                    }
                AppLog.d(
                    T.STATS,
                    "StatsDataSourceImpl: fetchRegionViews " +
                        "success - ${regions.size} regions"
                )
                RegionViewsDataResult.Success(
                    RegionViewsData(
                        regions = regions,
                        totalViews =
                            summary?.totalViews?.toLong() ?: 0L,
                        otherViews =
                            summary?.otherViews?.toLong() ?: 0L
                    )
                )
            }
            else -> logErrorAndReturn("fetchRegionViews", result) {
                RegionViewsDataResult.Error(it)
            }
        }
    }

    private fun buildCityViewsParams(
        dateRange: StatsDateRange,
        max: Int
    ) = when (dateRange) {
        is StatsDateRange.Preset -> StatsCityViewsParams(
            period = StatsCityViewsPeriod.DAY,
            date = dateRange.date,
            num = dateRange.num.toUInt(),
            max = max.coerceAtLeast(1).toUInt(),
            locale = wpComLanguage,
            summarize = true
        )
        is StatsDateRange.Custom -> StatsCityViewsParams(
            period = StatsCityViewsPeriod.DAY,
            date = dateRange.date,
            startDate = dateRange.startDate,
            max = max.coerceAtLeast(1).toUInt(),
            locale = wpComLanguage,
            summarize = true
        )
    }

    override suspend fun fetchCityViews(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int
    ): CityViewsDataResult {
        val params = buildCityViewsParams(dateRange, max)
        val result = wpComApiClient.request { requestBuilder ->
            requestBuilder.statsCityViews().getStatsCityViews(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }
        logResultType("fetchCityViews", result)
        return when (result) {
            is WpRequestResult.Success -> {
                val summary = result.response.data.summary
                val countryInfo =
                    result.response.data.countryInfo.orEmpty()
                val cities =
                    summary?.views.orEmpty().map { cityView ->
                        val code = cityView.countryCode.orEmpty()
                        val info = countryInfo[code]
                        CityViewItem(
                            location =
                                cityView.location.orEmpty(),
                            countryCode = code,
                            views =
                                cityView.views?.toLong() ?: 0L,
                            latitude =
                                cityView.coordinates?.latitude,
                            longitude =
                                cityView.coordinates?.longitude,
                            flagIconUrl = info?.flagIcon
                        )
                    }
                AppLog.d(
                    T.STATS,
                    "StatsDataSourceImpl: fetchCityViews " +
                        "success - ${cities.size} cities"
                )
                CityViewsDataResult.Success(
                    CityViewsData(
                        cities = cities,
                        totalViews =
                            summary?.totalViews?.toLong() ?: 0L,
                        otherViews =
                            summary?.otherViews?.toLong() ?: 0L
                    )
                )
            }
            else -> logErrorAndReturn("fetchCityViews", result) {
                CityViewsDataResult.Error(it)
            }
        }
    }

    private fun buildTopAuthorsParams(dateRange: StatsDateRange, max: Int) = when (dateRange) {
        is StatsDateRange.Preset -> StatsTopAuthorsParams(
            period = StatsTopAuthorsPeriod.DAY,
            date = dateRange.date,
            num = dateRange.num.toUInt(),
            max = if (max > 0) max.toUInt() else null,
            locale = wpComLanguage,
            summarize = true
        )
        is StatsDateRange.Custom -> StatsTopAuthorsParams(
            period = StatsTopAuthorsPeriod.DAY,
            date = dateRange.date,
            startDate = dateRange.startDate,
            max = if (max > 0) max.toUInt() else null,
            locale = wpComLanguage,
            summarize = true
        )
    }

    override suspend fun fetchTopAuthors(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int
    ): TopAuthorsDataResult {
        val params = buildTopAuthorsParams(dateRange, max)
        AppLog.d(T.STATS, "fetchTopAuthors - siteId=$siteId, dateRange=$dateRange, max=$max")

        val result = wpComApiClient.request { requestBuilder ->
            requestBuilder.statsTopAuthors().getStatsTopAuthors(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }

        AppLog.d(T.STATS, "StatsDataSourceImpl: fetchTopAuthors result type: ${result::class.simpleName}")

        return when (result) {
            is WpRequestResult.Success -> {
                val authors = result.response.data.summary?.authors.orEmpty()
                AppLog.d(
                    T.STATS,
                    "StatsDataSourceImpl: fetchTopAuthors success - ${authors.size} authors"
                )

                val authorItems = authors.map { author ->
                    TopAuthorItem(
                        name = author.name,
                        avatarUrl = author.avatar,
                        views = author.views.toLong()
                    )
                }
                val totalViews = authorItems.sumOf { it.views }

                TopAuthorsDataResult.Success(
                    TopAuthorsData(
                        authors = authorItems,
                        totalViews = totalViews
                    )
                )
            }
            is WpRequestResult.WpError -> {
                AppLog.e(
                    T.STATS,
                    "StatsDataSourceImpl: fetchTopAuthors WpError - ${result.errorMessage}"
                )
                TopAuthorsDataResult.Error(result.errorMessage)
            }
            is WpRequestResult.ResponseParsingError<*> -> {
                AppLog.e(
                    T.STATS,
                    "StatsDataSourceImpl: fetchTopAuthors ResponseParsingError - $result"
                )
                TopAuthorsDataResult.Error("Response parsing error: $result")
            }
            else -> {
                AppLog.e(
                    T.STATS,
                    "StatsDataSourceImpl: fetchTopAuthors unexpected result - $result"
                )
                TopAuthorsDataResult.Error("Unknown error: ${result::class.simpleName}")
            }
        }
    }

    private fun logResultType(methodName: String, result: WpRequestResult<*>) {
        AppLog.d(
            T.STATS,
            "StatsDataSourceImpl: $methodName result type: " +
                "${result::class.simpleName}"
        )
    }

    private fun <T> logErrorAndReturn(
        methodName: String,
        result: WpRequestResult<*>,
        errorFactory: (String) -> T
    ): T {
        val (logMessage, errorMessage) = when (result) {
            is WpRequestResult.WpError -> {
                "StatsDataSourceImpl: $methodName WpError - " +
                    result.errorMessage to result.errorMessage
            }
            is WpRequestResult.ResponseParsingError<*> -> {
                "StatsDataSourceImpl: $methodName " +
                    "ResponseParsingError - $result" to
                    "Response parsing error: $result"
            }
            else -> {
                "StatsDataSourceImpl: $methodName " +
                    "unexpected result - $result" to
                    "Unknown error: ${result::class.simpleName}"
            }
        }
        AppLog.e(T.STATS, logMessage)
        return errorFactory(errorMessage)
    }
}
