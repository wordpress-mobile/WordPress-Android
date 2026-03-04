package org.wordpress.android.ui.newstats.components

import android.os.Parcelable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.ui.newstats.StatsColors
import org.wordpress.android.ui.newstats.util.formatStatValue
import java.util.Locale

/**
 * Represents the change in views compared to the previous period.
 * This is a shared sealed class used across different stats cards (Countries, Top Authors, etc.)
 */
sealed class StatsViewChange : Parcelable {
    @Parcelize
    data class Positive(val value: Long, val percentage: Double) : StatsViewChange()
    @Parcelize
    data class Negative(val value: Long, val percentage: Double) : StatsViewChange()
    @Parcelize
    data object NoChange : StatsViewChange()
}

/**
 * A shared change indicator component that displays the change in views.
 * Shows positive changes in green and negative changes in red.
 * Does not render anything for NoChange.
 *
 * @param change The change value to display
 */
@Composable
fun StatsChangeIndicator(change: StatsViewChange) {
    val (text, color) = when (change) {
        is StatsViewChange.Positive -> Pair(
            "+${formatStatValue(change.value)} (${
                String.format(Locale.getDefault(), "%.1f%%", change.percentage)
            })",
            StatsColors.ChangeBadgePositive
        )
        is StatsViewChange.Negative -> Pair(
            "-${formatStatValue(change.value)} (${
                String.format(Locale.getDefault(), "%.1f%%", change.percentage)
            })",
            StatsColors.ChangeBadgeNegative
        )
        is StatsViewChange.NoChange -> return
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}
