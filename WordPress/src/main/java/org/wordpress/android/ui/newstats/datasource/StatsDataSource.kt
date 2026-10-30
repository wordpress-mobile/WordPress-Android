package org.wordpress.android.ui.newstats.datasource

/**
 * Data source interface for fetching stats data.
 * This abstraction allows mocking the data layer in tests without needing access to uniffi objects.
 */
interface StatsDataSource {
    /**
     * Fetches stats data for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param unit The time unit for the stats (HOUR, DAY, etc.)
     * @param quantity The number of data points to fetch
     * @param endDate The end date for the stats period (format: yyyy-MM-dd), sent as the `date` query param
     * @param startDate Optional start date for the stats period (format: yyyy-MM-dd)
     * @param statFields Optional list of stat fields to restrict the response to. When null the API
     * returns its default set of fields.
     * @return Result containing the stats data or an error
     */
    suspend fun fetchStatsVisits(
        siteId: Long,
        unit: StatsUnit,
        quantity: Int,
        endDate: String,
        startDate: String? = null,
        statFields: List<StatsVisitField>? = null
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

    /**
     * Fetches region views stats for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param dateRange The date range parameters for the query
     * @param max Maximum number of regions to return
     * @return Result containing the region views data or an error
     */
    suspend fun fetchRegionViews(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int = 10
    ): RegionViewsDataResult

    /**
     * Fetches city views stats for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param dateRange The date range parameters for the query
     * @param max Maximum number of cities to return
     * @return Result containing the city views data or an error
     */
    suspend fun fetchCityViews(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int = 10
    ): CityViewsDataResult

    /**
     * Fetches top authors stats for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param dateRange The date range parameters for the query
     * @param max Maximum number of authors to return
     * @return Result containing the top authors data or an error
     */
    suspend fun fetchTopAuthors(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int = 10
    ): TopAuthorsDataResult

    /**
     * Fetches clicks stats for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param dateRange The date range parameters for the query
     * @param max Maximum number of items to return
     * @return Result containing the clicks data or an error
     */
    suspend fun fetchClicks(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int = 10
    ): ClicksDataResult

    /**
     * Fetches search terms stats for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param dateRange The date range parameters for the query
     * @param max Maximum number of items to return
     * @return Result containing the search terms data or an error
     */
    suspend fun fetchSearchTerms(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int = 10
    ): SearchTermsDataResult

    /**
     * Fetches video plays stats for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param dateRange The date range parameters for the query
     * @param max Maximum number of items to return
     * @return Result containing the video plays data or an error
     */
    suspend fun fetchVideoPlays(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int = 10
    ): VideoPlaysDataResult

    /**
     * Fetches file downloads stats for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param dateRange The date range parameters for the query
     * @param max Maximum number of items to return
     * @return Result containing the file downloads data or an error
     */
    suspend fun fetchFileDownloads(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int = 10
    ): FileDownloadsDataResult

    /**
     * Fetches archive stats for a specific site: the views a site's archive pages (categories,
     * tags, author and search result pages, and any custom taxonomy) received.
     *
     * @param siteId The WordPress.com site ID
     * @param dateRange The date range parameters for the query
     * @param max Maximum number of items to return
     * @return Result containing the archive data or an error
     */
    suspend fun fetchArchives(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int = 10
    ): ArchivesDataResult

    /**
     * Fetches device screen size stats for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param dateRange The date range parameters for the query
     * @param max Maximum number of items to return
     * @return Result containing the device screen size data or an error
     */
    suspend fun fetchDevicesScreensize(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int = 10
    ): DevicesDataResult

    /**
     * Fetches device browser stats for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param dateRange The date range parameters for the query
     * @param max Maximum number of items to return
     * @return Result containing the device browser data or an error
     */
    suspend fun fetchDevicesBrowser(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int = 10
    ): DevicesDataResult

    /**
     * Fetches device platform stats for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param dateRange The date range parameters for the query
     * @param max Maximum number of items to return
     * @return Result containing the device platform data or an error
     */
    suspend fun fetchDevicesPlatform(
        siteId: Long,
        dateRange: StatsDateRange,
        max: Int = 10
    ): DevicesDataResult

    /**
     * Fetches stats insights for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @return Result containing the insights data or an error
     */
    suspend fun fetchStatsInsights(
        siteId: Long
    ): StatsInsightsDataResult

    /**
     * Fetches stats summary for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @return Result containing the summary data or an error
     */
    suspend fun fetchStatsSummary(
        siteId: Long
    ): StatsSummaryDataResult

    /**
     * Fetches tags and categories stats for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param max Maximum number of tag groups to return
     * @return Result containing the tags data or an error
     */
    suspend fun fetchStatsTags(
        siteId: Long,
        max: Int = 10
    ): StatsTagsDataResult

    /**
     * Fetches view stats for a single post.
     *
     * @param siteId The WordPress.com site ID
     * @param postId The post ID; 0 is the site's home page
     * @return Result containing the post's view data or an error
     */
    suspend fun fetchPostViews(
        siteId: Long,
        postId: Long
    ): PostViewsDataResult

    /**
     * Fetches subscriber count stats for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param quantity Number of data points to return
     * @param unit Time unit: "day", "week", "month", "year"
     * @param date Optional date in YYYY-MM-DD format
     * @return Result containing subscriber count data
     */
    suspend fun fetchStatsSubscribers(
        siteId: Long,
        quantity: Int = 1,
        unit: String? = null,
        date: String? = null
    ): StatsSubscribersDataResult

    /**
     * Fetches a list of subscribers by user type.
     *
     * @param siteId The WordPress.com site ID
     * @param perPage Number of subscribers per page
     * @param page Page number (1-based)
     * @return Result containing subscriber items or an error
     */
    suspend fun fetchSubscribersByUserType(
        siteId: Long,
        perPage: Int = 10,
        page: Int = 1
    ): SubscribersByUserTypeDataResult

    /**
     * Fetches email stats summary for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param quantity Number of email items to return
     * @return Result containing email summary items or an error
     */
    suspend fun fetchStatsEmailsSummary(
        siteId: Long,
        quantity: Int = 10
    ): StatsEmailsSummaryDataResult

    /**
     * Fetches UTM stats for a specific site.
     *
     * @param siteId The WordPress.com site ID
     * @param keys UTM key names to query
     * @param date End date for the query (format: yyyy-MM-dd)
     * @param days Number of days to include
     * @param max Maximum number of items to return (0 = no limit)
     * @param queryTopPosts Whether to include top posts
     * @return Result containing UTM data or an error
     */
    suspend fun fetchUtm(
        siteId: Long,
        keys: List<String>,
        date: String,
        days: Int,
        max: Int = 0,
        queryTopPosts: Boolean = true
    ): UtmDataResult
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
    MONTH,
    YEAR
}

/**
 * A stat field that can be requested from the stats visits endpoint.
 * Mirrors the fields exposed by the response accessors.
 */
enum class StatsVisitField {
    VIEWS,
    VISITORS,
    LIKES,
    REBLOGS,
    COMMENTS,
    POSTS
}

/**
 * Result wrapper for stats visits fetch operation.
 */
sealed class StatsVisitsDataResult {
    data class Success(val data: StatsVisitsData) : StatsVisitsDataResult()
    data class Error(val errorType: StatsErrorType) : StatsVisitsDataResult()
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
    data class Error(val errorType: StatsErrorType) : TopPostsDataResult()
}

/**
 * A single top post item from the API.
 */
data class TopPostDataItem(
    val id: Long,
    val title: String,
    val views: Long,
    val url: String? = null
)

/**
 * Result wrapper for referrers fetch operation.
 */
sealed class ReferrersDataResult {
    data class Success(val items: List<ReferrerDataItem>) : ReferrersDataResult()
    data class Error(val errorType: StatsErrorType) : ReferrersDataResult()
}

/**
 * A single referrer item from the API. [url] is null for a group with no link of its own
 * (e.g. "Search Engines"), whose links live on its [children].
 */
data class ReferrerDataItem(
    val name: String,
    val url: String?,
    val views: Long,
    val children: List<ReferrerChildDataItem> = emptyList()
)

/**
 * A child referrer nested under a referrer group (e.g. a specific search engine).
 */
data class ReferrerChildDataItem(
    val name: String,
    val url: String?,
    val views: Long
)

/**
 * Result wrapper for country views fetch operation.
 */
sealed class CountryViewsDataResult {
    data class Success(val data: CountryViewsData) : CountryViewsDataResult()
    data class Error(val errorType: StatsErrorType) : CountryViewsDataResult()
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

/**
 * Result wrapper for region views fetch operation.
 */
sealed class RegionViewsDataResult {
    data class Success(val data: RegionViewsData) : RegionViewsDataResult()
    data class Error(val errorType: StatsErrorType) : RegionViewsDataResult()
}

/**
 * Region views data from the API.
 */
data class RegionViewsData(
    val regions: List<RegionViewItem>,
    val totalViews: Long,
    val otherViews: Long
)

/**
 * A single region view item from the API.
 */
data class RegionViewItem(
    val location: String,
    val countryCode: String,
    val views: Long,
    val flagIconUrl: String?
)

/**
 * Result wrapper for city views fetch operation.
 */
sealed class CityViewsDataResult {
    data class Success(val data: CityViewsData) : CityViewsDataResult()
    data class Error(val errorType: StatsErrorType) : CityViewsDataResult()
}

/**
 * City views data from the API.
 */
data class CityViewsData(
    val cities: List<CityViewItem>,
    val totalViews: Long,
    val otherViews: Long
)

/**
 * A single city view item from the API.
 */
data class CityViewItem(
    val location: String,
    val countryCode: String,
    val views: Long,
    val latitude: String?,
    val longitude: String?,
    val flagIconUrl: String?
)

/**
 * Result wrapper for top authors fetch operation.
 */
sealed class TopAuthorsDataResult {
    data class Success(val data: TopAuthorsData) : TopAuthorsDataResult()
    data class Error(val errorType: StatsErrorType) : TopAuthorsDataResult()
}

/**
 * Top authors data from the API.
 */
data class TopAuthorsData(
    val authors: List<TopAuthorItem>,
    val totalViews: Long
)

/**
 * A single top author item from the API.
 */
data class TopAuthorItem(
    val name: String,
    val avatarUrl: String?,
    val views: Long
)

/**
 * Result wrapper for clicks fetch operation.
 */
sealed class ClicksDataResult {
    data class Success(
        val items: List<ClickDataItem>
    ) : ClicksDataResult()
    data class Error(
        val errorType: StatsErrorType
    ) : ClicksDataResult()
}

/**
 * A single click item from the API. [url] is null for a grouped source with no link of its own,
 * whose links live on its [children].
 */
data class ClickDataItem(
    val name: String,
    val url: String?,
    val clicks: Long,
    val children: List<ClickChildDataItem> = emptyList()
)

data class ClickChildDataItem(
    val name: String,
    val url: String?,
    val clicks: Long
)

/**
 * Result wrapper for search terms fetch operation.
 */
sealed class SearchTermsDataResult {
    data class Success(
        val items: List<SearchTermDataItem>
    ) : SearchTermsDataResult()
    data class Error(
        val errorType: StatsErrorType
    ) : SearchTermsDataResult()
}

/**
 * A single search term item from the API.
 */
data class SearchTermDataItem(
    val name: String,
    val views: Long
)

/**
 * Result wrapper for video plays fetch operation.
 */
sealed class VideoPlaysDataResult {
    data class Success(
        val items: List<VideoPlayDataItem>
    ) : VideoPlaysDataResult()
    data class Error(
        val errorType: StatsErrorType
    ) : VideoPlaysDataResult()
}

/**
 * A single video play item from the API.
 */
data class VideoPlayDataItem(
    val title: String,
    val views: Long
)

/**
 * Result wrapper for file downloads fetch operation.
 */
sealed class FileDownloadsDataResult {
    data class Success(
        val items: List<FileDownloadDataItem>
    ) : FileDownloadsDataResult()
    data class Error(
        val errorType: StatsErrorType
    ) : FileDownloadsDataResult()
}

/**
 * A single file download item from the API.
 */
data class FileDownloadDataItem(
    val name: String,
    val downloads: Long
)

/**
 * Result wrapper for archive stats fetch operation.
 */
sealed class ArchivesDataResult {
    data class Success(
        val sections: List<ArchiveSectionDataItem>
    ) : ArchivesDataResult()
    data class Error(
        val errorType: StatsErrorType
    ) : ArchivesDataResult()
}

/**
 * The [ArchiveSectionDataItem.key] of the one group that holds every custom taxonomy the site has.
 * The API reports each taxonomy separately in its own `tax` map, and they are gathered under a
 * single group, as the web does, so a site with a dozen taxonomies doesn't bury its built-in
 * groups -- one entry per taxonomy, its terms nested under it.
 *
 * The name is borrowed from the API's own key for that map, which is never a built-in group key,
 * so the synthesised group can't collide with a real one.
 */
const val ARCHIVE_SECTION_KEY_TAXONOMIES = "tax"

/**
 * One group of archive pages. [key] is the API's own name for the group -- one of its short
 * built-in keys ("search", "cat", "tag", "author", "date", "err", "multiple", "other") or
 * [ARCHIVE_SECTION_KEY_TAXONOMIES] -- which is what the UI resolves into a label.
 *
 * [views] is the total across every entry kept for the group, including any dropped from [entries]
 * by the display cap, so the group's own number stays right however many of its entries are listed
 * under it. Entries the API returned with no term are not counted: they have no label to show, and
 * leaving them out is what the web does too.
 */
data class ArchiveSectionDataItem(
    val key: String,
    val views: Long,
    val entries: List<ArchiveEntryDataItem>
)

/**
 * A single archive page within a group, e.g. one category. [value] is the term itself, percent-
 * decoded as the web decodes it, and [href] the archive page it links to -- left encoded, being a
 * URL to open.
 *
 * [children] is only used by [ARCHIVE_SECTION_KEY_TAXONOMIES], whose entries are the site's custom
 * taxonomies rather than pages: there, [value] is the taxonomy's name, [href] is null and the
 * taxonomy's terms are the children. Empty for every other group, whose entries are pages already.
 */
data class ArchiveEntryDataItem(
    val value: String,
    val href: String?,
    val views: Long,
    val children: List<ArchiveEntryDataItem> = emptyList()
)

/**
 * Result wrapper for devices stats fetch operation.
 */
sealed class DevicesDataResult {
    data class Success(val data: DevicesData) : DevicesDataResult()
    data class Error(val errorType: StatsErrorType) : DevicesDataResult()
}

/**
 * Devices data from the API.
 * Contains top values as a map of device name to its value
 * (percentage for screen size, view count for browser/platform).
 */
data class DevicesData(val items: Map<String, Double>)

/**
 * Result wrapper for stats insights fetch operation.
 */
sealed class StatsInsightsDataResult {
    data class Success(
        val data: StatsInsightsData
    ) : StatsInsightsDataResult()
    data class Error(
        val errorType: StatsErrorType
    ) : StatsInsightsDataResult()
}

/**
 * Stats insights data from the API.
 */
data class StatsInsightsData(
    val highestHour: Int,
    val highestHourPercent: Double,
    val highestDayOfWeek: Int,
    val highestDayPercent: Double,
    val years: List<YearInsightsData>
)

/**
 * A single year's insights summary.
 */
data class YearInsightsData(
    val year: String,
    val totalPosts: Long,
    val totalWords: Long,
    val avgWords: Double,
    val totalLikes: Long,
    val avgLikes: Double,
    val totalComments: Long,
    val avgComments: Double
)

/**
 * Result wrapper for stats summary fetch operation.
 */
sealed class StatsSummaryDataResult {
    data class Success(
        val data: StatsSummaryData
    ) : StatsSummaryDataResult()
    data class Error(
        val errorType: StatsErrorType
    ) : StatsSummaryDataResult()
}

/**
 * All-time stats summary data from the API.
 */
data class StatsSummaryData(
    val views: Long,
    val visitors: Long,
    val posts: Long,
    val comments: Long,
    val viewsBestDay: String,
    val viewsBestDayTotal: Long
)

/**
 * Result wrapper for stats tags fetch operation.
 */
sealed class StatsTagsDataResult {
    data class Success(
        val data: StatsTagsData
    ) : StatsTagsDataResult()
    data class Error(
        val errorType: StatsErrorType
    ) : StatsTagsDataResult()
}

/**
 * Tags and categories data from the API.
 */
data class StatsTagsData(
    val tagGroups: List<TagGroupData>
)

/**
 * A group of tags associated with views.
 */
data class TagGroupData(
    val tags: List<TagData>,
    val views: Long
)

/**
 * A single tag or category item. [link] is the tag's archive page, opened when the row is tapped,
 * and is null when the tag has none.
 */
data class TagData(
    val tagType: String,
    val name: String,
    val link: String? = null
)

/**
 * Result wrapper for the per-post views fetch operation.
 */
sealed class PostViewsDataResult {
    data class Success(
        val data: PostViewsData
    ) : PostViewsDataResult()
    data class Error(
        val errorType: StatsErrorType
    ) : PostViewsDataResult()
}

/**
 * View stats for a single post, along with the post's own metadata. The API returns both in one
 * response, so no separate post fetch is needed.
 *
 * Post ID 0 is the site's home page, which isn't a post -- it has view stats but no [post].
 */
data class PostViewsData(
    val postId: Long,
    val totalViews: Long,
    /** The post's complete daily view history, oldest first. */
    val dailyViews: List<PostViewsDailyView>,
    /** The most recent weeks of daily views, most recent first. */
    val weeks: List<PostViewsWeek>,
    /** Yearly totals, most recent year first. */
    val years: List<PostViewsYear>,
    /** Yearly averages, most recent year first. */
    val averages: List<PostViewsYearAverage>,
    /** The post these stats belong to, or null for the site's home page. */
    val post: PostViewsPost?
)

/**
 * A single day's view count. The day is kept so the chart can label its range.
 */
data class PostViewsDailyView(
    /** The day the views were recorded on (format: yyyy-MM-dd). */
    val day: String,
    val views: Long
)

/**
 * The editorial metadata of the post whose stats were fetched.
 */
data class PostViewsPost(
    val title: String,
    /** Publication date in the site's timezone (format: yyyy-MM-dd HH:mm:ss). */
    val date: String,
    val likeCount: Long,
    val commentCount: Long,
    /**
     * The item's public URL, or null when the API supplies no permalink -- it is derived per
     * request rather than stored, so it can be missing even for an item that exists.
     */
    val url: String?,
    /** The WordPress post type ("post", "page", "attachment"), which decides how [url] is opened. */
    val type: String,
    /**
     * The WordPress post status ("publish", "draft", "trash", ...). An item keeps its view history
     * after it stops being public, so this says whether [url] still resolves for a reader.
     */
    val status: String
)

/**
 * A week's view total, its days, and how the total compares with the week before.
 *
 * [startDay] and [endDay] bound the week (format: yyyy-MM-dd); the most recent week may be
 * partial, in which case [days] stops short of seven.
 */
data class PostViewsWeek(
    val startDay: String,
    val endDay: String,
    val total: Long,
    val change: PostViewsChange,
    /** The week's days, oldest first. */
    val days: List<PostViewsDailyView> = emptyList()
)

/**
 * How a week's views compare with the week before.
 */
sealed class PostViewsChange {
    data class Percentage(
        val value: Double
    ) : PostViewsChange()

