package org.wordpress.android.ui.newstats.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.newstats.StatsColors
import org.wordpress.android.ui.newstats.util.formatStatValue
import java.util.Locale
import kotlin.math.abs

/**
 * A reusable summary card component for stats detail screens.
 * Displays total views with optional change indicator for period comparison.
 *
 * @param totalViews The total views count to display
 * @param dateRange The date range string to display
 * @param totalViewsChange Optional change value compared to previous period (null to hide indicator)
 * @param totalViewsChangePercent Optional change percentage (required if totalViewsChange is provided)
 * @param modifier Modifier for the card
 */
@Composable
fun StatsSummaryCard(
    totalViews: Long,
    dateRange: String,
    modifier: Modifier = Modifier,
    totalViewsChange: Long? = null,
    totalViewsChangePercent: Double? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.stats_views),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dateRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatStatValue(totalViews),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                if (totalViewsChange != null && totalViewsChangePercent != null) {
                    ViewsChangeIndicator(
                        change = totalViewsChange,
                        changePercent = totalViewsChangePercent
                    )
                }
            }
        }
    }
}

@Composable
private fun ViewsChangeIndicator(
    change: Long,
    changePercent: Double
) {
    if (change == 0L) return

    val isPositive = change > 0
    val sign = if (isPositive) "+" else "-"
    val color = if (isPositive) StatsColors.ChangeBadgePositive else StatsColors.ChangeBadgeNegative
    val arrowIcon = if (isPositive) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = arrowIcon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Text(
            text = "$sign${formatStatValue(abs(change))} (${
                String.format(Locale.getDefault(), "%.1f%%", abs(changePercent))
            })",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsSummaryCardWithChangePreview() {
    AppThemeM3 {
        StatsSummaryCard(
            totalViews = 5400,
            dateRange = "Last 7 days",
            totalViewsChange = 69,
            totalViewsChangePercent = 1.3
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsSummaryCardWithoutChangePreview() {
    AppThemeM3 {
        StatsSummaryCard(
            totalViews = 5400,
            dateRange = "Last 7 days"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatsSummaryCardNegativeChangePreview() {
    AppThemeM3 {
        StatsSummaryCard(
            totalViews = 4200,
            dateRange = "Last 30 days",
            totalViewsChange = -150,
            totalViewsChangePercent = -3.4
        )
    }
}
