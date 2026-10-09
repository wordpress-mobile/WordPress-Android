package org.wordpress.android.ui.newstats.analytics

import dagger.Reusable
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.mysite.SelectedSiteRepository
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
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Every Tracks event New Stats reports, as one typed call per interaction.
 *
 * Why a class rather than extension functions on [AnalyticsTrackerWrapper] (the pattern old Stats
 * uses in `StatsAnalyticsUtils`): the site is resolved here, once, so no call site can forget it.
 * That is what CMM-2273 had to go back and fix across 42 old-Stats events, and New Stats is being
 * built out faster than that pass could keep up with.
 *
 * Resolving the site from [SelectedSiteRepository] is correct everywhere in New Stats: the screen
 * always renders the selected site, and `NewStatsActivity.onCreate` applies the Intent's site
 * before it tracks anything, so there is no window where this returns the previous one.
 *
 * Event names and property values mirror the iOS JetpackStats module - see
 * [NewStatsAnalyticsNames] for the value mapping and what deviates.
 *
 * No event carries anything identifying: no post, author or subscriber ids, no titles, no URLs.
 * Only the type of thing interacted with, and counts already aggregated by the API.
 */
@Reusable
class NewStatsTracker @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val selectedSiteRepository: SelectedSiteRepository
) {
    /** Which way the date navigation arrows moved the range. */
    enum class NavigationDirection(val analyticsName: String) {
        PREVIOUS("previous"),
        NEXT("next")
    }

    /** Which card-reordering menu item was used. Matches iOS `MoveDirection.analyticsName`. */
    enum class CardMoveDirection(val analyticsName: String) {
        UP("move_up"),
        DOWN("move_down"),
        TOP("move_to_top"),
        BOTTOM("move_to_bottom")
    }

    // region Screens

    fun trackMainScreenShown() = track(Stat.JETPACK_STATS_MAIN_SCREEN_SHOWN)

    /**
     * The tab now on screen. Reported on arrival, including the tab New Stats opens on - unlike
     * [trackTabSelected], which only fires when the user moves between tabs.
     */
    fun trackTabShown(tab: StatsTab) = track(
        when (tab) {
            StatsTab.TRAFFIC -> Stat.JETPACK_STATS_TRAFFIC_TAB_SHOWN
            StatsTab.INSIGHTS -> Stat.JETPACK_STATS_INSIGHTS_TAB_SHOWN
            StatsTab.SUBSCRIBERS -> Stat.JETPACK_STATS_SUBSCRIBERS_TAB_SHOWN
        }
    )

    fun trackTabSelected(from: StatsTab, to: StatsTab) = track(
        Stat.JETPACK_STATS_TAB_SELECTED,
        mapOf(
            TAB_NAME to to.analyticsName,
            PREVIOUS_TAB to from.analyticsName
        )
    )

    fun trackPostDetailsScreenShown() = track(Stat.JETPACK_STATS_POST_DETAILS_SCREEN_SHOWN)

    fun trackDetailScreenShown(cardType: StatsCardType) =
        trackDetailScreenShown(cardType.analyticsName)

    fun trackDetailScreenShown(cardType: InsightsCardType) =
        trackDetailScreenShown(cardType.analyticsName)

    fun trackDetailScreenShown(cardType: SubscribersCardType) =
        trackDetailScreenShown(cardType.analyticsName)

    private fun trackDetailScreenShown(screen: String) =
        track(Stat.JETPACK_STATS_DETAIL_SCREEN_SHOWN, mapOf(SCREEN to screen))

    // endregion

    // region Date range

    fun trackDateRangePresetSelected(period: StatsPeriod) = track(
        Stat.JETPACK_STATS_DATE_RANGE_PRESET_SELECTED,
        mapOf(SELECTED_PRESET to period.analyticsName)
    )

    /** Dates go out as plain ISO calendar dates - a range the user picked, not a timestamp. */
    fun trackCustomDateRangeSelected(startDate: LocalDate, endDate: LocalDate) = track(
        Stat.JETPACK_STATS_CUSTOM_DATE_RANGE_SELECTED,
        mapOf(
            START_DATE to DateTimeFormatter.ISO_LOCAL_DATE.format(startDate),
            END_DATE to DateTimeFormatter.ISO_LOCAL_DATE.format(endDate)
        )
    )

    /**
     * [currentPeriod] is the range being left, so a sequence of taps reads as a path rather than
     * as a run of identical events.
     */
    fun trackDateNavigationButtonTapped(
        direction: NavigationDirection,
        currentPeriod: StatsPeriod
    ) = track(
        Stat.JETPACK_STATS_DATE_NAVIGATION_BUTTON_TAPPED,
        mapOf(
            DIRECTION to direction.analyticsName,
            CURRENT_PERIOD_TYPE to currentPeriod.analyticsName
        )
    )

    // endregion

    // region Cards

    /**
     * Traffic cards report the iOS card *kind* as `card_type` - `today`, `chart` or `top_list` -
     * so the event can be read across platforms, and the list itself as `item_type`, which is the
     * breakdown iOS has no card type for. Insights and Subscribers cards exist only on Android, so
     * they report their own name as `card_type` and carry no `item_type`.
     */
    fun trackCardAdded(cardType: StatsCardType) =
        trackCardAdded(cardType.cardAnalyticsName, cardType.itemTypeOrNull())

    fun trackCardAdded(cardType: InsightsCardType) = trackCardAdded(cardType.analyticsName)

    fun trackCardAdded(cardType: SubscribersCardType) = trackCardAdded(cardType.analyticsName)

    /** See [trackCardAdded] for how a Traffic card's `card_type` and `item_type` are split. */
    fun trackCardRemoved(cardType: StatsCardType) =
        trackCardRemoved(cardType.cardAnalyticsName, cardType.itemTypeOrNull())

    fun trackCardRemoved(cardType: InsightsCardType) = trackCardRemoved(cardType.analyticsName)

    fun trackCardRemoved(cardType: SubscribersCardType) = trackCardRemoved(cardType.analyticsName)

    /** See [trackCardAdded] for how a Traffic card's `card_type` and `item_type` are split. */
    fun trackCardMoved(
        cardType: StatsCardType,
        direction: CardMoveDirection,
        fromIndex: Int,
        toIndex: Int
    ) = trackCardMoved(
        cardType.cardAnalyticsName,
        direction,
        fromIndex,
        toIndex,
        cardType.itemTypeOrNull()
    )

    fun trackCardMoved(
        cardType: InsightsCardType,
        direction: CardMoveDirection,
        fromIndex: Int,
        toIndex: Int
    ) = trackCardMoved(cardType.analyticsName, direction, fromIndex, toIndex)

    fun trackCardMoved(
        cardType: SubscribersCardType,
        direction: CardMoveDirection,
        fromIndex: Int,
        toIndex: Int
    ) = trackCardMoved(cardType.analyticsName, direction, fromIndex, toIndex)

    private fun trackCardAdded(cardType: String, itemType: String? = null) = track(
        Stat.JETPACK_STATS_CARD_ADDED,
        cardProperties(cardType, itemType)
    )

    private fun trackCardRemoved(cardType: String, itemType: String? = null) = track(
        Stat.JETPACK_STATS_CARD_REMOVED,
        cardProperties(cardType, itemType)
    )

    private fun trackCardMoved(
        cardType: String,
        direction: CardMoveDirection,
        fromIndex: Int,
        toIndex: Int,
        itemType: String? = null
    ) = track(
        Stat.JETPACK_STATS_CARD_MOVED,
        cardProperties(cardType, itemType) + mapOf(
            ACTION to direction.analyticsName,
            FROM_INDEX to fromIndex,
            TO_INDEX to toIndex
        )
    )

    private fun cardProperties(cardType: String, itemType: String?): Map<String, Any?> = buildMap {
        put(CARD_TYPE, cardType)
        itemType?.let { put(ITEM_TYPE, it) }
    }

    /** The list behind a Traffic card, or null for the two cards that have no list. */
    private fun StatsCardType.itemTypeOrNull(): String? = when (this) {
        StatsCardType.TODAYS_STATS, StatsCardType.VIEWS_STATS -> null
        else -> analyticsName
    }

    // endregion

    // region Chart

    fun trackChartTypeChanged(from: ChartType, to: ChartType) = track(
        Stat.JETPACK_STATS_CHART_TYPE_CHANGED,
        mapOf(
            FROM_TYPE to from.analyticsName,
            TO_TYPE to to.analyticsName
        )
    )

    fun trackChartMetricSelected(metric: StatsMetric) = track(
        Stat.JETPACK_STATS_CHART_METRIC_SELECTED,
        mapOf(METRIC to metric.analyticsName)
    )

    /**
     * A bar tapped on the Views chart. [unit] is the width of one bar, which is what says whether
     * the tap could drill down any further; [value] is the bar's own total for [metric].
     */
    fun trackChartBarSelected(metric: StatsMetric, unit: StatsUnit, value: Long) = track(
        Stat.JETPACK_STATS_CHART_BAR_SELECTED,
        mapOf(
            FROM_GRANULARITY to unit.analyticsName,
            METRIC to metric.analyticsName,
            VALUE to value
        )
    )

    // endregion

    // region Lists

    /**
     * A row tapped in any stats list, on a card or on a detail screen. iOS additionally reports a
     * `metric` here, because its top lists are ranked by a metric the reader picks; Android's are
     * not, so there is nothing to report until they are.
     */
    fun trackTopListItemTapped(cardType: StatsCardType) =
        trackTopListItemTapped(cardType.analyticsName)

    fun trackTopListItemTapped(cardType: InsightsCardType) =
        trackTopListItemTapped(cardType.analyticsName)

    private fun trackTopListItemTapped(itemType: String) = track(
        Stat.JETPACK_STATS_TOP_LIST_ITEM_TAPPED,
        mapOf(ITEM_TYPE to itemType)
    )

    fun trackLocationLevelChanged(from: LocationType, to: LocationType) = track(
        Stat.JETPACK_STATS_LOCATION_LEVEL_CHANGED,
        mapOf(
            FROM_LEVEL to from.analyticsName,
            TO_LEVEL to to.analyticsName
        )
    )

    fun trackDeviceBreakdownChanged(from: DeviceType, to: DeviceType) = track(
        Stat.JETPACK_STATS_DEVICE_BREAKDOWN_CHANGED,
        mapOf(
            FROM_BREAKDOWN to from.analyticsName,
            TO_BREAKDOWN to to.analyticsName
        )
    )

    fun trackUtmParamGroupingChanged(from: UtmCategory, to: UtmCategory) = track(
        Stat.JETPACK_STATS_UTM_PARAM_GROUPING_CHANGED,
        mapOf(
            FROM_GROUPING to from.analyticsName,
            TO_GROUPING to to.analyticsName
        )
    )

    /** The Subscribers chart's range selector, reported as the granularity it switched between. */
    fun trackSubscribersChartRangeChanged(from: SubscribersGraphTab, to: SubscribersGraphTab) = track(
        Stat.JETPACK_STATS_SUBSCRIBERS_CHART_RANGE_CHANGED,
        mapOf(
            FROM_GRANULARITY to from.analyticsName,
            TO_GRANULARITY to to.analyticsName
        )
    )

    // endregion

    /**
     * Attaches the viewed site, so every event here carries a `blog_id` without the call site
     * having to remember to pass one (CMM-2273). A null site still tracks: the wrapper drops the
     * site properties and reports the interaction, which is better than losing the event.
     */
    private fun track(stat: Stat, properties: Map<String, Any?>? = null) =
        analyticsTracker.track(stat, selectedSiteRepository.getSelectedSite(), properties)

    companion object {
        private const val TAB_NAME = "tab_name"
        private const val PREVIOUS_TAB = "previous_tab"
        private const val SCREEN = "screen"
        private const val SELECTED_PRESET = "selected_preset"
        private const val START_DATE = "start_date"
        private const val END_DATE = "end_date"
        private const val DIRECTION = "direction"
        private const val CURRENT_PERIOD_TYPE = "current_period_type"
        private const val CARD_TYPE = "card_type"
        private const val ACTION = "action"
        private const val FROM_INDEX = "from_index"
        private const val TO_INDEX = "to_index"
        private const val FROM_TYPE = "from_type"
        private const val TO_TYPE = "to_type"
        private const val METRIC = "metric"
        private const val FROM_GRANULARITY = "from_granularity"
        private const val TO_GRANULARITY = "to_granularity"
        private const val VALUE = "value"
        private const val ITEM_TYPE = "item_type"
        private const val FROM_LEVEL = "from_level"
        private const val TO_LEVEL = "to_level"
        private const val FROM_BREAKDOWN = "from_breakdown"
        private const val TO_BREAKDOWN = "to_breakdown"
        private const val FROM_GROUPING = "from_grouping"
        private const val TO_GROUPING = "to_grouping"
    }
}

/**
 * The `from, to` index pair moving [card] in [direction] would produce in this list, or null when
 * the move would change nothing.
 *
 * Mirrors the guards the card configuration repositories apply before they write, so the
 * reordering menu items that are inert at the ends of the list don't report a move that never
 * happened. The list is each tab's visible cards, which is the same order the repository holds.
 */
internal fun <T> List<T>.cardMoveIndices(
    card: T,
    direction: NewStatsTracker.CardMoveDirection
): Pair<Int, Int>? {
    val from = indexOf(card)
    if (from < 0) return null
    val isFirst = from == 0
    val isLast = from == size - 1
    // Null for a direction that is inert at this end of the list, so no move is reported.
    val to = when (direction) {
        NewStatsTracker.CardMoveDirection.UP -> if (isFirst) null else from - 1
        NewStatsTracker.CardMoveDirection.TOP -> if (isFirst) null else 0
        NewStatsTracker.CardMoveDirection.DOWN -> if (isLast) null else from + 1
        NewStatsTracker.CardMoveDirection.BOTTOM -> if (isLast) null else size - 1
    }
    return to?.let { from to it }
}