    /** The previous week had no views, so the API reports an unbounded change. */
    data object Infinite : PostViewsChange()

    /** The week has no predecessor to compare against. */
    data object None : PostViewsChange()
}

/**
 * A year's view total, with the months it is made of.
 */
data class PostViewsYear(
    val year: String,
    val total: Long,
    /** Views per month, keyed 1-12. The API leaves out months that had none. */
    val months: Map<Int, Long> = emptyMap()
)

/**
 * A year's average daily views, with the months it is made of.
 */
data class PostViewsYearAverage(
    val year: String,
    val overall: Long,
    /** Average daily views per month, keyed 1-12. The API leaves out months that had none. */
    val months: Map<Int, Long> = emptyMap()
)

/**
 * Result wrapper for stats subscribers fetch operation.
 */
sealed class StatsSubscribersDataResult {
    data class Success(
        val data: StatsSubscribersData
    ) : StatsSubscribersDataResult()
    data class Error(
        val errorType: StatsErrorType
    ) : StatsSubscribersDataResult()
}

/**
 * Stats subscribers data from the API.
 */
data class StatsSubscribersData(
    val subscribersData: List<SubscribersDataPoint>
)

/**
 * A single data point for subscriber count at a date.
 */
data class SubscribersDataPoint(
    val date: String,
    val count: Long
)

/**
 * Result wrapper for subscribers by user type fetch operation.
 */
sealed class SubscribersByUserTypeDataResult {
    data class Success(
        val items: List<SubscriberItem>
    ) : SubscribersByUserTypeDataResult()
    data class Error(
        val errorType: StatsErrorType
    ) : SubscribersByUserTypeDataResult()
}

/**
 * A single subscriber item.
 */
data class SubscriberItem(
    val displayName: String,
    val subscribedSince: String
)

/**
 * Result wrapper for stats emails summary fetch operation.
 */
sealed class StatsEmailsSummaryDataResult {
    data class Success(
        val items: List<EmailSummaryItem>
    ) : StatsEmailsSummaryDataResult()
    data class Error(
        val errorType: StatsErrorType
    ) : StatsEmailsSummaryDataResult()
}

/**
 * A single email summary item.
 */
data class EmailSummaryItem(
    val title: String,
    val opens: Long,
    val clicks: Long
)

/**
 * Result wrapper for UTM stats fetch operation.
 */
sealed class UtmDataResult {
    data class Success(val data: UtmData) : UtmDataResult()
    data class Error(
        val errorType: StatsErrorType
    ) : UtmDataResult()
}

/**
 * UTM stats data from the API.
 */
data class UtmData(
    val topUtmValues: Map<String, Long>,
    val topPosts: Map<String, List<UtmPostData>>
)

/**
 * A single UTM post item from the API.
 */
data class UtmPostData(
    val title: String,
    val views: Long
)
