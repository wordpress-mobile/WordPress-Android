package org.wordpress.android.ui.stats.refresh

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.stats.InsightsMostPopularModel
import org.wordpress.android.fluxc.store.InsightsStore
import org.wordpress.android.ui.stats.refresh.BlockListItem.Empty
import org.wordpress.android.ui.stats.refresh.BlockListItem.ListItem
import org.wordpress.android.ui.stats.refresh.BlockListItem.Title
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

class InsightsMostPopularUseCase
@Inject constructor(private val insightsStore: InsightsStore) {
    suspend fun loadMostPopularInsights(site: SiteModel, refresh: Boolean, forced: Boolean): InsightsItem {
        return if (refresh) {
            val response = insightsStore.fetchMostPopularInsights(site, forced)
            val model = response.model
            val error = response.error

            if (error != null) {
                Failed(R.string.stats_insights_all_time_stats, error.message ?: error.type.name)
            } else {
                loadMostPopularInsightsItem(model)
            }
        } else {
            val model = insightsStore.getMostPopularInsights(site)
            loadMostPopularInsightsItem(model)
        }
    }

    private fun loadMostPopularInsightsItem(model: InsightsMostPopularModel?): ListInsightItem {
        val items = mutableListOf<BlockListItem>()
        items.add(Title(R.string.stats_insights_popular))
        if (model == null) {
            items.add(Empty)
        } else {
            items.add(
                    ListItem(
                            getDay(model.highestDayOfWeek),
                            "${model.highestDayPercent.roundToInt()}%",
                            true
                    )
            )
            items.add(
                    ListItem(
                            getHour(model.highestHour),
                            "${model.highestHourPercent.roundToInt()}%",
                            true
                    )
            )
        }
        return ListInsightItem(items)
    }

    private fun getDay(dayOfTheWeek: Int): String {
        val c = Calendar.getInstance()
        c.firstDayOfWeek = Calendar.MONDAY
        c.timeInMillis = System.currentTimeMillis()
        when (dayOfTheWeek) {
            0 -> c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            1 -> c.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
            2 -> c.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
            3 -> c.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY)
            4 -> c.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
            5 -> c.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            6 -> c.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }

        val formatter = SimpleDateFormat("EEEE", Locale.getDefault())
        return formatter.format(c.time)
    }

    private fun getHour(hour: Int): String {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, 0)

        val formatter = SimpleDateFormat("HH", Locale.getDefault())
        return formatter.format(c.time)
    }
}
