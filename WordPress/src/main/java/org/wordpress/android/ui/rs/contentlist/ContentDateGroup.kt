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
 * ("Today", "Yesterday", "This week", "Earlier in July", "March 2025").
 *
 * Shared by the posts and comments rs screens - nothing here is post-specific. The pages list
 * draws no date headers at all: it sorts alphabetically by title, so date buckets would be
 * meaningless there.
 */
sealed interface ContentDateGroup {
    /** Stable identity for the header's LazyColumn key. */
    val key: String

    data object Today : ContentDateGroup {
        override val key = "today"
    }

    data object Yesterday : ContentDateGroup {
        override val key = "yesterday"
    }

    /** Within the last week, but older than yesterday. */
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
 * Anything dated in the future lands in [ContentDateGroup.Today] or [ContentDateGroup.ThisWeek]
 * alongside the most recent posts. Callers that list future-dated content - the Scheduled tab -
 * should turn grouping off rather than rely on that, since those headers read wrong over a list of
 * things yet to publish.
 */
object ContentDateGrouper {
    private val WEEK_MILLIS = TimeUnit.DAYS.toMillis(7)
    private val DAY_MILLIS = TimeUnit.DAYS.toMillis(1)

    fun groupOf(
        millis: Long,
        now: Long = System.currentTimeMillis(),
        locale: Locale = Locale.getDefault()
    ): ContentDateGroup {
        val then = Calendar.getInstance().apply { timeInMillis = millis }
        val today = Calendar.getInstance().apply { timeInMillis = now }
        val yesterday = Calendar.getInstance().apply { timeInMillis = now - DAY_MILLIS }
        val sameYear = then.get(Calendar.YEAR) == today.get(Calendar.YEAR)
        val sameMonth = sameYear && then.get(Calendar.MONTH) == today.get(Calendar.MONTH)

        return when {
            // Calendar days, not 24-hour windows: something posted at 23:00 last night is
            // "Yesterday" at 09:00 today, not "Today", which an elapsed-time test would say.
            then.isSameDayAs(today) -> ContentDateGroup.Today
            then.isSameDayAs(yesterday) -> ContentDateGroup.Yesterday
            millis >= now - WEEK_MILLIS -> ContentDateGroup.ThisWeek
            sameMonth -> ContentDateGroup.EarlierThisMonth(format(MONTH_PATTERN, millis, locale))
            sameYear -> ContentDateGroup.SpecificMonth(format(MONTH_PATTERN, millis, locale))
            else -> ContentDateGroup.SpecificMonth(format(MONTH_YEAR_PATTERN, millis, locale))
        }
    }

    private fun Calendar.isSameDayAs(other: Calendar) =
        get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
            get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)

    // Standalone month form ('L'), which is what a bare header needs in languages that inflect the
    // month differently when it appears next to a day number.
    private fun format(pattern: String, millis: Long, locale: Locale) =
        SimpleDateFormat(pattern, locale).format(Date(millis))

    private const val MONTH_PATTERN = "LLLL"
    private const val MONTH_YEAR_PATTERN = "LLLL yyyy"
}

@Composable
fun ContentDateGroup.label(): String = when (this) {
    is ContentDateGroup.Today -> stringResource(R.string.content_list_group_today)
    is ContentDateGroup.Yesterday -> stringResource(R.string.content_list_group_yesterday)
    is ContentDateGroup.ThisWeek -> stringResource(R.string.content_list_group_this_week)
    is ContentDateGroup.EarlierThisMonth ->
        stringResource(R.string.content_list_group_earlier_in, monthName)
    is ContentDateGroup.SpecificMonth -> label
}
