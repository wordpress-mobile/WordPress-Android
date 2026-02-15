package org.wordpress.android.ui.newstats.datasource

/**
 * Data source interface for fetching stats data.
 * This abstraction allows mocking the data layer in tests without needing access to uniffi objects.
 */
interface StatsDataSource {
    /**
     * Initializes the data source with the access token.
     */
    fun init(accessToken: String)

    /**
     * Fetches stats data for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param unit The time unit for the stats (HOUR, DAY, etc.)
     * @param quantity The number of data points to fetch
     * @param endDate The end date for the stats period (format: yyyy-MM-dd)
     * @return Result containing the stats data or an error
     */
    suspend fun fetchStatsVisits(
        siteId: Long,
        unit: StatsUnit,
        quantity: Int,
        endDate: String
    ): StatsVisitsDataResult

    /**
     * Fetches top posts and pages for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param dateRange The date range parameters for the query
     * @param max Maximum number of items to return
     * @return Result containing the top posts data or an error
     */
    suspend fun fetchTopPostsAndPages(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int = 10
    ): TopPostsDataResult

    /**
     * Fetches referrer stats for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param dateRange The date range parameters for the query
     * @param max Maximum number of items to return
     * @return Result containing the referrers data or an error
     */
    suspend fun fetchReferrers(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int = 10
    ): ReferrersDataResult

    /**
     * Fetches country views stats for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param dateRange The date range parameters for the query
     * @param max Maximum number of countries to return
     * @return Result containing the country views data or an error
     */
    suspend fun fetchCountryViews(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int = 10
    ): CountryViewsDataResult
}

/**
 * Date range parameters for stats queries.
 */
sealed class StatsDateRange {
    /**
     * Preset period using num (number of days) and date (end date, typically today).
     */
    data class Preset(val num: Int, val date: String) : StatsDateRange()

    /**
     * Custom period using startDate and date (end date).
     */
    data class Custom(val startDate: String, val date: String) : StatsDateRange()
}

/**
 * Time unit for stats data.
 */
enum class StatsUnit {
    HOUR,
    DAY,
    WEEK,
    MONTH
}

/**
 * Result wrapper for stats visits fetch operation.
 */
sealed class StatsVisitsDataResult {
    data class Success(val data: StatsVisitsData) : StatsVisitsDataResult()
    data class Error(val message: String) : StatsVisitsDataResult()
}

/**
 * Stats visits data from the API.
 * Contains all the data points for views, visitors, likes, comments, and posts.
 */
data class StatsVisitsData(
    val visits: List<VisitsDataPoint>,
    val visitors: List<VisitorsDataPoint>,
    val likes: List<LikesDataPoint>,
    val comments: List<CommentsDataPoint>,
    val posts: List<PostsDataPoint>
)

/**
 * Data point for visits/views.
 */
data class VisitsDataPoint(
    val period: String,
    val visits: Long
)

/**
 * Data point for visitors.
 */
data class VisitorsDataPoint(
    val period: String,
    val visitors: Long
)

/**
 * Data point for likes.
 */
data class LikesDataPoint(
    val period: String,
    val likes: Long
)

/**
 * Data point for comments.
 */
data class CommentsDataPoint(
    val period: String,
    val comments: Long
)

/**
 * Data point for posts.
 */
data class PostsDataPoint(
    val period: String,
    val posts: Long
)

/**
 * Result wrapper for top posts fetch operation.
 */
sealed class TopPostsDataResult {
    data class Success(val items: List<TopPostDataItem>) : TopPostsDataResult()
    data class Error(val message: String) : TopPostsDataResult()
}

/**
 * A single top post item from the API.
 */
data class TopPostDataItem(
    val id: Long,
    val title: String,
    val views: Long
)

/**
 * Result wrapper for referrers fetch operation.
 */
sealed class ReferrersDataResult {
    data class Success(val items: List<ReferrerDataItem>) : ReferrersDataResult()
    data class Error(val message: String) : ReferrersDataResult()
}

/**
 * A single referrer item from the API.
 */
data class ReferrerDataItem(
    val name: String,
    val views: Long
)

/**
 * Result wrapper for country views fetch operation.
 */
sealed class CountryViewsDataResult {
    data class Success(val data: CountryViewsData) : CountryViewsDataResult()
    data class Error(val message: String) : CountryViewsDataResult()
}

/**
 * Country views data from the API.
 */
data class CountryViewsData(
    val countries: List<CountryViewItem>,
    val totalViews: Long,
    val otherViews: Long
)

/**
 * A single country view item from the API.
 */
data class CountryViewItem(
    val countryCode: String,
    val countryName: String,
    val views: Long,
    val flagIconUrl: String?
)
