package org.wordpress.android.ui.newstats.datasource

import org.wordpress.android.networking.restapi.WpComApiClientProvider
import org.wordpress.android.util.LocaleManagerWrapper
import rs.wordpress.api.kotlin.WpComApiClient
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.StatsCityViewsParams
import uniffi.wp_api.StatsCityViewsPeriod
import uniffi.wp_api.StatsClicksParams
import uniffi.wp_api.StatsClicksPeriod
import uniffi.wp_api.StatsCountryViewsParams
import uniffi.wp_api.StatsCountryViewsPeriod
import uniffi.wp_api.StatsFileDownloadsParams
import uniffi.wp_api.StatsFileDownloadsPeriod
import uniffi.wp_api.StatsReferrersParams
import uniffi.wp_api.StatsReferrersPeriod
import uniffi.wp_api.StatsRegionViewsParams
import uniffi.wp_api.StatsRegionViewsPeriod
import uniffi.wp_api.StatsDevicesParams
import uniffi.wp_api.StatsDevicesPeriod
import uniffi.wp_api.StatsSearchTermsParams
import uniffi.wp_api.StatsSearchTermsPeriod
import uniffi.wp_api.StatsTopAuthorsParams
import uniffi.wp_api.StatsTopAuthorsPeriod
import uniffi.wp_api.StatsTopPostsParams
import uniffi.wp_api.StatsTopPostsPeriod
import uniffi.wp_api.StatsVideoPlaysParams
import uniffi.wp_api.StatsVideoPlaysPeriod
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
@Suppress("LargeClass")
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

    @Volatile
    private var wpComApiClient: WpComApiClient? = null

    private fun getOrCreateClient(): WpComApiClient {
        val token = accessToken
        check(token != null) { "DataSource not initialized" }
        return wpComApiClient
            ?: wpComApiClientProvider.getWpComApiClient(token)
                .also { wpComApiClient = it }
    }

    override fun init(accessToken: String) {
        if (this.accessToken != accessToken) {
            this.accessToken = accessToken
            wpComApiClient = null
        }
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

        val result = getOrCreateClient().request { requestBuilder ->
            requestBuilder.statsVisits().getStatsVisits(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }

        logResultType("fetchStatsVisits", result)

        return when (result) {
            is WpRequestResult.Success -> {
                AppLog.d(T.STATS, "StatsDataSourceImpl: fetchStatsVisits success")
                StatsVisitsDataResult.Success(mapToStatsVisitsData(result.response.data))
            }
            else -> logErrorAndReturn("fetchStatsVisits", result) {
                StatsVisitsDataResult.Error(it)
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
        val result = getOrCreateClient().request { requestBuilder ->
            requestBuilder.statsTopPosts().getStatsTopPosts(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }

        logResultType("fetchTopPostsAndPages", result)

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
            else -> logErrorAndReturn("fetchTopPostsAndPages", result) {
                TopPostsDataResult.Error(it)
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

        val result = getOrCreateClient().request { requestBuilder ->
            requestBuilder.statsReferrers().getStatsReferrers(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }

        logResultType("fetchReferrers", result)

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
            else -> logErrorAndReturn("fetchReferrers", result) {
                ReferrersDataResult.Error(it)
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
        val result = getOrCreateClient().request { requestBuilder ->
            requestBuilder.statsCountryViews().getStatsCountryViews(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }

        logResultType("fetchCountryViews", result)

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
            else -> logErrorAndReturn("fetchCountryViews", result) {
                CountryViewsDataResult.Error(it)
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
        val result = getOrCreateClient().request { requestBuilder ->
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
        val result = getOrCreateClient().request { requestBuilder ->
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

        val result = getOrCreateClient().request { requestBuilder ->
            requestBuilder.statsTopAuthors().getStatsTopAuthors(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }

        logResultType("fetchTopAuthors", result)

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
            else -> logErrorAndReturn("fetchTopAuthors", result) {
                TopAuthorsDataResult.Error(it)
            }
        }
    }

    private fun buildClicksParams(
        dateRange: StatsDateRange,
        max: Int
    ) = when (dateRange) {
        is StatsDateRange.Preset -> StatsClicksParams(
            period = StatsClicksPeriod.DAY,
            date = dateRange.date,
            num = dateRange.num.toUInt(),
            max = if (max > 0) max.toUInt() else null,
            summarize = true
        )
        is StatsDateRange.Custom -> StatsClicksParams(
            period = StatsClicksPeriod.DAY,
            date = dateRange.date,
            startDate = dateRange.startDate,
            max = if (max > 0) max.toUInt() else null,
            summarize = true
        )
    }

    private fun buildDevicesParams(
        dateRange: StatsDateRange,
        max: Int
    ) = when (dateRange) {
        is StatsDateRange.Preset -> StatsDevicesParams(
            period = StatsDevicesPeriod.DAY,
            date = dateRange.date,
            num = dateRange.num.toUInt(),
            max = max.coerceAtLeast(1).toUInt(),
            summarize = true
        )
        is StatsDateRange.Custom -> StatsDevicesParams(
            period = StatsDevicesPeriod.DAY,
            date = dateRange.date,
            startDate = dateRange.startDate,
            max = max.coerceAtLeast(1).toUInt(),
            summarize = true
        )
    }

    override suspend fun fetchClicks(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int
    ): ClicksDataResult {
        val params = buildClicksParams(dateRange, max)
        val result = getOrCreateClient().request { api ->
            api.statsClicks().getStatsClicks(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }
        logResultType("fetchClicks", result)
        return when (result) {
            is WpRequestResult.Success -> {
                val clicks = result.response.data
                    .summary?.clicks.orEmpty()
                AppLog.d(
                    T.STATS,
                    "StatsDataSourceImpl: fetchClicks " +
                        "success - ${clicks.size} clicks"
                )
                ClicksDataResult.Success(
                    clicks.map { entry ->
                        ClickDataItem(
                            name = entry.name.orEmpty(),
                            clicks = entry.views?.toLong()
                                ?: 0L
                        )
                    }
                )
            }
            else -> logErrorAndReturn(
                "fetchClicks", result
            ) {
                ClicksDataResult.Error(it)
            }
        }
    }

    override suspend fun fetchDevicesScreensize(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int
    ): DevicesDataResult {
        val params = buildDevicesParams(dateRange, max)
        val result = getOrCreateClient().request { requestBuilder ->
            requestBuilder.statsDevicesScreensize()
                .getStatsDevicesScreensize(
                    wpComSiteId = siteId.toULong(),
                    params = params
                )
        }
        logResultType("fetchDevicesScreensize", result)
        return when (result) {
            is WpRequestResult.Success -> {
                val topValues = result.response.data.topValues
                AppLog.d(
                    T.STATS,
                    "StatsDataSourceImpl: fetchDevicesScreensize " +
                        "success - ${topValues.size} items"
                )
                DevicesDataResult.Success(
                    DevicesData(items = topValues)
                )
            }
            is WpRequestResult.WpError -> logErrorAndReturn(
                "fetchDevicesScreensize", result
            ) {
                DevicesDataResult.Error(it)
            }
            else -> logErrorAndReturn(
                "fetchDevicesScreensize", result
            ) {
                DevicesDataResult.Error(it)
            }
        }
    }

    private fun buildSearchTermsParams(
        dateRange: StatsDateRange,
        max: Int
    ) = when (dateRange) {
        is StatsDateRange.Preset -> StatsSearchTermsParams(
            period = StatsSearchTermsPeriod.DAY,
            date = dateRange.date,
            num = dateRange.num.toUInt(),
            max = if (max > 0) max.toUInt() else null,
            summarize = true
        )
        is StatsDateRange.Custom -> StatsSearchTermsParams(
            period = StatsSearchTermsPeriod.DAY,
            date = dateRange.date,
            startDate = dateRange.startDate,
            max = if (max > 0) max.toUInt() else null,
            summarize = true
        )
    }

    override suspend fun fetchSearchTerms(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int
    ): SearchTermsDataResult {
        val params = buildSearchTermsParams(dateRange, max)
        val result = getOrCreateClient().request { api ->
            api.statsSearchTerms().getStatsSearchTerms(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }
        logResultType("fetchSearchTerms", result)
        return when (result) {
            is WpRequestResult.Success -> {
                val terms = result.response.data
                    .summary?.searchTerms.orEmpty()
                AppLog.d(
                    T.STATS,
                    "StatsDataSourceImpl: fetchSearchTerms " +
                        "success - ${terms.size} terms"
                )
                SearchTermsDataResult.Success(
                    terms.map { entry ->
                        SearchTermDataItem(
                            name = entry.term.orEmpty(),
                            views = entry.views?.toLong()
                                ?: 0L
                        )
                    }
                )
            }
            else -> logErrorAndReturn(
                "fetchSearchTerms", result
            ) {
                SearchTermsDataResult.Error(it)
            }
        }
    }

    override suspend fun fetchDevicesBrowser(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int
    ): DevicesDataResult {
        val params = buildDevicesParams(dateRange, max)
        val result = getOrCreateClient().request { requestBuilder ->
            requestBuilder.statsDevicesBrowser()
                .getStatsDevicesBrowser(
                    wpComSiteId = siteId.toULong(),
                    params = params
                )
        }
        logResultType("fetchDevicesBrowser", result)
        return when (result) {
            is WpRequestResult.Success -> {
                val topValues = result.response.data.topValues
                AppLog.d(
                    T.STATS,
                    "StatsDataSourceImpl: fetchDevicesBrowser " +
                        "success - ${topValues.size} items"
                )
                DevicesDataResult.Success(
                    DevicesData(items = topValues)
                )
            }
            is WpRequestResult.WpError -> logErrorAndReturn(
                "fetchDevicesBrowser", result
            ) {
                DevicesDataResult.Error(it)
            }
            else -> logErrorAndReturn(
                "fetchDevicesBrowser", result
            ) {
                DevicesDataResult.Error(it)
            }
        }
    }

    private fun buildVideoPlaysParams(
        dateRange: StatsDateRange,
        max: Int
    ) = when (dateRange) {
        is StatsDateRange.Preset -> StatsVideoPlaysParams(
            period = StatsVideoPlaysPeriod.DAY,
            date = dateRange.date,
            num = dateRange.num.toUInt(),
            max = if (max > 0) max.toUInt() else null,
            summarize = true
        )
        is StatsDateRange.Custom -> StatsVideoPlaysParams(
            period = StatsVideoPlaysPeriod.DAY,
            date = dateRange.date,
            startDate = dateRange.startDate,
            max = if (max > 0) max.toUInt() else null,
            summarize = true
        )
    }

    override suspend fun fetchVideoPlays(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int
    ): VideoPlaysDataResult {
        val params = buildVideoPlaysParams(dateRange, max)
        val result = getOrCreateClient().request { api ->
            api.statsVideoPlays().getStatsVideoPlays(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }
        logResultType("fetchVideoPlays", result)
        return when (result) {
            is WpRequestResult.Success -> {
                val plays = result.response.data
                    .days.summary.data.orEmpty()
                AppLog.d(
                    T.STATS,
                    "StatsDataSourceImpl: fetchVideoPlays " +
                        "success - ${plays.size} plays"
                )
                VideoPlaysDataResult.Success(
                    plays.map { entry ->
                        VideoPlayDataItem(
                            title = entry.title.orEmpty(),
                            views = entry.views?.toLong()
                                ?: 0L
                        )
                    }
                )
            }
            else -> logErrorAndReturn(
                "fetchVideoPlays", result
            ) {
                VideoPlaysDataResult.Error(it)
            }
        }
    }

    override suspend fun fetchDevicesPlatform(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int
    ): DevicesDataResult {
        val params = buildDevicesParams(dateRange, max)
        val result = getOrCreateClient().request { requestBuilder ->
            requestBuilder.statsDevicesPlatform()
                .getStatsDevicesPlatform(
                    wpComSiteId = siteId.toULong(),
                    params = params
                )
        }
        logResultType("fetchDevicesPlatform", result)
        return when (result) {
            is WpRequestResult.Success -> {
                val topValues = result.response.data.topValues
                AppLog.d(
                    T.STATS,
                    "StatsDataSourceImpl: fetchDevicesPlatform " +
                        "success - ${topValues.size} items"
                )
                DevicesDataResult.Success(
                    DevicesData(items = topValues)
                )
            }
            is WpRequestResult.WpError -> logErrorAndReturn(
                "fetchDevicesPlatform", result
            ) {
                DevicesDataResult.Error(it)
            }
            else -> logErrorAndReturn(
                "fetchDevicesPlatform", result
            ) {
                DevicesDataResult.Error(it)
            }
        }
    }

    private fun buildFileDownloadsParams(
        dateRange: StatsDateRange,
        max: Int
    ) = when (dateRange) {
        is StatsDateRange.Preset -> StatsFileDownloadsParams(
            period = StatsFileDownloadsPeriod.DAY,
            date = dateRange.date,
            num = dateRange.num.toUInt(),
            max = if (max > 0) max.toUInt() else null,
            summarize = true
        )
        is StatsDateRange.Custom -> StatsFileDownloadsParams(
            period = StatsFileDownloadsPeriod.DAY,
            date = dateRange.date,
            startDate = dateRange.startDate,
            max = if (max > 0) max.toUInt() else null,
            summarize = true
        )
    }

    override suspend fun fetchFileDownloads(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int
    ): FileDownloadsDataResult {
        val params = buildFileDownloadsParams(dateRange, max)
        val result = getOrCreateClient().request { api ->
            api.statsFileDownloads().getStatsFileDownloads(
                wpComSiteId = siteId.toULong(),
                params = params
            )
        }
        logResultType("fetchFileDownloads", result)
        return when (result) {
            is WpRequestResult.Success -> {
                val files = result.response.data
                    .summary?.files.orEmpty()
                AppLog.d(
                    T.STATS,
                    "StatsDataSourceImpl: " +
                        "fetchFileDownloads success " +
                        "- ${files.size} files"
                )
                FileDownloadsDataResult.Success(
                    files.map { entry ->
                        FileDownloadDataItem(
                            name = entry.filename
                                .orEmpty(),
                            downloads =
                                entry.downloads?.toLong()
                                    ?: 0L
                        )
                    }
                )
            }
            else -> logErrorAndReturn(
                "fetchFileDownloads", result
            ) {
                FileDownloadsDataResult.Error(it)
            }
        }
    }

    private fun logResultType(
        methodName: String,
        result: WpRequestResult<*>
    ) {
        val typeName = resultTypeName(result)
        AppLog.d(
            T.STATS,
            "StatsDataSourceImpl: $methodName " +
                "result type: $typeName"
        )
    }

    private fun resultTypeName(
        result: WpRequestResult<*>
    ): String = when (result) {
        is WpRequestResult.Success -> "Success"
        is WpRequestResult.WpError -> "WpError"
        is WpRequestResult.ResponseParsingError<*> ->
            "ResponseParsingError"
        is WpRequestResult.RequestExecutionFailed<*> ->
            "RequestExecutionFailed"
        is WpRequestResult.InvalidHttpStatusCode<*> ->
            "InvalidHttpStatusCode"
        else -> "Unknown"
    }

    private fun <R> logErrorAndReturn(
        methodName: String,
        result: WpRequestResult<*>,
        errorFactory: (StatsErrorType) -> R
    ): R {
        val (logMessage, errorType) = classifyError(methodName, result)
        AppLog.e(T.STATS, logMessage)
        return errorFactory(errorType)
    }

    private fun classifyError(
        methodName: String,
        result: WpRequestResult<*>
    ): Pair<String, StatsErrorType> = when (result) {
        is WpRequestResult.WpError -> {
            val statusCode = result.statusCode.toInt()
            val errorType = if (
                statusCode == HTTP_FORBIDDEN ||
                statusCode == HTTP_UNAUTHORIZED
            ) {
                StatsErrorType.AUTH_ERROR
            } else {
                StatsErrorType.API_ERROR
            }
            "StatsDataSourceImpl: $methodName WpError " +
                "(status=$statusCode) - ${result.errorMessage}" to
                errorType
        }
        is WpRequestResult.ResponseParsingError<*> -> {
            "StatsDataSourceImpl: $methodName " +
                "ResponseParsingError - $result" to
                StatsErrorType.PARSING_ERROR
        }
        is WpRequestResult.RequestExecutionFailed<*> -> {
            val statusCode = result.statusCode?.toInt()
            val errorType = if (
                statusCode == HTTP_FORBIDDEN ||
                statusCode == HTTP_UNAUTHORIZED
            ) {
                StatsErrorType.AUTH_ERROR
            } else {
                StatsErrorType.NETWORK_ERROR
            }
            "StatsDataSourceImpl: $methodName " +
                "RequestExecutionFailed " +
                "(status=$statusCode, " +
                "reason=${result.reason})" to errorType
        }
        is WpRequestResult.InvalidHttpStatusCode<*> -> {
            "StatsDataSourceImpl: $methodName " +
                "InvalidHttpStatusCode - " +
                "${result.statusCode}" to StatsErrorType.API_ERROR
        }
        else -> {
            "StatsDataSourceImpl: $methodName " +
                "unexpected result - $result" to
                StatsErrorType.UNKNOWN
        }
    }

    companion object {
        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
    }
}
