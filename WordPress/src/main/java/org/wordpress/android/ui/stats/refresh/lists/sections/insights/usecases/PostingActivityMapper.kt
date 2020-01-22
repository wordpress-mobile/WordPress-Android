package org.wordpress.android.ui.stats.refresh.lists.sections.insights.usecases

import org.wordpress.android.R
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
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Calendar
import javax.inject.Inject

private const val VERY_HIGH_LEVEL = 0.75
private const val HIGH_LEVEL = 0.5
private const val MEDIUM_LEVEL = 0.25
private const val LOW_LEVEL = 0

class PostingActivityMapper
@Inject constructor(
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val resourceProvider: ResourceProvider
) {
    fun buildActivityItem(months: List<Month>, max: Int): ActivityItem {
        val blocks = mutableListOf<Block>()
        val veryHighLimit = (max * VERY_HIGH_LEVEL).toInt()
        val highLimit = (max * HIGH_LEVEL).toInt()
        val mediumLimit = (max * MEDIUM_LEVEL).toInt()
        for (month in months) {
            val boxDaysForAccessibility = mutableListOf<Pair<Box, Int>>()
            val firstDayOfMonth = Calendar.getInstance(localeManagerWrapper.getLocale())
            firstDayOfMonth.set(month.year, month.month, firstDayOfMonth.getActualMinimum(Calendar.DAY_OF_MONTH))
            val getMonthDisplayName = { style: Int ->
                firstDayOfMonth.getDisplayName(
                        Calendar.MONTH,
                        style,
                        localeManagerWrapper.getLocale()
                )
            }
            val firstDayOfWeek = Calendar.getInstance(localeManagerWrapper.getLocale())
            firstDayOfWeek.time = firstDayOfMonth.time
            firstDayOfWeek.set(Calendar.DAY_OF_WEEK, firstDayOfWeek.firstDayOfWeek)
            val boxes = mutableListOf<Box>()
            while (firstDayOfWeek.before(firstDayOfMonth)) {
                boxes.add(INVISIBLE)
                firstDayOfWeek.add(Calendar.DAY_OF_MONTH, 1)
            }
            for (day in month.days) {
                val box = when {
                    day.value > veryHighLimit -> VERY_HIGH
                    day.value > highLimit -> HIGH
                    day.value > mediumLimit -> MEDIUM
                    day.value > LOW_LEVEL -> LOW
                    else -> VERY_LOW
                }
                boxes.add(box)
                boxDaysForAccessibility.add(Pair(box, day.key))
            }
            val monthDisplayName = getMonthDisplayName(Calendar.LONG)
            val labelContentDescription = resourceProvider.getString(
                    R.string.stats_posting_activity_label_content_description,
                    monthDisplayName
            )
            blocks.add(
                    Block(
                            getMonthDisplayName(Calendar.SHORT),
                            boxes,
                            labelContentDescription,
                            addBlockContentDescriptions(boxDaysForAccessibility, monthDisplayName)
                    )
            )
        }

        return ActivityItem(blocks)
    }

    private fun addBlockContentDescriptions(boxes: List<Pair<Box, Int>>, month: String): List<String> {
        val resolveBoxTypeStringId = { box: Box ->
            when (box) {
                MEDIUM -> R.string.stats_box_type_medium
                HIGH -> R.string.stats_box_type_high
                VERY_HIGH -> R.string.stats_box_type_very_high
                else -> R.string.stats_box_type_low
            }
        }

        val descriptions = mutableListOf<String>()
        boxes.filter { box -> box.first != INVISIBLE && box.first != VERY_LOW }
                .sortedByDescending { box -> box.first }
                .groupBy { box -> box.first }
                .forEach { entry ->
                    val readableBoxType = resourceProvider.getString(
                            resolveBoxTypeStringId(
                                    entry.key
                            )
                    )
                    val days = entry.value.map { box -> box.second }.joinToString(separator = ". ")
                    val activityDescription = resourceProvider.getString(
                            R.string.stats_posting_activity_content_description,
                            readableBoxType,
                            month,
                            days
                    )
                    descriptions.add(activityDescription)
                }

        return descriptions
    }
}
