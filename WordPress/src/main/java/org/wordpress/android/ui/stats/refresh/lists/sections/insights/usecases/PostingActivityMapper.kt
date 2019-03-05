package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import org.wordpress.android.fluxc.model.stats.insights.PostingActivityModel.Month
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Block
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.HIGH
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.INVISIBLE
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.LOW
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.MEDIUM
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.VERY_HIGH
import org.wordpress.android.ui.stats.refresh.lists.sections.BlockListItem.ActivityItem.Box.VERY_LOW
import org.wordpress.android.util.LocaleManagerWrapper
import java.util.Calendar
import javax.inject.Inject

private const val VERY_HIGH_LEVEL = 0.75
private const val HIGH_LEVEL = 0.5
private const val MEDIUM_LEVEL = 0.25
private const val LOW_LEVEL = 0

class PostingActivityMapper
@Inject constructor(private val localeManagerWrapper: LocaleManagerWrapper) {
    fun buildActivityItem(months: List<Month>, max: Int): ActivityItem {
        val blocks = mutableListOf<Block>()
        val veryHighLimit = (max * VERY_HIGH_LEVEL).toInt()
        val highLimit = (max * HIGH_LEVEL).toInt()
        val mediumLimit = (max * MEDIUM_LEVEL).toInt()
        for (month in months) {
            val firstDayOfMonth = Calendar.getInstance(localeManagerWrapper.getLocale())
            firstDayOfMonth.set(month.year, month.month, firstDayOfMonth.getActualMinimum(Calendar.DAY_OF_MONTH))
            val firstDayOfWeek = Calendar.getInstance(localeManagerWrapper.getLocale())
            firstDayOfWeek.time = firstDayOfMonth.time
            firstDayOfWeek.set(Calendar.DAY_OF_WEEK, firstDayOfWeek.firstDayOfWeek)
            val boxes = mutableListOf<Box>()
            while (firstDayOfWeek.before(firstDayOfMonth)) {
                boxes.add(INVISIBLE)
                firstDayOfWeek.add(Calendar.DAY_OF_MONTH, 1)
            }
            for (day in month.days) {
                boxes.add(
                        when {
                            day.value > veryHighLimit -> VERY_HIGH
                            day.value > highLimit -> HIGH
                            day.value > mediumLimit -> MEDIUM
                            day.value > LOW_LEVEL -> LOW
                            else -> VERY_LOW
                        }
                )
            }
            blocks.add(
                    Block(
                            firstDayOfMonth.getDisplayName(
                                    Calendar.MONTH,
                                    Calendar.SHORT,
                                    localeManagerWrapper.getLocale()
                            ),
                            boxes
                    )
            )
        }
        return ActivityItem(blocks)
    }
}
