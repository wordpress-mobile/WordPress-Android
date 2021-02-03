package org.wordpress.android.util.analytics

import androidx.core.util.Pair
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.analytics.AnalyticsTracker.Stat.ACTIVITY_LOG_FILTER_BAR_ACTIVITY_TYPE_SELECTED
import org.wordpress.android.fluxc.model.SiteModel
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
    fun trackDateRangeFilterButtonClicked(rewindableOnly: Boolean) {
        if (rewindableOnly) {
            tracker.track(Stat.JETPACK_BACKUP_FILTER_BAR_DATE_RANGE_BUTTON_TAPPED)
        } else {
            tracker.track(Stat.ACTIVITY_LOG_FILTER_BAR_DATE_RANGE_BUTTON_TAPPED)
        }
    }

    fun trackDateRangeFilterSelected(dateRange: Pair<Long, Long>?, rewindableOnly: Boolean) {
        val start = dateRange?.first
        val end = dateRange?.second
        if (start == null || end == null) {
            trackDateRangeFilterCleared(rewindableOnly)
        } else {
            val map = mutableMapOf<String, Any>()
            // Number of selected days
            map["duration"] = dateTimeUtilsWrapper.daysBetween(Date(start), Date(end)) + 1
            // Distance from the startDate to today (in days)
            map["distance"] = dateTimeUtilsWrapper.daysBetween(Date(start), Date())
            if (rewindableOnly) {
                tracker.track(Stat.JETPACK_BACKUP_FILTER_BAR_DATE_RANGE_SELECTED, map)
            } else {
                tracker.track(Stat.ACTIVITY_LOG_FILTER_BAR_DATE_RANGE_SELECTED, map)
            }
        }
    }

    fun trackDateRangeFilterCleared(rewindableOnly: Boolean) {
        if (rewindableOnly) {
            tracker.track(Stat.JETPACK_BACKUP_FILTER_BAR_DATE_RANGE_RESET)
        } else {
            tracker.track(Stat.ACTIVITY_LOG_FILTER_BAR_DATE_RANGE_RESET)
        }
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

    fun trackRestoreStarted(rewindId: String, site: SiteModel, rewindableOnly: Boolean) {
        val properties = mapOf("rewind_id" to rewindId)
        if (rewindableOnly) {
            AnalyticsUtils.trackWithSiteDetails(Stat.JETPACK_BACKUP_REWIND_STARTED, site, properties)
        } else {
            AnalyticsUtils.trackWithSiteDetails(Stat.ACTIVITY_LOG_REWIND_STARTED, site, properties)
        }
    }
}
