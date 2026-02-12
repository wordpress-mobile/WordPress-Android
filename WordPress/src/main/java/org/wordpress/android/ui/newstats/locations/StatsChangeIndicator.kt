package org.wordpress.android.ui.newstats.countries

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.wordpress.android.ui.newstats.StatsColors
import org.wordpress.android.ui.newstats.util.formatStatValue
import java.util.Locale

/**
 * A shared change indicator component that displays the change in views.
 * Shows positive changes in green and negative changes in red.
 * Does not render anything for NoChange.
 *
 * @param change The change value to display
 */
@Composable
fun StatsChangeIndicator(change: CountryViewChange) {
    val (text, color) = when (change) {
        is CountryViewChange.Positive -> Pair(
            "+${formatStatValue(change.value)} (${
                String.format(Locale.getDefault(), "%.1f%%", change.percentage)
            })",
            StatsColors.ChangeBadgePositive
        )
        is CountryViewChange.Negative -> Pair(
            "-${formatStatValue(change.value)} (${
                String.format(Locale.getDefault(), "%.1f%%", change.percentage)
            })",
            StatsColors.ChangeBadgeNegative
        )
        is CountryViewChange.NoChange -> return
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}
