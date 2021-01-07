package org.wordpress.android.util.analytics

import androidx.core.util.Pair
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.ACTIVITY_LOG_FILTER_BAR_ACTIVITY_TYPE_SELECTED
import org.wordpress.android.fluxc.model.activity.ActivityTypeModel
import org.wordpress.android.util.DateTimeUtilsWrapper
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityLogTracker @Inject constructor(
    private val tracker: AnalyticsTrackerWrapper,
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper
) {
    fun trackDateRangeFilterButtonClicked() {
        tracker.track(Stat.ACTIVITY_LOG_FILTER_BAR_DATE_RANGE_BUTTON_TAPPED)
    }

    fun trackDateRangeFilterSelected(dateRange: Pair<Long, Long>?) {
        val start = dateRange?.first
        val end = dateRange?.second
        if (start == null || end == null) {
            trackDateRangeFilterCleared()
        } else {
            val map = mutableMapOf<String, Any>()
            // Number of selected days
            map["duration"] = dateTimeUtilsWrapper.daysBetween(Date(start), Date(end)) + 1
            // Distance from the startDate to today (in days)
            map["distance"] = dateTimeUtilsWrapper.daysBetween(Date(start), Date())
            tracker.track(Stat.ACTIVITY_LOG_FILTER_BAR_DATE_RANGE_SELECTED, map)
        }
    }

    fun trackDateRangeFilterCleared() {
        tracker.track(Stat.ACTIVITY_LOG_FILTER_BAR_DATE_RANGE_RESET)
    }

    fun trackActivityTypeFilterButtonClicked() {
        tracker.track(Stat.ACTIVITY_LOG_FILTER_BAR_ACTIVITY_TYPE_BUTTON_TAPPED)
    }

    fun trackActivityTypeFilterSelected(selectedTypes: List<ActivityTypeModel>) {
        if (selectedTypes.isEmpty()) {
            trackActivityTypeFilterCleared()
        } else {
            val map = mutableMapOf<String, Any>()
            map["num_groups_selected"] = selectedTypes.size.toString()
            map["num_total_activities_selected"] = selectedTypes.map { it.count }.reduce { acc, count -> acc + count }
            selectedTypes.forEach { map["group_${it.key}"] = true }
            tracker.track(ACTIVITY_LOG_FILTER_BAR_ACTIVITY_TYPE_SELECTED, map)
        }
    }

    fun trackActivityTypeFilterCleared() {
        tracker.track(Stat.ACTIVITY_LOG_FILTER_BAR_ACTIVITY_TYPE_RESET)
    }
}
