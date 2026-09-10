package org.wordpress.android.ui.rs.contentlist

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.wordpress.android.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Date bucket a row falls into, used to draw the group headers in the redesigned content list
 * ("This week", "Earlier in July", "March 2025").
 *
 * Shared by the posts and pages rs screens - nothing here is post-specific.
 */
sealed interface ContentDateGroup {
    /** Stable identity for the header's LazyColumn key. */
    val key: String

    data object ThisWeek : ContentDateGroup {
        override val key = "this_week"
    }

    /** Older than a week but still inside the current calendar month. */
    data class EarlierThisMonth(val monthName: String) : ContentDateGroup {
        override val key get() = "earlier_in_$monthName"
    }

    /** Any month before the current one. Carries its own fully formatted label. */
    data class SpecificMonth(val label: String) : ContentDateGroup {
        override val key get() = "month_$label"
    }
}

/**
 * Buckets timestamps into [ContentDateGroup]s.
 *
 * Anything dated in the future lands in [ContentDateGroup.ThisWeek] alongside the most recent
 * posts. Callers that list future-dated content - the Scheduled tab - should turn grouping off
 * rather than rely on that, since "This week" reads wrong over a list of things yet to publish.
 */
object ContentDateGrouper {
    private val WEEK_MILLIS = TimeUnit.DAYS.toMillis(7)

    fun groupOf(
        millis: Long,
        now: Long = System.currentTimeMillis(),
        locale: Locale = Locale.getDefault()
    ): ContentDateGroup {
        if (millis >= now - WEEK_MILLIS) return ContentDateGroup.ThisWeek

        val then = Calendar.getInstance().apply { timeInMillis = millis }
        val today = Calendar.getInstance().apply { timeInMillis = now }
        val sameYear = then.get(Calendar.YEAR) == today.get(Calendar.YEAR)

        if (sameYear && then.get(Calendar.MONTH) == today.get(Calendar.MONTH)) {
            return ContentDateGroup.EarlierThisMonth(format(MONTH_PATTERN, millis, locale))
        }
        val pattern = if (sameYear) MONTH_PATTERN else MONTH_YEAR_PATTERN
        return ContentDateGroup.SpecificMonth(format(pattern, millis, locale))
    }

    // Standalone month form ('L'), which is what a bare header needs in languages that inflect the
    // month differently when it appears next to a day number.
    private fun format(pattern: String, millis: Long, locale: Locale) =
        SimpleDateFormat(pattern, locale).format(Date(millis))

    private const val MONTH_PATTERN = "LLLL"
    private const val MONTH_YEAR_PATTERN = "LLLL yyyy"
}

@Composable
fun ContentDateGroup.label(): String = when (this) {
    is ContentDateGroup.ThisWeek -> stringResource(R.string.content_list_group_this_week)
    is ContentDateGroup.EarlierThisMonth ->
        stringResource(R.string.content_list_group_earlier_in, monthName)
    is ContentDateGroup.SpecificMonth -> label
}
