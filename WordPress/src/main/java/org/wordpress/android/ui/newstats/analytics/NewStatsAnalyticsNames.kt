package org.wordpress.android.ui.newstats.analytics

import org.wordpress.android.ui.newstats.InsightsCardType
import org.wordpress.android.ui.newstats.StatsCardType
import org.wordpress.android.ui.newstats.StatsPeriod
import org.wordpress.android.ui.newstats.StatsTab
import org.wordpress.android.ui.newstats.datasource.StatsUnit
import org.wordpress.android.ui.newstats.devices.DeviceType
import org.wordpress.android.ui.newstats.locations.LocationType
import org.wordpress.android.ui.newstats.subscribers.SubscribersCardType
import org.wordpress.android.ui.newstats.subscribers.subscribersgraph.SubscribersGraphTab
import org.wordpress.android.ui.newstats.utm.UtmCategory
import org.wordpress.android.ui.newstats.viewsstats.ChartType
import org.wordpress.android.ui.newstats.viewsstats.StatsMetric

/**
 * The strings New Stats reports its own types as, kept in one place so an event's property values
 * can't drift from each other between call sites.
 *
 * Every value here matches the `analyticsName` the iOS JetpackStats module reports for the same
 * concept (see `StatsEvent.swift`), so a dashboard can read both platforms as one series. Where a
 * name reads oddly for Android it is because iOS already ships it that way - the mismatches are
 * called out individually below. Nothing here is a display string: none of it is localized, and
 * none of it should be shown to a user.
 */

/** Matches iOS `StatsTab.analyticsName`. */
internal val StatsTab.analyticsName: String
    get() = when (this) {
        StatsTab.TRAFFIC -> "traffic"
        StatsTab.INSIGHTS -> "insights"
        StatsTab.SUBSCRIBERS -> "subscribers"
    }

/**
 * Matches iOS `DateIntervalPreset.analyticsName`, which is the same set of snake_case preset keys
 * [StatsPeriod.toTypeString] already persists - so the stored period and the reported one can't
 * disagree. A custom range reports as `custom`.
 */
internal val StatsPeriod.analyticsName: String
    get() = toTypeString()

/** Matches iOS `SiteMetric.analyticsName`, which is also this enum's persisted storage key. */
internal val StatsMetric.analyticsName: String
    get() = storageKey

/** Matches iOS `ChartType.rawValue`, which is also this enum's persisted storage key. */
internal val ChartType.analyticsName: String
    get() = storageKey

/** Matches iOS `DateRangeGranularity.analyticsName` - singular, unlike this enum's own names. */
internal val StatsUnit.analyticsName: String
    get() = when (this) {
        StatsUnit.HOUR -> "hour"
        StatsUnit.DAY -> "day"
        StatsUnit.WEEK -> "week"
        StatsUnit.MONTH -> "month"
        StatsUnit.YEAR -> "year"
    }

/**
 * Matches iOS `TopListItemType.analyticsName` for every card that exists on both platforms, which
 * is why two of these don't read like their enum entry:
 *
 * - [StatsCardType.CLICKS] reports as `external_links`. Both platforms show the same thing (clicks
 *   on outbound links); iOS names the type after the link rather than the tap.
 * - [StatsCardType.VIDEO_PLAYS] reports as `videos`.
 *
 * The two cards with no top list behind them take iOS's card names instead: Today's stats is
 * `today`, and the Views card is `chart`.
 */
internal val StatsCardType.analyticsName: String
    get() = when (this) {
        StatsCardType.TODAYS_STATS -> "today"
        StatsCardType.VIEWS_STATS -> "chart"
        StatsCardType.MOST_VIEWED_POSTS_AND_PAGES -> "posts_and_pages"
        StatsCardType.MOST_VIEWED_REFERRERS -> "referrers"
        StatsCardType.LOCATIONS -> "locations"
        StatsCardType.AUTHORS -> "authors"
        StatsCardType.CLICKS -> "external_links"
        StatsCardType.SEARCH_TERMS -> "search_terms"
        StatsCardType.VIDEO_PLAYS -> "videos"
        StatsCardType.FILE_DOWNLOADS -> "file_downloads"
        StatsCardType.DEVICES -> "devices"
        StatsCardType.UTM -> "utm"
    }

/** Insights cards are Android-only, so these names have no iOS counterpart to match. */
internal val InsightsCardType.analyticsName: String
    get() = when (this) {
        InsightsCardType.YEAR_IN_REVIEW -> "year_in_review"
        InsightsCardType.ALL_TIME_STATS -> "all_time_stats"
        InsightsCardType.LATEST_POST -> "latest_post"
        InsightsCardType.MOST_POPULAR_DAY -> "most_popular_day"
        InsightsCardType.MOST_POPULAR_TIME -> "most_popular_time"
        InsightsCardType.TAGS_AND_CATEGORIES -> "tags_and_categories"
    }

/** Subscribers cards are Android-only, so these names have no iOS counterpart to match. */
internal val SubscribersCardType.analyticsName: String
    get() = when (this) {
        SubscribersCardType.ALL_TIME_SUBSCRIBERS -> "all_time_subscribers"
        SubscribersCardType.SUBSCRIBERS_GRAPH -> "subscribers_graph"
        SubscribersCardType.SUBSCRIBERS_LIST -> "subscribers_list"
        SubscribersCardType.EMAILS -> "emails"
    }

/** Matches iOS `LocationLevel.analyticsName` (its raw values, which are plural). */
internal val LocationType.analyticsName: String
    get() = when (this) {
        LocationType.COUNTRIES -> "countries"
        LocationType.REGIONS -> "regions"
        LocationType.CITIES -> "cities"
    }

/** Matches iOS `DeviceBreakdown.analyticsName`. */
internal val DeviceType.analyticsName: String
    get() = when (this) {
        DeviceType.SCREENSIZE -> "screensize"
        DeviceType.BROWSER -> "browser"
        DeviceType.PLATFORM -> "platform"
    }

/**
 * Matches iOS `UTMParamGrouping.analyticsName`, which is its camelCase raw value. Deliberately not
 * snake_case like everything else here: these are the values iOS has already been reporting.
 */
internal val UtmCategory.analyticsName: String
    get() = when (this) {
        UtmCategory.SOURCE_MEDIUM -> "sourceMedium"
        UtmCategory.CAMPAIGN_SOURCE_MEDIUM -> "campaignSourceMedium"
        UtmCategory.SOURCE -> "source"
        UtmCategory.MEDIUM -> "medium"
        UtmCategory.CAMPAIGN -> "campaign"
    }

/**
 * The Subscribers chart's range selector. Each tab is a fixed span of one granularity (30 days, 12
 * weeks, 6 months, 3 years), so the granularity alone names the tab - and it is already the string
 * the tab sends to the API, which happens to be the same singular form [StatsUnit] reports.
 */
internal val SubscribersGraphTab.analyticsName: String
    get() = unit
